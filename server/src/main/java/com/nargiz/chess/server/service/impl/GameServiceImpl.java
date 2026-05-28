package com.nargiz.chess.server.service.impl;

import com.nargiz.chess.server.events.MemberDisconnectedEvent;
import com.nargiz.chess.server.exceptions.ServiceException;
import com.nargiz.chess.shared.command.response.ErrorResponse;
import com.nargiz.chess.server.network.TCPServer;
import com.nargiz.chess.server.repository.ServerRepository;
import com.nargiz.chess.server.service.GameService;
import com.nargiz.chess.shared.command.ActionCommand;
import com.nargiz.chess.shared.command.ChessCommand;
import com.nargiz.chess.shared.command.UpdateGameState;
import com.nargiz.chess.shared.command.UpdateMembers;
import com.nargiz.chess.shared.command.response.StartGameResponse;
import com.nargiz.chess.shared.events.ApplicationEventBus;
import com.nargiz.chess.shared.ioc.anotation.Component;
import com.nargiz.chess.shared.ioc.anotation.Inject;
import com.nargiz.chess.shared.ioc.anotation.PostConstruct;
import com.nargiz.chess.shared.models.*;
import com.nargiz.chess.shared.models.actions.KillFigure;
import com.nargiz.chess.shared.models.actions.TransformFigure;
import com.nargiz.chess.shared.models.enums.ActionType;
import com.nargiz.chess.shared.models.enums.ColorType;
import com.nargiz.chess.shared.models.enums.FigureType;
import com.nargiz.chess.shared.models.enums.GameState;
import com.nargiz.chess.shared.models.figures.KingData;
import java.util.*;
import java.util.stream.Collectors;

import static com.nargiz.chess.shared.models.enums.ColorType.WHITE;
import static com.nargiz.chess.shared.models.enums.GameState.DRAW;
import static com.nargiz.chess.shared.models.enums.GameState.PLAYING;

@Component
public class GameServiceImpl implements GameService {

    @Inject
    ApplicationEventBus eventBus;

    @Inject
    ServerRepository serverRepository;

    @Inject
    TCPServer server;

    @Override
    public void createLobby(UUID hostId, String name, String hostName, int memberCount) {
        serverRepository.createLobby(hostId, name, hostName, memberCount);
    }

    @Override
    public void connect(UUID hostId, UUID guestId, String guestName) {
        serverRepository.addMember(hostId, guestId, guestName);

        Set<MemberData> members = serverRepository.getMemberList(hostId);
        broadcastLobby(hostId, new UpdateMembers(members));
    }
    @Override
    public void notifyDisconnect(UUID disconnectedUserId, String message) {
        LobbyData lobby = serverRepository.getLobbyByMember(disconnectedUserId);
        if (lobby == null) return;

        Set<UUID> otherPlayers = new HashSet<>(lobby.getMembers().keySet());
        otherPlayers.add(lobby.getHost().getId());
        otherPlayers.remove(disconnectedUserId);

        server.broadcast(otherPlayers, new ErrorResponse(message));
    }

    @Override
    public void removeMember(UUID memberId) {
        LobbyData lobby = serverRepository.getLobbyByMember(memberId);

        serverRepository.removeMember(memberId);

        Set<MemberData> members = new HashSet<>(lobby.getMembers().values());
        broadcastLobby(memberId, new UpdateMembers(members));
    }

    @Override
    public GameData startGame(UUID userId) {
        GameData gameData = serverRepository.createGame(userId);
        broadcastLobby(userId,
            StartGameResponse.builder()
                    .whiteUserId(gameData.getWhite().getId())
                    .blackUserId(gameData.getBlack().getId())
                    .whitePlayerName(gameData.getWhite().getName())
                .blackPlayerName(gameData.getBlack().getName())
                .figures(gameData.getBoard().values())
                .build()
        );

        return gameData;
    }

    @Override
    public String getHostName(UUID hostId) {
        return serverRepository.getHost(hostId).getName();
    }

    @Override
    public boolean isHost(UUID userId) {
        MemberData host = serverRepository.getHost(userId);
        return host != null && host.getId().equals(userId);
    }

    @Override
    public void performGameAction(ActionCommand command) {
        GameData gameData = serverRepository.getGame(command.getUserId());
        if (gameData == null) {
            throw new ServiceException("Game is not found");
        }
        synchronized (gameData) {
            try {
                validateCommand(command, gameData);

                FigureData actionFigure = gameData.getFigureAt(command.getFromPosition());
                if (actionFigure == null || actionFigure.isDead() || !actionFigure.getColor().equals(gameData.getCurrentColor())) {
                    throw new ServiceException("Wrong position");
                }

                ActionResult result = actionFigure.validateAction(gameData, command);
                if (!result.getErrors().isEmpty()) {
                    throw new ServiceException(result.getErrors().stream().map(ValidationError::getMessage).collect(Collectors.joining("\n")));
                }

                result.getUpdates().forEach(action -> action.doAction(gameData));

                boolean hasCopy = gameData.getBoardHistory().stream().anyMatch(g -> g.hasCopy(gameData.getBoard()));

                if (!hasCopy) {
                    gameData.getBoardHistory().add(gameData.getBoard().clone());
                }

                HistoryData historyData = createHistory(gameData, command, actionFigure, result);

                if (historyData.getAction().equals(ActionType.ATTACK)) {
                    gameData.getBoardHistory().clear();
                }

                gameData.setState(PLAYING);
                if (historyData.isMate()) {
                    gameData.setState(WHITE.equals(gameData.getCurrentColor()) ? GameState.WHITE_WINS : GameState.BLACK_WINS);
                } else if (isDraw(gameData, historyData)) {
                    gameData.setState(DRAW);
                }

                gameData.getHistory().add(historyData);

                broadcastLobby(command.getUserId(),
                        UpdateGameState.builder()
                                .figures(gameData.getBoard().values())
                                .historyData(gameData.getHistory())
                                .state(gameData.getState())
                                .build()
                );
            } catch (Exception e) {
                restoreBoard(command.getUserId(), gameData);
                throw e;
            }
        }
    }

    private boolean isDraw(GameData gameData, HistoryData historyData) {
        boolean isOpponentLocked = gameData.getBoard().values().stream()
                .filter(f -> f.getColor().equals(historyData.getColor().getOpponentColor()))
                .allMatch(f -> f.isLocked(gameData));
        if (!historyData.isCheck() && !historyData.isMate() && isOpponentLocked) {
            return true;
        }

        boolean isLast30ActionsDraw = gameData.getHistory().size() >= 30 && gameData.getHistory().reversed().subList(0, 30).stream()
                .noneMatch(h ->
                        h.getAction().equals(ActionType.ATTACK) || h.getFigure().equals(FigureType.PAWN)
                );

        if (isLast30ActionsDraw) {
            return true;
        }

        boolean hasSame3States = gameData.getBoardHistory().stream().anyMatch(b -> b.getCopyCount() == 3);

        if (hasSame3States) {
            return true;
        }

        return false;

    }

    private HistoryData createHistory(GameData gameData, ActionCommand command, FigureData actionFigure, ActionResult result) {
        HistoryData historyData = HistoryData.builder()
                .color(actionFigure.getColor())
                .figure(actionFigure.getFigureType())
                .from(command.getFromPosition())
                .to(command.getToPosition())
                .action(ActionType.MOVE)
                .build();

        if (FigureType.KING.equals(actionFigure.getFigureType())) {
            historyData.setShortCastling(command.getToPosition().getColumn() - command.getFromPosition().getColumn() > 1);
            historyData.setLongCastling(command.getToPosition().getColumn() - command.getFromPosition().getColumn() < -1);
        }

        KillFigure killFigureAction = result.getUpdates().stream()
                .filter(action -> action instanceof KillFigure)
                .map(action -> (KillFigure) action)
                .findFirst()
                .orElse(null);

        if (killFigureAction != null) {
            historyData.setAction(ActionType.ATTACK);
            historyData.setEnPassant(killFigureAction.isEnPassant());
        }

        TransformFigure transformFigure = result.getUpdates().stream()
                .filter(action -> action instanceof TransformFigure)
                .map(action -> (TransformFigure) action)
                .findFirst()
                .orElse(null);

        if (transformFigure != null) {
            historyData.setTransform(transformFigure.getFigureType());
        }

        MemberData opponent = gameData.getOpponentPlayer();
        KingData opponentKing = opponent.getKing();
        boolean isCheck = gameData.getBoard().values().stream()
                .filter(FigureData::isAlive)
                .filter(f -> f.isOpponentColor(opponentKing))
                .anyMatch(f -> f.canAttack(gameData, opponentKing.getPosition(), opponentKing, opponentKing.getPosition()));
        historyData.setCheck(isCheck);

        if (isCheck) {
            boolean isAttackerUnderAttack = gameData.getBoard().values().stream()
                    .filter(f -> f.isOpponentColor(actionFigure))
                    .anyMatch(f -> f.canAttack(gameData, actionFigure.getPosition(), actionFigure, actionFigure.getPosition()));

            boolean canBeBlocked = canBeBlocked(gameData, opponentKing.getColor(), actionFigure.getPosition(), opponentKing.getPosition());
            historyData.setMate(!isAttackerUnderAttack && !canBeBlocked && opponentKing.isLocked(gameData));
        }

        return historyData;
    }

    private boolean canBeBlocked(GameData gameData, ColorType color, CellPosition from, CellPosition to) {
        CellPosition pathPosition = from;
        while(!pathPosition.equals(to)) {
            int dRow = 0;
            if (to.getRow() > pathPosition.getRow()) {
                dRow = 1;
            } else if (to.getRow() < pathPosition.getRow()) {
                dRow = -1;
            }
            int dColumn = 0;
            if (to.getColumn() > pathPosition.getColumn()) {
                dColumn = 1;
            } else if (to.getColumn() < pathPosition.getColumn()) {
                dColumn = -1;
            }
            pathPosition = pathPosition.shift(dRow, dColumn);

            if (!pathPosition.equals(to) && canBeBlocked(gameData, color, pathPosition)) {
                return true;
            }
        }
        return false;
    }

    private boolean canBeBlocked(GameData gameData, ColorType color, CellPosition pathPosition) {
        return gameData.getBoard().values().stream()
                .filter(f -> f.getColor().equals(color))
                .anyMatch(f -> f.canMove(gameData, pathPosition));
    }

    private static void validateCommand(ActionCommand command, GameData gameData) {
        if (!GameState.PLAYING.equals(gameData.getState())) {
            throw new ServiceException("Wrong game state: " + gameData.getState());
        }

        if (
                !gameData.getWhite().getId().equals(command.getUserId())
                        && !gameData.getBlack().getId().equals(command.getUserId())
        ) {
            throw new ServiceException("Access denied. Wrong player");
        }

        if (!gameData.getCurrentPlayer().getId().equals(command.getUserId())) {
            throw new ServiceException("Wrong turn");
        }

        if (FigureData.isOutOfBoard(command.getFromPosition())) {
            throw new ServiceException("Wrong starting position");
        }

        if (FigureData.isOutOfBoard(command.getToPosition())) {
            throw new ServiceException("Wrong end position");
        }
    }

    private void removeMember(MemberDisconnectedEvent memberDisconnectedEvent) {
        removeMember(memberDisconnectedEvent.getMemberId());
    }

    private void restoreBoard(UUID memberId, GameData gameData) {
        server.broadcast(Set.of(memberId),
                UpdateGameState.builder()
                        .figures(gameData.getBoard().values())
                        .historyData(gameData.getHistory())
                        .state(gameData.getState())
                        .build()
        );
    }

    private void broadcastLobby(UUID memberId, ChessCommand command) {
        LobbyData lobby = serverRepository.getLobbyByMember(memberId);
        Set<UUID> members = new HashSet<>(lobby.getMembers().keySet());
        members.add(lobby.getHost().getId());
        server.broadcast(members, command);
    }

    @PostConstruct
    private void init() {
        eventBus.subscribeOn(
                MemberDisconnectedEvent.class,
                this::removeMember
        );
    }

}

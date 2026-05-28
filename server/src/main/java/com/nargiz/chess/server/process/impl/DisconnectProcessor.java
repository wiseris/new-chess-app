package com.nargiz.chess.server.process.impl;

import com.nargiz.chess.server.process.ServerCommandProcessor;
import com.nargiz.chess.server.service.GameService;
import com.nargiz.chess.shared.command.Disconnect;
import com.nargiz.chess.shared.command.response.ChessCommandResponse;
import com.nargiz.chess.shared.ioc.anotation.Component;
import com.nargiz.chess.shared.ioc.anotation.Inject;

@Component
public class DisconnectProcessor implements ServerCommandProcessor<Disconnect, ChessCommandResponse> {

    @Inject
    private GameService gameService;

    @Override
    public ChessCommandResponse processCommand(Disconnect command) {
        String message = command.isGraceful()
                ? "Player " + command.getUserId() + " left gracefully (FIN)"
                : "Player " + command.getUserId() + " disconnected abruptly (RST)";

        System.out.println(message);

        String reason = command.isGraceful()
                ? (command.getReason() != null ? command.getReason() : "Player left the game")
                : "Connection reset (RST)";

        gameService.removeMember(command.getUserId());

        return null;
    }

    @Override
    public Class<Disconnect> getCommandClass() {
        return Disconnect.class;
    }
}
package com.nargiz.chess.shared.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nargiz.chess.shared.command.ActionCommand;
import com.nargiz.chess.shared.models.enums.ColorType;
import com.nargiz.chess.shared.models.enums.FigureType;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class FigureData {
    @Builder.Default
    private UUID id = UUID.randomUUID();
    private CellPosition position;
    private ColorType color;
    private boolean moved;
    private boolean dead;
    private FigureType figureType;

    public ActionResult validateAction(GameData gameData, ActionCommand action) {
        return null;
    }

    public boolean canAttack(GameData gameData, CellPosition targetPosition, FigureData exclude, CellPosition replacedPosition) {
        return false;
    };

    public boolean isLocked(GameData gameData) {
        return false;
    };

    public boolean isUnderDefense(GameData gameData) {
        return gameData.getBoard().values().stream()
                .filter(this::isSameColor)
                .anyMatch(f -> f.canAttack(gameData, getPosition(), f, getPosition()));
    }

    public boolean canMove(GameData gameData, CellPosition targetPosition) {
        return true;
    }

    public static boolean isOutOfBoard(CellPosition targetPosition) {
        return targetPosition.getColumn() > 8
                || targetPosition.getColumn() < 1
                || targetPosition.getRow() > 8
                || targetPosition.getRow() < 1;
    }

    public FigureData at(int row, int column) {
        setPosition(new CellPosition(row, column));
        return this;
    }

    public boolean isAlive() {
        return !dead;
    }

    public boolean isSameColor(FigureData other) {
        return this.color.equals(other.color);
    }

    public boolean isSameType(FigureData other) {
        return this.figureType.equals(other.figureType);
    }

    public boolean isOpponentColor(FigureData other) {
        return !isSameColor(other);
    }

    public boolean isSame(FigureData f) {
        return isSameColor(f) && isSameType(f);
    }
}

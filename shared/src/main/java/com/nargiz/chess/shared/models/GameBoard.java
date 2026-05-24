package com.nargiz.chess.shared.models;

import java.util.HashMap;
import java.util.Map;

public class GameBoard extends HashMap<CellPosition, FigureData> {

    int copyCount;

    public GameBoard() {
    }

    public GameBoard(Map<? extends CellPosition, ? extends FigureData> m) {
        super(m);
    }

    public GameBoard clone() {
        return new GameBoard(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof GameBoard otherGameBoard) {
            if (!this.keySet().equals(otherGameBoard.keySet())) {
                return false;
            }

            return values().stream().allMatch(f -> {
                FigureData otherFigure = otherGameBoard.get(f.getPosition());
                return otherFigure.isSame(f);
            });

        }
        return false;
    }

    public boolean hasCopy(GameBoard otherGame) {
        boolean isEqual = equals(otherGame);
        if (isEqual) {
            incrementCopy();
        }
        return isEqual;
    }

    public void incrementCopy() {
        copyCount++;
    }

    public int getCopyCount() {
        return copyCount;
    }
}

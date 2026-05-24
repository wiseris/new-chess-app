package com.nargiz.chess.shared.models.enums;

public enum ColorType {
    WHITE, BLACK;

    public ColorType getOpponentColor() {
        if (this.equals(WHITE)) {
            return BLACK;
        }
        return WHITE;
    }
}

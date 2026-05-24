package com.nargiz.chess.shared.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.lang.reflect.Array;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class CellPosition {
    private static final char[] COLUMN_SYMBOLS = {'a','b','c','d','e','f','g','h'};

    private int row;
    private int column;

    public CellPosition shift(int drow, int dcolumn) {
        return new CellPosition(row + drow, column + dcolumn);
    }

    public boolean isColumnBetween(CellPosition a, CellPosition b) {
        return Math.min(a.column, b.column) < column
                && Math.max(a.column, b.column) > column;
    }

    public boolean isRowBetween(CellPosition a, CellPosition b) {
        return Math.min(a.row, b.row) < row
                && Math.max(a.row, b.row) > row;
    }

    public boolean isLineBetween(CellPosition a, CellPosition b) {
        return isSameRow(a, b) && isColumnBetween(a, b)
                || isSameColumn(a, b) && isRowBetween(a, b);
    }

    public boolean isDiagonalBetween(CellPosition a, CellPosition b) {
        if (!isSameDiagonal(a, b)) {
            // a and b is not on same diagonal
            return false;
        }

        return isSameDiagonal(a, this)
                && isColumnBetween(a,b)
                && isRowBetween(a,b);
    }

    public static boolean isSameDiagonal(CellPosition a, CellPosition b) {
        return Math.abs(a.row - b.row) == Math.abs(a.column - b.column);
    }

    public boolean isSameRow(CellPosition b) {
        return b != null && row == b.row;
    }

    public boolean isSameRow(CellPosition a, CellPosition b) {
        return isSameRow(a) && isSameRow(b);
    }

    public boolean isSameColumn(CellPosition b) {
        return b != null && column == b.column;
    }

    public boolean isSameColumn(CellPosition a, CellPosition b) {
        return isSameColumn(a) && isSameColumn(b);
    }

    public boolean isSameDiagonal(CellPosition b) {
        return isSameDiagonal(this, b);
    }

    public static boolean isOutOfBoard(int row, int column) {
        return column > 8
                || column < 1
                || row > 8
                || row < 1;
    }

    public char getColumnSymbol() {
        return COLUMN_SYMBOLS[column - 1];
    }

    public String notation() {
        return "%s%s".formatted(getColumnSymbol(), getRow());
    }

    public boolean isOutOfBoard() {
        return isOutOfBoard(row, column);
    }

}

package com.mati.huffman.ui;

import java.util.Locale;

/**
 * Formats Unicode code points for presentation without changing their value.
 */
public final class SymbolFormatter {

    private static final int MIN_CODE_POINT = 0;
    private static final int MAX_CODE_POINT = 0x10FFFF;

    private SymbolFormatter() {
    }

    public static String displaySymbol(int codePoint) {
        validateCodePoint(codePoint);
        return switch (codePoint) {
            case 0x20 -> "SPACE";
            case 0x09 -> "TAB";
            case 0x0A -> "LF";
            case 0x0D -> "CR";
            default -> new String(Character.toChars(codePoint));
        };
    }

    public static String unicodeLabel(int codePoint) {
        validateCodePoint(codePoint);
        return String.format(Locale.ROOT, "U+%04X", codePoint);
    }

    private static void validateCodePoint(int codePoint) {
        if (codePoint < MIN_CODE_POINT || codePoint > MAX_CODE_POINT) {
            throw new IllegalArgumentException(
                    "Symbol must be a valid Unicode code point"
            );
        }
    }
}

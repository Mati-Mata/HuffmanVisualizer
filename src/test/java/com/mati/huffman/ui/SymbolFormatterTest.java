package com.mati.huffman.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SymbolFormatterTest {

    @Test
    void formatsSpace() {
        assertEquals("SPACE", SymbolFormatter.displaySymbol(0x20));
    }

    @Test
    void formatsTab() {
        assertEquals("TAB", SymbolFormatter.displaySymbol(0x09));
    }

    @Test
    void formatsLineFeed() {
        assertEquals("LF", SymbolFormatter.displaySymbol(0x0A));
    }

    @Test
    void formatsCarriageReturn() {
        assertEquals("CR", SymbolFormatter.displaySymbol(0x0D));
    }

    @Test
    void formatsRegularUnicodeSymbol() {
        assertEquals("ñ", SymbolFormatter.displaySymbol(0x00F1));
        assertEquals("U+00F1", SymbolFormatter.unicodeLabel(0x00F1));
    }

    @Test
    void formatsSupplementaryEmoji() {
        int smilingFace = "🙂".codePointAt(0);

        assertEquals("🙂", SymbolFormatter.displaySymbol(smilingFace));
        assertEquals("U+1F642", SymbolFormatter.unicodeLabel(smilingFace));
    }

    @Test
    void rejectsInvalidCodePoint() {
        assertThrows(
                IllegalArgumentException.class,
                () -> SymbolFormatter.displaySymbol(-1)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> SymbolFormatter.unicodeLabel(0x110000)
        );
    }
}

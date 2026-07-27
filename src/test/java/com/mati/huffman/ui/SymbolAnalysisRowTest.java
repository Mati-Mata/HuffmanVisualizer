package com.mati.huffman.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SymbolAnalysisRowTest {

    @Test
    void preservesRawProbabilityAndCalculatesCodeLength() {
        double probability = 1.0 / 3.0;

        SymbolAnalysisRow row = new SymbolAnalysisRow(
                0x41,
                "A",
                "U+0041",
                2,
                probability,
                "101"
        );

        assertEquals(probability, row.probability());
        assertEquals(3, row.codeLength());
    }

    @Test
    void acceptsSupplementaryCodePoint() {
        int smilingFace = "🙂".codePointAt(0);

        SymbolAnalysisRow row = new SymbolAnalysisRow(
                smilingFace,
                "🙂",
                "U+1F642",
                1,
                1.0,
                "0"
        );

        assertEquals(smilingFace, row.codePoint());
    }

    @Test
    void rejectsInvalidValues() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new SymbolAnalysisRow(
                        0x41, "A", "U+0041", 0, 1.0, "0"
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new SymbolAnalysisRow(
                        0x41, "A", "U+0041", 1, 0.0, "0"
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new SymbolAnalysisRow(
                        0x41, "A", "U+0041", 1, 1.0, ""
                )
        );
    }
}

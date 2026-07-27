package com.mati.huffman.service;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrequencyAnalyzerTest {

    private final FrequencyAnalyzer analyzer = new FrequencyAnalyzer();

    @Test
    void countsBanana() {
        Map<Integer, Long> frequencies = analyzer.analyze("BANANA");

        assertEquals(3L, frequencies.get(codePointOf("A")));
        assertEquals(2L, frequencies.get(codePointOf("N")));
        assertEquals(1L, frequencies.get(codePointOf("B")));
        assertEquals(3, frequencies.size());
    }

    @Test
    void returnsEmptyMapForEmptyText() {
        assertTrue(analyzer.analyze("").isEmpty());
    }

    @Test
    void rejectsNullText() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> analyzer.analyze(null)
        );

        assertEquals("Text must not be null", exception.getMessage());
    }

    @Test
    void countsSingleSymbol() {
        assertEquals(
                Map.of(codePointOf("Z"), 1L),
                analyzer.analyze("Z")
        );
    }

    @Test
    void countsRepeatedSymbol() {
        assertEquals(
                Map.of(codePointOf("x"), 5L),
                analyzer.analyze("xxxxx")
        );
    }

    @Test
    void countsAccentedTextWithoutNormalization() {
        Map<Integer, Long> frequencies = analyzer.analyze("áéñ");

        assertEquals(1L, frequencies.get(codePointOf("á")));
        assertEquals(1L, frequencies.get(codePointOf("é")));
        assertEquals(1L, frequencies.get(codePointOf("ñ")));
        assertEquals(3, frequencies.size());
    }

    @Test
    void countsSupplementaryCodePointsAsSingleSymbols() {
        int smilingFace = "🙂".codePointAt(0);
        int rocket = "🚀".codePointAt(0);

        Map<Integer, Long> frequencies = analyzer.analyze("🙂🙂🚀");

        assertEquals(2L, frequencies.get(smilingFace));
        assertEquals(1L, frequencies.get(rocket));
        assertEquals(2, frequencies.size());
    }

    @Test
    void countsSpacesWithoutTrimming() {
        assertEquals(
                Map.of(codePointOf(" "), 3L),
                analyzer.analyze("   ")
        );
    }

    @Test
    void countsTabs() {
        assertEquals(
                Map.of(codePointOf("\t"), 2L),
                analyzer.analyze("\t\t")
        );
    }

    @Test
    void countsCarriageReturnsAndLineFeedsSeparately() {
        Map<Integer, Long> frequencies = analyzer.analyze("\n\r\n");

        assertEquals(2L, frequencies.get(codePointOf("\n")));
        assertEquals(1L, frequencies.get(codePointOf("\r")));
    }

    @Test
    void distinguishesUppercaseAndLowercase() {
        Map<Integer, Long> frequencies = analyzer.analyze("AaA");

        assertEquals(2L, frequencies.get(codePointOf("A")));
        assertEquals(1L, frequencies.get(codePointOf("a")));
        assertEquals(2, frequencies.size());
    }

    @Test
    void countsMixedUnicodeText() {
        int smilingFace = "🙂".codePointAt(0);
        Map<Integer, Long> frequencies = analyzer.analyze("A🙂A\nñ🙂");

        assertEquals(2L, frequencies.get(codePointOf("A")));
        assertEquals(2L, frequencies.get(smilingFace));
        assertEquals(1L, frequencies.get(codePointOf("\n")));
        assertEquals(1L, frequencies.get(codePointOf("ñ")));
        assertEquals(4, frequencies.size());
    }

    @Test
    void ordersKeysByAscendingCodePoint() {
        Map<Integer, Long> frequencies = analyzer.analyze("ñ🙂aA\n");

        List<Integer> keys = new ArrayList<>(frequencies.keySet());
        List<Integer> expected = keys.stream().sorted().toList();

        assertEquals(expected, keys);
    }

    @Test
    void returnsUnmodifiableResult() {
        Map<Integer, Long> frequencies = analyzer.analyze("AB");

        assertThrows(
                UnsupportedOperationException.class,
                () -> frequencies.put(codePointOf("C"), 1L)
        );
        assertThrows(
                UnsupportedOperationException.class,
                frequencies::clear
        );
    }

    private static int codePointOf(String symbol) {
        return symbol.codePointAt(0);
    }
}

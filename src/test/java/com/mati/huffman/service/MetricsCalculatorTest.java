package com.mati.huffman.service;

import com.mati.huffman.model.CompressionMetrics;
import com.mati.huffman.model.HuffmanNode;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetricsCalculatorTest {

    private static final double TOLERANCE = 1e-9;

    private final MetricsCalculator calculator = new MetricsCalculator();

    @Test
    void calculatesBananaMetrics() {
        Analysis analysis = analyze("BANANA");

        assertEquals(6L, analysis.metrics().totalSymbolCount());
        assertEquals(3L, analysis.metrics().distinctSymbolCount());
        assertEquals(6L, analysis.metrics().originalUtf8ByteSize());
        assertEquals(48L, analysis.metrics().originalUtf8BitSize());
        assertEquals(9L, analysis.metrics().huffmanMessageBitSize());
    }

    @Test
    void calculatesSingleSymbolMetrics() {
        Analysis analysis = analyze("A");

        assertEquals(1L, analysis.metrics().totalSymbolCount());
        assertEquals(1L, analysis.metrics().huffmanMessageBitSize());
        assertEquals(1.0, analysis.metrics().averageCodeLength(), TOLERANCE);
    }

    @Test
    void calculatesRepeatedSymbolMetrics() {
        Analysis analysis = analyze("AAAAAA");

        assertEquals(6L, analysis.metrics().totalSymbolCount());
        assertEquals(6L, analysis.metrics().huffmanMessageBitSize());
        assertEquals(1.0, analysis.metrics().averageCodeLength(), TOLERANCE);
    }

    @Test
    void calculatesAsciiMetrics() {
        assertCoherent("ordinary ASCII text");
    }

    @Test
    void calculatesMetricsWithSpaces() {
        assertCoherent("A B  C ");
    }

    @Test
    void calculatesMetricsWithLineBreaks() {
        assertCoherent("A\nB\r\nC");
    }

    @Test
    void calculatesSpanishMetrics() {
        assertCoherent("El pingüino comió piña y tomó café, señor.");
    }

    @Test
    void calculatesChineseMetrics() {
        assertCoherent("数据压缩示例");
    }

    @Test
    void calculatesEmojiMetrics() {
        assertCoherent("🙂🙂🚀");
    }

    @Test
    void calculatesMixedUnicodeMetrics() {
        assertCoherent("A🙂A\nñ数据🙂");
    }

    @Test
    void calculatesCorrectProbabilities() {
        Analysis analysis = analyze("BANANA");

        assertEquals(
                3.0 / 6.0,
                analysis.probabilities().get(codePointOf("A")),
                TOLERANCE
        );
        assertEquals(
                2.0 / 6.0,
                analysis.probabilities().get(codePointOf("N")),
                TOLERANCE
        );
        assertEquals(
                1.0 / 6.0,
                analysis.probabilities().get(codePointOf("B")),
                TOLERANCE
        );
    }

    @Test
    void probabilitiesSumApproximatelyToOne() {
        double sum = analyze("A🙂A\nñ数据🙂")
                .probabilities()
                .values()
                .stream()
                .mapToDouble(Double::doubleValue)
                .sum();

        assertEquals(1.0, sum, TOLERANCE);
    }

    @Test
    void calculatesUtf8Size() {
        String text = "A🙂ñ数据";
        Analysis analysis = analyze(text);

        assertEquals(
                text.getBytes(StandardCharsets.UTF_8).length,
                analysis.metrics().originalUtf8ByteSize()
        );
        assertEquals(
                text.getBytes(StandardCharsets.UTF_8).length * 8L,
                analysis.metrics().originalUtf8BitSize()
        );
    }

    @Test
    void huffmanSizeMatchesEncodedLength() {
        Analysis analysis = analyze("BANANA🙂数据");

        assertEquals(
                analysis.encodedBits().length(),
                analysis.metrics().huffmanMessageBitSize()
        );
    }

    @Test
    void calculatesWeightedAverageCodeLength() {
        CompressionMetrics metrics = analyze("BANANA").metrics();

        assertEquals(1.5, metrics.averageCodeLength(), TOLERANCE);
    }

    @Test
    void calculatesTheoreticalSavingPercentage() {
        CompressionMetrics metrics = analyze("BANANA").metrics();

        assertEquals(81.25, metrics.theoreticalSavingPercentage(), TOLERANCE);
    }

    @Test
    void allowsNegativeSavingPercentage() {
        int symbol = codePointOf("A");
        CompressionMetrics metrics = calculator.calculate(
                "A",
                Map.of(symbol, 1L),
                Map.of(symbol, "000000000")
        );

        assertTrue(metrics.theoreticalSavingPercentage() < 0.0);
        assertEquals(-12.5, metrics.theoreticalSavingPercentage(), TOLERANCE);
    }

    @Test
    void rejectsNullText() {
        assertThrows(
                NullPointerException.class,
                () -> calculator.calculate(null, Map.of(), Map.of())
        );
    }

    @Test
    void returnsZeroMetricsForEmptyText() {
        CompressionMetrics metrics = calculator.calculate(
                "",
                Map.of(),
                Map.of()
        );

        assertEquals(0L, metrics.totalSymbolCount());
        assertEquals(0L, metrics.originalUtf8BitSize());
        assertEquals(0L, metrics.huffmanMessageBitSize());
        assertEquals(0.0, metrics.averageCodeLength(), TOLERANCE);
        assertEquals(0.0, metrics.theoreticalSavingPercentage(), TOLERANCE);
        assertTrue(calculator.calculateProbabilities(Map.of()).isEmpty());
    }

    @Test
    void rejectsNullFrequencyMap() {
        assertThrows(
                NullPointerException.class,
                () -> calculator.calculate("A", null, Map.of())
        );
    }

    @Test
    void rejectsNullCodeMap() {
        assertThrows(
                NullPointerException.class,
                () -> calculator.calculate("A", Map.of(0x41, 1L), null)
        );
    }

    @Test
    void rejectsMissingCode() {
        assertThrows(
                IllegalArgumentException.class,
                () -> calculator.calculate(
                        "AB",
                        Map.of(0x41, 1L, 0x42, 1L),
                        Map.of(0x41, "0")
                )
        );
    }

    @Test
    void rejectsNullCode() {
        Map<Integer, String> codes = new HashMap<>();
        codes.put(0x41, null);

        assertThrows(
                IllegalArgumentException.class,
                () -> calculator.calculate(
                        "A",
                        Map.of(0x41, 1L),
                        codes
                )
        );
    }

    @Test
    void rejectsEmptyCode() {
        assertThrows(
                IllegalArgumentException.class,
                () -> calculator.calculate(
                        "A",
                        Map.of(0x41, 1L),
                        Map.of(0x41, "")
                )
        );
    }

    @Test
    void rejectsNonBinaryCode() {
        assertThrows(
                IllegalArgumentException.class,
                () -> calculator.calculate(
                        "A",
                        Map.of(0x41, 1L),
                        Map.of(0x41, "02")
                )
        );
    }

    @Test
    void doesNotModifyInputMaps() {
        Map<Integer, Long> frequencies = new LinkedHashMap<>();
        frequencies.put(0x42, 1L);
        frequencies.put(0x41, 1L);
        Map<Integer, String> codes = new LinkedHashMap<>();
        codes.put(0x42, "1");
        codes.put(0x41, "0");
        Map<Integer, Long> originalFrequencies =
                new LinkedHashMap<>(frequencies);
        Map<Integer, String> originalCodes = new LinkedHashMap<>(codes);

        calculator.calculate("AB", frequencies, codes);

        assertEquals(originalFrequencies, frequencies);
        assertEquals(originalCodes, codes);
        assertEquals(
                List.copyOf(originalFrequencies.keySet()),
                List.copyOf(frequencies.keySet())
        );
    }

    @Test
    void probabilityResultIsUnmodifiable() {
        Map<Integer, Double> probabilities =
                calculator.calculateProbabilities(Map.of(0x41, 1L));

        assertThrows(
                UnsupportedOperationException.class,
                () -> probabilities.put(0x42, 0.5)
        );
    }

    @Test
    void detectsFrequencyTotalOverflow() {
        assertThrows(
                ArithmeticException.class,
                () -> calculator.calculateProbabilities(Map.of(
                        0x41, Long.MAX_VALUE,
                        0x42, 1L
                ))
        );
    }

    private void assertCoherent(String text) {
        Analysis analysis = analyze(text);

        assertEquals(
                text.codePoints().count(),
                analysis.metrics().totalSymbolCount()
        );
        assertEquals(
                analysis.frequencies().size(),
                analysis.metrics().distinctSymbolCount()
        );
        assertEquals(
                analysis.encodedBits().length(),
                analysis.metrics().huffmanMessageBitSize()
        );
    }

    private Analysis analyze(String text) {
        Map<Integer, Long> frequencies =
                new FrequencyAnalyzer().analyze(text);
        HuffmanNode root = new HuffmanTreeBuilder().build(frequencies);
        Map<Integer, String> codes =
                new HuffmanCodeGenerator().generate(root);
        String encodedBits = new HuffmanCodec().encode(text, codes);

        return new Analysis(
                frequencies,
                calculator.calculateProbabilities(frequencies),
                codes,
                encodedBits,
                calculator.calculate(text, frequencies, codes)
        );
    }

    private static int codePointOf(String symbol) {
        return symbol.codePointAt(0);
    }

    private record Analysis(
            Map<Integer, Long> frequencies,
            Map<Integer, Double> probabilities,
            Map<Integer, String> codes,
            String encodedBits,
            CompressionMetrics metrics
    ) {
    }
}

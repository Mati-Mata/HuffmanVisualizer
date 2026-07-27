package com.mati.huffman.service;

import com.mati.huffman.model.CompressionMetrics;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Calculates probabilities and message-only Huffman compression metrics.
 */
public final class MetricsCalculator {

    private static final int MIN_CODE_POINT = 0;
    private static final int MAX_CODE_POINT = 0x10FFFF;

    /**
     * Calculates an ordered, unmodifiable probability map.
     *
     * <p>An empty frequency map produces an empty probability map.</p>
     */
    public Map<Integer, Double> calculateProbabilities(
            Map<Integer, Long> frequencies
    ) {
        long total = validateAndTotalFrequencies(frequencies);
        if (total == 0) {
            return Collections.emptyMap();
        }

        Map<Integer, Double> probabilities = new TreeMap<>();
        for (Map.Entry<Integer, Long> entry : frequencies.entrySet()) {
            probabilities.put(
                    entry.getKey(),
                    entry.getValue() / (double) total
            );
        }
        return Collections.unmodifiableMap(probabilities);
    }

    /**
     * Calculates metrics for text, its exact frequencies, and its complete
     * Huffman code table.
     *
     * <p>Empty text is represented by zero-valued metrics when both maps are
     * empty.</p>
     */
    public CompressionMetrics calculate(
            String text,
            Map<Integer, Long> frequencies,
            Map<Integer, String> codes
    ) {
        Objects.requireNonNull(text, "Text must not be null");
        long total = validateAndTotalFrequencies(frequencies);
        Objects.requireNonNull(codes, "Codes must not be null");

        Map<Integer, Long> actualFrequencies = countCodePoints(text);
        if (!actualFrequencies.equals(frequencies)) {
            throw new IllegalArgumentException(
                    "Frequencies must match the supplied text"
            );
        }
        if (!frequencies.keySet().equals(codes.keySet())) {
            throw new IllegalArgumentException(
                    "Codes must contain exactly the frequency-map symbols"
            );
        }

        Map<Integer, Double> probabilities =
                calculateProbabilities(frequencies);
        long huffmanBits = 0;
        double averageCodeLength = 0.0;
        for (Map.Entry<Integer, Long> entry : frequencies.entrySet()) {
            String code = validateCode(
                    codes.get(entry.getKey()),
                    entry.getKey()
            );
            long symbolBits = Math.multiplyExact(
                    entry.getValue(),
                    (long) code.length()
            );
            huffmanBits = Math.addExact(huffmanBits, symbolBits);
            averageCodeLength += probabilities.get(entry.getKey())
                    * code.length();
        }

        long originalBytes = text.getBytes(StandardCharsets.UTF_8).length;
        long originalBits = Math.multiplyExact(originalBytes, 8L);
        double savingPercentage = originalBits == 0
                ? 0.0
                : (1.0 - huffmanBits / (double) originalBits) * 100.0;

        return new CompressionMetrics(
                total,
                frequencies.size(),
                originalBytes,
                originalBits,
                huffmanBits,
                averageCodeLength,
                savingPercentage
        );
    }

    private static long validateAndTotalFrequencies(
            Map<Integer, Long> frequencies
    ) {
        Objects.requireNonNull(
                frequencies,
                "Frequencies must not be null"
        );
        long total = 0;
        for (Map.Entry<Integer, Long> entry : frequencies.entrySet()) {
            Integer symbol = Objects.requireNonNull(
                    entry.getKey(),
                    "Symbol must not be null"
            );
            if (symbol < MIN_CODE_POINT || symbol > MAX_CODE_POINT) {
                throw new IllegalArgumentException(
                        "Frequency map contains an invalid Unicode code point"
                );
            }
            Long frequency = Objects.requireNonNull(
                    entry.getValue(),
                    "Frequency must not be null"
            );
            if (frequency <= 0) {
                throw new IllegalArgumentException(
                        "Frequency must be greater than zero"
                );
            }
            total = Math.addExact(total, frequency);
        }
        return total;
    }

    private static String validateCode(String code, int symbol) {
        if (code == null) {
            throw new IllegalArgumentException(
                    "Missing Huffman code for code point " + symbol
            );
        }
        if (code.isEmpty()) {
            throw new IllegalArgumentException(
                    "Huffman code must not be empty"
            );
        }
        for (int index = 0; index < code.length(); index++) {
            int bit = code.codePointAt(index);
            if (bit != 0x30 && bit != 0x31) {
                throw new IllegalArgumentException(
                        "Huffman code must contain only 0 and 1"
                );
            }
        }
        return code;
    }

    private static Map<Integer, Long> countCodePoints(String text) {
        Map<Integer, Long> frequencies = new TreeMap<>();
        text.codePoints().forEachOrdered(codePoint ->
                frequencies.merge(codePoint, 1L, Math::addExact)
        );
        return frequencies;
    }
}

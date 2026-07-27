package com.mati.huffman.model;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Immutable result of one complete Huffman compression round-trip.
 */
public final class CompressionResult {

    private static final double TOLERANCE = 1e-9;

    private final String originalText;
    private final Map<Integer, Long> frequencies;
    private final Map<Integer, Double> probabilities;
    private final HuffmanNode root;
    private final Map<Integer, String> codes;
    private final String encodedBits;
    private final String reconstructedText;
    private final CompressionMetrics metrics;

    public CompressionResult(
            String originalText,
            Map<Integer, Long> frequencies,
            Map<Integer, Double> probabilities,
            HuffmanNode root,
            Map<Integer, String> codes,
            String encodedBits,
            String reconstructedText,
            CompressionMetrics metrics
    ) {
        this.originalText = Objects.requireNonNull(
                originalText,
                "Original text must not be null"
        );
        if (originalText.isEmpty()) {
            throw new IllegalArgumentException(
                    "Compression result requires non-empty original text"
            );
        }
        this.frequencies = immutableSortedCopy(
                frequencies,
                "Frequencies must not be null"
        );
        this.probabilities = immutableSortedCopy(
                probabilities,
                "Probabilities must not be null"
        );
        this.root = Objects.requireNonNull(root, "Root must not be null");
        this.codes = immutableSortedCopy(
                codes,
                "Codes must not be null"
        );
        this.encodedBits = Objects.requireNonNull(
                encodedBits,
                "Encoded bits must not be null"
        );
        this.reconstructedText = Objects.requireNonNull(
                reconstructedText,
                "Reconstructed text must not be null"
        );
        this.metrics = Objects.requireNonNull(
                metrics,
                "Metrics must not be null"
        );

        validateConsistency();
    }

    public String originalText() {
        return originalText;
    }

    public Map<Integer, Long> frequencies() {
        return frequencies;
    }

    public Map<Integer, Double> probabilities() {
        return probabilities;
    }

    public HuffmanNode root() {
        return root;
    }

    public Map<Integer, String> codes() {
        return codes;
    }

    public String encodedBits() {
        return encodedBits;
    }

    public String reconstructedText() {
        return reconstructedText;
    }

    public CompressionMetrics metrics() {
        return metrics;
    }

    public boolean roundTripSuccessful() {
        return originalText.equals(reconstructedText);
    }

    private void validateConsistency() {
        if (!roundTripSuccessful()) {
            throw new IllegalArgumentException(
                    "Reconstructed text must match original text"
            );
        }
        if (!frequencies.keySet().equals(probabilities.keySet())
                || !frequencies.keySet().equals(codes.keySet())) {
            throw new IllegalArgumentException(
                    "Frequency, probability, and code maps must contain the same symbols"
            );
        }
        if (frequencies.isEmpty()) {
            throw new IllegalArgumentException(
                    "Compression result requires at least one symbol"
            );
        }

        Map<Integer, Long> actualFrequencies = countCodePoints(originalText);
        if (!actualFrequencies.equals(frequencies)) {
            throw new IllegalArgumentException(
                    "Frequencies must match the original text"
            );
        }

        long total = 0;
        double probabilitySum = 0.0;
        for (Map.Entry<Integer, Long> entry : frequencies.entrySet()) {
            long frequency = Objects.requireNonNull(
                    entry.getValue(),
                    "Frequency must not be null"
            );
            if (frequency <= 0) {
                throw new IllegalArgumentException(
                        "Frequency must be greater than zero"
                );
            }
            total = Math.addExact(total, frequency);

            Double probability = Objects.requireNonNull(
                    probabilities.get(entry.getKey()),
                    "Probability must not be null"
            );
            double expectedProbability = frequency / (double) metrics.totalSymbolCount();
            if (!Double.isFinite(probability)
                    || Math.abs(probability - expectedProbability) > TOLERANCE) {
                throw new IllegalArgumentException(
                        "Probability is inconsistent with frequency"
                );
            }
            probabilitySum += probability;

            validateCode(codes.get(entry.getKey()));
        }

        if (Math.abs(probabilitySum - 1.0) > TOLERANCE) {
            throw new IllegalArgumentException(
                    "Probabilities must sum to 1"
            );
        }
        if (metrics.totalSymbolCount() != total
                || metrics.distinctSymbolCount() != frequencies.size()
                || root.frequency() != total
                || metrics.huffmanMessageBitSize() != encodedBits.length()) {
            throw new IllegalArgumentException(
                    "Metrics, tree, or encoded bits are inconsistent with frequencies"
            );
        }
        validateBinary(encodedBits, "Encoded bits");
    }

    private static Map<Integer, Long> countCodePoints(String text) {
        Map<Integer, Long> result = new TreeMap<>();
        text.codePoints().forEachOrdered(codePoint ->
                result.merge(codePoint, 1L, Math::addExact)
        );
        return result;
    }

    private static void validateCode(String code) {
        Objects.requireNonNull(code, "Huffman code must not be null");
        if (code.isEmpty()) {
            throw new IllegalArgumentException(
                    "Huffman code must not be empty"
            );
        }
        validateBinary(code, "Huffman code");
    }

    private static void validateBinary(String value, String name) {
        for (int index = 0; index < value.length(); index++) {
            int bit = value.codePointAt(index);
            if (bit != 0x30 && bit != 0x31) {
                throw new IllegalArgumentException(
                        name + " must contain only 0 and 1"
                );
            }
        }
    }

    private static <V> Map<Integer, V> immutableSortedCopy(
            Map<Integer, V> source,
            String nullMessage
    ) {
        Objects.requireNonNull(source, nullMessage);
        return Collections.unmodifiableMap(new TreeMap<>(source));
    }
}

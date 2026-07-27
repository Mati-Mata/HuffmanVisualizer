package com.mati.huffman.model;

/**
 * Immutable compression measurements.
 *
 * @param totalSymbolCount number of Unicode code points in the source
 * @param distinctSymbolCount number of distinct Unicode code points
 * @param originalUtf8ByteSize source size encoded as UTF-8
 * @param originalUtf8BitSize UTF-8 byte size multiplied by eight
 * @param huffmanMessageBitSize encoded message bits only; this excludes the
 * tree, code table, headers, padding, file-format data, and other metadata
 * @param averageCodeLength weighted average Huffman code length
 * @param theoreticalSavingPercentage comparison of message bits with original
 * UTF-8 bits; it may be negative
 */
public record CompressionMetrics(
        long totalSymbolCount,
        long distinctSymbolCount,
        long originalUtf8ByteSize,
        long originalUtf8BitSize,
        long huffmanMessageBitSize,
        double averageCodeLength,
        double theoreticalSavingPercentage
) {

    private static final double TOLERANCE = 1e-9;

    public CompressionMetrics {
        requireNonNegative(totalSymbolCount, "Total symbol count");
        requireNonNegative(distinctSymbolCount, "Distinct symbol count");
        requireNonNegative(originalUtf8ByteSize, "Original UTF-8 byte size");
        requireNonNegative(originalUtf8BitSize, "Original UTF-8 bit size");
        requireNonNegative(
                huffmanMessageBitSize,
                "Huffman message bit size"
        );
        if (distinctSymbolCount > totalSymbolCount) {
            throw new IllegalArgumentException(
                    "Distinct symbol count cannot exceed total symbol count"
            );
        }

        long expectedOriginalBits = Math.multiplyExact(
                originalUtf8ByteSize,
                8L
        );
        if (originalUtf8BitSize != expectedOriginalBits) {
            throw new IllegalArgumentException(
                    "Original UTF-8 bit size must equal byte size multiplied by 8"
            );
        }
        if (!Double.isFinite(averageCodeLength)
                || averageCodeLength < 0) {
            throw new IllegalArgumentException(
                    "Average code length must be finite and non-negative"
            );
        }
        if (!Double.isFinite(theoreticalSavingPercentage)) {
            throw new IllegalArgumentException(
                    "Theoretical saving percentage must be finite"
            );
        }

        if (totalSymbolCount == 0) {
            if (distinctSymbolCount != 0
                    || huffmanMessageBitSize != 0
                    || averageCodeLength != 0.0
                    || originalUtf8ByteSize != 0
                    || theoreticalSavingPercentage != 0.0) {
                throw new IllegalArgumentException(
                        "Empty input metrics must contain only zero values"
                );
            }
        } else {
            if (distinctSymbolCount == 0
                    || originalUtf8BitSize == 0
                    || huffmanMessageBitSize == 0) {
                throw new IllegalArgumentException(
                        "Non-empty input metrics require positive sizes and symbols"
                );
            }

            double expectedAverage =
                    huffmanMessageBitSize / (double) totalSymbolCount;
            requireApproximatelyEqual(
                    expectedAverage,
                    averageCodeLength,
                    "Average code length is inconsistent with message size"
            );

            double expectedSaving = (1.0
                    - huffmanMessageBitSize
                    / (double) originalUtf8BitSize) * 100.0;
            requireApproximatelyEqual(
                    expectedSaving,
                    theoreticalSavingPercentage,
                    "Saving percentage is inconsistent with bit sizes"
            );
        }
    }

    private static void requireNonNegative(long value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(
                    name + " must not be negative"
            );
        }
    }

    private static void requireApproximatelyEqual(
            double expected,
            double actual,
            String message
    ) {
        double scale = Math.max(1.0, Math.abs(expected));
        if (Math.abs(expected - actual) > TOLERANCE * scale) {
            throw new IllegalArgumentException(message);
        }
    }
}

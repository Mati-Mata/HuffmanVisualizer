package com.mati.huffman.ui;

import java.util.Objects;

/**
 * Immutable presentation row for one analyzed Unicode symbol.
 */
public record SymbolAnalysisRow(
        int codePoint,
        String symbol,
        String unicodeCode,
        long frequency,
        double probability,
        String huffmanCode
) {

    public SymbolAnalysisRow {
        if (codePoint < 0 || codePoint > 0x10FFFF) {
            throw new IllegalArgumentException(
                    "Code point must be valid Unicode"
            );
        }
        Objects.requireNonNull(symbol, "Symbol label must not be null");
        Objects.requireNonNull(
                unicodeCode,
                "Unicode label must not be null"
        );
        if (frequency <= 0) {
            throw new IllegalArgumentException(
                    "Frequency must be greater than zero"
            );
        }
        if (!Double.isFinite(probability)
                || probability <= 0.0
                || probability > 1.0) {
            throw new IllegalArgumentException(
                    "Probability must be finite and between 0 and 1"
            );
        }
        Objects.requireNonNull(
                huffmanCode,
                "Huffman code must not be null"
        );
        if (huffmanCode.isEmpty()) {
            throw new IllegalArgumentException(
                    "Huffman code must not be empty"
            );
        }
    }

    public int codeLength() {
        return huffmanCode.length();
    }
}

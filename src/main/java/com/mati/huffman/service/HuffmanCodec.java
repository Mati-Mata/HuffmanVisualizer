package com.mati.huffman.service;

import com.mati.huffman.model.HuffmanNode;

import java.util.Map;
import java.util.Objects;

/**
 * Encodes Unicode text with a Huffman code table and decodes binary text with
 * its corresponding Huffman tree.
 */
public final class HuffmanCodec {

    private static final int ZERO = 0x30;
    private static final int ONE = 0x31;
    private static final int MIN_CODE_POINT = 0;
    private static final int MAX_CODE_POINT = 0x10FFFF;

    /**
     * Encodes text without modifying it or the supplied code table.
     */
    public String encode(String text, Map<Integer, String> codes) {
        Objects.requireNonNull(text, "Text must not be null");
        Objects.requireNonNull(codes, "Codes must not be null");
        validateCodes(codes);

        StringBuilder encoded = new StringBuilder();
        text.codePoints().forEachOrdered(codePoint -> {
            String code = codes.get(codePoint);
            if (code == null) {
                throw new IllegalArgumentException(
                        "No Huffman code exists for code point " + codePoint
                );
            }
            encoded.append(code);
        });
        return encoded.toString();
    }

    /**
     * Decodes a binary sequence with its corresponding Huffman tree.
     */
    public String decode(String bits, HuffmanNode root) {
        Objects.requireNonNull(bits, "Bits must not be null");
        Objects.requireNonNull(root, "Root must not be null");

        if (root.isLeaf()) {
            return decodeSingleSymbol(bits, root);
        }

        StringBuilder decoded = new StringBuilder();
        HuffmanNode current = root;
        for (int index = 0; index < bits.length(); index++) {
            int bit = bits.codePointAt(index);
            validateBit(bit, index);

            current = childForBit(current, bit, index);
            if (current.isLeaf()) {
                decoded.appendCodePoint(leafSymbol(current));
                current = root;
            }
        }

        if (current != root) {
            throw new IllegalArgumentException(
                    "Binary sequence ends in the middle of a tree path"
            );
        }
        return decoded.toString();
    }

    private static void validateCodes(Map<Integer, String> codes) {
        for (Map.Entry<Integer, String> entry : codes.entrySet()) {
            Integer symbol = Objects.requireNonNull(
                    entry.getKey(),
                    "Code point must not be null"
            );
            if (symbol < MIN_CODE_POINT || symbol > MAX_CODE_POINT) {
                throw new IllegalArgumentException(
                        "Code table contains an invalid Unicode code point"
                );
            }

            String code = Objects.requireNonNull(
                    entry.getValue(),
                    "Huffman code must not be null"
            );
            if (code.isEmpty()) {
                throw new IllegalArgumentException(
                        "Huffman code must not be empty"
                );
            }
            for (int index = 0; index < code.length(); index++) {
                validateBit(code.codePointAt(index), index);
            }
        }
    }

    private static String decodeSingleSymbol(
            String bits,
            HuffmanNode root
    ) {
        int symbol = leafSymbol(root);
        StringBuilder decoded = new StringBuilder();
        for (int index = 0; index < bits.length(); index++) {
            int bit = bits.codePointAt(index);
            validateBit(bit, index);
            if (bit != ZERO) {
                throw new IllegalArgumentException(
                        "A single-symbol tree accepts only bit 0"
                );
            }
            decoded.appendCodePoint(symbol);
        }
        return decoded.toString();
    }

    private static HuffmanNode childForBit(
            HuffmanNode node,
            int bit,
            int index
    ) {
        if (node.isLeaf()) {
            throw new IllegalArgumentException(
                    "Binary path cannot continue after a leaf at index "
                            + index
            );
        }
        if (node.symbol().isPresent()) {
            throw new IllegalArgumentException(
                    "An internal node must not represent a symbol"
            );
        }

        return (bit == ZERO ? node.leftChild() : node.rightChild())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Binary path has no child for bit at index " + index
                ));
    }

    private static int leafSymbol(HuffmanNode leaf) {
        if (leaf.leftChild().isPresent() || leaf.rightChild().isPresent()) {
            throw new IllegalArgumentException(
                    "A leaf node must not have children"
            );
        }
        return leaf.symbol().orElseThrow(() ->
                new IllegalArgumentException(
                        "A leaf node must represent a symbol"
                )
        );
    }

    private static void validateBit(int bit, int index) {
        if (bit != ZERO && bit != ONE) {
            throw new IllegalArgumentException(
                    "Binary value contains a character other than 0 or 1"
                            + " at index " + index
            );
        }
    }
}

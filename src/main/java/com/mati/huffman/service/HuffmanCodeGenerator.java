package com.mati.huffman.service;

import com.mati.huffman.model.HuffmanNode;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Generates deterministic binary codes from an existing Huffman tree.
 */
public final class HuffmanCodeGenerator {

    /**
     * Generates one code for every leaf. Left edges append {@code 0}; right
     * edges append {@code 1}.
     *
     * @param root root of an existing Huffman tree
     * @return an unmodifiable map ordered by ascending Unicode code point
     */
    public Map<Integer, String> generate(HuffmanNode root) {
        Objects.requireNonNull(root, "Root must not be null");

        Map<Integer, String> codes = new TreeMap<>();
        if (root.isLeaf()) {
            addLeafCode(root, "0", codes);
        } else {
            collectCodes(root, new StringBuilder(), codes);
        }
        return Collections.unmodifiableMap(codes);
    }

    private static void collectCodes(
            HuffmanNode node,
            StringBuilder path,
            Map<Integer, String> codes
    ) {
        if (node.isLeaf()) {
            addLeafCode(node, path.toString(), codes);
            return;
        }
        if (node.symbol().isPresent()) {
            throw new IllegalArgumentException(
                    "An internal node must not represent a symbol"
            );
        }

        HuffmanNode left = node.leftChild().orElseThrow(() ->
                new IllegalArgumentException(
                        "An internal node must have a left child"
                )
        );
        HuffmanNode right = node.rightChild().orElseThrow(() ->
                new IllegalArgumentException(
                        "An internal node must have a right child"
                )
        );

        path.append("0");
        collectCodes(left, path, codes);
        path.setLength(path.length() - 1);

        path.append("1");
        collectCodes(right, path, codes);
        path.setLength(path.length() - 1);
    }

    private static void addLeafCode(
            HuffmanNode leaf,
            String code,
            Map<Integer, String> codes
    ) {
        if (leaf.leftChild().isPresent() || leaf.rightChild().isPresent()) {
            throw new IllegalArgumentException(
                    "A leaf node must not have children"
            );
        }
        int symbol = leaf.symbol().orElseThrow(() ->
                new IllegalArgumentException(
                        "A leaf node must represent a symbol"
                )
        );
        if (codes.putIfAbsent(symbol, code) != null) {
            throw new IllegalArgumentException(
                    "The tree contains a duplicate leaf symbol"
            );
        }
    }
}

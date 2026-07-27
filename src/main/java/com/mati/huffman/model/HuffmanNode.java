package com.mati.huffman.model;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Immutable node in a Huffman tree.
 *
 * <p>A node is created either as a leaf with a Unicode code point and a
 * positive frequency, or as an internal node with two children. Internal-node
 * frequencies are derived from their children and cannot be supplied
 * independently.</p>
 */
public sealed abstract class HuffmanNode
        permits HuffmanNode.LeafNode, HuffmanNode.InternalNode {

    private static final int MIN_CODE_POINT = 0;
    private static final int MAX_CODE_POINT = 0x10FFFF;

    private final long frequency;
    private final int minimumCodePoint;

    private HuffmanNode(long frequency, int minimumCodePoint) {
        if (frequency <= 0) {
            throw new IllegalArgumentException("Frequency must be greater than zero");
        }
        validateCodePoint(minimumCodePoint);
        this.frequency = frequency;
        this.minimumCodePoint = minimumCodePoint;
    }

    /**
     * Creates a leaf for one Unicode code point.
     *
     * @param symbol Unicode code point represented by the leaf
     * @param frequency number of occurrences; must be positive
     * @return an immutable leaf node
     */
    public static HuffmanNode leaf(int symbol, long frequency) {
        return new LeafNode(symbol, frequency);
    }

    /**
     * Creates an internal node whose frequency is the exact sum of its
     * children's frequencies.
     *
     * @param leftChild left child, later associated with bit {@code 0}
     * @param rightChild right child, later associated with bit {@code 1}
     * @return an immutable internal node
     * @throws ArithmeticException if the frequency sum overflows a {@code long}
     */
    public static HuffmanNode internal(
            HuffmanNode leftChild,
            HuffmanNode rightChild
    ) {
        return new InternalNode(leftChild, rightChild);
    }

    public final long frequency() {
        return frequency;
    }

    public final int minimumCodePoint() {
        return minimumCodePoint;
    }

    public abstract boolean isLeaf();

    /**
     * Returns the represented code point for a leaf, or an empty value for an
     * internal node.
     */
    public abstract OptionalInt symbol();

    public abstract Optional<HuffmanNode> leftChild();

    public abstract Optional<HuffmanNode> rightChild();

    private static void validateCodePoint(int codePoint) {
        if (codePoint < MIN_CODE_POINT || codePoint > MAX_CODE_POINT) {
            throw new IllegalArgumentException(
                    "Symbol must be a valid Unicode code point"
            );
        }
    }

    private static long combinedFrequency(
            HuffmanNode leftChild,
            HuffmanNode rightChild
    ) {
        Objects.requireNonNull(leftChild, "Left child must not be null");
        Objects.requireNonNull(rightChild, "Right child must not be null");
        return Math.addExact(leftChild.frequency(), rightChild.frequency());
    }

    private static int smallestCodePoint(
            HuffmanNode leftChild,
            HuffmanNode rightChild
    ) {
        return Math.min(
                leftChild.minimumCodePoint(),
                rightChild.minimumCodePoint()
        );
    }

    private static final class LeafNode extends HuffmanNode {
        private final int symbol;

        private LeafNode(int symbol, long frequency) {
            super(frequency, symbol);
            this.symbol = symbol;
        }

        @Override
        public boolean isLeaf() {
            return true;
        }

        @Override
        public OptionalInt symbol() {
            return OptionalInt.of(symbol);
        }

        @Override
        public Optional<HuffmanNode> leftChild() {
            return Optional.empty();
        }

        @Override
        public Optional<HuffmanNode> rightChild() {
            return Optional.empty();
        }
    }

    private static final class InternalNode extends HuffmanNode {
        private final HuffmanNode leftChild;
        private final HuffmanNode rightChild;

        private InternalNode(
                HuffmanNode leftChild,
                HuffmanNode rightChild
        ) {
            super(
                    combinedFrequency(leftChild, rightChild),
                    smallestCodePoint(leftChild, rightChild)
            );
            this.leftChild = leftChild;
            this.rightChild = rightChild;
        }

        @Override
        public boolean isLeaf() {
            return false;
        }

        @Override
        public OptionalInt symbol() {
            return OptionalInt.empty();
        }

        @Override
        public Optional<HuffmanNode> leftChild() {
            return Optional.of(leftChild);
        }

        @Override
        public Optional<HuffmanNode> rightChild() {
            return Optional.of(rightChild);
        }
    }
}

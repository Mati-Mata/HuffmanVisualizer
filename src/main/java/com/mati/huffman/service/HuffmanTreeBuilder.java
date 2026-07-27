package com.mati.huffman.service;

import com.mati.huffman.model.HuffmanNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;

/**
 * Builds deterministic Huffman trees from Unicode code-point frequencies.
 */
public final class HuffmanTreeBuilder {

    /**
     * Total ordering for queued nodes. Creation order is unique within one
     * build and is used only after all node-derived tie-breakers.
     */
    private static final Comparator<QueueEntry> NODE_PRIORITY =
            Comparator.comparingLong(
                            (QueueEntry entry) -> entry.node().frequency()
                    )
                    .thenComparingInt(
                            entry -> entry.node().minimumCodePoint()
                    )
                    .thenComparingInt(
                            entry -> entry.node().isLeaf() ? 0 : 1
                    )
                    .thenComparingLong(QueueEntry::creationOrder);

    /**
     * Builds one Huffman tree without modifying the supplied map.
     *
     * @param frequencies positive frequencies keyed by Unicode code point
     * @return the root of the deterministic Huffman tree
     * @throws NullPointerException if the map, a key, or a frequency is null
     * @throws IllegalArgumentException if the map is empty, a frequency is not
     * positive, or a key is not a valid Unicode code point
     * @throws ArithmeticException if the total frequency overflows a
     * {@code long}
     */
    public HuffmanNode build(Map<Integer, Long> frequencies) {
        Objects.requireNonNull(frequencies, "Frequencies must not be null");
        if (frequencies.isEmpty()) {
            throw new IllegalArgumentException(
                    "Cannot build a Huffman tree from an empty frequency map"
            );
        }

        List<LeafInput> inputs = new ArrayList<>(frequencies.size());
        long expectedTotal = 0;
        for (Map.Entry<Integer, Long> entry : frequencies.entrySet()) {
            Integer symbol = Objects.requireNonNull(
                    entry.getKey(),
                    "Symbol must not be null"
            );
            Long frequency = Objects.requireNonNull(
                    entry.getValue(),
                    "Frequency must not be null"
            );
            if (frequency <= 0) {
                throw new IllegalArgumentException(
                        "Frequency must be greater than zero"
                );
            }

            expectedTotal = Math.addExact(expectedTotal, frequency);
            inputs.add(new LeafInput(symbol, frequency));
        }
        inputs.sort(Comparator.comparingInt(LeafInput::symbol));

        PriorityQueue<QueueEntry> queue = new PriorityQueue<>(NODE_PRIORITY);
        long creationOrder = 0;
        for (LeafInput input : inputs) {
            queue.add(new QueueEntry(
                    HuffmanNode.leaf(input.symbol(), input.frequency()),
                    creationOrder++
            ));
        }

        while (queue.size() > 1) {
            HuffmanNode leftChild = queue.remove().node();
            HuffmanNode rightChild = queue.remove().node();
            HuffmanNode parent = HuffmanNode.internal(leftChild, rightChild);
            queue.add(new QueueEntry(parent, creationOrder++));
        }

        HuffmanNode root = queue.remove().node();
        if (root.frequency() != expectedTotal) {
            throw new IllegalStateException(
                    "Root frequency does not match the input total"
            );
        }
        return root;
    }

    private record LeafInput(int symbol, long frequency) {
    }

    private record QueueEntry(HuffmanNode node, long creationOrder) {
    }
}

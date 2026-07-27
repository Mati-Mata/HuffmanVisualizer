package com.mati.huffman.ui;

import com.mati.huffman.model.HuffmanNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Deterministic, JavaFX-independent layout for a Huffman tree.
 *
 * <p>The first pass measures every subtree. The second pass places children
 * inside those measured areas and centers each parent between its children.</p>
 */
public final class HuffmanTreeLayout {

    public static final int DEFAULT_MAX_LEAVES = 192;
    public static final double LEAF_WIDTH = 124.0;
    public static final double LEAF_HEIGHT = 88.0;
    public static final double INTERNAL_WIDTH = 82.0;
    public static final double INTERNAL_HEIGHT = 52.0;
    public static final double HORIZONTAL_GAP = 28.0;
    public static final double LEVEL_GAP = 82.0;
    public static final double MARGIN = 36.0;

    public LayoutResult layout(HuffmanNode root) {
        Objects.requireNonNull(root, "Tree root must not be null");

        MeasuredNode measuredRoot = measure(root);
        List<PositionedNode> nodes = new ArrayList<>();
        List<PositionedEdge> edges = new ArrayList<>();
        place(measuredRoot, MARGIN, 0, nodes, edges);

        double width = measuredRoot.subtreeWidth() + (2 * MARGIN);
        double height = (2 * MARGIN)
                + (measuredRoot.depth() * LEVEL_GAP)
                + measuredRoot.levelHeight();
        return new LayoutResult(nodes, edges, width, height,
                measuredRoot.leafCount());
    }

    public int countLeaves(HuffmanNode root) {
        Objects.requireNonNull(root, "Tree root must not be null");
        if (root.isLeaf()) {
            return 1;
        }
        return Math.addExact(
                countLeaves(requiredLeft(root)),
                countLeaves(requiredRight(root))
        );
    }

    public boolean exceedsLeafLimit(HuffmanNode root, int maximumLeaves) {
        if (maximumLeaves <= 0) {
            throw new IllegalArgumentException(
                    "Maximum leaf count must be positive"
            );
        }
        return countLeaves(root) > maximumLeaves;
    }

    private MeasuredNode measure(HuffmanNode node) {
        if (node.isLeaf()) {
            return new MeasuredNode(
                    node, LEAF_WIDTH, LEAF_HEIGHT, 0, 1, null, null
            );
        }

        MeasuredNode left = measure(requiredLeft(node));
        MeasuredNode right = measure(requiredRight(node));
        double childrenWidth = left.subtreeWidth()
                + HORIZONTAL_GAP
                + right.subtreeWidth();
        return new MeasuredNode(
                node,
                Math.max(INTERNAL_WIDTH, childrenWidth),
                INTERNAL_HEIGHT,
                1 + Math.max(left.depth(), right.depth()),
                Math.addExact(left.leafCount(), right.leafCount()),
                left,
                right
        );
    }

    private PositionedNode place(
            MeasuredNode measured,
            double subtreeLeft,
            int level,
            List<PositionedNode> nodes,
            List<PositionedEdge> edges
    ) {
        double centerY = MARGIN + (level * LEVEL_GAP)
                + (measured.levelHeight() / 2);
        PositionedNode positioned;

        if (measured.node().isLeaf()) {
            positioned = new PositionedNode(
                    measured.node(),
                    subtreeLeft + (measured.subtreeWidth() / 2),
                    centerY,
                    LEAF_WIDTH,
                    LEAF_HEIGHT,
                    level
            );
        } else {
            PositionedNode left = place(
                    measured.left(), subtreeLeft, level + 1, nodes, edges
            );
            double rightStart = subtreeLeft
                    + measured.left().subtreeWidth()
                    + HORIZONTAL_GAP;
            PositionedNode right = place(
                    measured.right(), rightStart, level + 1, nodes, edges
            );
            positioned = new PositionedNode(
                    measured.node(),
                    (left.centerX() + right.centerX()) / 2,
                    centerY,
                    INTERNAL_WIDTH,
                    INTERNAL_HEIGHT,
                    level
            );
            edges.add(new PositionedEdge(positioned, left, 0));
            edges.add(new PositionedEdge(positioned, right, 1));
        }

        nodes.add(positioned);
        return positioned;
    }

    private static HuffmanNode requiredLeft(HuffmanNode node) {
        return node.leftChild().orElseThrow(() ->
                new IllegalArgumentException(
                        "An internal Huffman node must have a left child"
                )
        );
    }

    private static HuffmanNode requiredRight(HuffmanNode node) {
        return node.rightChild().orElseThrow(() ->
                new IllegalArgumentException(
                        "An internal Huffman node must have a right child"
                )
        );
    }

    private record MeasuredNode(
            HuffmanNode node,
            double subtreeWidth,
            double levelHeight,
            int depth,
            int leafCount,
            MeasuredNode left,
            MeasuredNode right
    ) {
    }

    public record PositionedNode(
            HuffmanNode node,
            double centerX,
            double centerY,
            double width,
            double height,
            int level
    ) {
        public PositionedNode {
            Objects.requireNonNull(node, "Positioned node must not be null");
        }
    }

    public record PositionedEdge(
            PositionedNode parent,
            PositionedNode child,
            int bit
    ) {
        public PositionedEdge {
            Objects.requireNonNull(parent, "Edge parent must not be null");
            Objects.requireNonNull(child, "Edge child must not be null");
            if (bit != 0 && bit != 1) {
                throw new IllegalArgumentException("Edge bit must be 0 or 1");
            }
        }
    }

    public record LayoutResult(
            List<PositionedNode> nodes,
            List<PositionedEdge> edges,
            double width,
            double height,
            int leafCount
    ) {
        public LayoutResult {
            nodes = List.copyOf(nodes);
            edges = List.copyOf(edges);
            if (width <= 0 || height <= 0 || leafCount <= 0) {
                throw new IllegalArgumentException(
                        "Layout dimensions and leaf count must be positive"
                );
            }
        }
    }
}

package com.mati.huffman.ui;

import com.mati.huffman.model.HuffmanNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HuffmanTreeLayoutTest {

    private static final double EPSILON = 0.000_001;

    private final HuffmanTreeLayout layoutEngine = new HuffmanTreeLayout();

    @Test
    void laysOutSingleLeaf() {
        HuffmanNode leaf = HuffmanNode.leaf('A', 3);

        HuffmanTreeLayout.LayoutResult result = layoutEngine.layout(leaf);

        assertEquals(1, result.nodes().size());
        assertEquals(0, result.edges().size());
        assertEquals(1, result.leafCount());
        assertEquals(HuffmanTreeLayout.MARGIN
                        + (HuffmanTreeLayout.LEAF_WIDTH / 2),
                result.nodes().getFirst().centerX(), EPSILON);
    }

    @Test
    void laysOutTwoLeavesAndBothEdges() {
        HuffmanNode root = HuffmanNode.internal(
                HuffmanNode.leaf('A', 1),
                HuffmanNode.leaf('B', 1)
        );

        HuffmanTreeLayout.LayoutResult result = layoutEngine.layout(root);

        assertEquals(3, result.nodes().size());
        assertEquals(2, result.edges().size());
        assertEquals(List.of(0, 1), result.edges().stream()
                .map(HuffmanTreeLayout.PositionedEdge::bit)
                .sorted()
                .toList());
    }

    @Test
    void laysOutBalancedTreeWithoutSameLevelOverlap() {
        HuffmanTreeLayout.LayoutResult result =
                layoutEngine.layout(balancedTree());

        assertNoSameLevelOverlap(result);
        assertEquals(4, result.leafCount());
    }

    @Test
    void laysOutUnbalancedTreeWithoutSameLevelOverlap() {
        HuffmanTreeLayout.LayoutResult result =
                layoutEngine.layout(unbalancedTree());

        assertNoSameLevelOverlap(result);
        assertEquals(4, result.leafCount());
    }

    @Test
    void centersEveryParentBetweenItsChildren() {
        HuffmanTreeLayout.LayoutResult result =
                layoutEngine.layout(unbalancedTree());

        for (HuffmanTreeLayout.PositionedEdge leftEdge
                : result.edges().stream().filter(edge -> edge.bit() == 0).toList()) {
            HuffmanTreeLayout.PositionedEdge rightEdge = result.edges().stream()
                    .filter(edge -> edge.parent() == leftEdge.parent())
                    .filter(edge -> edge.bit() == 1)
                    .findFirst()
                    .orElseThrow();
            assertEquals(
                    (leftEdge.child().centerX()
                            + rightEdge.child().centerX()) / 2,
                    leftEdge.parent().centerX(),
                    EPSILON
            );
        }
    }

    @Test
    void leavesRequiredHorizontalGapBetweenSiblingSubtrees() {
        HuffmanTreeLayout.LayoutResult result =
                layoutEngine.layout(balancedTree());
        List<HuffmanTreeLayout.PositionedNode> leaves = result.nodes().stream()
                .filter(position -> position.node().isLeaf())
                .sorted(Comparator.comparingDouble(
                        HuffmanTreeLayout.PositionedNode::centerX
                ))
                .toList();

        for (int index = 1; index < leaves.size(); index++) {
            double previousRight = leaves.get(index - 1).centerX()
                    + (leaves.get(index - 1).width() / 2);
            double currentLeft = leaves.get(index).centerX()
                    - (leaves.get(index).width() / 2);
            assertTrue(currentLeft - previousRight
                    >= HuffmanTreeLayout.HORIZONTAL_GAP - EPSILON);
        }
    }

    @Test
    void usesUniformVerticalSeparationBetweenLevels() {
        HuffmanTreeLayout.LayoutResult result =
                layoutEngine.layout(balancedTree());
        for (HuffmanTreeLayout.PositionedEdge edge : result.edges()) {
            assertEquals(
                    HuffmanTreeLayout.LEVEL_GAP
                            + ((edge.child().height()
                            - edge.parent().height()) / 2),
                    edge.child().centerY() - edge.parent().centerY(),
                    EPSILON
            );
        }
    }

    @Test
    void dimensionsArePositive() {
        HuffmanTreeLayout.LayoutResult result =
                layoutEngine.layout(unbalancedTree());

        assertTrue(result.width() > 0);
        assertTrue(result.height() > 0);
        result.nodes().forEach(node -> {
            assertTrue(node.width() > 0);
            assertTrue(node.height() > 0);
        });
    }

    @Test
    void positionsAreDeterministic() {
        HuffmanNode root = balancedTree();

        assertEquals(layoutSignature(layoutEngine.layout(root)),
                layoutSignature(layoutEngine.layout(root)));
    }

    @Test
    void nodesAtSameLevelDoNotOverlap() {
        assertNoSameLevelOverlap(layoutEngine.layout(unbalancedTree()));
        assertNoSameLevelOverlap(layoutEngine.layout(balancedTree()));
    }

    @Test
    void totalSizeContainsEveryNode() {
        HuffmanTreeLayout.LayoutResult result =
                layoutEngine.layout(unbalancedTree());

        result.nodes().forEach(node -> {
            assertTrue(node.centerX() - (node.width() / 2) >= 0);
            assertTrue(node.centerY() - (node.height() / 2) >= 0);
            assertTrue(node.centerX() + (node.width() / 2)
                    <= result.width() + EPSILON);
            assertTrue(node.centerY() + (node.height() / 2)
                    <= result.height() + EPSILON);
        });
    }

    @Test
    void independentLayoutsDoNotRetainNodesFromPreviousTree() {
        HuffmanNode first = balancedTree();
        HuffmanNode second = HuffmanNode.leaf(0x1F642, 2);

        HuffmanTreeLayout.LayoutResult firstResult =
                layoutEngine.layout(first);
        HuffmanTreeLayout.LayoutResult secondResult =
                layoutEngine.layout(second);

        assertEquals(7, firstResult.nodes().size());
        assertEquals(1, secondResult.nodes().size());
        assertTrue(secondResult.nodes().stream()
                .allMatch(node -> node.node() == second));
    }

    @Test
    void enforcesDocumentedVisualLeafLimit() {
        HuffmanNode atLimit = treeWithLeaves(
                HuffmanTreeLayout.DEFAULT_MAX_LEAVES
        );
        HuffmanNode overLimit = treeWithLeaves(
                HuffmanTreeLayout.DEFAULT_MAX_LEAVES + 1
        );

        assertFalse(layoutEngine.exceedsLeafLimit(
                atLimit, HuffmanTreeLayout.DEFAULT_MAX_LEAVES
        ));
        assertTrue(layoutEngine.exceedsLeafLimit(
                overLimit, HuffmanTreeLayout.DEFAULT_MAX_LEAVES
        ));
    }

    private static void assertNoSameLevelOverlap(
            HuffmanTreeLayout.LayoutResult result
    ) {
        int deepestLevel = result.nodes().stream()
                .mapToInt(HuffmanTreeLayout.PositionedNode::level)
                .max()
                .orElseThrow();
        for (int level = 0; level <= deepestLevel; level++) {
            int currentLevel = level;
            List<HuffmanTreeLayout.PositionedNode> nodes =
                    result.nodes().stream()
                            .filter(node -> node.level() == currentLevel)
                            .sorted(Comparator.comparingDouble(
                                    HuffmanTreeLayout.PositionedNode::centerX
                            ))
                            .toList();
            for (int index = 1; index < nodes.size(); index++) {
                HuffmanTreeLayout.PositionedNode previous =
                        nodes.get(index - 1);
                HuffmanTreeLayout.PositionedNode current = nodes.get(index);
                assertTrue(
                        previous.centerX() + (previous.width() / 2)
                                <= current.centerX()
                                - (current.width() / 2) + EPSILON
                );
            }
        }
    }

    private static List<String> layoutSignature(
            HuffmanTreeLayout.LayoutResult result
    ) {
        return result.nodes().stream()
                .map(node -> node.node().minimumCodePoint()
                        + ":" + node.centerX()
                        + ":" + node.centerY()
                        + ":" + node.width()
                        + ":" + node.height())
                .sorted()
                .toList();
    }

    private static HuffmanNode balancedTree() {
        return HuffmanNode.internal(
                HuffmanNode.internal(
                        HuffmanNode.leaf('A', 1),
                        HuffmanNode.leaf('B', 1)
                ),
                HuffmanNode.internal(
                        HuffmanNode.leaf('C', 1),
                        HuffmanNode.leaf('D', 1)
                )
        );
    }

    private static HuffmanNode unbalancedTree() {
        return HuffmanNode.internal(
                HuffmanNode.leaf('A', 1),
                HuffmanNode.internal(
                        HuffmanNode.leaf('B', 1),
                        HuffmanNode.internal(
                                HuffmanNode.leaf('C', 1),
                                HuffmanNode.leaf('D', 1)
                        )
                )
        );
    }

    private static HuffmanNode treeWithLeaves(int leafCount) {
        List<HuffmanNode> nodes = new ArrayList<>(leafCount);
        for (int index = 0; index < leafCount; index++) {
            nodes.add(HuffmanNode.leaf(index, 1));
        }
        while (nodes.size() > 1) {
            List<HuffmanNode> next = new ArrayList<>((nodes.size() + 1) / 2);
            for (int index = 0; index < nodes.size(); index += 2) {
                if (index + 1 == nodes.size()) {
                    next.add(nodes.get(index));
                } else {
                    next.add(HuffmanNode.internal(
                            nodes.get(index), nodes.get(index + 1)
                    ));
                }
            }
            nodes = next;
        }
        return nodes.getFirst();
    }
}

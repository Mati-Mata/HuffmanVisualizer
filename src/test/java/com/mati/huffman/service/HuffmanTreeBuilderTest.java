package com.mati.huffman.service;

import com.mati.huffman.model.HuffmanNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HuffmanTreeBuilderTest {

    private final HuffmanTreeBuilder builder = new HuffmanTreeBuilder();

    @Test
    void buildsSingleSymbolTree() {
        int symbol = codePointOf("A");

        HuffmanNode root = builder.build(Map.of(symbol, 7L));

        assertTrue(root.isLeaf());
        assertEquals(symbol, root.symbol().orElseThrow());
        assertEquals(7L, root.frequency());
    }

    @Test
    void buildsExpectedTreeForBanana() {
        Map<Integer, Long> frequencies = frequenciesFor("BANANA");

        HuffmanNode root = builder.build(frequencies);

        assertEquals(
                "I(6,L(65:3),I(3,L(66:1),L(78:2)))",
                serialize(root)
        );
    }

    @Test
    void rootFrequencyEqualsInputTotal() {
        HuffmanNode root = builder.build(Map.of(
                codePointOf("A"), 5L,
                codePointOf("B"), 8L,
                codePointOf("C"), 13L
        ));

        assertEquals(26L, root.frequency());
    }

    @Test
    void buildsTwoSymbolTreeInPriorityOrder() {
        HuffmanNode root = builder.build(Map.of(
                codePointOf("A"), 4L,
                codePointOf("B"), 2L
        ));

        assertEquals(
                "I(6,L(66:2),L(65:4))",
                serialize(root)
        );
    }

    @Test
    void buildsTreeWithSeveralSymbols() {
        HuffmanNode root = builder.build(Map.of(
                codePointOf("A"), 2L,
                codePointOf("B"), 3L,
                codePointOf("C"), 5L,
                codePointOf("D"), 7L,
                codePointOf("E"), 11L
        ));

        assertFalse(root.isLeaf());
        assertEquals(28L, root.frequency());
        assertEquals(5, leafSymbols(root).size());
    }

    @Test
    void resolvesEqualFrequenciesDeterministically() {
        Map<Integer, Long> frequencies = Map.of(
                codePointOf("A"), 1L,
                codePointOf("B"), 1L,
                codePointOf("C"), 1L,
                codePointOf("D"), 1L
        );

        assertEquals(
                "I(4,I(2,L(65:1),L(66:1)),I(2,L(67:1),L(68:1)))",
                serialize(builder.build(frequencies))
        );
    }

    @Test
    void repeatedBuildsProduceIdenticalStructure() {
        Map<Integer, Long> frequencies = Map.of(
                codePointOf("A"), 2L,
                codePointOf("B"), 2L,
                codePointOf("C"), 3L,
                codePointOf("D"), 3L,
                codePointOf("E"), 5L
        );
        String expected = serialize(builder.build(frequencies));

        for (int repetition = 0; repetition < 20; repetition++) {
            assertEquals(expected, serialize(builder.build(frequencies)));
        }
    }

    @Test
    void insertionOrderDoesNotAffectStructure() {
        Map<Integer, Long> firstOrder = new LinkedHashMap<>();
        firstOrder.put(codePointOf("A"), 1L);
        firstOrder.put(codePointOf("B"), 1L);
        firstOrder.put(codePointOf("C"), 1L);
        firstOrder.put(codePointOf("D"), 1L);

        Map<Integer, Long> reverseOrder = new LinkedHashMap<>();
        reverseOrder.put(codePointOf("D"), 1L);
        reverseOrder.put(codePointOf("C"), 1L);
        reverseOrder.put(codePointOf("B"), 1L);
        reverseOrder.put(codePointOf("A"), 1L);

        assertEquals(
                serialize(builder.build(firstOrder)),
                serialize(builder.build(reverseOrder))
        );
    }

    @Test
    void rejectsEmptyMap() {
        assertThrows(
                IllegalArgumentException.class,
                () -> builder.build(Map.of())
        );
    }

    @Test
    void rejectsNullMap() {
        assertThrows(
                NullPointerException.class,
                () -> builder.build(null)
        );
    }

    @Test
    void rejectsNullKey() {
        Map<Integer, Long> frequencies = new HashMap<>();
        frequencies.put(null, 1L);

        assertThrows(
                NullPointerException.class,
                () -> builder.build(frequencies)
        );
    }

    @Test
    void rejectsNullFrequency() {
        Map<Integer, Long> frequencies = new HashMap<>();
        frequencies.put(codePointOf("A"), null);

        assertThrows(
                NullPointerException.class,
                () -> builder.build(frequencies)
        );
    }

    @Test
    void rejectsZeroFrequency() {
        assertThrows(
                IllegalArgumentException.class,
                () -> builder.build(Map.of(codePointOf("A"), 0L))
        );
    }

    @Test
    void rejectsNegativeFrequency() {
        assertThrows(
                IllegalArgumentException.class,
                () -> builder.build(Map.of(codePointOf("A"), -1L))
        );
    }

    @Test
    void doesNotModifyInputMap() {
        Map<Integer, Long> frequencies = new LinkedHashMap<>();
        frequencies.put(codePointOf("B"), 2L);
        frequencies.put(codePointOf("A"), 1L);
        Map<Integer, Long> original = new LinkedHashMap<>(frequencies);

        builder.build(frequencies);

        assertEquals(original, frequencies);
        assertEquals(
                new ArrayList<>(original.keySet()),
                new ArrayList<>(frequencies.keySet())
        );
    }

    @Test
    void acceptsLargeFrequenciesWhenTheirSumFitsInLong() {
        HuffmanNode root = builder.build(Map.of(
                codePointOf("A"), Long.MAX_VALUE - 1,
                codePointOf("B"), 1L
        ));

        assertEquals(Long.MAX_VALUE, root.frequency());
    }

    @Test
    void rejectsFrequencyTotalOverflow() {
        assertThrows(
                ArithmeticException.class,
                () -> builder.build(Map.of(
                        codePointOf("A"), Long.MAX_VALUE,
                        codePointOf("B"), 1L
                ))
        );
    }

    @Test
    void everyOriginalSymbolAppearsExactlyOnceAsLeaf() {
        Map<Integer, Long> frequencies = Map.of(
                codePointOf("A"), 3L,
                codePointOf("ñ"), 2L,
                "🙂".codePointAt(0), 4L,
                "🚀".codePointAt(0), 1L
        );

        List<Integer> actualSymbols = leafSymbols(
                builder.build(frequencies)
        ).stream().sorted().toList();
        List<Integer> expectedSymbols = frequencies.keySet()
                .stream()
                .sorted()
                .toList();

        assertEquals(expectedSymbols, actualSymbols);
    }

    @Test
    void everyInternalNodeHasTwoChildren() {
        HuffmanNode root = builder.build(frequenciesFor("BANANA🙂🚀"));

        assertEveryInternalNodeHasTwoChildren(root);
    }

    @Test
    void everyInternalFrequencyEqualsItsChildrenSum() {
        HuffmanNode root = builder.build(frequenciesFor("BANANA🙂🚀"));

        assertInternalFrequencies(root);
    }

    private static Map<Integer, Long> frequenciesFor(String text) {
        return new FrequencyAnalyzer().analyze(text);
    }

    private static int codePointOf(String symbol) {
        return symbol.codePointAt(0);
    }

    private static String serialize(HuffmanNode node) {
        if (node.isLeaf()) {
            return "L(" + node.symbol().orElseThrow()
                    + ":" + node.frequency() + ")";
        }
        return "I(" + node.frequency()
                + "," + serialize(node.leftChild().orElseThrow())
                + "," + serialize(node.rightChild().orElseThrow()) + ")";
    }

    private static List<Integer> leafSymbols(HuffmanNode node) {
        List<Integer> symbols = new ArrayList<>();
        collectLeafSymbols(node, symbols);
        return symbols;
    }

    private static void collectLeafSymbols(
            HuffmanNode node,
            List<Integer> symbols
    ) {
        if (node.isLeaf()) {
            symbols.add(node.symbol().orElseThrow());
            return;
        }
        collectLeafSymbols(node.leftChild().orElseThrow(), symbols);
        collectLeafSymbols(node.rightChild().orElseThrow(), symbols);
    }

    private static void assertEveryInternalNodeHasTwoChildren(
            HuffmanNode node
    ) {
        if (node.isLeaf()) {
            assertTrue(node.leftChild().isEmpty());
            assertTrue(node.rightChild().isEmpty());
            return;
        }

        HuffmanNode left = node.leftChild().orElseThrow();
        HuffmanNode right = node.rightChild().orElseThrow();
        assertEveryInternalNodeHasTwoChildren(left);
        assertEveryInternalNodeHasTwoChildren(right);
    }

    private static void assertInternalFrequencies(HuffmanNode node) {
        if (node.isLeaf()) {
            return;
        }

        HuffmanNode left = node.leftChild().orElseThrow();
        HuffmanNode right = node.rightChild().orElseThrow();
        assertEquals(
                Math.addExact(left.frequency(), right.frequency()),
                node.frequency()
        );
        assertInternalFrequencies(left);
        assertInternalFrequencies(right);
    }
}

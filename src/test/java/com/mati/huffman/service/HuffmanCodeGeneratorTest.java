package com.mati.huffman.service;

import com.mati.huffman.model.HuffmanNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HuffmanCodeGeneratorTest {

    private final HuffmanCodeGenerator generator = new HuffmanCodeGenerator();

    @Test
    void assignsZeroToSingleLeaf() {
        int symbol = codePointOf("A");

        Map<Integer, String> codes = generator.generate(
                HuffmanNode.leaf(symbol, 5)
        );

        assertEquals(Map.of(symbol, "0"), codes);
    }

    @Test
    void assignsZeroLeftAndOneRightForTwoLeaves() {
        int leftSymbol = codePointOf("A");
        int rightSymbol = codePointOf("B");
        HuffmanNode root = HuffmanNode.internal(
                HuffmanNode.leaf(leftSymbol, 1),
                HuffmanNode.leaf(rightSymbol, 1)
        );

        Map<Integer, String> codes = generator.generate(root);

        assertEquals("0", codes.get(leftSymbol));
        assertEquals("1", codes.get(rightSymbol));
    }

    @Test
    void generatesExpectedCodesForBanana() {
        Map<Integer, String> codes = generator.generate(treeFor("BANANA"));

        assertEquals("0", codes.get(codePointOf("A")));
        assertEquals("10", codes.get(codePointOf("B")));
        assertEquals("11", codes.get(codePointOf("N")));
    }

    @Test
    void assignsExactlyOneCodeToEveryLeaf() {
        HuffmanNode root = treeFor("ABCDE🙂🚀");
        Set<Integer> leaves = new HashSet<>();
        collectLeafSymbols(root, leaves);

        Map<Integer, String> codes = generator.generate(root);

        assertEquals(leaves, codes.keySet());
        assertEquals(leaves.size(), codes.size());
    }

    @Test
    void doesNotAssignCodesToInternalNodes() {
        HuffmanNode left = HuffmanNode.internal(
                HuffmanNode.leaf(codePointOf("A"), 1),
                HuffmanNode.leaf(codePointOf("B"), 1)
        );
        HuffmanNode root = HuffmanNode.internal(
                left,
                HuffmanNode.leaf(codePointOf("C"), 2)
        );

        Map<Integer, String> codes = generator.generate(root);

        assertEquals(3, codes.size());
        assertEquals(
                Set.of(
                        codePointOf("A"),
                        codePointOf("B"),
                        codePointOf("C")
                ),
                codes.keySet()
        );
    }

    @Test
    void generatesOnlyBinaryCodes() {
        Map<Integer, String> codes = generator.generate(
                treeFor("BANANA🙂🚀")
        );

        assertTrue(codes.values().stream().allMatch(code ->
                code.codePoints().allMatch(bit -> bit == 0x30 || bit == 0x31)
        ));
    }

    @Test
    void generatedCodesArePrefixFree() {
        List<String> codes = new ArrayList<>(
                generator.generate(treeFor("BANANA🙂🚀")).values()
        );

        for (int first = 0; first < codes.size(); first++) {
            for (int second = 0; second < codes.size(); second++) {
                if (first != second) {
                    assertFalse(codes.get(second).startsWith(codes.get(first)));
                }
            }
        }
    }

    @Test
    void repeatedGenerationIsDeterministic() {
        HuffmanNode root = treeFor("deterministic🙂text");
        Map<Integer, String> expected = generator.generate(root);

        for (int repetition = 0; repetition < 20; repetition++) {
            assertEquals(expected, generator.generate(root));
        }
    }

    @Test
    void rejectsNullRoot() {
        assertThrows(
                NullPointerException.class,
                () -> generator.generate(null)
        );
    }

    @Test
    void returnsUnmodifiableMap() {
        Map<Integer, String> codes = generator.generate(treeFor("AB"));

        assertThrows(
                UnsupportedOperationException.class,
                () -> codes.put(codePointOf("C"), "10")
        );
        assertThrows(
                UnsupportedOperationException.class,
                codes::clear
        );
    }

    @Test
    void supportsSupplementaryCodePointLeaf() {
        int smilingFace = "🙂".codePointAt(0);

        Map<Integer, String> codes = generator.generate(
                HuffmanNode.leaf(smilingFace, 2)
        );

        assertEquals("0", codes.get(smilingFace));
    }

    private static HuffmanNode treeFor(String text) {
        Map<Integer, Long> frequencies =
                new FrequencyAnalyzer().analyze(text);
        return new HuffmanTreeBuilder().build(frequencies);
    }

    private static int codePointOf(String symbol) {
        return symbol.codePointAt(0);
    }

    private static void collectLeafSymbols(
            HuffmanNode node,
            Set<Integer> symbols
    ) {
        if (node.isLeaf()) {
            symbols.add(node.symbol().orElseThrow());
            return;
        }
        collectLeafSymbols(node.leftChild().orElseThrow(), symbols);
        collectLeafSymbols(node.rightChild().orElseThrow(), symbols);
    }
}

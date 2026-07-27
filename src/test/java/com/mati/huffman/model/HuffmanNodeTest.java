package com.mati.huffman.model;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HuffmanNodeTest {

    @Test
    void createsAsciiLeaf() {
        HuffmanNode leaf = HuffmanNode.leaf(0x41, 4);

        assertTrue(leaf.isLeaf());
        assertEquals(0x41, leaf.symbol().orElseThrow());
        assertEquals(4, leaf.frequency());
        assertEquals(0x41, leaf.minimumCodePoint());
    }

    @Test
    void createsLeafWithSupplementaryCodePoint() {
        int smilingFace = 0x1F642;

        HuffmanNode leaf = HuffmanNode.leaf(smilingFace, 2);

        assertEquals(smilingFace, leaf.symbol().orElseThrow());
        assertEquals(smilingFace, leaf.minimumCodePoint());
    }

    @Test
    void createsInternalNodeWithBothChildren() {
        HuffmanNode left = HuffmanNode.leaf(0x41, 2);
        HuffmanNode right = HuffmanNode.leaf(0x42, 3);

        HuffmanNode internal = HuffmanNode.internal(left, right);

        assertFalse(internal.isLeaf());
        assertSame(left, internal.leftChild().orElseThrow());
        assertSame(right, internal.rightChild().orElseThrow());
        assertTrue(internal.symbol().isEmpty());
    }

    @Test
    void derivesInternalFrequencyFromChildren() {
        HuffmanNode left = HuffmanNode.leaf(0x41, 7);
        HuffmanNode right = HuffmanNode.leaf(0x42, 11);

        HuffmanNode internal = HuffmanNode.internal(left, right);

        assertEquals(18, internal.frequency());
    }

    @Test
    void reportsLeafAndInternalNodeKinds() {
        HuffmanNode leaf = HuffmanNode.leaf(0x41, 1);
        HuffmanNode internal = HuffmanNode.internal(
                leaf,
                HuffmanNode.leaf(0x42, 1)
        );

        assertTrue(leaf.isLeaf());
        assertFalse(internal.isLeaf());
    }

    @Test
    void calculatesMinimumCodePointAcrossSubtree() {
        HuffmanNode higherSubtree = HuffmanNode.internal(
                HuffmanNode.leaf(0x1F642, 1),
                HuffmanNode.leaf(0x7A, 1)
        );
        HuffmanNode root = HuffmanNode.internal(
                higherSubtree,
                HuffmanNode.leaf(0x20, 1)
        );

        assertEquals(0x7A, higherSubtree.minimumCodePoint());
        assertEquals(0x20, root.minimumCodePoint());
    }

    @Test
    void rejectsZeroFrequency() {
        assertThrows(
                IllegalArgumentException.class,
                () -> HuffmanNode.leaf(0x41, 0)
        );
    }

    @Test
    void rejectsNegativeFrequency() {
        assertThrows(
                IllegalArgumentException.class,
                () -> HuffmanNode.leaf(0x41, -1)
        );
    }

    @Test
    void rejectsNullChildren() {
        HuffmanNode leaf = HuffmanNode.leaf(0x41, 1);

        assertThrows(
                NullPointerException.class,
                () -> HuffmanNode.internal(null, leaf)
        );
        assertThrows(
                NullPointerException.class,
                () -> HuffmanNode.internal(leaf, null)
        );
    }

    @Test
    void preventsStructurallyInvalidStates() {
        HuffmanNode leaf = HuffmanNode.leaf(0x41, 1);
        HuffmanNode internal = HuffmanNode.internal(
                leaf,
                HuffmanNode.leaf(0x42, 1)
        );

        assertTrue(leaf.leftChild().isEmpty());
        assertTrue(leaf.rightChild().isEmpty());
        assertTrue(internal.symbol().isEmpty());
        for (Constructor<?> constructor : HuffmanNode.class.getDeclaredConstructors()) {
            assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        }
    }

    @Test
    void rejectsCodePointsOutsideUnicodeRange() {
        assertThrows(
                IllegalArgumentException.class,
                () -> HuffmanNode.leaf(-1, 1)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> HuffmanNode.leaf(0x110000, 1)
        );
    }

    @Test
    void rejectsInternalFrequencyOverflow() {
        HuffmanNode maximum = HuffmanNode.leaf(0x41, Long.MAX_VALUE);
        HuffmanNode one = HuffmanNode.leaf(0x42, 1);

        assertThrows(
                ArithmeticException.class,
                () -> HuffmanNode.internal(maximum, one)
        );
    }
}

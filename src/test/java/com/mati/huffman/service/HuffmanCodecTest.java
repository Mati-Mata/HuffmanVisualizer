package com.mati.huffman.service;

import com.mati.huffman.model.HuffmanNode;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HuffmanCodecTest {

    private final HuffmanCodec codec = new HuffmanCodec();

    @Test
    void encodesAndDecodesBanana() {
        Pipeline pipeline = pipelineFor("BANANA");

        String bits = codec.encode("BANANA", pipeline.codes());

        assertEquals("100110110", bits);
        assertEquals("BANANA", codec.decode(bits, pipeline.root()));
    }

    @Test
    void roundTripsSingleSymbol() {
        assertRoundTrip("A");
    }

    @Test
    void roundTripsRepeatedSymbol() {
        assertRoundTrip("AAAAAAAA");
    }

    @Test
    void roundTripsEmptyTextWithExistingTreeAndCodes() {
        Pipeline pipeline = pipelineFor("reference");

        String bits = codec.encode("", pipeline.codes());

        assertEquals("", bits);
        assertEquals("", codec.decode(bits, pipeline.root()));
    }

    @Test
    void roundTripsSpaces() {
        assertRoundTrip("A B  C");
    }

    @Test
    void roundTripsTabs() {
        assertRoundTrip("A\tB\t\tC");
    }

    @Test
    void roundTripsLineBreaks() {
        assertRoundTrip("A\nB\r\nC");
    }

    @Test
    void roundTripsSpanishText() {
        assertRoundTrip("El pingüino comió piña y tomó café. ¡Sí, señor!");
    }

    @Test
    void roundTripsChineseText() {
        assertRoundTrip("数据压缩示例");
    }

    @Test
    void roundTripsSupplementaryCodePoints() {
        assertRoundTrip("🙂🙂🚀");
    }

    @Test
    void roundTripsMixedUnicodeText() {
        assertRoundTrip("A🙂A\nñ🙂");
    }

    @Test
    void encodeRejectsNullText() {
        assertThrows(
                NullPointerException.class,
                () -> codec.encode(null, Map.of())
        );
    }

    @Test
    void encodeRejectsNullCodes() {
        assertThrows(
                NullPointerException.class,
                () -> codec.encode("A", null)
        );
    }

    @Test
    void encodeRejectsSymbolWithoutCode() {
        assertThrows(
                IllegalArgumentException.class,
                () -> codec.encode(
                        "AB",
                        Map.of(codePointOf("A"), "0")
                )
        );
    }

    @Test
    void encodeRejectsNullCode() {
        Map<Integer, String> codes = new HashMap<>();
        codes.put(codePointOf("A"), null);

        assertThrows(
                NullPointerException.class,
                () -> codec.encode("A", codes)
        );
    }

    @Test
    void encodeRejectsEmptyCode() {
        assertThrows(
                IllegalArgumentException.class,
                () -> codec.encode(
                        "A",
                        Map.of(codePointOf("A"), "")
                )
        );
    }

    @Test
    void encodeRejectsNonBinaryCode() {
        assertThrows(
                IllegalArgumentException.class,
                () -> codec.encode(
                        "A",
                        Map.of(codePointOf("A"), "02")
                )
        );
    }

    @Test
    void decodeRejectsNullBits() {
        assertThrows(
                NullPointerException.class,
                () -> codec.decode(null, HuffmanNode.leaf(0x41, 1))
        );
    }

    @Test
    void decodeRejectsNullRoot() {
        assertThrows(
                NullPointerException.class,
                () -> codec.decode("0", null)
        );
    }

    @Test
    void decodeRejectsNonBinaryCharacters() {
        HuffmanNode root = pipelineFor("AB").root();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> codec.decode("0x1", root)
        );

        assertEquals(
                "Binary value contains a character other than 0 or 1"
                        + " at index 1",
                exception.getMessage()
        );
    }

    @Test
    void decodeRejectsInvalidPath() {
        HuffmanNode singleLeaf = HuffmanNode.leaf(codePointOf("A"), 2);

        assertThrows(
                IllegalArgumentException.class,
                () -> codec.decode("01", singleLeaf)
        );
    }

    @Test
    void decodeRejectsSequenceEndingInsideTreePath() {
        HuffmanNode root = HuffmanNode.internal(
                HuffmanNode.leaf(codePointOf("A"), 1),
                HuffmanNode.internal(
                        HuffmanNode.leaf(codePointOf("B"), 1),
                        HuffmanNode.leaf(codePointOf("C"), 1)
                )
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> codec.decode("1", root)
        );

        assertEquals(
                "Binary sequence ends in the middle of a tree path",
                exception.getMessage()
        );
    }

    @Test
    void singleLeafRejectsBitOne() {
        HuffmanNode root = HuffmanNode.leaf(codePointOf("A"), 1);

        assertThrows(
                IllegalArgumentException.class,
                () -> codec.decode("1", root)
        );
    }

    @Test
    void encodeDoesNotModifyCodeMap() {
        Pipeline pipeline = pipelineFor("BANANA");
        Map<Integer, String> mutableCodes =
                new LinkedHashMap<>(pipeline.codes());
        Map<Integer, String> original =
                new LinkedHashMap<>(mutableCodes);

        codec.encode("BANANA", mutableCodes);

        assertEquals(original, mutableCodes);
    }

    @Test
    void codecDoesNotModifyTree() {
        Pipeline pipeline = pipelineFor("A🙂A\nñ🙂");
        String before = serialize(pipeline.root());

        String bits = codec.encode("A🙂A\nñ🙂", pipeline.codes());
        codec.decode(bits, pipeline.root());

        assertEquals(before, serialize(pipeline.root()));
    }

    @Test
    void repeatedRoundTripsProduceSameResult() {
        String text = "Repetición 🙂 数据\n";
        Pipeline pipeline = pipelineFor(text);
        String expectedBits = codec.encode(text, pipeline.codes());

        for (int repetition = 0; repetition < 20; repetition++) {
            String bits = codec.encode(text, pipeline.codes());
            assertEquals(expectedBits, bits);
            assertEquals(text, codec.decode(bits, pipeline.root()));
        }
    }

    private void assertRoundTrip(String text) {
        Pipeline pipeline = pipelineFor(text);
        String bits = codec.encode(text, pipeline.codes());

        assertEquals(text, codec.decode(bits, pipeline.root()));
    }

    private static Pipeline pipelineFor(String text) {
        Map<Integer, Long> frequencies =
                new FrequencyAnalyzer().analyze(text);
        HuffmanNode root = new HuffmanTreeBuilder().build(frequencies);
        Map<Integer, String> codes =
                new HuffmanCodeGenerator().generate(root);
        return new Pipeline(root, codes);
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

    private record Pipeline(
            HuffmanNode root,
            Map<Integer, String> codes
    ) {
    }
}

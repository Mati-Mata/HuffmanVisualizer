package com.mati.huffman.service;

import com.mati.huffman.model.CompressionMetrics;
import com.mati.huffman.model.CompressionResult;
import com.mati.huffman.model.HuffmanNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HuffmanCompressionServiceTest {

    private final HuffmanCompressionService service =
            new HuffmanCompressionService();

    @Test
    void bananaProducesCompleteResult() {
        CompressionResult result = service.compress("BANANA");

        assertEquals("BANANA", result.originalText());
        assertEquals("BANANA", result.reconstructedText());
        assertEquals(3, result.frequencies().size());
        assertEquals(3, result.probabilities().size());
        assertEquals(3, result.codes().size());
        assertFalse(result.encodedBits().isEmpty());
        assertTrue(result.roundTripSuccessful());
    }

    @Test
    void roundTripsSingleSymbol() {
        assertRoundTrip("A");
    }

    @Test
    void roundTripsRepeatedText() {
        assertRoundTrip("AAAAAAAAAAAA");
    }

    @Test
    void roundTripsSpaces() {
        assertRoundTrip("A B  C ");
    }

    @Test
    void roundTripsLineBreaks() {
        assertRoundTrip("A\nB\r\nC\n");
    }

    @Test
    void roundTripsSpanishText() {
        assertRoundTrip("El pingüino comió piña y tomó café, señor.");
    }

    @Test
    void roundTripsChineseText() {
        assertRoundTrip("数据压缩示例");
    }

    @Test
    void roundTripsEmojis() {
        assertRoundTrip("🙂🙂🚀");
    }

    @Test
    void roundTripsMixedUnicodeText() {
        assertRoundTrip("A🙂A\nñ数据🙂");
    }

    @Test
    void rejectsNullText() {
        assertThrows(
                NullPointerException.class,
                () -> service.compress(null)
        );
    }

    @Test
    void rejectsEmptyText() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.compress("")
        );
    }

    @Test
    void acceptsTextContainingOnlySpaces() {
        assertRoundTrip("     ");
    }

    @Test
    void acceptsTextContainingOnlyLineBreaks() {
        assertRoundTrip("\n\r\n\n");
    }

    @Test
    void frequenciesMatchText() {
        String text = "BANANA🙂🙂";
        CompressionResult result = service.compress(text);

        assertEquals(
                new FrequencyAnalyzer().analyze(text),
                result.frequencies()
        );
    }

    @Test
    void codesMatchTree() {
        CompressionResult result = service.compress("BANANA🙂");

        assertEquals(
                new HuffmanCodeGenerator().generate(result.root()),
                result.codes()
        );
    }

    @Test
    void encodedBitsMatchCodec() {
        CompressionResult result = service.compress("BANANA🙂");

        assertEquals(
                new HuffmanCodec().encode(
                        result.originalText(),
                        result.codes()
                ),
                result.encodedBits()
        );
    }

    @Test
    void reconstructedTextIsIdentical() {
        String text = " \tA🙂\nñ数据  ";
        CompressionResult result = service.compress(text);

        assertEquals(text, result.reconstructedText());
        assertTrue(result.roundTripSuccessful());
    }

    @Test
    void metricsMatchResultData() {
        CompressionResult result = service.compress("BANANA🙂");
        CompressionMetrics expected = new MetricsCalculator().calculate(
                result.originalText(),
                result.frequencies(),
                result.codes()
        );

        assertEquals(expected, result.metrics());
        assertEquals(
                result.encodedBits().length(),
                result.metrics().huffmanMessageBitSize()
        );
    }

    @Test
    void multipleCallsDoNotShareMutableState() {
        CompressionResult first = service.compress("BANANA");
        CompressionResult second = service.compress("BANANA");

        assertNotSame(first, second);
        assertNotSame(first.root(), second.root());
        assertNotSame(first.frequencies(), second.frequencies());
        assertNotSame(first.codes(), second.codes());
        assertThrows(
                UnsupportedOperationException.class,
                () -> first.codes().clear()
        );
        assertEquals(second.codes(), first.codes());
    }

    @Test
    void sameTextProducesDeterministicResult() {
        String text = "determinismo🙂BANANA";
        CompressionResult first = service.compress(text);
        CompressionResult second = service.compress(text);

        assertEquals(first.frequencies(), second.frequencies());
        assertEquals(first.probabilities(), second.probabilities());
        assertEquals(first.codes(), second.codes());
        assertEquals(first.encodedBits(), second.encodedBits());
        assertEquals(first.metrics(), second.metrics());
        assertEquals(serialize(first.root()), serialize(second.root()));
    }

    private void assertRoundTrip(String text) {
        CompressionResult result = service.compress(text);

        assertEquals(text, result.originalText());
        assertEquals(text, result.reconstructedText());
        assertTrue(result.roundTripSuccessful());
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
}

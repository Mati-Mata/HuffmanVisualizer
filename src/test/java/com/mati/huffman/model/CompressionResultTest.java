package com.mati.huffman.model;

import com.mati.huffman.service.FrequencyAnalyzer;
import com.mati.huffman.service.HuffmanCodeGenerator;
import com.mati.huffman.service.HuffmanCodec;
import com.mati.huffman.service.HuffmanTreeBuilder;
import com.mati.huffman.service.MetricsCalculator;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompressionResultTest {

    @Test
    void constructsValidResult() {
        Fixture fixture = fixtureFor("BANANA");

        CompressionResult result = createResult(fixture);

        assertEquals("BANANA", result.originalText());
        assertEquals(fixture.encodedBits(), result.encodedBits());
    }

    @Test
    void defensivelyCopiesMaps() {
        Fixture fixture = fixtureFor("BANANA");
        Map<Integer, Long> frequencies =
                new LinkedHashMap<>(fixture.frequencies());
        Map<Integer, Double> probabilities =
                new LinkedHashMap<>(fixture.probabilities());
        Map<Integer, String> codes =
                new LinkedHashMap<>(fixture.codes());

        CompressionResult result = new CompressionResult(
                fixture.text(),
                frequencies,
                probabilities,
                fixture.root(),
                codes,
                fixture.encodedBits(),
                fixture.text(),
                fixture.metrics()
        );
        frequencies.clear();
        probabilities.clear();
        codes.clear();

        assertEquals(fixture.frequencies(), result.frequencies());
        assertEquals(fixture.probabilities(), result.probabilities());
        assertEquals(fixture.codes(), result.codes());
    }

    @Test
    void exposesUnmodifiableMaps() {
        CompressionResult result = createResult(fixtureFor("BANANA"));

        assertThrows(
                UnsupportedOperationException.class,
                () -> result.frequencies().clear()
        );
        assertThrows(
                UnsupportedOperationException.class,
                () -> result.probabilities().clear()
        );
        assertThrows(
                UnsupportedOperationException.class,
                () -> result.codes().clear()
        );
    }

    @Test
    void rejectsNullRequiredValues() {
        Fixture fixture = fixtureFor("BANANA");

        assertThrows(
                NullPointerException.class,
                () -> new CompressionResult(
                        null,
                        fixture.frequencies(),
                        fixture.probabilities(),
                        fixture.root(),
                        fixture.codes(),
                        fixture.encodedBits(),
                        fixture.text(),
                        fixture.metrics()
                )
        );
        assertThrows(
                NullPointerException.class,
                () -> new CompressionResult(
                        fixture.text(),
                        null,
                        fixture.probabilities(),
                        fixture.root(),
                        fixture.codes(),
                        fixture.encodedBits(),
                        fixture.text(),
                        fixture.metrics()
                )
        );
        assertThrows(
                NullPointerException.class,
                () -> new CompressionResult(
                        fixture.text(),
                        fixture.frequencies(),
                        null,
                        fixture.root(),
                        fixture.codes(),
                        fixture.encodedBits(),
                        fixture.text(),
                        fixture.metrics()
                )
        );
        assertThrows(
                NullPointerException.class,
                () -> new CompressionResult(
                        fixture.text(),
                        fixture.frequencies(),
                        fixture.probabilities(),
                        null,
                        fixture.codes(),
                        fixture.encodedBits(),
                        fixture.text(),
                        fixture.metrics()
                )
        );
        assertThrows(
                NullPointerException.class,
                () -> new CompressionResult(
                        fixture.text(),
                        fixture.frequencies(),
                        fixture.probabilities(),
                        fixture.root(),
                        null,
                        fixture.encodedBits(),
                        fixture.text(),
                        fixture.metrics()
                )
        );
        assertThrows(
                NullPointerException.class,
                () -> new CompressionResult(
                        fixture.text(),
                        fixture.frequencies(),
                        fixture.probabilities(),
                        fixture.root(),
                        fixture.codes(),
                        null,
                        fixture.text(),
                        fixture.metrics()
                )
        );
        assertThrows(
                NullPointerException.class,
                () -> new CompressionResult(
                        fixture.text(),
                        fixture.frequencies(),
                        fixture.probabilities(),
                        fixture.root(),
                        fixture.codes(),
                        fixture.encodedBits(),
                        null,
                        fixture.metrics()
                )
        );
        assertThrows(
                NullPointerException.class,
                () -> new CompressionResult(
                        fixture.text(),
                        fixture.frequencies(),
                        fixture.probabilities(),
                        fixture.root(),
                        fixture.codes(),
                        fixture.encodedBits(),
                        fixture.text(),
                        null
                )
        );
    }

    @Test
    void reportsSuccessfulRoundTrip() {
        assertTrue(createResult(fixtureFor("A🙂A")).roundTripSuccessful());
    }

    @Test
    void preservesOriginalText() {
        String text = "  A🙂\nñ  ";

        assertEquals(text, createResult(fixtureFor(text)).originalText());
    }

    @Test
    void preservesReconstructedText() {
        String text = "数据🙂\n";

        assertEquals(
                text,
                createResult(fixtureFor(text)).reconstructedText()
        );
    }

    @Test
    void exposesRootAndMetrics() {
        Fixture fixture = fixtureFor("BANANA");
        CompressionResult result = createResult(fixture);

        assertSame(fixture.root(), result.root());
        assertSame(fixture.metrics(), result.metrics());
    }

    @Test
    void rejectsIncoherentStructures() {
        Fixture fixture = fixtureFor("BANANA");

        assertThrows(
                IllegalArgumentException.class,
                () -> new CompressionResult(
                        fixture.text(),
                        fixture.frequencies(),
                        fixture.probabilities(),
                        fixture.root(),
                        fixture.codes(),
                        fixture.encodedBits(),
                        "BANAN",
                        fixture.metrics()
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new CompressionResult(
                        fixture.text(),
                        Map.of(codePointOf("A"), 6L),
                        fixture.probabilities(),
                        fixture.root(),
                        fixture.codes(),
                        fixture.encodedBits(),
                        fixture.text(),
                        fixture.metrics()
                )
        );
    }

    private static CompressionResult createResult(Fixture fixture) {
        return new CompressionResult(
                fixture.text(),
                fixture.frequencies(),
                fixture.probabilities(),
                fixture.root(),
                fixture.codes(),
                fixture.encodedBits(),
                fixture.reconstructedText(),
                fixture.metrics()
        );
    }

    private static Fixture fixtureFor(String text) {
        FrequencyAnalyzer analyzer = new FrequencyAnalyzer();
        Map<Integer, Long> frequencies = analyzer.analyze(text);
        HuffmanNode root = new HuffmanTreeBuilder().build(frequencies);
        Map<Integer, String> codes =
                new HuffmanCodeGenerator().generate(root);
        HuffmanCodec codec = new HuffmanCodec();
        String encodedBits = codec.encode(text, codes);
        String reconstructed = codec.decode(encodedBits, root);
        MetricsCalculator calculator = new MetricsCalculator();

        return new Fixture(
                text,
                frequencies,
                calculator.calculateProbabilities(frequencies),
                root,
                codes,
                encodedBits,
                reconstructed,
                calculator.calculate(text, frequencies, codes)
        );
    }

    private static int codePointOf(String symbol) {
        return symbol.codePointAt(0);
    }

    private record Fixture(
            String text,
            Map<Integer, Long> frequencies,
            Map<Integer, Double> probabilities,
            HuffmanNode root,
            Map<Integer, String> codes,
            String encodedBits,
            String reconstructedText,
            CompressionMetrics metrics
    ) {
    }
}

package com.mati.huffman.service;

import com.mati.huffman.model.CompressionMetrics;
import com.mati.huffman.model.CompressionResult;
import com.mati.huffman.model.HuffmanNode;

import java.util.Map;
import java.util.Objects;

/**
 * Orchestrates one complete Huffman compression and verification pipeline.
 */
public final class HuffmanCompressionService {

    private final FrequencyAnalyzer frequencyAnalyzer;
    private final HuffmanTreeBuilder treeBuilder;
    private final HuffmanCodeGenerator codeGenerator;
    private final HuffmanCodec codec;
    private final MetricsCalculator metricsCalculator;

    public HuffmanCompressionService() {
        this.frequencyAnalyzer = new FrequencyAnalyzer();
        this.treeBuilder = new HuffmanTreeBuilder();
        this.codeGenerator = new HuffmanCodeGenerator();
        this.codec = new HuffmanCodec();
        this.metricsCalculator = new MetricsCalculator();
    }

    /**
     * Compresses, reconstructs, verifies, and measures non-empty text.
     */
    public CompressionResult compress(String text) {
        Objects.requireNonNull(text, "Text must not be null");
        if (text.isEmpty()) {
            throw new IllegalArgumentException(
                    "Text must not be empty"
            );
        }

        Map<Integer, Long> frequencies = frequencyAnalyzer.analyze(text);
        HuffmanNode root = treeBuilder.build(frequencies);
        Map<Integer, String> codes = codeGenerator.generate(root);
        String encodedBits = codec.encode(text, codes);
        String reconstructedText = codec.decode(encodedBits, root);
        if (!text.equals(reconstructedText)) {
            throw new IllegalStateException(
                    "Huffman round-trip verification failed"
            );
        }

        Map<Integer, Double> probabilities =
                metricsCalculator.calculateProbabilities(frequencies);
        CompressionMetrics metrics = metricsCalculator.calculate(
                text,
                frequencies,
                codes
        );

        return new CompressionResult(
                text,
                frequencies,
                probabilities,
                root,
                codes,
                encodedBits,
                reconstructedText,
                metrics
        );
    }
}

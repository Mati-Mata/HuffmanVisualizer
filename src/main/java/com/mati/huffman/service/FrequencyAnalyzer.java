package com.mati.huffman.service;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Counts Unicode code-point occurrences in text.
 */
public final class FrequencyAnalyzer {

    /**
     * Counts every Unicode code point without normalizing or otherwise
     * transforming the supplied text.
     *
     * @param text text to analyze; must not be {@code null}
     * @return an unmodifiable map ordered by ascending Unicode code point
     * @throws NullPointerException if {@code text} is {@code null}
     */
    public Map<Integer, Long> analyze(String text) {
        Objects.requireNonNull(text, "Text must not be null");

        Map<Integer, Long> frequencies = new TreeMap<>();
        text.codePoints().forEach(codePoint ->
                frequencies.merge(codePoint, 1L, Math::addExact)
        );

        return Collections.unmodifiableMap(frequencies);
    }
}

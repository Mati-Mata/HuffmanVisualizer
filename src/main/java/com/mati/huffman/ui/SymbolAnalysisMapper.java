package com.mati.huffman.ui;

import com.mati.huffman.model.CompressionResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Converts an immutable compression result into presentation rows.
 */
public final class SymbolAnalysisMapper {

    public List<SymbolAnalysisRow> rowsFor(CompressionResult result) {
        Objects.requireNonNull(result, "Compression result must not be null");

        List<SymbolAnalysisRow> rows = new ArrayList<>(
                result.frequencies().size()
        );
        for (Map.Entry<Integer, Long> entry
                : result.frequencies().entrySet()) {
            int codePoint = entry.getKey();
            rows.add(new SymbolAnalysisRow(
                    codePoint,
                    SymbolFormatter.displaySymbol(codePoint),
                    SymbolFormatter.unicodeLabel(codePoint),
                    entry.getValue(),
                    result.probabilities().get(codePoint),
                    result.codes().get(codePoint)
            ));
        }
        return List.copyOf(rows);
    }
}

package com.mati.huffman.ui;

import com.mati.huffman.model.CompressionResult;
import com.mati.huffman.service.HuffmanCompressionService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SymbolAnalysisMapperTest {

    private final SymbolAnalysisMapper mapper = new SymbolAnalysisMapper();

    @Test
    void convertsResultToRowsInCodePointOrder() {
        CompressionResult result =
                new HuffmanCompressionService().compress("ñ A🙂\n\t");

        List<SymbolAnalysisRow> rows = mapper.rowsFor(result);
        List<Integer> codePoints = rows.stream()
                .map(SymbolAnalysisRow::codePoint)
                .toList();

        assertEquals(codePoints.stream().sorted().toList(), codePoints);
        assertEquals(result.frequencies().size(), rows.size());
    }

    @Test
    void representsInvisibleAndSupplementarySymbols() {
        CompressionResult result =
                new HuffmanCompressionService().compress(" \t\n\r🙂");

        List<SymbolAnalysisRow> rows = mapper.rowsFor(result);

        assertEquals(
                "SPACE",
                rowFor(rows, 0x20).symbol()
        );
        assertEquals("TAB", rowFor(rows, 0x09).symbol());
        assertEquals("LF", rowFor(rows, 0x0A).symbol());
        assertEquals("CR", rowFor(rows, 0x0D).symbol());
        int smilingFace = "🙂".codePointAt(0);
        assertEquals("🙂", rowFor(rows, smilingFace).symbol());
        assertEquals(
                "U+1F642",
                rowFor(rows, smilingFace).unicodeCode()
        );
    }

    @Test
    void preservesAnalysisValuesWithoutRounding() {
        CompressionResult result =
                new HuffmanCompressionService().compress("BANANA");
        SymbolAnalysisRow row = rowFor(
                mapper.rowsFor(result),
                "N".codePointAt(0)
        );

        assertEquals(result.frequencies().get(row.codePoint()), row.frequency());
        assertEquals(
                result.probabilities().get(row.codePoint()),
                row.probability()
        );
        assertEquals(result.codes().get(row.codePoint()), row.huffmanCode());
    }

    @Test
    void returnsUnmodifiableRows() {
        List<SymbolAnalysisRow> rows = mapper.rowsFor(
                new HuffmanCompressionService().compress("AB")
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> rows.clear()
        );
    }

    @Test
    void rejectsNullResult() {
        assertThrows(
                NullPointerException.class,
                () -> mapper.rowsFor(null)
        );
    }

    private static SymbolAnalysisRow rowFor(
            List<SymbolAnalysisRow> rows,
            int codePoint
    ) {
        return rows.stream()
                .filter(row -> row.codePoint() == codePoint)
                .findFirst()
                .orElseThrow();
    }
}

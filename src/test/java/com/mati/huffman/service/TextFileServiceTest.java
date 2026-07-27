package com.mati.huffman.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TextFileServiceTest {

    @TempDir
    Path temporaryDirectory;

    private final TextFileService service = new TextFileService();

    @Test
    void readsAsciiUtf8() throws IOException {
        assertReadsExactly("BANANA");
    }

    @Test
    void readsSpanishAccents() throws IOException {
        assertReadsExactly("Árbol, canción, pingüino y ñandú");
    }

    @Test
    void readsChineseText() throws IOException {
        assertReadsExactly("你好，世界");
    }

    @Test
    void readsSupplementaryEmoji() throws IOException {
        assertReadsExactly("🙂🙂🚀");
    }

    @Test
    void preservesSpaces() throws IOException {
        assertReadsExactly("  A   B  ");
    }

    @Test
    void preservesTabs() throws IOException {
        assertReadsExactly("\tA\t\tB\t");
    }

    @Test
    void preservesDifferentLineEndings() throws IOException {
        assertReadsExactly("primera\r\nsegunda\ntercera\rcuarta");
    }

    @Test
    void readsEmptyFileAsEmptyText() throws IOException {
        assertReadsExactly("");
    }

    @Test
    void rejectsNullPath() {
        assertThrows(NullPointerException.class,
                () -> service.readUtf8(null));
    }

    @Test
    void rejectsMissingFile() {
        Path missing = temporaryDirectory.resolve("missing.txt");

        assertThrows(IOException.class,
                () -> service.readUtf8(missing));
    }

    @Test
    void rejectsDirectory() {
        assertThrows(IOException.class,
                () -> service.readUtf8(temporaryDirectory));
    }

    @Test
    void rejectsFileOverLimitWithoutDecodingIt() throws IOException {
        Path file = temporaryDirectory.resolve("large.txt");
        byte[] oversized = new byte[
                Math.toIntExact(TextFileService.MAX_FILE_SIZE_BYTES + 1)
        ];
        Files.write(file, oversized);

        assertThrows(IOException.class,
                () -> service.readUtf8(file));
    }

    @Test
    void rejectsMalformedUtf8() throws IOException {
        Path file = temporaryDirectory.resolve("malformed.txt");
        Files.write(file, new byte[]{(byte) 0xC3, (byte) 0x28});

        assertThrows(CharacterCodingException.class,
                () -> service.readUtf8(file));
    }

    @Test
    void preservesMixedContentExactly() throws IOException {
        assertReadsExactly(" Inicio\t🙂\r\n中\nñ  fin ");
    }

    @Test
    void consecutiveCallsDoNotShareState() throws IOException {
        Path first = write("first.txt", "primero");
        Path second = write("second.txt", "segundo🙂");

        assertEquals("primero", service.readUtf8(first));
        assertEquals("segundo🙂", service.readUtf8(second));
        assertEquals("primero", service.readUtf8(first));
    }

    private void assertReadsExactly(String content) throws IOException {
        Path file = write("sample.txt", content);
        assertEquals(content, service.readUtf8(file));
    }

    private Path write(String name, String content) throws IOException {
        Path file = temporaryDirectory.resolve(name);
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }
}

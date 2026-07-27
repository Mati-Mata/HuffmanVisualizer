package com.mati.huffman.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Validates and reads small text files using strict UTF-8 decoding.
 */
public final class TextFileService {

    public static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;
    public static final int MAX_FILE_SIZE_MIB = 5;

    /**
     * Reads a regular, readable file without replacing malformed UTF-8 input.
     *
     * @param path file to read
     * @return the exact decoded content
     * @throws IOException if validation or reading fails
     * @throws CharacterCodingException if the bytes are not valid UTF-8
     */
    public String readUtf8(Path path) throws IOException {
        Objects.requireNonNull(path, "File path must not be null");

        if (!Files.exists(path)) {
            throw new NoSuchFileException(path.toString());
        }
        if (!Files.isRegularFile(path)) {
            throw new IOException("The selected path is not a regular file");
        }
        if (!Files.isReadable(path)) {
            throw new IOException("The selected file is not readable");
        }

        long declaredSize = Files.size(path);
        if (declaredSize > MAX_FILE_SIZE_BYTES) {
            throw fileTooLarge(declaredSize);
        }

        byte[] bytes;
        try (InputStream input = Files.newInputStream(path)) {
            bytes = input.readNBytes(
                    Math.toIntExact(MAX_FILE_SIZE_BYTES + 1)
            );
        }
        if (bytes.length > MAX_FILE_SIZE_BYTES) {
            throw fileTooLarge(bytes.length);
        }

        return StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString();
    }

    private static IOException fileTooLarge(long actualBytes) {
        return new IOException(
                "File exceeds the " + MAX_FILE_SIZE_MIB
                        + " MiB limit (actual size: "
                        + actualBytes + " bytes)"
        );
    }
}

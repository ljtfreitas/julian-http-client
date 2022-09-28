/*
 * Copyright (C) 2021 Tiago de Freitas Lima
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.github.ljtfreitas.julian.http;

import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Optional;

import static com.github.ljtfreitas.julian.Preconditions.isTrue;
import static com.github.ljtfreitas.julian.Preconditions.nonNull;

public interface Upload<T> {

    String name();

    T content();

    Optional<String> fileName();

    Optional<MediaType> mediaType();

    static Upload<byte[]> ofBytes(byte[] content, String name) {
        return new ByteArrayUpload(content, name);
    }

    static Upload<byte[]> ofBytes(byte[] content, String name, String fileName) {
        return new ByteArrayUpload(content, name, fileName);
    }

    static Upload<byte[]> ofBytes(byte[] content, String name, String fileName, MediaType mediaType) {
        return new ByteArrayUpload(content, name, fileName, mediaType);
    }

    static Upload<ByteBuffer> ofByteBuffer(ByteBuffer content, String name) {
        return new ByteBufferUpload(content, name);
    }

    static Upload<ByteBuffer> ofByteBuffer(ByteBuffer content, String name, String fileName) {
        return new ByteBufferUpload(content, name, fileName);
    }

    static Upload<ByteBuffer> ofByteBuffer(ByteBuffer content, String name, String fileName, MediaType mediaType) {
        return new ByteBufferUpload(content, name, fileName, mediaType);
    }

    static Upload<File> ofFile(File file, String name) {
        return new FileUpload(file, name);
    }

    static Upload<File> ofFile(File file, String name, String fileName) {
        return new FileUpload(file, name, fileName);
    }

    static Upload<File> ofFile(File file, String name, String fileName, MediaType mediaType) {
        return new FileUpload(file, name, fileName, mediaType);
    }

    static Upload<InputStream> ofInputStream(InputStream content, String name) {
        return new InputStreamUpload(content, name);
    }

    static Upload<InputStream> ofInputStream(InputStream content, String name, String fileName) {
        return new InputStreamUpload(content, name, fileName);
    }

    static Upload<InputStream> ofInputStream(InputStream content, String name, String fileName, MediaType mediaType) {
        return new InputStreamUpload(content, name, fileName, mediaType);
    }

    static Upload<Path> ofPath(Path path, String name) {
        return new PathUpload(path, name);
    }

    static Upload<Path> ofPath(Path path, String name, String fileName) {
        return new PathUpload(path, name, fileName);
    }

    static Upload<Path> ofPath(Path path, String name, String fileName, MediaType mediaType) {
        return new PathUpload(path, name, fileName, mediaType);
    }

    abstract class UploadContent<T> implements Upload<T> {

        private final T content;
        private final String name;
        private final Optional<String> fileName;
        private final Optional<MediaType> mediaType;

        private UploadContent(T content, String name) {
            this(content, name, null, null);
        }

        private UploadContent(T content, String name, String fileName) {
            this(content, name, fileName, null);
        }

        private UploadContent(T content, String name, String fileName, MediaType mediaType) {
            this.content = nonNull(content);
            this.name = nonNull(name);
            this.fileName = Optional.ofNullable(fileName);
            this.mediaType = Optional.ofNullable(mediaType);
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public T content() {
            return content;
        }

        @Override
        public Optional<String> fileName() {
            return fileName;
        }

        @Override
        public Optional<MediaType> mediaType() {
            return mediaType;
        }
    }

    class ByteArrayUpload extends UploadContent<byte[]> {

        private ByteArrayUpload(byte[] content, String name) {
            super(content, name);
        }

        private ByteArrayUpload(byte[] content, String name, String fileName) {
            super(content, name, fileName);
        }

        private ByteArrayUpload(byte[] content, String name, String fileName, MediaType mediaType) {
            super(content, name, fileName, mediaType);
        }
    }

    class ByteBufferUpload extends UploadContent<ByteBuffer> {

        private ByteBufferUpload(ByteBuffer content, String name) {
            super(content, name);
        }

        private ByteBufferUpload(ByteBuffer content, String name, String fileName) {
            super(content, name, fileName);
        }

        private ByteBufferUpload(ByteBuffer content, String name, String fileName, MediaType mediaType) {
            super(content, name, fileName, mediaType);
        }
    }

    class FileUpload extends UploadContent<File> {

        private FileUpload(File content, String name) {
            super(content, name);
        }

        private FileUpload(File content, String name, String fileName) {
            super(content, name, fileName);
        }

        private FileUpload(File content, String name, String fileName, MediaType mediaType) {
            super(isTrue(content, File::isFile, () -> "File should be a file...not a directory."), name, fileName, mediaType);
        }
    }

    class InputStreamUpload extends UploadContent<InputStream> {

        private InputStreamUpload(InputStream content, String name) {
            super(content, name);
        }

        private InputStreamUpload(InputStream content, String name, String fileName) {
            super(content, name, fileName);
        }

        private InputStreamUpload(InputStream content, String name, String fileName, MediaType mediaType) {
            super(content, name, fileName, mediaType);
        }
    }

    class PathUpload extends UploadContent<Path> {

        private PathUpload(Path content, String name) {
            super(content, name);
        }

        private PathUpload(Path content, String name, String fileName) {
            super(content, name, fileName);
        }

        private PathUpload(Path content, String name, String fileName, MediaType mediaType) {
            super(isTrue(content, (p -> p.toFile().isFile()), () -> "Path should be a file."), name, fileName, mediaType);
        }
    }
}

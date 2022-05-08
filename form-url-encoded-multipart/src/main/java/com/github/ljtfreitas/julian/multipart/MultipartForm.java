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

package com.github.ljtfreitas.julian.multipart;

import com.github.ljtfreitas.julian.http.MediaType;

import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableCollection;

public class MultipartForm {

    private final Collection<Part> parts;

    public MultipartForm() {
        this.parts = emptyList();
    }

    public MultipartForm(Collection<Part> parts) {
        this.parts = unmodifiableCollection(parts);
    }

    public Optional<Part> select(String name) {
        return parts.stream().filter(part -> part.is(name)).findFirst();
    }

    public MultipartForm join(Part... parts) {
        return join(asList(parts));
    }

    public MultipartForm join(Collection<Part> parts) {
        Collection<Part> all = new ArrayList<>(this.parts);
        all.addAll(parts);

        return new MultipartForm(all);
    }

    public Collection<Part> parts() {
        return parts;
    }

    public static class Part {

        private final String name;
        private final Collection<Object> values;
        private final String fileName;
        private final MediaType contentType;

        private Part(String name, Object value, String fileName, MediaType contentType) {
            this(name, Collections.singleton(value), fileName, contentType);
        }

        private Part(String name, Collection<Object> values, String fileName, MediaType contentType) {
            this.name = name;
            this.values = values;
            this.fileName = fileName;
            this.contentType = contentType;
        }

        public String name() {
            return name;
        }

        public Collection<Object> values() {
            return values;
        }

        public String fileName() {
            return fileName;
        }

        public MediaType mediaType() {
            return contentType;
        }

        public boolean is(String name) {
            return this.name.equals(name);
        }

        public static Part create(String name, String value) {
            return create(name, value, null);
        }

        public static Part create(String name, String value, String fileName) {
            return new Part(name, value, fileName, MediaType.valueOf("text/plain"));
        }

        public static Part create(String name, File file) {
            return create(name, file, null);
        }

        public static Part create(String name, File file, String fileName) {
            return create(name, file, fileName, null);
        }

        public static Part create(String name, File file, String fileName, MediaType mediaType) {
            return new Part(name, file, fileName, mediaType);
        }

        public static Part create(String name, Path path, String fileName) {
            return create(name, path, fileName, null);
        }

        public static Part create(String name, Path path, String fileName, MediaType mediaType) {
            return new Part(name, path, fileName, mediaType);
        }

        public static Part create(String name, InputStream stream, String fileName) {
            return create(name, stream, fileName, null);
        }

        public static Part create(String name, InputStream stream, String fileName, MediaType mediaType) {
            return new Part(name, stream, fileName, mediaType);
        }

        public static Part create(String name, byte[] content, String fileName) {
            return create(name, content, fileName, null);
        }

        public static Part create(String name, byte[] content, String fileName, MediaType mediaType) {
            return new Part(name, content, fileName, mediaType);
        }

        public static Part create(String name, ByteBuffer buffer, String fileName) {
            return create(name, buffer, fileName, null);
        }

        public static Part create(String name, ByteBuffer buffer, String fileName, MediaType mediaType) {
            return new Part(name, buffer, fileName, mediaType);
        }
    }
}

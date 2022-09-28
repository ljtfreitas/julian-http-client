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

package com.github.ljtfreitas.julian.http.codec.form.multipart;

import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.http.HTTPRequestBody;
import com.github.ljtfreitas.julian.http.MediaType;
import com.github.ljtfreitas.julian.http.codec.MultipartFormDataHTTPRequestWriter;
import com.github.ljtfreitas.julian.multipart.MultipartForm;
import com.github.ljtfreitas.julian.multipart.MultipartForm.Part;

import java.io.File;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toUnmodifiableList;

public class FileUploadHTTPRequestWriter implements MultipartFormDataHTTPRequestWriter<File> {

    private static final FileUploadHTTPRequestWriter SINGLE_INSTANCE = new FileUploadHTTPRequestWriter();

    private final MapMultipartFormHTTPRequestWriter mapMultipartFormHTTPRequestWriter;

    public FileUploadHTTPRequestWriter() {
        this(BoundaryGen.RANDOM);
    }

    public FileUploadHTTPRequestWriter(BoundaryGen boundaryGen) {
        this.mapMultipartFormHTTPRequestWriter = new MapMultipartFormHTTPRequestWriter(boundaryGen);
    }

    @Override
    public boolean writable(MediaType candidate, JavaType javaType) {
        return supports(candidate) && javaType.is(File.class);
    }

    @Override
    public HTTPRequestBody write(File file, Charset encoding) {
        Map<String, MultipartForm.Part> form = Map.of("file", MultipartForm.Part.create("file", file));
        return mapMultipartFormHTTPRequestWriter.write(form, encoding);
    }

    public static FileUploadHTTPRequestWriter provider() {
        return SINGLE_INSTANCE;
    }
}

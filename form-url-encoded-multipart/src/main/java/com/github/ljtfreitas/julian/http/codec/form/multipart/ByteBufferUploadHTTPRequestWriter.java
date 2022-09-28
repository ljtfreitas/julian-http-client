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
import com.github.ljtfreitas.julian.multipart.MultipartForm.Part;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Map;

public class ByteBufferUploadHTTPRequestWriter implements MultipartFormDataHTTPRequestWriter<ByteBuffer> {

    private static final ByteBufferUploadHTTPRequestWriter SINGLE_INSTANCE = new ByteBufferUploadHTTPRequestWriter();

    private final MapMultipartFormHTTPRequestWriter mapMultipartFormHTTPRequestWriter;

    public ByteBufferUploadHTTPRequestWriter() {
        this(BoundaryGen.RANDOM);
    }

    public ByteBufferUploadHTTPRequestWriter(BoundaryGen boundaryGen) {
        this.mapMultipartFormHTTPRequestWriter = new MapMultipartFormHTTPRequestWriter(boundaryGen);
    }

    @Override
    public boolean writable(MediaType candidate, JavaType javaType) {
        return supports(candidate) && javaType.is(ByteBuffer.class);
    }

    @Override
    public HTTPRequestBody write(ByteBuffer contentAsBuffer, Charset encoding) {
        Map<String, Part> form = Map.of("file", Part.create("file", contentAsBuffer));
        return mapMultipartFormHTTPRequestWriter.write(form, encoding);
    }

    public static ByteBufferUploadHTTPRequestWriter provider() {
        return SINGLE_INSTANCE;
    }
}

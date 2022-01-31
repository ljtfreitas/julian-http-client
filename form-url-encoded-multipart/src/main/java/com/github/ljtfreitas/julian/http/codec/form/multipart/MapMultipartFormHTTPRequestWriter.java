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
import com.github.ljtfreitas.julian.http.DefaultHTTPRequestBody;
import com.github.ljtfreitas.julian.http.HTTPRequestBody;
import com.github.ljtfreitas.julian.http.MediaType;
import com.github.ljtfreitas.julian.http.codec.HTTPRequestWriterException;
import com.github.ljtfreitas.julian.http.codec.MultipartFormDataHTTPRequestWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.http.HttpRequest.BodyPublishers;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.Flow.Publisher;

public class MapMultipartFormHTTPRequestWriter implements MultipartFormDataHTTPRequestWriter<Map<String, ?>> {

    private static final MapMultipartFormHTTPRequestWriter SINGLE_INSTANCE = new MapMultipartFormHTTPRequestWriter();

    private final BoundaryGen boundaryGen;
    private final MultipartFormFieldSerializers serializers = new MultipartFormFieldSerializers();

    public MapMultipartFormHTTPRequestWriter() {
        this(BoundaryGen.RANDOM);
    }

    public MapMultipartFormHTTPRequestWriter(BoundaryGen boundaryGen) {
        this.boundaryGen = boundaryGen;
    }

    @Override
    public boolean writable(MediaType candidate, JavaType javaType) {
        return false;
    }

    @Override
    public HTTPRequestBody write(Map<String, ?> map, Charset encoding) {
        String boundary = boundaryGen.run();

        MediaType mediaType = MediaType.MULTIPART_FORM_DATA.parameter("boundary", boundary);

        return new DefaultHTTPRequestBody(mediaType, () -> serialize(map, encoding, "----" + boundary));
    }

    @SuppressWarnings("rawtypes")
    private Publisher<ByteBuffer> serialize(Map<String, ?> form, Charset encoding, String boundary) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            form.forEach((name, value) -> serializers.select(value.getClass())
                    .write(boundary, new MultipartFormField(name, value), encoding, output));

            output.write('\r');
            output.write('\n');
            output.write((boundary + "--").getBytes());

            output.flush();
            output.close();

            return BodyPublishers.ofByteArray(output.toByteArray());

        } catch (IOException e) {
            throw new HTTPRequestWriterException("Multipart/form serialization failed.", e);
        }
    }

    public static MapMultipartFormHTTPRequestWriter provider() {
        return SINGLE_INSTANCE;
    }
}

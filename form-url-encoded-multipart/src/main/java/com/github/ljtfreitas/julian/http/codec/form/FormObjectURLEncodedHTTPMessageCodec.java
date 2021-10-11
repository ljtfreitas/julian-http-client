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

package com.github.ljtfreitas.julian.http.codec.form;

import com.github.ljtfreitas.julian.Form;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.http.DefaultHTTPRequestBody;
import com.github.ljtfreitas.julian.http.HTTPRequestBody;
import com.github.ljtfreitas.julian.http.MediaType;
import com.github.ljtfreitas.julian.http.codec.FormURLEncodedHTTPMessageCodec;
import com.github.ljtfreitas.julian.http.codec.HTTPResponseReaderException;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.http.HttpRequest.BodyPublishers;
import java.nio.charset.Charset;
import java.util.stream.Collectors;

public class FormObjectURLEncodedHTTPMessageCodec implements FormURLEncodedHTTPMessageCodec<Form> {

    @Override
    public boolean writable(MediaType candidate, JavaType javaType) {
        return supports(candidate) && javaType.is(Form.class);
    }

    @Override
    public HTTPRequestBody write(Form form, Charset encoding) {
        return new DefaultHTTPRequestBody(FORM_URL_ENCODED_MEDIA_TYPE, () -> BodyPublishers.ofString(form.serialize(), encoding));
    }

    @Override
    public boolean readable(MediaType candidate, JavaType javaType) {
        return supports(candidate) && javaType.is(Form.class);
    }

    @Override
    public Form read(byte[] body, JavaType javaType) {
        try (InputStream stream = new ByteArrayInputStream(body);
            InputStreamReader reader = new InputStreamReader(stream);
            BufferedReader buffer = new BufferedReader(reader)) {

            String content = buffer.lines().collect(Collectors.joining("\n"));

            return Form.parse(content);

        } catch (IOException e) {
            throw new HTTPResponseReaderException("Form deserialization failed. The target type was: " + javaType, e);
        }
    }
}

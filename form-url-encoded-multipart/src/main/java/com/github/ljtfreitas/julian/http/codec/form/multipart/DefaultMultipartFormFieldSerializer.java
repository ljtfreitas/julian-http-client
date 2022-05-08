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

import com.github.ljtfreitas.julian.Message;
import com.github.ljtfreitas.julian.http.MediaType;
import com.github.ljtfreitas.julian.http.codec.HTTPRequestWriterException;

import java.io.OutputStream;
import java.nio.charset.Charset;

class DefaultMultipartFormFieldSerializer implements MultipartFormFieldSerializer<Object> {

    public static final MediaType MEDIA_TYPE_TEXT_PLAIN = MediaType.valueOf("text/plain");

    @Override
    public boolean supports(Class<?> candidate) {
        return true;
    }

    @Override
    public void write(String boundary, MultipartFormField<Object> field, Charset charset, OutputStream output) {
        ContentDisposition contentDisposition = new ContentDisposition(field.name, field.fileName.orElse(null));

        new MultipartFormFieldWriter(output, boundary, contentDisposition, MEDIA_TYPE_TEXT_PLAIN)
                .write(o -> o.write(field.value.toString().getBytes(charset)))
                .prop(e -> new HTTPRequestWriterException(Message.format("Cannot write multipart/form-data field {0}", field.name), e));
    }
}

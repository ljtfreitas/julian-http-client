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

import com.github.ljtfreitas.julian.multipart.MultipartForm;

import java.io.OutputStream;
import java.nio.charset.Charset;

class PartSerializer implements MultipartFormFieldSerializer<MultipartForm.Part> {

    private final MultipartFormFieldSerializers serializers;

    PartSerializer(MultipartFormFieldSerializers serializers) {
        this.serializers = serializers;
    }

    @Override
    public boolean supports(Class<?> candidate) {
        return MultipartForm.Part.class.equals(candidate);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void write(String boundary, MultipartFormField<MultipartForm.Part> field, Charset charset, OutputStream output) {
        MultipartForm.Part part = field.value;

        part.values().forEach(v -> serializers.select(v.getClass())
                .write(boundary, new MultipartFormField(field.name, v, part.fileName(), part.mediaType()), charset, output));
    }
}

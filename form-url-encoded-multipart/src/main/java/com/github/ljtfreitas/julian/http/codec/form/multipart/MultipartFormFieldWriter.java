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

import java.io.IOException;
import java.io.OutputStream;

import com.github.ljtfreitas.julian.Except;
import com.github.ljtfreitas.julian.Except.ThrowableConsumer;
import com.github.ljtfreitas.julian.http.HTTPHeader;
import com.github.ljtfreitas.julian.http.MediaType;

class MultipartFormFieldWriter {

    private static final String CONTENT_TYPE_HEADER_NAME = HTTPHeader.CONTENT_TYPE + ": ";

    private final OutputStream output;
    private final String boundary;
    private final ContentDisposition contentDisposition;
    private final MediaType mediaType;

    MultipartFormFieldWriter(OutputStream output, String boundary, ContentDisposition contentDisposition, MediaType mediaType) {
        this.output = output;
        this.boundary = boundary;
        this.contentDisposition = contentDisposition;
        this.mediaType = mediaType;
    }

    Except<Void> write(ThrowableConsumer<OutputStream> content) {
        return Except.just(() -> {
            output.write(boundary.getBytes());
            output.write('\r');
            output.write('\n');

            headers(contentDisposition, mediaType);

            output.write('\r');
            output.write('\n');

            content.accept(output);

            output.write('\r');
            output.write('\n');

            output.flush();
        });
    }

    private void headers(ContentDisposition contentDisposition, MediaType mediaType) throws IOException {
        output.write(contentDisposition.header().getBytes());
        output.write('\r');
        output.write('\n');

        if (mediaType != null) {
            output.write(CONTENT_TYPE_HEADER_NAME.getBytes());
            output.write(mediaType.toString().getBytes());
            output.write('\r');
            output.write('\n');
        }
    }
}

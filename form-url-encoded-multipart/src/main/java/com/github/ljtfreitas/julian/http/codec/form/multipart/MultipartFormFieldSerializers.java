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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

class MultipartFormFieldSerializers {

    private final Collection<MultipartFormFieldSerializer<?>> serializers = new ArrayList<>();

    MultipartFormFieldSerializers() {
        serializers.add(ByteArraySerializer.get());
        serializers.add(ByteBufferSerializer.get());
        serializers.add(FileSerializer.get());
        serializers.add(InputStreamSerializer.get());
        serializers.add(PathSerializer.get());
        serializers.add(new PartSerializer(this));
        serializers.add(new IterableSerializer(this));
    }

    MultipartFormFieldSerializer<?> select(Class<?> candidate) {
        return serializers.stream().filter(s -> s.supports(candidate)).findFirst()
                .orElseGet(DefaultMultipartFormFieldSerializer::new);
    }
}

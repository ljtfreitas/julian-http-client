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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.emptyMap;

public class HTTPResponseFailures {

    private final Map<HTTPStatusCode, HTTPResponseFailure> failures;

    public HTTPResponseFailures() {
        this(emptyMap());
    }

    private HTTPResponseFailures(Map<HTTPStatusCode, HTTPResponseFailure> failures) {
        this.failures = failures;
    }

    public HTTPResponseFailures join(HTTPStatusCode code, HTTPResponseFailure failure) {
        Map<HTTPStatusCode, HTTPResponseFailure> failures = new HashMap<>(this.failures);
        failures.put(code, failure);

        return new HTTPResponseFailures(failures);
    }

    public HTTPResponseFailures join(HTTPStatusGroup group, HTTPResponseFailure failure) {
        Map<HTTPStatusCode, HTTPResponseFailure> failures = new HashMap<>(this.failures);

        group.range().forEach(code -> HTTPStatusCode.select(code).ifPresent(s -> failures.put(s, failure)));

        return new HTTPResponseFailures(failures);
    }

    public HTTPResponseFailure fold() {
        return failures.isEmpty() ? DefaultHTTPResponseFailure.get() : new ConditionalHTTPResponseFailure(failures);
    }

    public Map<HTTPStatusCode, HTTPResponseFailure> all() {
        return failures;
    }

    public static HTTPResponseFailures empty() {
        return new HTTPResponseFailures();
    }

}

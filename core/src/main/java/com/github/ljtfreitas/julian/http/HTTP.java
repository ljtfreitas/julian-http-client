/*
 * Copyright (C) 2021 Tiago de Freitas Lima
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package com.github.ljtfreitas.julian.http;

import com.github.ljtfreitas.julian.Promise;

import java.net.URI;

public interface HTTP {

    <T> Promise<HTTPResponse<T>> run(HTTPEndpoint request);

    default HTTP.DSL asDSL() {
        return new DoneHTTPDSL(this);
    }

    interface DSL {

        HTTPDSLRequest GET(URI path);

        HTTPDSLRequest POST(URI path);

        HTTPDSLRequest PUT(URI path);

        HTTPDSLRequest PATCH(URI path);

        HTTPDSLRequest DELETE(URI path);

        HTTPDSLRequest HEAD(URI path);

        HTTPDSLRequest OPTIONS(URI path);

        HTTPDSLRequest TRACE(URI path);
    }
}

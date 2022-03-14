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

import java.net.URI;

class DoneHTTPDSL implements HTTP.DSL {

    private final HTTP http;

    DoneHTTPDSL(HTTP http) {
        this.http = http;
    }

    @Override
    public HTTPDSLRequest GET(URI path) {
        return new HTTPDSLRequest(http, path, HTTPMethod.GET);
    }

    @Override
    public HTTPDSLRequest POST(URI path) {
        return new HTTPDSLRequest(http, path, HTTPMethod.POST);
    }

    @Override
    public HTTPDSLRequest PUT(URI path) {
        return new HTTPDSLRequest(http, path, HTTPMethod.PUT);
    }

    @Override
    public HTTPDSLRequest PATCH(URI path) {
        return new HTTPDSLRequest(http, path, HTTPMethod.PATCH);
    }

    @Override
    public HTTPDSLRequest DELETE(URI path) {
        return new HTTPDSLRequest(http, path, HTTPMethod.DELETE);
    }

    @Override
    public HTTPDSLRequest HEAD(URI path) {
        return new HTTPDSLRequest(http, path, HTTPMethod.HEAD);
    }

    @Override
    public HTTPDSLRequest OPTIONS(URI path) {
        return new HTTPDSLRequest(http, path, HTTPMethod.OPTIONS);
    }

    @Override
    public HTTPDSLRequest TRACE(URI path) {
        return new HTTPDSLRequest(http, path, HTTPMethod.TRACE);
    }
}

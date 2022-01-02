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

package com.github.ljtfreitas.julian.http.auth;

import com.github.ljtfreitas.julian.http.HTTPException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.http.HTTPHeader;
import com.github.ljtfreitas.julian.http.HTTPHeaders;
import com.github.ljtfreitas.julian.http.HTTPRequest;

import static com.github.ljtfreitas.julian.http.HTTPHeader.AUTHORIZATION;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HTTPAuthenticationInterceptorTest {

    @Test
    void authorizationHeader(@Mock HTTPRequest<Void> request) {
        HTTPAuthenticationInterceptor interceptor = new HTTPAuthenticationInterceptor(() -> "abc1234");

        when(request.headers()).thenReturn(HTTPHeaders.empty());

        when(request.headers(ArgumentMatchers.any(HTTPHeaders.class))).then(a -> {
            @SuppressWarnings("unchecked") HTTPRequest<Void> newHTTPRequest = mock(HTTPRequest.class);
            when(newHTTPRequest.headers()).thenReturn(a.getArgument(0, HTTPHeaders.class));
            return newHTTPRequest;
        });

        Promise<HTTPRequest<Void>, HTTPException> authorized = interceptor.intercepts(Promise.done(request));

        assertThat(authorized.join().unsafe().headers(), contains(new HTTPHeader(AUTHORIZATION, "abc1234")));
    }
}
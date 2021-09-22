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

import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.http.client.HTTPClientResponse;

import java.util.function.Supplier;

public class DefaultHTTPResponseFailure implements HTTPResponseFailure {

	private static final DefaultHTTPResponseFailure SINGLE_INSTANCE = new DefaultHTTPResponseFailure();

	@Override
	public <T> HTTPResponse<T> apply(HTTPClientResponse response, JavaType javaType) {
		HTTPStatus status = response.status();
		HTTPHeaders headers = response.headers();

		Supplier<HTTPResponseException> ex = () -> HTTPStatusCode.select(status.code())
				.<HTTPResponseException> map(httpStatusCode -> failure(httpStatusCode, headers))
				.orElseGet(() -> unknown(status, headers));

		return new FailureHTTPResponse<>(status, headers, ex);
	}

	private HTTPUknownFailureResponseException unknown(HTTPStatus status, HTTPHeaders headers) {
		return new HTTPUknownFailureResponseException(status, headers);
	}

	private HTTPFailureResponseException failure(HTTPStatusCode status, HTTPHeaders headers) {
		switch (status) {
			case BAD_REQUEST:
				return new HTTPClientFailureResponseException.BadRequest(headers);
	
			case UNAUTHORIZED:
				return new HTTPClientFailureResponseException.Unauthorized(headers);
		
			case FORBIDDEN:
				return new HTTPClientFailureResponseException.Forbidden(headers);
		
			case NOT_FOUND:
				return new HTTPClientFailureResponseException.NotFound(headers);
			
			case METHOD_NOT_ALLOWED:
				return new HTTPClientFailureResponseException.MethodNotAllowed(headers);
			
			case NOT_ACCEPTABLE:
				return new HTTPClientFailureResponseException.NotAcceptable(headers);
			
			case PROXY_AUTHENTATION_REQUIRED:
				return new HTTPClientFailureResponseException.ProxyAuthenticationRequired(headers);
			
			case REQUEST_TIMEOUT:
				return new HTTPClientFailureResponseException.RequestTimeout(headers);
			
			case CONFLICT:
				return new HTTPClientFailureResponseException.Conflict(headers);
			
			case GONE:
				return new HTTPClientFailureResponseException.Gone(headers);
			
			case LENGTH_REQUIRED:
				return new HTTPClientFailureResponseException.LengthRequired(headers);
			
			case PRECONDITION_FAILED:
				return new HTTPClientFailureResponseException.PreconditionFailed(headers);
			
			case REQUEST_ENTITY_TOO_LARGE:
				return new HTTPClientFailureResponseException.RequestEntityTooLarge(headers);
			
			case REQUEST_URI_TOO_LONG:
				return new HTTPClientFailureResponseException.RequestURITooLong(headers);
			
			case UNSUPPORTED_MEDIA_TYPE:
				return new HTTPClientFailureResponseException.UnsupportedMediaType(headers);
			
			case REQUESTED_RANGE_NOT_SATISFIABLE:
				return new HTTPClientFailureResponseException.RequestedRangeNotSatisfiable(headers);
			
			case EXPECTATION_FAILED:
				return new HTTPClientFailureResponseException.ExpectationFailed(headers);
			
			case INTERNAL_SERVER_ERROR:
				return new HTTPServerFailureResponseException.InternalServerError(headers);
			
			case NOT_IMPLEMENTED:
				return new HTTPServerFailureResponseException.NotImplemented(headers);
			
			case BAD_GATEWAY:
				return new HTTPServerFailureResponseException.BadGateway(headers);
			
			case SERVICE_UNAVAILABLE:
				return new HTTPServerFailureResponseException.ServiceUnavailable(headers);
			
			case GATEWAY_TIMEOUT:
				return new HTTPServerFailureResponseException.GatewayTimeout(headers);
			
			case HTTP_VERSION_NOT_SUPPORTED:
				return new HTTPServerFailureResponseException.HTTPVersionNotSupported(headers);
			
			default:
				return new HTTPFailureResponseException(status, headers);
		}
	}

	public static DefaultHTTPResponseFailure get() {
		return SINGLE_INSTANCE;
	}
}

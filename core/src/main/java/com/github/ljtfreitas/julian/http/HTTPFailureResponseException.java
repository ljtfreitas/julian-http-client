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

import com.github.ljtfreitas.julian.Promise;

public class HTTPFailureResponseException extends HTTPResponseException {

	private static final long serialVersionUID = 1L;

	public HTTPFailureResponseException(HTTPStatusCode status, HTTPHeaders headers, Promise<byte[]> body) {
		super(new HTTPStatus(status.value(), status.message()), headers, body);
	}

	public static HTTPFailureResponseException create(HTTPStatusCode status, HTTPHeaders headers, byte[] responseBody) {
		return create(status, headers, Promise.done(responseBody));
	}

	public static HTTPFailureResponseException create(HTTPStatusCode status, HTTPHeaders headers, Promise<byte[]> responseBody) {
		switch (status) {
			case BAD_REQUEST:
				return new HTTPClientFailureResponseException.BadRequest(headers, responseBody);

			case UNAUTHORIZED:
				return new HTTPClientFailureResponseException.Unauthorized(headers, responseBody);

			case FORBIDDEN:
				return new HTTPClientFailureResponseException.Forbidden(headers, responseBody);

			case NOT_FOUND:
				return new HTTPClientFailureResponseException.NotFound(headers, responseBody);

			case METHOD_NOT_ALLOWED:
				return new HTTPClientFailureResponseException.MethodNotAllowed(headers, responseBody);

			case NOT_ACCEPTABLE:
				return new HTTPClientFailureResponseException.NotAcceptable(headers, responseBody);

			case PROXY_AUTHENTATION_REQUIRED:
				return new HTTPClientFailureResponseException.ProxyAuthenticationRequired(headers, responseBody);

			case REQUEST_TIMEOUT:
				return new HTTPClientFailureResponseException.RequestTimeout(headers, responseBody);

			case CONFLICT:
				return new HTTPClientFailureResponseException.Conflict(headers, responseBody);

			case GONE:
				return new HTTPClientFailureResponseException.Gone(headers, responseBody);

			case LENGTH_REQUIRED:
				return new HTTPClientFailureResponseException.LengthRequired(headers, responseBody);

			case PRECONDITION_FAILED:
				return new HTTPClientFailureResponseException.PreconditionFailed(headers, responseBody);

			case REQUEST_ENTITY_TOO_LARGE:
				return new HTTPClientFailureResponseException.RequestEntityTooLarge(headers, responseBody);

			case REQUEST_URI_TOO_LONG:
				return new HTTPClientFailureResponseException.RequestURITooLong(headers, responseBody);

			case UNSUPPORTED_MEDIA_TYPE:
				return new HTTPClientFailureResponseException.UnsupportedMediaType(headers, responseBody);

			case REQUESTED_RANGE_NOT_SATISFIABLE:
				return new HTTPClientFailureResponseException.RequestedRangeNotSatisfiable(headers, responseBody);

			case EXPECTATION_FAILED:
				return new HTTPClientFailureResponseException.ExpectationFailed(headers, responseBody);

			case INTERNAL_SERVER_ERROR:
				return new HTTPServerFailureResponseException.InternalServerError(headers, responseBody);

			case NOT_IMPLEMENTED:
				return new HTTPServerFailureResponseException.NotImplemented(headers, responseBody);

			case BAD_GATEWAY:
				return new HTTPServerFailureResponseException.BadGateway(headers, responseBody);

			case SERVICE_UNAVAILABLE:
				return new HTTPServerFailureResponseException.ServiceUnavailable(headers, responseBody);

			case GATEWAY_TIMEOUT:
				return new HTTPServerFailureResponseException.GatewayTimeout(headers, responseBody);

			case HTTP_VERSION_NOT_SUPPORTED:
				return new HTTPServerFailureResponseException.HTTPVersionNotSupported(headers, responseBody);

			default:
				return new HTTPFailureResponseException(status, headers, responseBody);
		}
	}
}

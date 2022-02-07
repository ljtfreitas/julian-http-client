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

import static com.github.ljtfreitas.julian.http.HTTPStatusCode.BAD_REQUEST;
import static com.github.ljtfreitas.julian.http.HTTPStatusCode.CONFLICT;
import static com.github.ljtfreitas.julian.http.HTTPStatusCode.EXPECTATION_FAILED;
import static com.github.ljtfreitas.julian.http.HTTPStatusCode.FORBIDDEN;
import static com.github.ljtfreitas.julian.http.HTTPStatusCode.GONE;
import static com.github.ljtfreitas.julian.http.HTTPStatusCode.LENGTH_REQUIRED;
import static com.github.ljtfreitas.julian.http.HTTPStatusCode.METHOD_NOT_ALLOWED;
import static com.github.ljtfreitas.julian.http.HTTPStatusCode.NOT_ACCEPTABLE;
import static com.github.ljtfreitas.julian.http.HTTPStatusCode.NOT_FOUND;
import static com.github.ljtfreitas.julian.http.HTTPStatusCode.PRECONDITION_FAILED;
import static com.github.ljtfreitas.julian.http.HTTPStatusCode.PROXY_AUTHENTATION_REQUIRED;
import static com.github.ljtfreitas.julian.http.HTTPStatusCode.REQUESTED_RANGE_NOT_SATISFIABLE;
import static com.github.ljtfreitas.julian.http.HTTPStatusCode.REQUEST_ENTITY_TOO_LARGE;
import static com.github.ljtfreitas.julian.http.HTTPStatusCode.REQUEST_TIMEOUT;
import static com.github.ljtfreitas.julian.http.HTTPStatusCode.REQUEST_URI_TOO_LONG;
import static com.github.ljtfreitas.julian.http.HTTPStatusCode.UNAUTHORIZED;
import static com.github.ljtfreitas.julian.http.HTTPStatusCode.UNSUPPORTED_MEDIA_TYPE;

public class HTTPClientFailureResponseException extends HTTPFailureResponseException {

	private static final long serialVersionUID = 1L;

	HTTPClientFailureResponseException(HTTPStatusCode status, HTTPHeaders headers, Promise<byte[]> body) {
		super(status, headers, body);
	}

	public static class BadRequest extends HTTPClientFailureResponseException {

		private static final long serialVersionUID = 1L;

		public BadRequest(HTTPHeaders headers, Promise<byte[]> body) {
			super(BAD_REQUEST, headers, body);
		}
	}

	public static class Unauthorized extends HTTPClientFailureResponseException {

		private static final long serialVersionUID = 1L;

		public Unauthorized(HTTPHeaders headers, Promise<byte[]> body) {
			super(UNAUTHORIZED, headers, body);
		}
	}

	public static class Forbidden extends HTTPClientFailureResponseException {

		private static final long serialVersionUID = 1L;

		public Forbidden(HTTPHeaders headers, Promise<byte[]> body) {
			super(FORBIDDEN, headers, body);
		}
	}

	public static class NotFound extends HTTPClientFailureResponseException {

		private static final long serialVersionUID = 1L;

		public NotFound(HTTPHeaders headers, Promise<byte[]> body) {
			super(NOT_FOUND, headers, body);
		}
	}

	public static class MethodNotAllowed extends HTTPClientFailureResponseException {

		private static final long serialVersionUID = 1L;

		public MethodNotAllowed(HTTPHeaders headers, Promise<byte[]> body) {
			super(METHOD_NOT_ALLOWED, headers, body);
		}
	}

	public static class NotAcceptable extends HTTPClientFailureResponseException {

		private static final long serialVersionUID = 1L;

		public NotAcceptable(HTTPHeaders headers, Promise<byte[]> body) {
			super(NOT_ACCEPTABLE, headers, body);
		}
	}

	public static class ProxyAuthenticationRequired extends HTTPClientFailureResponseException {

		private static final long serialVersionUID = 1L;

		public ProxyAuthenticationRequired(HTTPHeaders headers, Promise<byte[]> body) {
			super(PROXY_AUTHENTATION_REQUIRED, headers, body);
		}
	}

	public static class RequestTimeout extends HTTPClientFailureResponseException {

		private static final long serialVersionUID = 1L;

		public RequestTimeout(HTTPHeaders headers, Promise<byte[]> body) {
			super(REQUEST_TIMEOUT, headers, body);
		}
	}

	public static class Conflict extends HTTPClientFailureResponseException {

		private static final long serialVersionUID = 1L;

		public Conflict(HTTPHeaders headers, Promise<byte[]> body) {
			super(CONFLICT, headers, body);
		}
	}

	public static class Gone extends HTTPClientFailureResponseException {

		private static final long serialVersionUID = 1L;

		public Gone(HTTPHeaders headers, Promise<byte[]> body) {
			super(GONE, headers, body);
		}
	}

	public static class LengthRequired extends HTTPClientFailureResponseException {

		private static final long serialVersionUID = 1L;

		public LengthRequired(HTTPHeaders headers, Promise<byte[]> body) {
			super(LENGTH_REQUIRED, headers, body);
		}
	}

	public static class PreconditionFailed extends HTTPClientFailureResponseException {

		private static final long serialVersionUID = 1L;

		public PreconditionFailed(HTTPHeaders headers, Promise<byte[]> body) {
			super(PRECONDITION_FAILED, headers, body);
		}
	}

	public static class RequestEntityTooLarge extends HTTPClientFailureResponseException {

		private static final long serialVersionUID = 1L;

		public RequestEntityTooLarge(HTTPHeaders headers, Promise<byte[]> body) {
			super(REQUEST_ENTITY_TOO_LARGE, headers, body);
		}
	}

	public static class RequestURITooLong extends HTTPClientFailureResponseException {

		private static final long serialVersionUID = 1L;

		public RequestURITooLong(HTTPHeaders headers, Promise<byte[]> body) {
			super(REQUEST_URI_TOO_LONG, headers, body);
		}
	}

	public static class UnsupportedMediaType extends HTTPClientFailureResponseException {

		private static final long serialVersionUID = 1L;

		public UnsupportedMediaType(HTTPHeaders headers, Promise<byte[]> body) {
			super(UNSUPPORTED_MEDIA_TYPE, headers, body);
		}
	}

	public static class RequestedRangeNotSatisfiable extends HTTPClientFailureResponseException {

		private static final long serialVersionUID = 1L;

		public RequestedRangeNotSatisfiable(HTTPHeaders headers, Promise<byte[]> body) {
			super(REQUESTED_RANGE_NOT_SATISFIABLE, headers, body);
		}
	}

	public static class ExpectationFailed extends HTTPClientFailureResponseException {

		private static final long serialVersionUID = 1L;

		public ExpectationFailed(HTTPHeaders headers, Promise<byte[]> body) {
			super(EXPECTATION_FAILED, headers, body);
		}
	}
}

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

	HTTPClientFailureResponseException(HTTPStatusCode status, HTTPHeaders headers) {
		super(status, headers);
	}

	public static class BadRequest extends HTTPClientFailureResponseException {

		private static final long serialVersionUID = 1L;

		public BadRequest(HTTPHeaders headers) {
			super(BAD_REQUEST, headers);
		}
	}

	public static class Unauthorized extends HTTPClientFailureResponseException {

		private static final long serialVersionUID = 1L;

		public Unauthorized(HTTPHeaders headers) {
			super(UNAUTHORIZED, headers);
		}
	}

	public static class Forbidden extends HTTPClientFailureResponseException {

		private static final long serialVersionUID = 1L;

		public Forbidden(HTTPHeaders headers) {
			super(FORBIDDEN, headers);
		}
	}

	public static class NotFound extends HTTPClientFailureResponseException {

		private static final long serialVersionUID = 1L;

		public NotFound(HTTPHeaders headers) {
			super(NOT_FOUND, headers);
		}
	}

	public static class MethodNotAllowed extends HTTPClientFailureResponseException {

		private static final long serialVersionUID = 1L;

		public MethodNotAllowed(HTTPHeaders headers) {
			super(METHOD_NOT_ALLOWED, headers);
		}
	}

	public static class NotAcceptable extends HTTPClientFailureResponseException {

		private static final long serialVersionUID = 1L;

		public NotAcceptable(HTTPHeaders headers) {
			super(NOT_ACCEPTABLE, headers);
		}
	}

	public static class ProxyAuthenticationRequired extends HTTPClientFailureResponseException {

		private static final long serialVersionUID = 1L;

		public ProxyAuthenticationRequired(HTTPHeaders headers) {
			super(PROXY_AUTHENTATION_REQUIRED, headers);
		}
	}

	public static class RequestTimeout extends HTTPClientFailureResponseException {

		private static final long serialVersionUID = 1L;

		public RequestTimeout(HTTPHeaders headers) {
			super(REQUEST_TIMEOUT, headers);
		}
	}

	public static class Conflict extends HTTPClientFailureResponseException {

		private static final long serialVersionUID = 1L;

		public Conflict(HTTPHeaders headers) {
			super(CONFLICT, headers);
		}
	}

	public static class Gone extends HTTPClientFailureResponseException {

		private static final long serialVersionUID = 1L;

		public Gone(HTTPHeaders headers) {
			super(GONE, headers);
		}
	}

	public static class LengthRequired extends HTTPClientFailureResponseException {

		private static final long serialVersionUID = 1L;

		public LengthRequired(HTTPHeaders headers) {
			super(LENGTH_REQUIRED, headers);
		}
	}

	public static class PreconditionFailed extends HTTPClientFailureResponseException {

		private static final long serialVersionUID = 1L;

		public PreconditionFailed(HTTPHeaders headers) {
			super(PRECONDITION_FAILED, headers);
		}
	}

	public static class RequestEntityTooLarge extends HTTPClientFailureResponseException {

		private static final long serialVersionUID = 1L;

		public RequestEntityTooLarge(HTTPHeaders headers) {
			super(REQUEST_ENTITY_TOO_LARGE, headers);
		}
	}

	public static class RequestURITooLong extends HTTPClientFailureResponseException {

		private static final long serialVersionUID = 1L;

		public RequestURITooLong(HTTPHeaders headers) {
			super(REQUEST_URI_TOO_LONG, headers);
		}
	}

	public static class UnsupportedMediaType extends HTTPClientFailureResponseException {

		private static final long serialVersionUID = 1L;

		public UnsupportedMediaType(HTTPHeaders headers) {
			super(UNSUPPORTED_MEDIA_TYPE, headers);
		}
	}

	public static class RequestedRangeNotSatisfiable extends HTTPClientFailureResponseException {

		private static final long serialVersionUID = 1L;

		public RequestedRangeNotSatisfiable(HTTPHeaders headers) {
			super(REQUESTED_RANGE_NOT_SATISFIABLE, headers);
		}
	}

	public static class ExpectationFailed extends HTTPClientFailureResponseException {

		private static final long serialVersionUID = 1L;

		public ExpectationFailed(HTTPHeaders headers) {
			super(EXPECTATION_FAILED, headers);
		}
	}
}
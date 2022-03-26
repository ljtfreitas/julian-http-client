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

import static com.github.ljtfreitas.julian.http.HTTPStatusCode.BAD_GATEWAY;
import static com.github.ljtfreitas.julian.http.HTTPStatusCode.BANDWIDTH_LIMIT_EXCEEDED;
import static com.github.ljtfreitas.julian.http.HTTPStatusCode.GATEWAY_TIMEOUT;
import static com.github.ljtfreitas.julian.http.HTTPStatusCode.HTTP_VERSION_NOT_SUPPORTED;
import static com.github.ljtfreitas.julian.http.HTTPStatusCode.INSUFFICIENT_STORAGE;
import static com.github.ljtfreitas.julian.http.HTTPStatusCode.INTERNAL_SERVER_ERROR;
import static com.github.ljtfreitas.julian.http.HTTPStatusCode.LOOP_DETECTED;
import static com.github.ljtfreitas.julian.http.HTTPStatusCode.NETWORK_AUTHENTICATION_REQUIRED;
import static com.github.ljtfreitas.julian.http.HTTPStatusCode.NOT_EXTENDED;
import static com.github.ljtfreitas.julian.http.HTTPStatusCode.NOT_IMPLEMENTED;
import static com.github.ljtfreitas.julian.http.HTTPStatusCode.SERVICE_UNAVAILABLE;
import static com.github.ljtfreitas.julian.http.HTTPStatusCode.VARIANT_ALSO_NEGOTIATES;

public class HTTPServerFailureResponseException extends HTTPFailureResponseException {

	private static final long serialVersionUID = 1L;

	HTTPServerFailureResponseException(HTTPStatusCode status, HTTPHeaders headers, Promise<byte[]> body) {
		super(status, headers, body);
	}

	public static class InternalServerError extends HTTPServerFailureResponseException {

		private static final long serialVersionUID = 1L;

		public InternalServerError(HTTPHeaders headers, Promise<byte[]> body) {
			super(INTERNAL_SERVER_ERROR, headers, body);
		}
	}

	public static class NotImplemented extends HTTPServerFailureResponseException {

		private static final long serialVersionUID = 1L;

		public NotImplemented(HTTPHeaders headers, Promise<byte[]> body) {
			super(NOT_IMPLEMENTED, headers, body);
		}
	}

	public static class BadGateway extends HTTPServerFailureResponseException {

		private static final long serialVersionUID = 1L;

		public BadGateway(HTTPHeaders headers, Promise<byte[]> body) {
			super(BAD_GATEWAY, headers, body);
		}
	}

	public static class ServiceUnavailable extends HTTPServerFailureResponseException {

		private static final long serialVersionUID = 1L;

		public ServiceUnavailable(HTTPHeaders headers, Promise<byte[]> body) {
			super(SERVICE_UNAVAILABLE, headers, body);
		}
	}

	public static class GatewayTimeout extends HTTPServerFailureResponseException {

		private static final long serialVersionUID = 1L;

		public GatewayTimeout(HTTPHeaders headers, Promise<byte[]> body) {
			super(GATEWAY_TIMEOUT, headers, body);
		}
	}

	public static class HTTPVersionNotSupported extends HTTPServerFailureResponseException {

		private static final long serialVersionUID = 1L;

		public HTTPVersionNotSupported(HTTPHeaders headers, Promise<byte[]> body) {
			super(HTTP_VERSION_NOT_SUPPORTED, headers, body);
		}
	}

	public static class VariantAlsoNegotiates extends HTTPServerFailureResponseException {

		private static final long serialVersionUID = 1L;

		public VariantAlsoNegotiates(HTTPHeaders headers, Promise<byte[]> body) {
			super(VARIANT_ALSO_NEGOTIATES, headers, body);
		}
	}

	public static class InsufficientStorage extends HTTPServerFailureResponseException {

		private static final long serialVersionUID = 1L;

		public InsufficientStorage(HTTPHeaders headers, Promise<byte[]> body) {
			super(INSUFFICIENT_STORAGE, headers, body);
		}
	}

	public static class LoopDetected extends HTTPServerFailureResponseException {

		private static final long serialVersionUID = 1L;

		public LoopDetected(HTTPHeaders headers, Promise<byte[]> body) {
			super(LOOP_DETECTED, headers, body);
		}
	}

	public static class BandwidthLimitExceeded extends HTTPServerFailureResponseException {

		private static final long serialVersionUID = 1L;

		public BandwidthLimitExceeded(HTTPHeaders headers, Promise<byte[]> body) {
			super(BANDWIDTH_LIMIT_EXCEEDED, headers, body);
		}
	}

	public static class NotExtended extends HTTPServerFailureResponseException {

		private static final long serialVersionUID = 1L;

		public NotExtended(HTTPHeaders headers, Promise<byte[]> body) {
			super(NOT_EXTENDED, headers, body);
		}
	}

	public static class NetWorkAuthenticationRequired extends HTTPServerFailureResponseException {

		private static final long serialVersionUID = 1L;

		public NetWorkAuthenticationRequired(HTTPHeaders headers, Promise<byte[]> body) {
			super(NETWORK_AUTHENTICATION_REQUIRED, headers, body);
		}
	}
}

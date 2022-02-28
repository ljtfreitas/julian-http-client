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
import java.util.Optional;

public enum HTTPStatusCode {

	// Informational 1xx
	CONTINUE(100, "Continue", HTTPStatusGroup.INFORMATIONAL),
	SWITCHING_PROTOCOLS(101, "Switching Protocols", HTTPStatusGroup.INFORMATIONAL),

	// Successful 2xx
	OK(200, "Ok", HTTPStatusGroup.SUCCESSFUL),
	CREATED(201, "Created", HTTPStatusGroup.SUCCESSFUL),
	ACCEPTED(202, "Accepted", HTTPStatusGroup.SUCCESSFUL),
	NON_AUTHORITATIVE_INFORMATION(203, "Non-Authoritative Information", HTTPStatusGroup.SUCCESSFUL),
	NO_CONTENT(204, "No Content", HTTPStatusGroup.SUCCESSFUL),
	RESET_CONTENT(205, "Reset Content", HTTPStatusGroup.SUCCESSFUL),
	PARTIAL_CONTENT(206, "Partial Content", HTTPStatusGroup.SUCCESSFUL),

	// Redirection 3xx
	MULTIPLES_CHOICES(300, "Multiple Choices", HTTPStatusGroup.REDIRECTION),
	MOVED_PERMANENTLY(301, "Moved Permanently", HTTPStatusGroup.REDIRECTION),
	FOUND(302, "Found", HTTPStatusGroup.REDIRECTION),
	SEE_OTHER(303, "See Other", HTTPStatusGroup.REDIRECTION),
	NOT_MODIFIED(304, "Not Modified", HTTPStatusGroup.REDIRECTION),
	USE_PROXY(305, "Use Proxy", HTTPStatusGroup.REDIRECTION),
	TEMPORARY_REDIRECT(307, "Temporary Redirect", HTTPStatusGroup.REDIRECTION),

	// Client Error 4xx
	BAD_REQUEST(400, "Bad Request", HTTPStatusGroup.CLIENT_ERROR),
	UNAUTHORIZED(401, "Unauthorized", HTTPStatusGroup.CLIENT_ERROR),
	FORBIDDEN(403, "Forbidden", HTTPStatusGroup.CLIENT_ERROR),
	NOT_FOUND(404, "Not Found", HTTPStatusGroup.CLIENT_ERROR),
	METHOD_NOT_ALLOWED(405, "Method Not Allowed", HTTPStatusGroup.CLIENT_ERROR),
	NOT_ACCEPTABLE(406, "Not Acceptable", HTTPStatusGroup.CLIENT_ERROR),
	PROXY_AUTHENTATION_REQUIRED(407, "Proxy Authentication Required", HTTPStatusGroup.CLIENT_ERROR),
	REQUEST_TIMEOUT(408, "Request Timeout", HTTPStatusGroup.CLIENT_ERROR),
	CONFLICT(409, "Conflict", HTTPStatusGroup.CLIENT_ERROR),
	GONE(410, "Gone", HTTPStatusGroup.CLIENT_ERROR),
	LENGTH_REQUIRED(411, "Length Required", HTTPStatusGroup.CLIENT_ERROR),
	PRECONDITION_FAILED(412, "Precondition Failed", HTTPStatusGroup.CLIENT_ERROR),
	REQUEST_ENTITY_TOO_LARGE(413, "Request Entity Too Large", HTTPStatusGroup.CLIENT_ERROR),
	REQUEST_URI_TOO_LONG(414, "Request-URI Too Long", HTTPStatusGroup.CLIENT_ERROR),
	UNSUPPORTED_MEDIA_TYPE(415, "Unsupported Media Type", HTTPStatusGroup.CLIENT_ERROR),
	REQUESTED_RANGE_NOT_SATISFIABLE(416, "Requested Range Not Satisfiable", HTTPStatusGroup.CLIENT_ERROR),
	EXPECTATION_FAILED(417, "Expectation Failed", HTTPStatusGroup.CLIENT_ERROR),

	// Server Error 5xx
	INTERNAL_SERVER_ERROR(500, "Internal Server Error", HTTPStatusGroup.SERVER_ERROR),
	NOT_IMPLEMENTED(501, "Not Implemented", HTTPStatusGroup.SERVER_ERROR),
	BAD_GATEWAY(502, "Bad Gateway", HTTPStatusGroup.SERVER_ERROR),
	SERVICE_UNAVAILABLE(503, "Service Unavailable", HTTPStatusGroup.SERVER_ERROR),
	GATEWAY_TIMEOUT(504, "Gateway Timeout", HTTPStatusGroup.SERVER_ERROR),
	HTTP_VERSION_NOT_SUPPORTED(505, "HTTP Version Not Supported", HTTPStatusGroup.SERVER_ERROR);

	private final int value;
	private final String message;
	private final HTTPStatusGroup group;

	HTTPStatusCode(int code, String message, HTTPStatusGroup group) {
		this.value = code;
		this.message = message;
		this.group = group;
	}

	public int value() {
		return value;
	}

	public String message() {
		return message;
	}

	public HTTPStatusGroup group() {
		return group;
	}

	public boolean onGroup(HTTPStatusGroup group) {
		return this.group == group;
	}

	public boolean informational() {
		return group == HTTPStatusGroup.INFORMATIONAL;
	}

	public boolean success() {
		return group == HTTPStatusGroup.SUCCESSFUL;
	}

	public boolean redirection() {
		return group == HTTPStatusGroup.REDIRECTION;
	}

	public boolean error() {
		return group == HTTPStatusGroup.CLIENT_ERROR || group == HTTPStatusGroup.SERVER_ERROR;
	}

	public static Optional<HTTPStatusCode> select(int code) {
		return Arrays.stream(values()).filter(s ->s.value == code).findFirst();
	}
}

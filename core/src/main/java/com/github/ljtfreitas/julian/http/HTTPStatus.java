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

import java.util.Objects;

import com.github.ljtfreitas.julian.Message;

public class HTTPStatus {

	private final int code;
	private final String message;

	public HTTPStatus(HTTPStatusCode status) {
		this(status.value(), status.message());
	}

	public HTTPStatus(int code, String message) {
		this.code = code;
		this.message = message;
	}

	public int code() {
		return code;
	}

	public String message() {
		return message;
	}

	public boolean is(HTTPStatusCode code) {
		return this.code == code.value();
	}

	public boolean informational() {
		return (code / 100) == 1;
	}

	public boolean success() {
		return (code / 100) == 2;
	}

	public boolean redirection() {
		return (code / 100) == 3;
	}

	public boolean error() {
		return a4xx() || a5xx();
	}

	boolean readable() {
		return !(informational() || code == 204 || code == 304);
	}

	private boolean a4xx() {
		return (code / 100) == 4;
	}

	private boolean a5xx() {
		return (code / 100) == 5;
	}

	@Override
	public int hashCode() {
		return Objects.hash(code);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		
		if (obj instanceof HTTPStatus) {
			return this.code == ((HTTPStatus) obj).code;

		} else return false;
	}

	@Override
	public String toString() {
		return message == null ? Message.format("{0}", code) : Message.format("{0} {1}", code, message);
	}

	public static HTTPStatus valueOf(int code) {
		return new HTTPStatus(code, null);
	}

	static HTTPStatus valueOf(HTTPStatusCode status) {
		return new HTTPStatus(status.value(), status.message());
	}
}

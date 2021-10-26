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
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.ljtfreitas.julian.Preconditions;

import static com.github.ljtfreitas.julian.Message.format;
import static com.github.ljtfreitas.julian.Preconditions.check;
import static com.github.ljtfreitas.julian.Preconditions.isFalse;
import static com.github.ljtfreitas.julian.Preconditions.nonNull;
import static java.util.Collections.unmodifiableCollection;
import static java.util.stream.Collectors.toUnmodifiableList;

public class HTTPHeader {

	public static final String ACCEPT = "Accept";
	public static final String ACCEPT_CHARSET = "Accept-Charset";
	public static final String ACCEPT_ENCODING = "Accept-Encoding";
	public static final String ACCEPT_LANGUAGE = "Accept-Language";
	public static final String ACCEPT_PATCH = "Accept-Patch";
	public static final String ACCEPT_RANGES = "Accept-Ranges";
	public static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
	public static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
	public static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
	public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
	public static final String ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers";
	public static final String ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";
	public static final String ACCESS_CONTROL_REQUEST_HEADERS = "Access-Control-Request-Headers";
	public static final String ACCESS_CONTROL_REQUEST_METHOD = "Access-Control-Request-Method";
	public static final String AGE = "Age";
	public static final String ALLOW = "Allow";
	public static final String AUTHORIZATION = "Authorization";
	public static final String CACHE_CONTROL = "Cache-Control";
	public static final String CONNECTION = "Connection";
	public static final String CONTENT_ENCODING = "Content-Encoding";
	public static final String CONTENT_DISPOSITION = "Content-Disposition";
	public static final String CONTENT_LANGUAGE = "Content-Language";
	public static final String CONTENT_LENGTH = "Content-Length";
	public static final String CONTENT_LOCATION = "Content-Location";
	public static final String CONTENT_RANGE = "Content-Range";
	public static final String CONTENT_TYPE = "Content-Type";
	public static final String COOKIE = "Cookie";
	public static final String DATE = "Date";
	public static final String ETAG = "ETag";
	public static final String EXPECT = "Expect";
	public static final String EXPIRES = "Expires";
	public static final String FROM = "From";
	public static final String HOST = "Host";
	public static final String IF_MATCH = "If-Match";
	public static final String IF_MODIFIED_SINCE = "If-Modified-Since";
	public static final String IF_NONE_MATCH = "If-None-Match";
	public static final String IF_RANGE = "If-Range";
	public static final String IF_UNMODIFIED_SINCE = "If-Unmodified-Since";
	public static final String LAST_MODIFIED = "Last-Modified";
	public static final String LINK = "Link";
	public static final String LOCATION = "Location";
	public static final String MAX_FORWARDS = "Max-Forwards";
	public static final String ORIGIN = "Origin";
	public static final String PRAGMA = "Pragma";
	public static final String PROXY_AUTHENTICATE = "Proxy-Authenticate";
	public static final String PROXY_AUTHORIZATION = "Proxy-Authorization";
	public static final String RANGE = "Range";
	public static final String REFERER = "Referer";
	public static final String RETRY_AFTER = "Retry-After";
	public static final String SERVER = "Server";
	public static final String SET_COOKIE = "Set-Cookie";
	public static final String SET_COOKIE2 = "Set-Cookie2";
	public static final String TE = "TE";
	public static final String TRAILER = "Trailer";
	public static final String TRANSFER_ENCODING = "Transfer-Encoding";
	public static final String UPGRADE = "Upgrade";
	public static final String USER_AGENT = "User-Agent";
	public static final String VARY = "Vary";
	public static final String VIA = "Via";
	public static final String WARNING = "Warning";
	public static final String WWW_AUTHENTICATE = "WWW-Authenticate";

	private final String name;
	private final Collection<String> values;

	public HTTPHeader(String name, String value) {
		this(name, List.of(value));
	}

	public HTTPHeader(String name, Collection<String> values) {
		this.name = nonNull(name);
		this.values = unmodifiableCollection(check(values, Preconditions::nonNull, headers -> isFalse(headers, Collection::isEmpty)));
	}

	public String name() {
		return name;
	}

	public String value() {
		return values.iterator().next();
	}

	public Collection<String> values() {
		return values;
	}

	public HTTPHeader join(HTTPHeader that) {
		return (is(that.name)) ? new HTTPHeader(name, Stream.concat(values.stream(), that.values.stream()).collect(toUnmodifiableList())) : this;
	}

	boolean is(String name) {
		return this.name.equalsIgnoreCase(name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, values);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;

		if (obj instanceof HTTPHeader) {
			HTTPHeader that = (HTTPHeader) obj;

			return name.equalsIgnoreCase(that.name)
				&& Arrays.equals(values.toArray(), that.values.toArray());
		
		} else {
			return false;
		}
	}

	@Override
	public String toString() {
		return format("{0}: {1}", name, String.join(", ", values));
	}
	
	public static HTTPHeader create(String name, Collection<String> values) {
		return new HTTPHeader(name, values);
	}
}

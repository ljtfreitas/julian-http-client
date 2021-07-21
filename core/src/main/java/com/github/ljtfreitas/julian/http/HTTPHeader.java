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

import com.github.ljtfreitas.julian.Preconditions;

import static com.github.ljtfreitas.julian.Message.format;
import static com.github.ljtfreitas.julian.Preconditions.check;
import static com.github.ljtfreitas.julian.Preconditions.isFalse;
import static com.github.ljtfreitas.julian.Preconditions.nonNull;
import static java.util.Collections.unmodifiableCollection;

public class HTTPHeader {

	public static final String AUTHORIZATION = "Authorization";
    public static final String CONTENT_TYPE = "Content-Type";

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

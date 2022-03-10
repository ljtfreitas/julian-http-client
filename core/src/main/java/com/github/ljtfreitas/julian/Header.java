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

package com.github.ljtfreitas.julian;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Stream;

import static com.github.ljtfreitas.julian.Message.format;
import static com.github.ljtfreitas.julian.Preconditions.nonNull;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableCollection;
import static java.util.stream.Collectors.toUnmodifiableList;

public class Header {

	private final String name;
	private final Collection<String> values;

	public Header(String name, String... values) {
		this(name, Arrays.asList(values));
	}

	public Header(String name, Collection<String> values) {
		this.name = nonNull(name);
		this.values = unmodifiableCollection(nonNull(values));
	}

	public String name() {
		return name;
	}

	public Collection<String> values() {
		return values;
	}
	
	public Header add(String value) {
		return new Header(name, Stream.concat(values.stream(), Stream.of(nonNull(value))).distinct().collect(toUnmodifiableList()));
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, values);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;

		if (obj instanceof Header) {
			Header that = (Header) obj;
			
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
	
	public static Header empty(String name) {
		return new Header(name, emptyList());
	}
}

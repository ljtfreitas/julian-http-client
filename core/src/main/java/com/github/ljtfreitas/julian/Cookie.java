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

import java.util.Objects;

import static com.github.ljtfreitas.julian.Message.format;
import static com.github.ljtfreitas.julian.Preconditions.nonNull;

public class Cookie {

	private final String name;
	private final String value;

	public Cookie(String name, Object value) {
		this.name = nonNull(name);
		this.value = nonNull(value).toString();
	}

	public String pair() {
		return format("{0}={1}", name, value); 
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(name, value);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;

		if (obj instanceof Cookie) {
			Cookie that = (Cookie) obj;
			
			return Objects.equals(name, that.name)
				&& Objects.equals(value, that.value);
		
		} else {
			return false;
		}
	}

	@Override
	public String toString() {
		return format("{0}: {1}", name, value);
	}
}

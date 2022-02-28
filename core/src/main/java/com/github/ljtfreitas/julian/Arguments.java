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
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.stream.IntStream.range;

public class Arguments {

	private final Object[] args;

	private Arguments(Object[] args) {
		this.args = args;
	}

	public Optional<Object> of(int position) {
		return args.length > position ? Optional.ofNullable(args[position]) : Optional.empty();
	}

	public Stream<Argument> of(Class<?> candidate) {
		return range(0, args.length)
				.filter(position -> candidate.isInstance(args[position]))
				.mapToObj(position -> new Argument(position, args[position]));
    }

	@Override
	public int hashCode() {
		return Arrays.hashCode(args);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Arguments)) return false;

		Arguments that = (Arguments) obj;

		return Arrays.equals(args, that.args);
	}

	@Override
	public String toString() {
		return Arrays.toString(args);
	}

	public static Arguments create(Object... args) {
		return new Arguments(args);
	}

	public static Arguments empty() {
		return new Arguments(new Object[0]);
	}

	public class Argument {

		private final int position;
		private final Object value;

		public Argument(int position, Object value) {
			this.position = position;
			this.value = value;
		}

		public int position() {
			return position;
		}

		public Object value() {
			return value;
		}
	}

}

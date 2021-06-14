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
import java.util.Objects;
import java.util.function.Supplier;

import static com.github.ljtfreitas.julian.Preconditions.Precondition.identity;

public class Preconditions {

	public static <T> T nonNull(T value) {
        return Objects.requireNonNull(value);
    }

	public static boolean isTrue(boolean value, Supplier<String> message) {
        return isTrue(value, identity(), message);
    }

	public static boolean isFalse(boolean value, Supplier<String> message) {
        return isFalse(value, identity(), message);
    }

	public static <T> T isTrue(T o, Precondition<T, Boolean> precondition, Supplier<String> message) {
        if (precondition.evaluate(o)) return o; else throw new IllegalArgumentException(message.get());
    }

	public static <T> T isFalse(T o, Precondition<T, Boolean> precondition) {
        return isFalse(o,  precondition, () -> null);
    }

	public static <T> T isFalse(T o, Precondition<T, Boolean> precondition, Supplier<String> message) {
        if (!precondition.evaluate(o)) return o; else throw new IllegalArgumentException(message.get());
    }

	public static <T> T state(T o, Precondition<T, Boolean> precondition, Supplier<String> message) {
        if (precondition.evaluate(o)) return o; else throw new IllegalStateException(message.get());
    }
	
    @SafeVarargs
    public static <T> T check(T value, Precondition<T, T>...preconditions) {
        return Arrays.stream(preconditions).reduce(value, (v, p) -> p.evaluate(v), (v, p) -> v);
    }

    @FunctionalInterface
    public interface Precondition<I, R> {
        R evaluate(I value);

        static <T> Precondition<T, T> identity() {
            return t -> t;
        }
    }
}

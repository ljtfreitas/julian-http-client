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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiFunction;

import static com.github.ljtfreitas.julian.Preconditions.nonNull;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.flatMapping;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toUnmodifiableList;

public class MultiMap<K, V> {

    private final Map<K, Collection<V>> values;

    public MultiMap(Map<K, ? extends Collection<V>> values) {
        this.values = unmodifiableMap(values);
    }

    public Map<K, Collection<V>> all() {
        return values;
    }

    public String serialize(BiFunction<K, V, String> fn, String delimiter) {
        return values.entrySet().stream()
                .flatMap(e -> e.getValue().stream().map(value -> fn.apply(e.getKey(), value)))
                .collect(joining("&"));
    }

    public MultiMap<K, V> join(K name, V... values) {
        Map<K, Collection<V>> joined = new LinkedHashMap<>(this.values);

        joined.merge(name, asList(values), (a, b) -> {
            Collection<V> e = new ArrayList<>(a);
            e.addAll(b);
            return e;
        });

        return new MultiMap<>(joined);
    }

    public MultiMap<K, V> join(MultiMap<K, V> that) {
        Map<K, Collection<V>> joined = new LinkedHashMap<>(this.values);

        that.values.forEach((name, values) -> {
            joined.merge(name, values, (a, b) -> {
                Collection<V> e = new ArrayList<>(a);
                e.addAll(b);
                return e;
            });
        });

        return new MultiMap<>(joined);
    }

    public static <K, V> MultiMap<K, V> empty() {
        return new MultiMap<>(emptyMap());
    }

    public static <K, V> MultiMap<K, V> valueOf(K name, V... values) {
        return new MultiMap<>(Map.of(name, asList(values)));
    }

    public static <K, V> MultiMap<K, V> valueOf(Map<K, ? extends V> values) {
        return new MultiMap<>(nonNull(values).entrySet().stream()
                .map(e -> Map.entry(e.getKey(), singleton(e.getValue())))
                .collect(groupingBy(Entry::getKey, flatMapping(e -> e.getValue().stream(), toUnmodifiableList()))));
    }
}

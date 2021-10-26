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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.github.ljtfreitas.julian.Endpoint.CallbackParameter;
import com.github.ljtfreitas.julian.Endpoint.Parameters;
import com.github.ljtfreitas.julian.JavaType.Parameterized;

class CompletionStageCallbackResponseT<T> implements ResponseT<Void, T> {

	private static final CompletionStageCallbackResponseT<Object> SINGLE_INSTANCE = new CompletionStageCallbackResponseT<>();

	@Override
	public <A> ResponseFn<Void, A> comp(Endpoint endpoint, ResponseFn<T, A> fn) {
		return new ResponseFn<>() {

			@Override
			public Void join(RequestIO<A> request, Arguments arguments) {
				CompletableFuture<T> future = request.comp(fn, arguments).future();

				success(endpoint.parameters(), arguments)
						.ifPresent(future::thenAccept);

				failure(endpoint.parameters(), arguments)
						.ifPresent(callback -> future.whenComplete((a, t) -> Optional.ofNullable(t).ifPresent(callback)));

				subscriber(endpoint.parameters(), fn.returnType(), arguments)
						.ifPresent(future::whenComplete);

				return null;
			}

			@SuppressWarnings("unchecked")
			private Optional<Consumer<T>> success(Parameters parameters, Arguments arguments) {
				return parameters.callbacks()
						.filter(c -> c.javaType().is(Consumer.class))
						.findFirst()
						.flatMap(c -> arguments.of(c.position()))
						.map(arg -> (Consumer<T>) arg);
			}

			@SuppressWarnings("unchecked")
			private Optional<Consumer<? super Throwable>> failure(Parameters parameters, Arguments arguments) {
				return parameters.callbacks()
						.filter(c -> consumer(c) && c.javaType().parameterized()
								.map(Parameterized::firstArg)
								.map(JavaType::valueOf)
								.filter(a -> a.is(Throwable.class))
								.isPresent())
						.findFirst()
						.flatMap(c -> arguments.of(c.position()))
						.map(arg -> (Consumer<? super Throwable>) arg);
			}

			@SuppressWarnings("unchecked")
			private Optional<BiConsumer<Object, ? super Throwable>> subscriber(Parameters parameters, JavaType returnType, Arguments arguments) {
				return parameters.callbacks()
						.filter(c -> biConsumer(c) && c.javaType().parameterized()
								.map(ParameterizedType::getActualTypeArguments)
								.filter(args -> JavaType.valueOf(args[0]).equals(returnType) && JavaType.valueOf(args[1]).is(Throwable.class))
								.isPresent())
						.findFirst()
						.flatMap(c -> arguments.of(c.position()))
						.map(arg -> (BiConsumer<Object, ? super Throwable>) arg);
			}

			@Override
			public JavaType returnType() {
				return fn.returnType();
			}
		};
	}
	
	@Override
	public boolean test(Endpoint endpoint) {
		return endpoint.returnType().equals(JavaType.none()) 
			&& endpoint.parameters().callbacks().anyMatch(c -> consumer(c) || biConsumer(c));
	}

	private boolean consumer(CallbackParameter callback) {
		JavaType javaType = callback.javaType();
		return javaType.is(Consumer.class) && javaType.parameterized().isPresent();
	}

	private boolean biConsumer(CallbackParameter callback) {
		JavaType javaType = callback.javaType();

		return javaType.compatible(BiConsumer.class)
			&& javaType.parameterized()
					.map(ParameterizedType::getActualTypeArguments)
					.filter(args -> JavaType.valueOf(args[1]).compatible(Throwable.class))
					.isPresent();
	}

	@Override
	public JavaType adapted(Endpoint endpoint) {
		return argument(endpoint, this::consumer).or(() -> argument(endpoint, this::biConsumer))
				.map(JavaType::valueOf)
				.orElseGet(JavaType::none);
	}

	private Optional<Type> argument(Endpoint endpoint, Predicate<CallbackParameter> p) {
		return endpoint.parameters().callbacks()
			.filter(p)
			.findFirst()
			.flatMap(c -> c.javaType().parameterized().map(Parameterized::firstArg));
	}

	public static CompletionStageCallbackResponseT<Object> get() {
		return SINGLE_INSTANCE;
	}
}

package com.github.ljtfreitas.julian.k;

import com.github.ljtfreitas.julian.Arguments;
import com.github.ljtfreitas.julian.Arguments.Argument;
import com.github.ljtfreitas.julian.Endpoint;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.Response;
import com.github.ljtfreitas.julian.ResponseFn;
import com.github.ljtfreitas.julian.ResponseT;
import com.github.ljtfreitas.julian.k.coroutines.Coroutines;
import kotlin.coroutines.Continuation;

import java.util.Optional;

public class SuspendKFunctionResponseT<T> implements ResponseT<T, T> {

    @Override
    public <A> ResponseFn<A, T> bind(Endpoint endpoint, ResponseFn<A, T> fn) {
        return new ResponseFn<>() {

            @SuppressWarnings("unchecked")
            @Override
            public T join(Promise<? extends Response<A>> response, Arguments arguments) {
                Continuation<T> continuation = arguments.of(Continuation.class).findFirst()
                        .map(Argument::value)
                        .map(Continuation.class::cast)
                        .orElseThrow();

                return (T) Coroutines.await(fn.run(response, arguments), continuation);
            }

            @Override
            public JavaType returnType() {
                return fn.returnType();
            }
        };
    }

    @Override
    public JavaType adapted(Endpoint endpoint) {
        SuspendKFunctionEndpoint suspend = (SuspendKFunctionEndpoint) endpoint;
        return Optional.ofNullable(suspend.continuationArgumentType())
                .flatMap(javaType -> javaType.parameterized()
                        .map(JavaType.Parameterized::firstArg)
                        .map(JavaType::valueOf)
                        .flatMap(JavaType::wildcard)
                        .filter(w -> w.getLowerBounds().length != 0)
                        .map(w -> w.getLowerBounds()[0]))
                .map(JavaType::valueOf)
                .orElseGet(JavaType::object);
    }

    @Override
    public boolean test(Endpoint endpoint) {
        return endpoint instanceof SuspendKFunctionEndpoint;
    }
}

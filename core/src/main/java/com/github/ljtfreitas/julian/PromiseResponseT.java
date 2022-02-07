package com.github.ljtfreitas.julian;

class PromiseResponseT<T> implements ResponseT<T, Promise<T>> {

    private static final PromiseResponseT<Object> SINGLE_INSTANCE = new PromiseResponseT<>();

    @Override
    public <A> ResponseFn<A, Promise<T>> bind(Endpoint endpoint, ResponseFn<A, T> fn) {
        return new ResponseFn<>() {

            @Override
            public Promise<T> join(Promise<? extends Response<A>> response, Arguments arguments) {
                return fn.run(response, arguments);
            }

            @Override
            public JavaType returnType() {
                return fn.returnType();
            }
        };
    }

    @Override
    public JavaType adapted(Endpoint endpoint) {
        return endpoint.returnType().parameterized().map(JavaType.Parameterized::firstArg).map(JavaType::valueOf)
                .orElseGet(JavaType::object);
    }

    @Override
    public boolean test(Endpoint endpoint) {
        return endpoint.returnType().is(Promise.class);
    }

    public static PromiseResponseT<Object> get() {
        return SINGLE_INSTANCE;
    }
}

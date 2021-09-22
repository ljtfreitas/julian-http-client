package com.github.ljtfreitas.julian;

class PromiseResponseT<T> implements ResponseT<Promise<T>, T> {

    private static final PromiseResponseT<Object> SINGLE_INSTANCE = new PromiseResponseT<>();

    @Override
    public <A> ResponseFn<Promise<T>, A> comp(Endpoint endpoint, ResponseFn<T, A> fn) {
        return new ResponseFn<>() {

            @Override
            public Promise<T> join(RequestIO<A> request, Arguments arguments) {
                return request.comp(fn, arguments);
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

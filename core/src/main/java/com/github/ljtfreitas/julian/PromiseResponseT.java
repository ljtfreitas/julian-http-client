package com.github.ljtfreitas.julian;

class PromiseResponseT implements ResponseT<Object, Promise<Object>> {

    private static final PromiseResponseT SINGLE_INSTANCE = new PromiseResponseT();

    @Override
    public <A> ResponseFn<A, Promise<Object>> bind(Endpoint endpoint, ResponseFn<A, Object> fn) {
        return new ResponseFn<>() {

            @Override
            public Promise<Object> join(Promise<? extends Response<A>> response, Arguments arguments) {
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

    public static PromiseResponseT get() {
        return SINGLE_INSTANCE;
    }
}

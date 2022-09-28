package com.github.ljtfreitas.julian;

class PromiseResponseT implements ResponseT<Object, Promise<Object>> {

    private static final PromiseResponseT SINGLE_INSTANCE = new PromiseResponseT();

    @Override
    public <A> ResponseFn<A, Promise<Object>> bind(Endpoint endpoint, ResponseFn<A, Object> next) {
        return new ResponseFn<>() {

            @Override
            public Promise<Object> join(Promise<? extends Response<A, ? extends Throwable>> response, Arguments arguments) {
                return next.run(response, arguments);
            }

            @Override
            public JavaType returnType() {
                return next.returnType();
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

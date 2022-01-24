package com.github.ljtfreitas.julian.reactor;

import com.github.ljtfreitas.julian.Arguments;
import com.github.ljtfreitas.julian.Endpoint;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.Response;
import com.github.ljtfreitas.julian.ResponseFn;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FluxResponseTTest {

    private final FluxResponseT<String> subject = new FluxResponseT<>();

    @Nested
    class Predicates {

        @Test
        void supported(@Mock Endpoint endpoint) {
            when(endpoint.returnType()).thenReturn(JavaType.parameterized(Flux.class, String.class));

            assertTrue(subject.test(endpoint));
        }

        @Test
        void unsupported(@Mock Endpoint endpoint) {
            when(endpoint.returnType()).thenReturn(JavaType.valueOf(String.class));

            assertFalse(subject.test(endpoint));
        }

    }

    @Nested
    class Adapted {

        @Test
        void parameterized(@Mock Endpoint endpoint) {
            when(endpoint.returnType()).thenReturn(JavaType.parameterized(Flux.class, String.class));

            JavaType adapted = subject.adapted(endpoint);

            assertEquals(JavaType.parameterized(Collection.class, String.class), adapted);
        }

        @Test
        void adaptToCollectionWhenTypeArgumentIsMissing(@Mock Endpoint endpoint) {
            when(endpoint.returnType()).thenReturn(JavaType.valueOf(Flux.class));

            JavaType adapted = subject.adapted(endpoint);

            assertEquals(JavaType.parameterized(Collection.class, Object.class), adapted);
        }
    }

    @Test
    void bind(@Mock Endpoint endpoint, @Mock Promise<Response<String, Exception>, Exception> response, @Mock ResponseFn<String, Collection<String>> fn) {
        Arguments arguments = Arguments.empty();

        when(fn.run(response, arguments)).thenReturn(Promise.done(List.of("one", "two", "three")));

        Flux<String> flux = subject.bind(endpoint, fn).join(response, arguments);

        StepVerifier.create(flux)
                .expectNext("one", "two", "three")
                .expectComplete();
    }

    @Test
    void bindAsMono(@Mock Endpoint endpoint, @Mock ResponseFn<String, Collection<String>> fn) {
        Arguments arguments = Arguments.empty();

        Promise<Response<String, Exception>, Exception> response = new MonoPromise<>(Mono.just(Response.done("one,two,three")));

        when(fn.run(response, arguments)).then(i -> response.then(r -> r.map(s -> List.of(s.split(",")))));

        Flux<String> flux = subject.bind(endpoint, fn).join(response, arguments);

        StepVerifier.create(flux)
                .expectNext("one", "two", "three")
                .expectComplete();
    }

    @Test
    void failure(@Mock Endpoint endpoint, @Mock Promise<Response<String, Exception>, Exception> response, @Mock ResponseFn<String, Collection<String>> fn) {
        Arguments arguments = Arguments.empty();

        RuntimeException exception = new RuntimeException("oops");
        when(fn.run(response, arguments)).then(i -> Promise.failed(exception));

        Flux<String> flux = subject.bind(endpoint, fn).join(response, arguments);

        StepVerifier.create(flux)
                .expectErrorMatches(t -> t.equals(exception));
    }

    @Test
    void failureAsMono(@Mock Endpoint endpoint, @Mock ResponseFn<String, Collection<String>> fn) {
        Arguments arguments = Arguments.empty();

        RuntimeException exception = new RuntimeException("oops");

        Promise<Response<String, Exception>, Exception> response = new MonoPromise<>(Mono.error(exception));

        when(fn.run(response, arguments)).then(i -> response);

        Flux<String> flux = subject.bind(endpoint, fn).join(response, arguments);

        StepVerifier.create(flux)
                .expectErrorMatches(t -> t.equals(exception));
    }
}
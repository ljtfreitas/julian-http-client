package com.github.ljtfreitas.julian.mutiny;

import com.github.ljtfreitas.julian.Arguments;
import com.github.ljtfreitas.julian.Endpoint;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.Response;
import com.github.ljtfreitas.julian.ResponseFn;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UniResponseTTest {

    private final UniResponseT<String> subject = new UniResponseT<>();

    @Nested
    class Predicates {

        @Test
        void supported(@Mock Endpoint endpoint) {
            when(endpoint.returnType()).thenReturn(JavaType.parameterized(Uni.class, String.class));

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
            when(endpoint.returnType()).thenReturn(JavaType.parameterized(Uni.class, String.class));

            JavaType adapted = subject.adapted(endpoint);

            assertEquals(JavaType.valueOf(String.class), adapted);
        }

        @Test
        void adaptToObjectWhenTypeArgumentIsMissing(@Mock Endpoint endpoint) {
            when(endpoint.returnType()).thenReturn(JavaType.valueOf(Uni.class));

            JavaType adapted = subject.adapted(endpoint);

            assertEquals(JavaType.valueOf(Object.class), adapted);
        }
    }

    @Test
    void bind(@Mock Endpoint endpoint, @Mock Promise<Response<String, Exception>, Exception> response, @Mock ResponseFn<String, String> fn) {
        Arguments arguments = Arguments.empty();

        when(fn.run(response, arguments)).thenReturn(Promise.done("hello"));

        Uni<String> uni = subject.bind(endpoint, fn).join(response, arguments);

        UniAssertSubscriber<String> subscriber = uni.subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted()
                .assertItem("hello");
    }

    @Test
    void failure(@Mock Endpoint endpoint, @Mock Promise<Response<String, Exception>, Exception> response, @Mock ResponseFn<String, String> fn) {
        Arguments arguments = Arguments.empty();

        RuntimeException exception = new RuntimeException("oops");
        when(fn.run(response, arguments)).then(i -> Promise.failed(exception));

        Uni<String> uni = subject.bind(endpoint, fn).join(response, arguments);

        UniAssertSubscriber<String> subscriber = uni.subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(RuntimeException.class, exception.getMessage());
    }
}
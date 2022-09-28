package com.github.ljtfreitas.julian.mutiny;

import com.github.ljtfreitas.julian.Arguments;
import com.github.ljtfreitas.julian.Endpoint;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.ObjectResponseT;
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

    @Mock
    private Endpoint endpoint;
    
    private final UniResponseT subject = new UniResponseT();

    @Nested
    class Predicates {

        @Test
        void supported() {
            when(endpoint.returnType()).thenReturn(JavaType.parameterized(Uni.class, String.class));

            assertTrue(subject.test(endpoint));
        }

        @Test
        void unsupported() {
            when(endpoint.returnType()).thenReturn(JavaType.valueOf(String.class));

            assertFalse(subject.test(endpoint));
        }
    }

    @Nested
    class Adapted {

        @Test
        void parameterized() {
            when(endpoint.returnType()).thenReturn(JavaType.parameterized(Uni.class, String.class));

            JavaType adapted = subject.adapted(endpoint);

            assertEquals(JavaType.valueOf(String.class), adapted);
        }

        @Test
        void adaptToObjectWhenTypeArgumentIsMissing() {
            when(endpoint.returnType()).thenReturn(JavaType.valueOf(Uni.class));

            JavaType adapted = subject.adapted(endpoint);

            assertEquals(JavaType.valueOf(Object.class), adapted);
        }
    }

    @Test
    void bind() {
        Promise<Response<String, Throwable>> response = Promise.done(Response.done("hello"));

        ResponseFn<String, Object> fn = new ObjectResponseT<>().bind(endpoint, null);

        Uni<Object> uni = subject.bind(endpoint, fn).join(response, Arguments.empty());

        UniAssertSubscriber<Object> subscriber = uni.subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted()
                .assertItem("hello");
    }

    @Test
    void failure() {
        RuntimeException exception = new RuntimeException("oops");

        Promise<Response<String, Throwable>> response = Promise.failed(exception);

        ResponseFn<String, Object> fn = new ObjectResponseT<>().bind(endpoint, null);

        Uni<Object> uni = subject.bind(endpoint, fn).join(response, Arguments.empty());

        UniAssertSubscriber<Object> subscriber = uni.subscribe().withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(RuntimeException.class, exception.getMessage());
    }
}
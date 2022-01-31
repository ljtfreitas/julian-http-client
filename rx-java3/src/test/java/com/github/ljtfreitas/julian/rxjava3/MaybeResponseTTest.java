package com.github.ljtfreitas.julian.rxjava3;

import com.github.ljtfreitas.julian.Arguments;
import com.github.ljtfreitas.julian.Endpoint;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.ObjectResponseT;
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.Response;
import com.github.ljtfreitas.julian.ResponseFn;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.observers.TestObserver;
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
class MaybeResponseTTest {

    @Mock
    private Endpoint endpoint;

    private final MaybeResponseT<String> subject = new MaybeResponseT<>();

    @Nested
    class Predicates {

        @Test
        void supported() {
            when(endpoint.returnType()).thenReturn(JavaType.parameterized(Maybe.class, String.class));

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
            when(endpoint.returnType()).thenReturn(JavaType.parameterized(Maybe.class, String.class));

            JavaType adapted = subject.adapted(endpoint);

            assertEquals(JavaType.valueOf(String.class), adapted);
        }

        @Test
        void adaptToObjectWhenTypeArgumentIsMissing() {
            when(endpoint.returnType()).thenReturn(JavaType.valueOf(Maybe.class));

            JavaType adapted = subject.adapted(endpoint);

            assertEquals(JavaType.valueOf(Object.class), adapted);
        }
    }

    @Test
    void bind() {
        Promise<Response<String, Exception>, Exception> response = Promise.done(Response.done("hello"));

        ResponseFn<String, String> fn = new ObjectResponseT<String>().bind(endpoint, null);

        Maybe<String> maybe = subject.bind(endpoint, fn).join(response, Arguments.empty());

        TestObserver<String> observer = new TestObserver<>();
        maybe.subscribe(observer);

        observer.assertComplete()
                .assertNoErrors()
                .assertValue("hello");
    }
}
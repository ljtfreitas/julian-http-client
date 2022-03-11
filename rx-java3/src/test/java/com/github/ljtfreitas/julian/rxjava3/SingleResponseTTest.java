package com.github.ljtfreitas.julian.rxjava3;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.ljtfreitas.julian.Arguments;
import com.github.ljtfreitas.julian.Endpoint;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.ObjectResponseT;
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.Response;
import com.github.ljtfreitas.julian.ResponseFn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SingleResponseTTest {

    @Mock
    private Endpoint endpoint;
    
    private final SingleResponseT subject = new SingleResponseT();

    @Nested
    class Predicates {

        @Test
        void supported() {
            when(endpoint.returnType()).thenReturn(JavaType.parameterized(Single.class, String.class));

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
            when(endpoint.returnType()).thenReturn(JavaType.parameterized(Single.class, String.class));

            JavaType adapted = subject.adapted(endpoint);

            assertEquals(JavaType.valueOf(String.class), adapted);
        }

        @Test
        void adaptToObjectWhenTypeArgumentIsMissing() {
            when(endpoint.returnType()).thenReturn(JavaType.valueOf(Single.class));

            JavaType adapted = subject.adapted(endpoint);

            assertEquals(JavaType.valueOf(Object.class), adapted);
        }
    }

    @Test
    void bind() {
        Promise<Response<String>> response = new SinglePromise<>(Single.just(Response.done("hello")));

        ResponseFn<String, Object> fn = new ObjectResponseT<>().bind(endpoint, null);

        Single<Object> single = subject.bind(endpoint, fn).join(response, Arguments.empty());

        TestObserver<Object> observer = new TestObserver<>();
        single.subscribe(observer);

        observer.assertComplete()
                .assertNoErrors()
                .assertValue("hello");
    }
}
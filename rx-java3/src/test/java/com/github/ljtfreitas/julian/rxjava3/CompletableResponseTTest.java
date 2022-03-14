package com.github.ljtfreitas.julian.rxjava3;

import com.github.ljtfreitas.julian.Arguments;
import com.github.ljtfreitas.julian.DefaultResponseT;
import com.github.ljtfreitas.julian.Endpoint;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.ObjectResponseT;
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.Response;
import com.github.ljtfreitas.julian.ResponseFn;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompletableResponseTTest {

    @Mock
    private Endpoint endpoint;
    
    private final CompletableResponseT subject = new CompletableResponseT();

    @Nested
    class Predicates {

        @Test
        void supported() {
            when(endpoint.returnType()).thenReturn(JavaType.valueOf(Completable.class));

            assertTrue(subject.test(endpoint));
        }

        @Test
        void unsupported() {
            when(endpoint.returnType()).thenReturn(JavaType.valueOf(String.class));

            assertFalse(subject.test(endpoint));
        }

    }

    @Test
    void adapted() {
        JavaType adapted = subject.adapted(null);

        assertEquals(JavaType.none(), adapted);
    }

    @Test
    void bind() {
        Promise<Response<Object>> response = new SinglePromise<>(Single.just(Response.done(null)));

        ResponseFn<Object, Response<Object>> bla = new DefaultResponseT().bind(endpoint, new ObjectResponseT<>().bind(endpoint, null));

        Arguments arguments = Arguments.empty();

        Completable completable = subject.bind(endpoint, bla).join(response, arguments);

        TestObserver<Void> observer = new TestObserver<>();
        completable.subscribe(observer);

        observer.assertComplete()
                .assertNoErrors();
    }
}
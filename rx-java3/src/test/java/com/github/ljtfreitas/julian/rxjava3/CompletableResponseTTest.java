package com.github.ljtfreitas.julian.rxjava3;

import com.github.ljtfreitas.julian.Arguments;
import com.github.ljtfreitas.julian.Endpoint;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.RequestIO;
import com.github.ljtfreitas.julian.ResponseFn;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.observers.TestObserver;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompletableResponseTTest {

    private final CompletableResponseT subject = new CompletableResponseT();

    @Nested
    class Predicates {

        @Test
        void supported(@Mock Endpoint endpoint) {
            when(endpoint.returnType()).thenReturn(JavaType.valueOf(Completable.class));

            assertTrue(subject.test(endpoint));
        }

        @Test
        void unsupported(@Mock Endpoint endpoint) {
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
    void bind(@Mock Endpoint endpoint, @Mock RequestIO<String> request, @Mock ResponseFn<String, Void> fn) {
        Arguments arguments = Arguments.empty();

        when(fn.run(request, arguments)).thenReturn(Promise.done(null));

        Completable completable = subject.bind(endpoint, fn).join(request, arguments);

        TestObserver<String> observer = new TestObserver<>();
        completable.subscribe(observer);

        observer.assertComplete()
                .assertNoErrors();
    }
}
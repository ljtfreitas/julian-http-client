package com.github.ljtfreitas.julian.rxjava3;

import com.github.ljtfreitas.julian.Arguments;
import com.github.ljtfreitas.julian.Endpoint;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.RequestIO;
import com.github.ljtfreitas.julian.ResponseFn;
import io.reactivex.rxjava3.core.Single;
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
class SingleResponseTTest {

    private final SingleResponseT<String> subject = new SingleResponseT<>();

    @Nested
    class Predicates {

        @Test
        void supported(@Mock Endpoint endpoint) {
            when(endpoint.returnType()).thenReturn(JavaType.parameterized(Single.class, String.class));

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
            when(endpoint.returnType()).thenReturn(JavaType.parameterized(Single.class, String.class));

            JavaType adapted = subject.adapted(endpoint);

            assertEquals(JavaType.valueOf(String.class), adapted);
        }

        @Test
        void adaptToCollectionWhenTypeArgumentIsMissing(@Mock Endpoint endpoint) {
            when(endpoint.returnType()).thenReturn(JavaType.valueOf(Single.class));

            JavaType adapted = subject.adapted(endpoint);

            assertEquals(JavaType.valueOf(Object.class), adapted);
        }
    }

    @Test
    void bind(@Mock Endpoint endpoint, @Mock RequestIO<String> request, @Mock ResponseFn<String, String> fn) {
        Arguments arguments = Arguments.empty();

        when(fn.run(request, arguments)).thenReturn(Promise.done("hello"));

        Single<String> single = subject.bind(endpoint, fn).join(request, arguments);

        TestObserver<String> observer = new TestObserver<>();
        single.subscribe(observer);

        observer.assertComplete()
                .assertNoErrors()
                .assertValue("hello");
    }
}
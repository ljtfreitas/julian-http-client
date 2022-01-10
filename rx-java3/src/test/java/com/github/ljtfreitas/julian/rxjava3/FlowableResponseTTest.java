package com.github.ljtfreitas.julian.rxjava3;

import com.github.ljtfreitas.julian.Arguments;
import com.github.ljtfreitas.julian.Endpoint;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.RequestIO;
import com.github.ljtfreitas.julian.ResponseFn;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableSubscriber;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FlowableResponseTTest {

    private final FlowableResponseT<String> subject = new FlowableResponseT<>();

    @Nested
    class Predicates {

        @Test
        void supported(@Mock Endpoint endpoint) {
            when(endpoint.returnType()).thenReturn(JavaType.parameterized(Flowable.class, String.class));

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
            when(endpoint.returnType()).thenReturn(JavaType.parameterized(Flowable.class, String.class));

            JavaType adapted = subject.adapted(endpoint);

            assertEquals(JavaType.parameterized(Collection.class, String.class), adapted);
        }

        @Test
        void adaptToCollectionWhenTypeArgumentIsMissing(@Mock Endpoint endpoint) {
            when(endpoint.returnType()).thenReturn(JavaType.valueOf(Flowable.class));

            JavaType adapted = subject.adapted(endpoint);

            assertEquals(JavaType.parameterized(Collection.class, Object.class), adapted);
        }
    }

    @Test
    void bind(@Mock Endpoint endpoint, @Mock RequestIO<String> request, @Mock ResponseFn<String, Collection<String>> fn) {
        Arguments arguments = Arguments.empty();

        when(fn.run(request, arguments)).thenReturn(Promise.done(List.of("one", "two", "three")));

        Flowable<String> flowable = subject.bind(endpoint, fn).join(request, arguments);

        TestSubscriber<String> subscriber = new TestSubscriber<>();
        flowable.subscribe(subscriber);

        subscriber.assertComplete()
                    .assertNoErrors()
                    .assertValues("one", "two", "three");
    }
}
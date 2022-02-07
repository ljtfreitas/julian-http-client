package com.github.ljtfreitas.julian.rxjava3;

import com.github.ljtfreitas.julian.Arguments;
import com.github.ljtfreitas.julian.CollectionResponseT;
import com.github.ljtfreitas.julian.Endpoint;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.ObjectResponseT;
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.Response;
import com.github.ljtfreitas.julian.ResponseFn;
import io.reactivex.rxjava3.core.Flowable;
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

    @Mock
    private Endpoint endpoint;
    
    private final FlowableResponseT<String> subject = new FlowableResponseT<>();

    @Nested
    class Predicates {

        @Test
        void supported() {
            when(endpoint.returnType()).thenReturn(JavaType.parameterized(Flowable.class, String.class));

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
            when(endpoint.returnType()).thenReturn(JavaType.parameterized(Flowable.class, String.class));

            JavaType adapted = subject.adapted(endpoint);

            assertEquals(JavaType.parameterized(Collection.class, String.class), adapted);
        }

        @Test
        void adaptToCollectionWhenTypeArgumentIsMissing() {
            when(endpoint.returnType()).thenReturn(JavaType.valueOf(Flowable.class));

            JavaType adapted = subject.adapted(endpoint);

            assertEquals(JavaType.parameterized(Collection.class, Object.class), adapted);
        }
    }

    @Test
    void bind() {
        Promise<Response<Collection<String>>> response = Promise.done(Response.done(List.of("one", "two", "three")));

        ResponseFn<Collection<String>, Collection<String>> fn = new CollectionResponseT<String>().bind(endpoint,
                new ObjectResponseT<Collection<String>>().bind(endpoint, null));

        when(endpoint.returnType()).thenReturn(JavaType.parameterized(Collection.class, String.class));

        Flowable<String> flowable = subject.bind(endpoint, fn).join(response, Arguments.empty());

        TestSubscriber<String> subscriber = new TestSubscriber<>();
        flowable.subscribe(subscriber);

        subscriber.assertComplete()
                    .assertNoErrors()
                    .assertValues("one", "two", "three");
    }
}
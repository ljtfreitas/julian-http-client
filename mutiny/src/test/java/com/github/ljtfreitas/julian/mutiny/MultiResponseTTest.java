package com.github.ljtfreitas.julian.mutiny;

import com.github.ljtfreitas.julian.Arguments;
import com.github.ljtfreitas.julian.CollectionResponseT;
import com.github.ljtfreitas.julian.Endpoint;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.ObjectResponseT;
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.Response;
import com.github.ljtfreitas.julian.ResponseFn;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
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
class MultiResponseTTest {

    @Mock
    private Endpoint endpoint;

    private final MultiResponseT subject = new MultiResponseT();

    @Nested
    class Predicates {

        @Test
        void supported() {
            when(endpoint.returnType()).thenReturn(JavaType.parameterized(Multi.class, String.class));

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
            when(endpoint.returnType()).thenReturn(JavaType.parameterized(Multi.class, String.class));

            JavaType adapted = subject.adapted(endpoint);

            assertEquals(JavaType.parameterized(Collection.class, String.class), adapted);
        }

        @Test
        void adaptToCollectionWhenTypeArgumentIsMissing() {
            when(endpoint.returnType()).thenReturn(JavaType.valueOf(Multi.class));

            JavaType adapted = subject.adapted(endpoint);

            assertEquals(JavaType.parameterized(Collection.class, Object.class), adapted);
        }
    }

    @Test
    void bind() {
        Promise<Response<Collection<String>>> response = Promise.done(Response.done(List.of("one", "two", "three")));

        when(endpoint.returnType()).thenReturn(JavaType.parameterized(Collection.class, String.class));

        ResponseFn<Collection<String>, Collection<Object>> fn = new CollectionResponseT().bind(endpoint,
                new ObjectResponseT<Collection<Object>>().bind(endpoint, null));

        Multi<Object> multi = subject.bind(endpoint, fn).join(response, Arguments.empty());

        AssertSubscriber<Object> subscriber = multi.subscribe().withSubscriber(AssertSubscriber.create(3));

        subscriber.assertCompleted()
                .assertItems("one", "two", "three");
    }

    @Test
    void failure() {
        RuntimeException exception = new RuntimeException("oops");

        Promise<Response<Collection<String>>> response = Promise.failed(exception);

        ResponseFn<Collection<String>, Collection<Object>> fn = new CollectionResponseT().bind(endpoint,
                new ObjectResponseT<Collection<Object>>().bind(endpoint, null));

        Multi<Object> multi = subject.bind(endpoint, fn).join(response, Arguments.empty());

        AssertSubscriber<Object> subscriber = multi.subscribe().withSubscriber(AssertSubscriber.create());

        subscriber.assertFailedWith(RuntimeException.class, exception.getMessage());
    }
}
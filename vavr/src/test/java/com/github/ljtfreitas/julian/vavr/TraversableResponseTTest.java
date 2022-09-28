package com.github.ljtfreitas.julian.vavr;

import com.github.ljtfreitas.julian.Arguments;
import com.github.ljtfreitas.julian.CollectionResponseT;
import com.github.ljtfreitas.julian.Endpoint;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.ObjectResponseT;
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.Response;
import com.github.ljtfreitas.julian.ResponseFn;
import io.vavr.collection.Traversable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TraversableResponseTTest {

    @Mock
    private Endpoint endpoint;

    private final TraversableResponseT responseT = new TraversableResponseT();

    @Nested
    class Predicates {

        @Test
        void supported() {
            when(endpoint.returnType()).thenReturn(JavaType.parameterized(Traversable.class, String.class));
            assertTrue(responseT.test(endpoint));
        }

        @Test
        void unsupported() {
            when(endpoint.returnType()).thenReturn(JavaType.valueOf(String.class));
            assertFalse(responseT.test(endpoint));
        }
    }

    @Nested
    class Adapt {

        @Test
        void adaptToCollection() {
            when(endpoint.returnType()).thenReturn(JavaType.parameterized(Traversable.class, String.class));

            JavaType adapted = responseT.adapted(endpoint);

            assertThat(adapted, equalTo(JavaType.parameterized(Collection.class, String.class)));
        }

        @Test
        @DisplayName("adapt to collection of Object when Traversable is not parameterized")
        void adaptToCollectionOfObjectWhenTraversableIsNotParameterized() {
            when(endpoint.returnType()).thenReturn(JavaType.valueOf(Traversable.class));

            JavaType adapted = responseT.adapted(endpoint);

            assertThat(adapted, equalTo(JavaType.parameterized(Collection.class, Object.class)));
        }
    }

    @Test
    void bind() {
        when(endpoint.returnType()).thenReturn(JavaType.parameterized(Collection.class, String.class));

        Collection<String> values = java.util.List.of("one", "two", "three");

        Promise<Response<Collection<String>, Throwable>> promise = Promise.done(Response.done(values));

        ResponseFn<Collection<String>, Collection<Object>> fn = new CollectionResponseT().bind(endpoint,
                new ObjectResponseT<Collection<Object>>().bind(endpoint, null));

        Traversable<Object> traversable = responseT.bind(endpoint, fn).join(promise, Arguments.empty());

        assertTrue(traversable.containsAll(values));
    }

    @Test
    void bindNullCollection() {
        when(endpoint.returnType()).thenReturn(JavaType.parameterized(Collection.class, String.class));

        Promise<Response<Collection<String>, Throwable>> promise = Promise.done(Response.done(null));

        ResponseFn<Collection<String>, Collection<Object>> fn = new CollectionResponseT().bind(endpoint,
                new ObjectResponseT<Collection<Object>>().bind(endpoint, null));

        Traversable<Object> traversable = responseT.bind(endpoint, fn).join(promise, Arguments.empty());

        assertTrue(traversable.isEmpty());
    }
}
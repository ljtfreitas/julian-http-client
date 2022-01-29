package com.github.ljtfreitas.julian.vavr;

import com.github.ljtfreitas.julian.Arguments;
import com.github.ljtfreitas.julian.Endpoint;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.Response;
import com.github.ljtfreitas.julian.ResponseFn;
import io.vavr.collection.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SetResponseTTest {

    @Mock
    private Endpoint endpoint;

    private final SetResponseT<String> responseT = new SetResponseT<>();

    @Nested
    class Predicates {

        @Test
        void supported() {
            when(endpoint.returnType()).thenReturn(JavaType.parameterized(Set.class, String.class));
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
            when(endpoint.returnType()).thenReturn(JavaType.parameterized(Set.class, String.class));

            JavaType adapted = responseT.adapted(endpoint);

            assertThat(adapted, equalTo(JavaType.parameterized(Collection.class, String.class)));
        }

        @Test
        @DisplayName("adapt to collection of Object when Set is not parameterized")
        void adaptToCollectionOfObjectWhenSetIsNotParameterized() {
            when(endpoint.returnType()).thenReturn(JavaType.valueOf(Set.class));

            JavaType adapted = responseT.adapted(endpoint);

            assertThat(adapted, equalTo(JavaType.parameterized(Collection.class, Object.class)));
        }
    }

    @Test
    void bind(@Mock Promise<Response<Collection<String>, Exception>, Exception> promise, @Mock ResponseFn<Collection<String>, Collection<String>> fn) {
        Arguments arguments = Arguments.empty();

        Collection<String> values = List.of("one", "two", "three");

        when(fn.run(promise, arguments)).thenReturn(Promise.done(values));

        Set<String> set = responseT.bind(endpoint, fn).join(promise, arguments);

        assertTrue(set.containsAll(values));
    }

    @Test
    void bindNullCollection(@Mock Promise<Response<Collection<String>, Exception>, Exception> promise, @Mock ResponseFn<Collection<String>, Collection<String>> fn) {
        Arguments arguments = Arguments.empty();

        when(fn.run(promise, arguments)).thenReturn(Promise.done(null));

        Set<String> set = responseT.bind(endpoint, fn).join(promise, arguments);

        assertTrue(set.isEmpty());
    }
}
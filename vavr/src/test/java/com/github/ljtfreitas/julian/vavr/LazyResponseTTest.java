package com.github.ljtfreitas.julian.vavr;

import com.github.ljtfreitas.julian.Arguments;
import com.github.ljtfreitas.julian.Endpoint;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.ObjectResponseT;
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.Response;
import com.github.ljtfreitas.julian.ResponseFn;
import io.vavr.Lazy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LazyResponseTTest {

    @Mock
    private Endpoint endpoint;

    private final LazyResponseT responseT = new LazyResponseT();

    @Nested
    class Predicates {

        @Test
        void support() {
            when(endpoint.returnType()).thenReturn(JavaType.parameterized(Lazy.class, String.class));
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
        void adaptToArgument() {
            when(endpoint.returnType()).thenReturn(JavaType.parameterized(Lazy.class, String.class));

            JavaType adapted = responseT.adapted(endpoint);

            assertThat(adapted, equalTo(JavaType.valueOf(String.class)));
        }

        @Test
        @DisplayName("adapt to Object when Lazy is not parameterized")
        void adaptToObjectWhenLazyIsNotParameterized() {
            when(endpoint.returnType()).thenReturn(JavaType.valueOf(Lazy.class));

            JavaType adapted = responseT.adapted(endpoint);

            assertThat(adapted, equalTo(JavaType.valueOf(Object.class)));
        }
    }

    @Test
    void bind() {
        String content = "hello";

        Lazy<Object> lazy = responseT.bind(endpoint, new ObjectResponseT<>().bind(endpoint, null))
                .join(Promise.done(Response.done(content)), Arguments.empty());

        assertFalse(lazy.isEvaluated());

        assertThat(lazy.get(), equalTo(content));
    }

    @Test
    void bindFailure() {
        RuntimeException failure = new RuntimeException("oops");

        Lazy<Object> lazy = responseT.bind(endpoint, new ObjectResponseT<>().bind(endpoint, null))
                .join(Promise.failed(failure), Arguments.empty());

        assertFalse(lazy.isEvaluated());

        RuntimeException actual = assertThrows(RuntimeException.class, lazy::get);

        assertSame(failure, actual);
    }
}
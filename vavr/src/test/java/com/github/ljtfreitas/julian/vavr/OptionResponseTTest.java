package com.github.ljtfreitas.julian.vavr;

import com.github.ljtfreitas.julian.Arguments;
import com.github.ljtfreitas.julian.Endpoint;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.Response;
import com.github.ljtfreitas.julian.ResponseFn;
import io.vavr.control.Option;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OptionResponseTTest {

    @Mock
    private Endpoint endpoint;

    private final OptionResponseT responseT = new OptionResponseT();

    @Nested
    class Predicates {

        @Test
        void supported() {
            when(endpoint.returnType()).thenReturn(JavaType.parameterized(Option.class, String.class));
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
            when(endpoint.returnType()).thenReturn(JavaType.parameterized(Option.class, String.class));

            JavaType adapted = responseT.adapted(endpoint);

            assertThat(adapted, equalTo(JavaType.valueOf(String.class)));
        }

        @Test
        @DisplayName("adapt to Object when Option is not parameterized")
        void adaptToObjectWhenOptionIsNotParameterized() {
            when(endpoint.returnType()).thenReturn(JavaType.valueOf(Option.class));

            JavaType adapted = responseT.adapted(endpoint);

            assertThat(adapted, equalTo(JavaType.valueOf(Object.class)));
        }
    }

    @Test
    void bind(@Mock Promise<Response<String>> promise, @Mock ResponseFn<String, Object> fn) {
        Arguments arguments = Arguments.empty();

        String content = "hello";

        when(fn.run(promise, arguments)).thenReturn(Promise.done(content));

        Option<Object> option = responseT.bind(endpoint, fn).join(promise, arguments);

        assertTrue(option.exists(content::equals));
    }

    @Test
    void bindNullValue(@Mock Promise<Response<String>> promise, @Mock ResponseFn<String, Object> fn) {
        Arguments arguments = Arguments.empty();

        when(fn.run(promise, arguments)).thenReturn(Promise.done(null));

        Option<Object> option = responseT.bind(endpoint, fn).join(promise, arguments);

        assertTrue(option.isEmpty());
    }
}
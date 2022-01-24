package com.github.ljtfreitas.julian;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExceptResponseTTest {

    @Mock
    private Endpoint endpoint;

    private final ExceptResponseT<String> subject = new ExceptResponseT<>();

    @Nested
    class Predicates {

        @Test
        void supported() {
            when(endpoint.returnType()).thenReturn(JavaType.parameterized(Except.class, String.class));

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
            when(endpoint.returnType()).thenReturn(JavaType.parameterized(Except.class, String.class));

            assertEquals(JavaType.valueOf(String.class), subject.adapted(endpoint));
        }

        @Test
        void simple() {
            when(endpoint.returnType()).thenReturn(JavaType.object());

            assertEquals(JavaType.object(), subject.adapted(endpoint));
        }
    }

    @Test
    void compose(@Mock ResponseFn<String, String> fn, @Mock Promise<Response<String, Exception>, Exception> response) {
        Arguments arguments = Arguments.empty();

        when(fn.run(same(response), eq(arguments))).thenReturn(Promise.done("expected"));

        Except<String> actual = subject.bind(endpoint, fn).join(response, arguments);

        assertEquals("expected", actual.unsafe());
    }
}
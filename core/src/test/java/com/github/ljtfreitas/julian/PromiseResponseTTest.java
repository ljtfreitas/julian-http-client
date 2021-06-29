package com.github.ljtfreitas.julian;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromiseResponseTTest {

    @Mock
    private Endpoint endpoint;

    private PromiseResponseT<String> responseT = new PromiseResponseT<>();

    @Nested
    class Predicates {

        @Test
        void supported() {
            when(endpoint.returnType()).thenReturn(JavaType.parameterized(Promise.class, String.class));

            assertTrue(responseT.test(endpoint));
        }

        @Test
        void unsupported() {
            when(endpoint.returnType()).thenReturn(JavaType.valueOf(String.class));

            assertFalse(responseT.test(endpoint));
        }
    }

    @Nested
    class Adapted {

        @Test
        void parameterized() {
            when(endpoint.returnType()).thenReturn(JavaType.parameterized(Promise.class, String.class));

            assertEquals(JavaType.valueOf(String.class), responseT.adapted(endpoint));
        }

        @Test
        void simple() {
            when(endpoint.returnType()).thenReturn(JavaType.object());

            assertEquals(JavaType.object(), responseT.adapted(endpoint));
        }
    }

    @Test
    void compose(@Mock ResponseFn<String, String> fn, @Mock RequestIO<String> request) throws Exception {
        Arguments arguments = Arguments.empty();

        when(fn.run(any(), eq(arguments))).thenReturn(Promise.done("expected"));
        when(request.comp(fn, arguments)).thenCallRealMethod();

        Promise<String> actual = responseT.comp(endpoint, fn).join(request, arguments);

        assertEquals("expected", actual.join().unsafe());
    }
}
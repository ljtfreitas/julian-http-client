package com.github.ljtfreitas.julian;

import java.util.function.Function;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultResponseTTest {

    @Mock
    private Endpoint endpoint;

    private DefaultResponseT<String> responseT = new DefaultResponseT<>();

    @Nested
    class Predicates {

        @Test
        void supported() {
            when(endpoint.returnType()).thenReturn(JavaType.parameterized(Response.class, String.class));

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
            when(endpoint.returnType()).thenReturn(JavaType.parameterized(Response.class, String.class));

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

        Response<String> response = Response.done("expected");

        when(fn.join(any(), eq(arguments))).thenReturn(response.body());
        when(request.execute()).then(a -> Promise.done(response));

        Response<String> actual = responseT.comp(endpoint, fn).join(request, arguments);

        assertEquals("expected", actual.body());
    }
}
package com.github.ljtfreitas.julian.http;

import com.github.ljtfreitas.julian.Endpoint;
import com.github.ljtfreitas.julian.Kind;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.ljtfreitas.julian.Arguments;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.ResponseFn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HTTPStatusResponseTTest {

    @Mock
    private Endpoint endpoint;

    private final HTTPStatusResponseT responseT = new HTTPStatusResponseT();

    @Nested
    class Predicates {

        @Test
        void supported() {
            when(endpoint.returnType()).thenReturn(JavaType.valueOf(HTTPStatus.class));

            assertTrue(responseT.test(endpoint));
        }

        @Test
        void unsupported() {
            when(endpoint.returnType()).thenReturn(JavaType.valueOf(String.class));

            assertFalse(responseT.test(endpoint));
        }
    }

    @Test
    void compose(@Mock ResponseFn<Void, Void> fn, @Mock HTTPResponse<Void> response) {
        Arguments arguments = Arguments.empty();

        HTTPStatus httpStatus = new HTTPStatus(HTTPStatusCode.OK);

        when(response.status()).thenReturn(httpStatus);
        when(response.cast(notNull())).thenCallRealMethod();

        HTTPStatus actual = responseT.bind(endpoint, fn).join(Promise.done(response), arguments);

        assertEquals(httpStatus, actual);
    }
}
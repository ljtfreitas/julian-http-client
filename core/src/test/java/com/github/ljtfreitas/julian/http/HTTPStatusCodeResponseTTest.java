package com.github.ljtfreitas.julian.http;

import com.github.ljtfreitas.julian.Endpoint;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.ljtfreitas.julian.Arguments;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.ResponseFn;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HTTPStatusCodeResponseTTest {

    @Mock
    private Endpoint endpoint;

    private final HTTPStatusCodeResponseT responseT = new HTTPStatusCodeResponseT();

    @Nested
    class Predicates {

        @Test
        void supported() {
            when(endpoint.returnType()).thenReturn(JavaType.valueOf(HTTPStatusCode.class));

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

        HTTPStatusCode httpStatusCode = HTTPStatusCode.OK;

        when(response.status()).thenReturn(new HTTPStatus(httpStatusCode));
        when(response.cast(notNull())).thenCallRealMethod();

        HTTPStatusCode actual = responseT.bind(endpoint, fn).join(Promise.done(response), arguments);

        assertEquals(httpStatusCode, actual);
    }
}
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
import com.github.ljtfreitas.julian.RequestIO;
import com.github.ljtfreitas.julian.ResponseFn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HTTPStatusResponseTTest {

    @Mock
    private Endpoint endpoint;

    private HTTPStatusResponseT responseT = new HTTPStatusResponseT();

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

    @SuppressWarnings("unchecked")
    @Test
    void compose(@Mock ResponseFn<Void, Void> fn, @Mock RequestIO<Void> request, @Mock HTTPResponse<Void> response) throws Exception {
        Arguments arguments = Arguments.empty();

        HTTPStatus httpStatus = new HTTPStatus(HTTPStatusCode.OK);

        when(response.status()).thenReturn(httpStatus);
        when(response.as(HTTPResponse.class)).thenCallRealMethod();
        when(request.execute()).then(a -> Promise.done(response));

        HTTPStatus actual = responseT.comp(endpoint, fn).join(request, arguments);

        assertEquals(httpStatus, actual);
    }
}
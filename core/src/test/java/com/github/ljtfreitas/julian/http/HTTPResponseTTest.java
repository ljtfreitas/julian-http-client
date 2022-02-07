package com.github.ljtfreitas.julian.http;

import com.github.ljtfreitas.julian.Arguments;
import com.github.ljtfreitas.julian.DefaultResponseT;
import com.github.ljtfreitas.julian.Endpoint;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.ObjectResponseT;
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.Response;
import com.github.ljtfreitas.julian.ResponseFn;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HTTPResponseTTest {

    @Mock
    private Endpoint endpoint;

    private final HTTPResponseT<String> responseT = new HTTPResponseT<>();

    @Nested
    class Predicates {

        @Test
        void supported() {
            when(endpoint.returnType()).thenReturn(JavaType.parameterized(HTTPResponse.class, String.class));

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
        void adaptToResponse() {
            when(endpoint.returnType()).thenReturn(JavaType.parameterized(HTTPResponse.class, String.class));

            assertEquals(JavaType.parameterized(Response.class, String.class, Exception.class), responseT.adapted(endpoint));
        }

        @Test
        void adaptToResponseOfObjectWhenHTTPResponseIsNotParameterized() {
            when(endpoint.returnType()).thenReturn(JavaType.valueOf(HTTPResponse.class));

            assertEquals(JavaType.parameterized(Response.class, Object.class, Exception.class), responseT.adapted(endpoint));
        }
    }

    @Test
    void compose() {
        Promise<HTTPResponse<String>> promise = Promise.done(HTTPResponse.success(HTTPStatus.valueOf(HTTPStatusCode.OK), HTTPHeaders.empty(), "hello"));

        ResponseFn<String, Response<String>> fn = new DefaultResponseT<String>().bind(endpoint,
                new ObjectResponseT<String>().bind(endpoint, null));

        HTTPResponse<String> actual = responseT.bind(endpoint, fn).join(promise, Arguments.empty());

        assertEquals("hello", actual.body().unsafe());
    }
}
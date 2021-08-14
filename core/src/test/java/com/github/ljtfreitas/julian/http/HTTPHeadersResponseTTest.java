package com.github.ljtfreitas.julian.http;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.ljtfreitas.julian.Arguments;
import com.github.ljtfreitas.julian.Endpoint;
import com.github.ljtfreitas.julian.Header;
import com.github.ljtfreitas.julian.Headers;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.RequestIO;
import com.github.ljtfreitas.julian.ResponseFn;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HTTPHeadersResponseTTest {

    @Mock
    private Endpoint endpoint;

    private HTTPHeadersResponseT responseT = new HTTPHeadersResponseT();

    @Nested
    class Predicates {

        @Test
        void supported() {
            when(endpoint.returnType()).thenReturn(JavaType.valueOf(HTTPHeaders.class));

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

        HTTPHeaders headers = HTTPHeaders.create(new HTTPHeader("X-Header", "x-header-content"));

        when(response.headers()).thenReturn(headers);
        when(response.as(HTTPResponse.class)).thenCallRealMethod();
        when(request.execute()).then(a -> Promise.done(response));

        HTTPHeaders actual = responseT.comp(endpoint, fn).join(request, arguments);

        assertThat(actual, contains(headers.all().toArray()));
    }
}
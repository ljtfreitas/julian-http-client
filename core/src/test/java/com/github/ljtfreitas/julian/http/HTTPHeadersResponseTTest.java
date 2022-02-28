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
class HTTPHeadersResponseTTest {

    @Mock
    private Endpoint endpoint;

    private final HTTPHeadersResponseT responseT = new HTTPHeadersResponseT();

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

    @Test
    void compose(@Mock ResponseFn<Void, Void> fn, @Mock HTTPResponse<Void> response) {
        Arguments arguments = Arguments.empty();

        HTTPHeaders headers = HTTPHeaders.create(new HTTPHeader("X-Header", "x-header-content"));

        when(response.headers()).thenReturn(headers);
        when(response.cast(notNull())).thenCallRealMethod();

        HTTPHeaders actual = responseT.bind(endpoint, fn).join(Promise.done(response), arguments);

        assertThat(actual, contains(headers.all().toArray()));
    }
}
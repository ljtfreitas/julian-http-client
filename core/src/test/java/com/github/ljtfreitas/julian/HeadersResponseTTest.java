package com.github.ljtfreitas.julian;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.ljtfreitas.julian.http.HTTPHeaders;
import com.github.ljtfreitas.julian.http.HTTPResponse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HeadersResponseTTest {

    @Mock
    private Endpoint endpoint;

    private HeadersResponseT responseT = new HeadersResponseT();

    @Nested
    class Predicates {

        @Test
        void supported() {
            when(endpoint.returnType()).thenReturn(JavaType.valueOf(Headers.class));

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

        Headers headers = Headers.create(new Header("X-Header", "x-header-content"));

        when(response.headers()).thenReturn(HTTPHeaders.valueOf(headers));
        when(response.as(HTTPResponse.class)).thenCallRealMethod();
        when(request.execute()).then(a -> Promise.done(response));

        Headers actual = responseT.comp(endpoint, fn).join(request, arguments);

        assertThat(actual, contains(headers.all().toArray()));
    }
}
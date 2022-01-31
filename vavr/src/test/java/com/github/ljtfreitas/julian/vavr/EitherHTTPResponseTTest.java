package com.github.ljtfreitas.julian.vavr;

import com.github.ljtfreitas.julian.Arguments;
import com.github.ljtfreitas.julian.DefaultResponseT;
import com.github.ljtfreitas.julian.Endpoint;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.ObjectResponseT;
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.ResponseFn;
import com.github.ljtfreitas.julian.http.FailureHTTPResponse;
import com.github.ljtfreitas.julian.http.HTTPHeader;
import com.github.ljtfreitas.julian.http.HTTPHeaders;
import com.github.ljtfreitas.julian.http.HTTPHeadersResponseT;
import com.github.ljtfreitas.julian.http.HTTPResponse;
import com.github.ljtfreitas.julian.http.HTTPResponseException;
import com.github.ljtfreitas.julian.http.HTTPResponseT;
import com.github.ljtfreitas.julian.http.HTTPServerFailureResponseException;
import com.github.ljtfreitas.julian.http.HTTPStatus;
import com.github.ljtfreitas.julian.http.HTTPStatusCode;
import com.github.ljtfreitas.julian.http.HTTPStatusResponseT;
import io.vavr.control.Either;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EitherHTTPResponseTTest {

    @Mock
    private Endpoint endpoint;

    private final EitherHTTPResponseT<String> responseT = new EitherHTTPResponseT<>();

    @Nested
    class Predicates {

        @Test
        void support() {
            when(endpoint.returnType()).thenReturn(JavaType.parameterized(Either.class, HTTPResponseException.class, String.class));
            assertTrue(responseT.test(endpoint));
        }

        @Test
        void unsupported() {
            when(endpoint.returnType()).thenReturn(JavaType.valueOf(String.class));
            assertFalse(responseT.test(endpoint));
        }

        @DisplayName("We should not support any other Exception class; just HTTPResponseException.")
        @Test
        void shouldNotSupportException() {
            when(endpoint.returnType()).thenReturn(JavaType.parameterized(Either.class, Exception.class, String.class));
            assertFalse(responseT.test(endpoint));
        }
    }

    @Nested
    class Adapt {

        @Test
        void adaptToHTTPResponseUsingRightAsArgument() {
            when(endpoint.returnType()).thenReturn(JavaType.parameterized(Either.class, HTTPResponseException.class, String.class));

            JavaType adapted = responseT.adapted(endpoint);

            assertThat(adapted, equalTo(JavaType.parameterized(HTTPResponse.class, String.class)));
        }

        @Test
        @DisplayName("adapt to Object when Either is not parameterized")
        void adaptToObjectWhenEitherIsNotParameterized() {
            when(endpoint.returnType()).thenReturn(JavaType.valueOf(Either.class));

            JavaType adapted = responseT.adapted(endpoint);

            assertThat(adapted, equalTo(JavaType.parameterized(HTTPResponse.class, Object.class)));
        }
    }

    @Test
    void bind() {
        String content = "hello";

        Promise<HTTPResponse<String>, Exception> response = Promise.done(HTTPResponse.success(new HTTPStatus(HTTPStatusCode.OK), HTTPHeaders.empty(), content));

        ResponseFn<String, HTTPResponse<String>> fn = new HTTPResponseT<String>().bind(endpoint,
                new DefaultResponseT<String>().bind(endpoint, new ObjectResponseT<String>().bind(endpoint, null)));

        Either<HTTPResponseException, String> either = responseT.bind(endpoint, fn).join(response, Arguments.empty());

        assertTrue(either.isRight());

        assertThat(either.get(), equalTo(content));
    }

    @Test
    void bindWithHTTPResponseHeaders() {
        HTTPHeaders headers = HTTPHeaders.create(new HTTPHeader("x-whatever", "whatever"));

        EitherHTTPResponseT<HTTPHeaders> responseT = new EitherHTTPResponseT<>();

        Promise<HTTPResponse<Void>, Exception> response = Promise.done(HTTPResponse.empty(new HTTPStatus(HTTPStatusCode.OK), headers));

        ResponseFn<Void, HTTPResponse<HTTPHeaders>> fn = new HTTPResponseT<HTTPHeaders>().bind(endpoint,
                new DefaultResponseT<HTTPHeaders>().bind(endpoint,
                        new HTTPHeadersResponseT().bind(endpoint,
                                new ObjectResponseT<Void>().bind(endpoint, null))));

        Either<HTTPResponseException, HTTPHeaders> either = responseT.bind(endpoint, fn).join(response, Arguments.empty());

        assertTrue(either.isRight());

        assertThat(either.get(), equalTo(headers));
    }

    @Test
    void bindWithHTTPResponseStatus() {
        HTTPStatus status = new HTTPStatus(HTTPStatusCode.OK);

        EitherHTTPResponseT<HTTPStatus> responseT = new EitherHTTPResponseT<>();

        Promise<HTTPResponse<Void>, Exception> response = Promise.done(HTTPResponse.empty(status, HTTPHeaders.empty()));

        ResponseFn<Void, HTTPResponse<HTTPStatus>> fn = new HTTPResponseT<HTTPStatus>().bind(endpoint,
                new DefaultResponseT<HTTPStatus>().bind(endpoint,
                        new HTTPStatusResponseT().bind(endpoint,
                                new ObjectResponseT<Void>().bind(endpoint, null))));

        Either<HTTPResponseException, HTTPStatus> either = responseT.bind(endpoint, fn).join(response, Arguments.empty());

        assertTrue(either.isRight());

        assertThat(either.get(), equalTo(status));
    }

    @Test
    void bindFailure() {
        HTTPServerFailureResponseException.InternalServerError failure = new HTTPServerFailureResponseException.InternalServerError(HTTPHeaders.empty(), "ooops".getBytes());

        Promise<HTTPResponse<String>, Exception> response = Promise.done(new FailureHTTPResponse<>(new HTTPStatus(HTTPStatusCode.OK), HTTPHeaders.empty(), failure));

        ResponseFn<String, HTTPResponse<String>> fn = new HTTPResponseT<String>().bind(endpoint,
                new DefaultResponseT<String>().bind(endpoint, new ObjectResponseT<String>().bind(endpoint, null)));

        Either<HTTPResponseException, String> either = responseT.bind(endpoint, fn).join(response, Arguments.empty());

        assertTrue(either.isLeft());

        assertSame(failure, either.swap().get());
    }
}
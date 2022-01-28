package com.github.ljtfreitas.julian.vavr;

import com.github.ljtfreitas.julian.Arguments;
import com.github.ljtfreitas.julian.Endpoint;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.ResponseFn;
import com.github.ljtfreitas.julian.http.FailureHTTPResponse;
import com.github.ljtfreitas.julian.http.HTTPHeaders;
import com.github.ljtfreitas.julian.http.HTTPResponse;
import com.github.ljtfreitas.julian.http.HTTPResponseException;
import com.github.ljtfreitas.julian.http.HTTPServerFailureResponseException;
import com.github.ljtfreitas.julian.http.HTTPStatus;
import com.github.ljtfreitas.julian.http.HTTPStatusCode;
import com.github.ljtfreitas.julian.http.SuccessHTTPResponse;
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
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.ArgumentMatchers.same;
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
        void adaptToRight() {
            when(endpoint.returnType()).thenReturn(JavaType.parameterized(Either.class, HTTPResponseException.class, String.class));

            JavaType adapted = responseT.adapted(endpoint);

            assertThat(adapted, equalTo(JavaType.valueOf(String.class)));
        }

        @Test
        @DisplayName("adapt to Object when Either is not parameterized")
        void adaptToObjectWhenEitherIsNotParameterized() {
            when(endpoint.returnType()).thenReturn(JavaType.valueOf(Either.class));

            JavaType adapted = responseT.adapted(endpoint);

            assertThat(adapted, equalTo(JavaType.valueOf(Object.class)));
        }
    }

    @Test
    void bind(@Mock ResponseFn<String, String> fn) {
        Arguments arguments = Arguments.empty();

        String content = "hello";

        Promise<HTTPResponse<String>, Exception> response = Promise.done(new SuccessHTTPResponse<>(new HTTPStatus(HTTPStatusCode.OK), HTTPHeaders.empty(), content));

        when(fn.join(notNull(), same(arguments))).thenReturn(content);

        Either<HTTPResponseException, String> either = responseT.bind(endpoint, fn).join(response, arguments);

        assertTrue(either.isRight());

        assertThat(either.get(), equalTo(content));
    }

    @Test
    void bindFailure(@Mock ResponseFn<String, String> fn) {
        Arguments arguments = Arguments.empty();

        HTTPServerFailureResponseException.InternalServerError failure = new HTTPServerFailureResponseException.InternalServerError(HTTPHeaders.empty(), "ooops".getBytes());

        Promise<HTTPResponse<String>, Exception> response = Promise.done(new FailureHTTPResponse<>(new HTTPStatus(HTTPStatusCode.OK), HTTPHeaders.empty(), failure));

        Either<HTTPResponseException, String> either = responseT.bind(endpoint, fn).join(response, arguments);

        assertTrue(either.isLeft());

        assertSame(failure, either.swap().get());
    }
}
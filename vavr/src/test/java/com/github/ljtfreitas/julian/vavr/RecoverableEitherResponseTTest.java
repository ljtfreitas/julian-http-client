package com.github.ljtfreitas.julian.vavr;

import com.github.ljtfreitas.julian.Arguments;
import com.github.ljtfreitas.julian.Endpoint;
import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.ObjectResponseT;
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.Response;
import com.github.ljtfreitas.julian.Subscriber;
import com.github.ljtfreitas.julian.http.FailureHTTPResponse;
import com.github.ljtfreitas.julian.http.HTTPClientFailureResponseException.BadRequest;
import com.github.ljtfreitas.julian.http.HTTPHeader;
import com.github.ljtfreitas.julian.http.HTTPHeaders;
import com.github.ljtfreitas.julian.http.RecoverableHTTPResponse;
import com.github.ljtfreitas.julian.http.UnrecoverableHTTPResponseException;
import com.github.ljtfreitas.julian.http.codec.HTTPResponseReaders;
import com.github.ljtfreitas.julian.http.codec.StringHTTPMessageCodec;
import io.vavr.control.Either;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecoverableEitherResponseTTest {

    @Mock
    private Endpoint endpoint;

    private final RecoverableEitherResponseT responseT = new RecoverableEitherResponseT();

    @Nested
    class Predicates {

        @Test
        void support() {
            when(endpoint.returnType()).thenReturn(JavaType.parameterized(Either.class, String.class, String.class));
            assertTrue(responseT.test(endpoint));
        }

        @DisplayName("We should support any type as left side of Either, except Throwable-compatible types")
        @Test
        void supportAnyType() {
            when(endpoint.returnType()).thenReturn(JavaType.parameterized(Either.class, String.class, String.class));
            assertTrue(responseT.test(endpoint));
        }

        @Test
        @DisplayName("Throwable-compatible types are not supported as left side of Either")
        void doesNotSupportThrowable() {
            when(endpoint.returnType()).thenReturn(JavaType.parameterized(Either.class, Exception.class, String.class));
            assertFalse(responseT.test(endpoint));
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
        void adaptToRight() {
            when(endpoint.returnType()).thenReturn(JavaType.parameterized(Either.class, String.class, String.class));

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
    void bind() {
        when(endpoint.returnType()).thenReturn(JavaType.parameterized(Either.class, String.class, String.class));

        String expected = "hello";

        Either<Object, Object> either = responseT.bind(endpoint, new ObjectResponseT<>().<String> bind(endpoint, null))
                .join(Promise.done(Response.done(expected)), Arguments.empty());

        assertTrue(either.isRight());

        assertThat(either.get(), equalTo(expected));
    }

    @Test
    void bindRecovered() {
        when(endpoint.returnType()).thenReturn(JavaType.parameterized(Either.class, String.class, String.class));

        String expected = "oops";

        BadRequest badRequest = new BadRequest(new HTTPHeaders(List.of(new HTTPHeader(HTTPHeader.CONTENT_TYPE, "text/plain"))),
                Promise.done(expected.getBytes()));

        FailureHTTPResponse<Object> failure = new FailureHTTPResponse<>(badRequest);

        Promise<Response<Object, ? extends Throwable>> response = Promise.done(new RecoverableHTTPResponse<>(failure,
                new HTTPResponseReaders(List.of(new StringHTTPMessageCodec()))));

        Either<Object, Object> either = responseT.bind(endpoint, new ObjectResponseT<>().bind(endpoint, null))
                .join(response, Arguments.empty());

        assertTrue(either.isLeft());

        String recovered = (String) either.swap().get();

        assertEquals(expected, recovered);
    }

    @DisplayName("In case it's impossible to convert the response to the desired recovered value, we get a failed Promise")
    @Test
    void impossibleToRecover() {
        when(endpoint.returnType()).thenReturn(JavaType.parameterized(Either.class, MyFailure.class, String.class));

        BadRequest badRequest = new BadRequest(new HTTPHeaders(List.of(new HTTPHeader(HTTPHeader.CONTENT_TYPE, "text/plain"))),
                Promise.done("oops".getBytes()));

        FailureHTTPResponse<Object> failure = new FailureHTTPResponse<>(badRequest);

        StringHTTPMessageCodec codec = new StringHTTPMessageCodec(); // isn't able to convert String to MyFailure

        Promise<Response<Object, ? extends Throwable>> response = Promise.done(new RecoverableHTTPResponse<>(failure,
                new HTTPResponseReaders(List.of(codec))));

        Promise<Either<Object, Object>> promise = responseT.bind(endpoint, new ObjectResponseT<>().bind(endpoint, null))
                .run(response, Arguments.empty());

        promise.subscribe(new Subscriber<>() {

            @Override
            public void success(Either<Object, Object> value) {
                fail("a success value was not expected here...");
            }

            @Override
            public void failure(Throwable failure) {
                assertThat(failure, instanceOf(UnrecoverableHTTPResponseException.class));
            }
        });
    }

    private static class MyFailure {}
}
package com.github.ljtfreitas.julian.http;

import com.github.ljtfreitas.julian.Attempt;
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.Subscriber;
import com.github.ljtfreitas.julian.http.HTTPResponse.HTTPResponseConsumer;
import com.github.ljtfreitas.julian.http.HTTPResponse.HTTPResponseFn;
import com.github.ljtfreitas.julian.http.HTTPResponse.HTTPResponseSubscriber;
import com.github.ljtfreitas.julian.http.HTTPServerFailureResponseException.GatewayTimeout;
import com.github.ljtfreitas.julian.http.HTTPServerFailureResponseException.InternalServerError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HTTPResponseTest {

    private final HTTPHeaders headers = HTTPHeaders.empty();

    @Nested
    class Success {

        private final HTTPResponse<String> response = HTTPResponse.success(HTTPStatus.valueOf(HTTPStatusCode.OK), headers, "hello");

        @Test
        void status() {
            assertEquals(HTTPStatus.valueOf(HTTPStatusCode.OK), response.status());
        }

        @Test
        void headers() {
            assertSame(headers, response.headers());
        }

        @Test
        @DisplayName("success HTTP response should returns HTTP content as body")
        void body() {
            assertEquals("hello", response.body().unsafe());
        }

        @Test
        @DisplayName("map using just HTTP response content")
        void map() {
            assertEquals("hello, world!", response.map(b -> b + ", world!").body().unsafe());
        }

        @Test
        @DisplayName("map using all HTTP response data")
        void mapAsResponse() {
            assertEquals("hello, world!", response.map((status, headers, b) -> {

                assertSame(response.status(), status);
                assertEquals(response.headers(), headers);

                return b + ", world!";
            }).body().unsafe());
        }

        @Test
        @DisplayName("just call success callback passing HTTP response content")
        void onSuccess(@Mock Consumer<String> consumer) {
            HTTPResponse<String> returned = response.onSuccess(consumer);

            assertSame(response, returned);

            verify(consumer).accept("hello");
        }

        @Test
        @DisplayName("just call success callback passing all HTTP response data")
        void onSuccessWithFullResponse(@Mock HTTPResponseConsumer<String> consumer) {
            HTTPResponse<String> returned = response.onSuccess(consumer);

            assertSame(response, returned);

            verify(consumer).accept(same(response.status()), same(response.headers()), eq("hello"));
        }

        @Test
        @DisplayName("subscribe to HTTP response content")
        void subscribe(@Mock Subscriber<String> subscriber) {
            HTTPResponse<String> returned = response.subscribe(subscriber);

            assertSame(response, returned);

            verify(subscriber).success("hello");
            verify(subscriber).done();
            verify(subscriber, never()).failure(any());
        }

        @Test
        @DisplayName("subscribe to all HTTP response data")
        void subscribeToFullResponse(@Mock HTTPResponseSubscriber<String> subscriber) {
            HTTPResponse<String> returned = response.subscribe(subscriber);

            assertSame(response, returned);

            verify(subscriber).success(same(response.status()), same(response.headers()), eq("hello"));
            verify(subscriber).done();
            verify(subscriber, never()).failure(any());
        }

        @Test
        @DisplayName("success HTTP response doesn't have to do nothing on failure callback")
        void onFailureDoNothing(@Mock Consumer<Exception> consumer) {
            HTTPResponse<String> returned = response.onFailure(consumer);

            assertSame(response, returned);

            verify(consumer, never()).accept(any());
        }

        @Test
        @DisplayName("success HTTP response doesn't have to do nothing on failure")
        void onRecoverExceptionDoNothing(@Mock Function<Exception, String> fn) {
            HTTPResponse<String> returned = response.recover(fn);

            assertSame(response, returned);

            verify(fn, never()).apply(any());
        }

        @Test
        @DisplayName("success HTTP response doesn't have to do nothing on recover")
        void onRecoverUsingPredicateDoNothing(@Mock Predicate<Exception> p, @Mock Function<Exception, String> fn) {
            HTTPResponse<String> returned = response.recover(p, fn);

            assertSame(response, returned);

            verify(p, never()).test(any());
            verify(fn, never()).apply(any());
        }

        @Test
        @DisplayName("success HTTP response doesn't have to do nothing on recover")
        void onRecoverUsingExceptionTypeDoNothing(@Mock Function<Exception, String> fn) {
            HTTPResponse<String> returned = response.recover(Exception.class, fn);

            assertSame(response, returned);

            verify(fn, never()).apply(any());
        }

        @Test
        @DisplayName("success HTTP response doesn't have to do nothing on recover")
        void onRecoverUsingStatusCodeDoNothing(@Mock HTTPResponseFn<byte[], String> fn) {
            HTTPResponse<String> returned = response.recover(HTTPStatusCode.INTERNAL_SERVER_ERROR, fn);

            assertSame(response, returned);

            verify(fn, never()).apply(any(), any(), any());
        }
    }

    @Nested
    class Failure {

        private final HTTPResponseException failure = new InternalServerError(headers, Promise.done("failure".getBytes()));
        private final HTTPResponse<String> response = HTTPResponse.failed(failure);

        @Test
        void status() {
            assertEquals(failure.status(), response.status());
        }

        @Test
        void headers() {
            assertSame(failure.headers(), response.headers());
        }

        @Test
        @DisplayName("failure HTTP response should throw an exception when we try to get the body")
        void body() {
            Attempt<String> body = response.body();

            assertTrue(body instanceof Attempt.Failure);

            HTTPResponseException exception = assertThrows(HTTPResponseException.class, body::unsafe);

            assertSame(failure, exception);
        }

        @Test
        @DisplayName("failure HTTP response doesn't have to do nothing on map")
        void onMapDoNothing() {
            HTTPResponse<String> responseAfterMap = response.map(b -> b + ", world!");

            assertTrue(responseAfterMap instanceof FailureHTTPResponse);
        }

        @Test
        @DisplayName("failure HTTP response doesn't have to do nothing on map")
        void onMapAsResponseDoNothing() {
            HTTPResponse<String> responseAfterMap = response.map((status, headers, b) -> fail("ooops...this should not be happened"));

            assertTrue(responseAfterMap instanceof FailureHTTPResponse);
        }

        @Test
        @DisplayName("failure HTTP response doesn't have to do nothing on success")
        void onSuccessDoNothing(@Mock Consumer<String> consumer) {
            HTTPResponse<String> returned = response.onSuccess(consumer);

            assertSame(response, returned);

            verify(consumer, never()).accept(any());
        }

        @Test
        @DisplayName("failure HTTP response doesn't have to do nothing on success")
        void onSuccessWithFullResponseDoNothing(@Mock HTTPResponseConsumer<String> consumer) {
            HTTPResponse<String> returned = response.onSuccess(consumer);

            assertSame(response, returned);

            verify(consumer, never()).accept(any(), any(), any());
        }

        @Test
        @DisplayName("subscribe to failure should pass a HTTPResponseException")
        void subscribe(@Mock Subscriber<String> subscriber) {
            HTTPResponse<String> returned = response.subscribe(subscriber);

            assertSame(response, returned);

            verify(subscriber).failure(failure);
            verify(subscriber).done();
            verify(subscriber, never()).success(any());
        }

        @Test
        @DisplayName("subscribe to failure with all HTTP response data should just pass a HTTPResponseException")
        void subscribeToFullResponseDoNothing(@Mock HTTPResponseSubscriber<String> subscriber) {
            HTTPResponse<String> returned = response.subscribe(subscriber);

            assertSame(response, returned);

            verify(subscriber).failure(failure);
            verify(subscriber).done();
            verify(subscriber, never()).success(any(), any(), any());
        }

        @Test
        @DisplayName("just call failure callback passing HTTPResponseException")
        void onFailureDoNothing(@Mock Consumer<Exception> consumer) {
            HTTPResponse<String> returned = response.onFailure(consumer);

            assertSame(response, returned);

            verify(consumer).accept(failure);
        }

        @Test
        @DisplayName("a failure HTTP response can recover for a successful one")
        void onRecover() {
            HTTPResponse<String> returned = response.recover(e -> "i'm recovered");

            assertEquals("i'm recovered", returned.body().unsafe());
        }

        @Test
        @DisplayName("a failure HTTP response can recover for a successful one using a exception predicate")
        void onRecoverUsingPredicate() {
            HTTPResponse<String> returned = response
                    .recover(e -> e instanceof InternalServerError, e -> "i'm recovered from an InternalServerError");


            assertEquals("i'm recovered from an InternalServerError", returned.body().unsafe());
        }

        @Test
        @DisplayName("a failure HTTP response can't recover for a successful one when the exception predicate doesn't match")
        void onRecoverUsingPredicateThatDontMatchDoNothing() {
            HTTPResponse<String> returned = response
                    .recover(e -> e instanceof GatewayTimeout, e -> "i'm recovered from a GatewayTimeout");

            assertSame(response, returned);
        }

        @Test
        @DisplayName("a failure HTTP response can recover for a successful one using an exception type")
        void onRecoverUsingExceptionType() {
            HTTPResponse<String> returned = response
                    .recover(InternalServerError.class, e -> "i'm recovered from an Internal Server Error");

            assertEquals("i'm recovered from an Internal Server Error", returned.body().unsafe());
        }

        @Test
        @DisplayName("a failure HTTP response can't recover for a successful one when the expected exception type doesn't match")
        void onRecoverUsingExceptionTypeThatDontMatchDoNothing() {
            HTTPResponse<String> returned = response
                    .recover(GatewayTimeout.class, e -> "i'm recovered from a GatewayTimeout");

            assertSame(response, returned);
        }

        @Test
        @DisplayName("a failure HTTP response can recover for a successful one using a HTTP status code")
        void onRecoverUsingStatusCode() {
            HTTPResponse<String> returned = response
                    .recover(HTTPStatusCode.INTERNAL_SERVER_ERROR, (status, headers, bodyAsBytes) -> "i'm recovered from an Internal Server Error");

            assertEquals("i'm recovered from an Internal Server Error", returned.body().unsafe());
        }

        @Test
        @DisplayName("a failure HTTP response can't recover for a successful one when the expected HTTP status code doesn't match")
        void onRecoverUsingStatusCodeThatDontMatchDoNothing() {
            HTTPResponse<String> returned = response
                    .recover(HTTPStatusCode.GATEWAY_TIMEOUT, (status, headers, bodyAsBytes) -> "i'm recovered from a GatewayTimeout");

            assertSame(response, returned);
        }
    }

    @Nested
    class Empty {

        private final HTTPResponse<String> response = HTTPResponse.empty(HTTPStatus.valueOf(HTTPStatusCode.NO_CONTENT), headers);

        @Test
        void status() {
            assertEquals(HTTPStatus.valueOf(HTTPStatusCode.NO_CONTENT), response.status());
        }

        @Test
        void headers() {
            assertSame(headers, response.headers());
        }

        @Test
        @DisplayName("empty HTTP response doesn't have a body")
        void body() {
            assertNull(response.body().unsafe());
        }

        @Test
        @DisplayName("empty HTTP response can be mapped for another content")
        void map() {
            HTTPResponse<String> responseAfterMap = response.map(value -> {
                assertNull(value);
                return "hello";
            });

            assertEquals("hello", responseAfterMap.body().unsafe());
        }

        @Test
        @DisplayName("empty HTTP response can be mapped for another content")
        void mapAsResponse() {
            assertEquals("hello", response.map((status, headers, b) -> {

                assertNull(b);
                assertSame(response.status(), status);
                assertEquals(response.headers(), headers);

                return "hello";

            }).body().unsafe());
        }

        @Test
        @DisplayName("just call success callback passing null as HTTP response content")
        void onSuccess(@Mock Consumer<String> consumer) {
            HTTPResponse<String> returned = response.onSuccess(consumer);

            assertSame(response, returned);

            verify(consumer).accept(null);
        }

        @Test
        @DisplayName("just call success callback passing HTTP response data and null as response content")
        void onSuccessWithFullResponse(@Mock HTTPResponseConsumer<String> consumer) {
            HTTPResponse<String> returned = response.onSuccess(consumer);

            assertSame(response, returned);

            verify(consumer).accept(same(response.status()), same(response.headers()), nullable(String.class));
        }

        @Test
        @DisplayName("subscribe to HTTP response content, but the content must be null")
        void subscribe(@Mock Subscriber<String> subscriber) {
            HTTPResponse<String> returned = response.subscribe(subscriber);

            assertSame(response, returned);

            verify(subscriber).success(null);
            verify(subscriber).done();
            verify(subscriber, never()).failure(any());
        }

        @Test
        @DisplayName("subscribe to all HTTP response data")
        void subscribeToFullResponse(@Mock HTTPResponseSubscriber<String> subscriber) {
            HTTPResponse<String> returned = response.subscribe(subscriber);

            assertSame(response, returned);

            verify(subscriber).success(same(response.status()), same(response.headers()), nullable(String.class));
            verify(subscriber).done();
            verify(subscriber, never()).failure(any());
        }

        @Test
        @DisplayName("empy HTTP response doesn't have to do nothing on failure callback")
        void onFailureDoNothing(@Mock Consumer<Exception> consumer) {
            HTTPResponse<String> returned = response.onFailure(consumer);

            assertSame(response, returned);

            verify(consumer, never()).accept(any());
        }

        @Test
        @DisplayName("empty HTTP response doesn't have to do nothing on failure")
        void onRecoverExceptionDoNothing(@Mock Function<Exception, String> fn) {
            HTTPResponse<String> returned = response.recover(fn);

            assertSame(response, returned);

            verify(fn, never()).apply(any());
        }

        @Test
        @DisplayName("empty HTTP response doesn't have to do nothing on recover")
        void onRecoverUsingPredicateDoNothing(@Mock Predicate<Exception> p, @Mock Function<Exception, String> fn) {
            HTTPResponse<String> returned = response.recover(p, fn);

            assertSame(response, returned);

            verify(p, never()).test(any());
            verify(fn, never()).apply(any());
        }

        @Test
        @DisplayName("empty HTTP response doesn't have to do nothing on recover")
        void onRecoverUsingExceptionTypeDoNothing(@Mock Function<Exception, String> fn) {
            HTTPResponse<String> returned = response.recover(Exception.class, fn);

            assertSame(response, returned);

            verify(fn, never()).apply(any());
        }

        @Test
        @DisplayName("empty HTTP response doesn't have to do nothing on recover")
        void onRecoverUsingStatusCodeDoNothing(@Mock HTTPResponseFn<byte[], String> fn) {
            HTTPResponse<String> returned = response.recover(HTTPStatusCode.INTERNAL_SERVER_ERROR, fn);

            assertSame(response, returned);

            verify(fn, never()).apply(any(), any(), any());
        }
    }

    @Nested
    class Lazy {

        @Nested
        class Success {

            private final HTTPResponse<String> success = HTTPResponse.lazy(HTTPStatus.valueOf(HTTPStatusCode.OK), headers, Promise.done("hello"));

            @Test
            void status() {
                assertEquals(HTTPStatus.valueOf(HTTPStatusCode.OK), success.status());
            }

            @Test
            void headers() {
                assertSame(headers, success.headers());
            }

            @Test
            @DisplayName("lazy, success HTTP response should returns HTTP content as body")
            void body() {
                assertEquals("hello", success.body().unsafe());
            }

            @Test
            @DisplayName("map using just HTTP response content")
            void map() {
                assertEquals("hello, world!", success.map(b -> b + ", world!").body().unsafe());
            }

            @Test
            @DisplayName("map using all HTTP response data")
            void mapAsResponse() {
                assertEquals("hello, world!", success.map((status, headers, b) -> {

                    assertSame(success.status(), status);
                    assertEquals(success.headers(), headers);

                    return b + ", world!";
                }).body().unsafe());
            }

            @Test
            @DisplayName("just call success callback passing HTTP response content")
            void onSuccess(@Mock Consumer<String> consumer) {
                HTTPResponse<String> returned = success.onSuccess(consumer);

                assertSame(success, returned);

                verify(consumer).accept("hello");
            }

            @Test
            @DisplayName("just call success callback passing all HTTP response data")
            void onSuccessWithFullResponse(@Mock HTTPResponseConsumer<String> consumer) {
                HTTPResponse<String> returned = success.onSuccess(consumer);

                assertSame(success, returned);

                verify(consumer).accept(same(success.status()), same(success.headers()), eq("hello"));
            }

            @Test
            @DisplayName("subscribe to HTTP response content")
            void subscribe(@Mock Subscriber<String> subscriber) {
                HTTPResponse<String> returned = success.subscribe(subscriber);

                assertTrue(returned instanceof LazyHTTPResponse);

                verify(subscriber).success("hello");
                verify(subscriber).done();
                verify(subscriber, never()).failure(any());
            }

            @Test
            @DisplayName("subscribe to all HTTP response data")
            void subscribeToFullResponse(@Mock HTTPResponseSubscriber<String> subscriber) {
                HTTPResponse<String> returned = success.subscribe(subscriber);

                assertTrue(returned instanceof LazyHTTPResponse);

                verify(subscriber).success(same(success.status()), same(success.headers()), eq("hello"));
                verify(subscriber).done();
                verify(subscriber, never()).failure(any());
            }

            @Test
            @DisplayName("lazy, success HTTP response doesn't have to do nothing on failure callback")
            void onFailureDoNothing(@Mock Consumer<Exception> consumer) {
                HTTPResponse<String> returned = success.onFailure(consumer);

                assertTrue(returned instanceof LazyHTTPResponse);

                verify(consumer, never()).accept(any());
            }

            @Test
            @DisplayName("lazy, success HTTP response doesn't have to do nothing on failure")
            void onRecoverExceptionDoNothing(@Mock Function<Exception, String> fn) {
                HTTPResponse<String> returned = success.recover(fn);

                assertTrue(returned instanceof LazyHTTPResponse);

                verify(fn, never()).apply(any());
            }

            @Test
            @DisplayName("lazy, success HTTP response doesn't have to do nothing on recover")
            void onRecoverUsingPredicateDoNothing(@Mock Predicate<Exception> p, @Mock Function<Exception, String> fn) {
                HTTPResponse<String> returned = success.recover(p, fn);

                assertTrue(returned instanceof LazyHTTPResponse);

                verify(p, never()).test(any());
                verify(fn, never()).apply(any());
            }

            @Test
            @DisplayName("lazy, success HTTP response doesn't have to do nothing on recover")
            void onRecoverUsingExceptionTypeDoNothing(@Mock Function<Exception, String> fn) {
                HTTPResponse<String> returned = success.recover(Exception.class, fn);

                assertTrue(returned instanceof LazyHTTPResponse);

                verify(fn, never()).apply(any());
            }

            @Test
            @DisplayName("lazy, success HTTP response doesn't have to do nothing on recover")
            void onRecoverUsingStatusCodeDoNothing(@Mock HTTPResponseFn<byte[], String> fn) {
                HTTPResponse<String> returned = success.recover(HTTPStatusCode.INTERNAL_SERVER_ERROR, fn);

                assertSame(success, returned);

                verify(fn, never()).apply(any(), any(), any());
            }
        }

        @Nested
        class Failure {

            private final HTTPResponseException failure = new InternalServerError(headers, Promise.done("failure".getBytes()));
            private final HTTPResponse<String> failed = HTTPResponse.lazy(failure.status(), failure.headers(), Promise.failed(failure));

            @Test
            void status() {
                assertEquals(failure.status(), failed.status());
            }

            @Test
            void headers() {
                assertSame(failure.headers(), failed.headers());
            }

            @Test
            @DisplayName("lazy, failure HTTP response should throw an exception when we try to get the body")
            void body() {
                Attempt<String> body = failed.body();

                assertTrue(body instanceof Attempt.Failure);

                HTTPResponseException exception = assertThrows(HTTPResponseException.class, body::unsafe);

                assertSame(failure, exception);
            }

            @Test
            @DisplayName("lazy, failure HTTP response doesn't have to do nothing on map")
            void onMapDoNothing() {
                HTTPResponse<String> responseAfterMap = failed.map(b -> b + ", world!");

                assertTrue(responseAfterMap instanceof LazyHTTPResponse);

                InternalServerError exception = assertThrows(InternalServerError.class, responseAfterMap.body()::unsafe);

                assertSame(failure, exception);
            }

            @Test
            @DisplayName("lazy, failure HTTP response doesn't have to do nothing on map")
            void onMapAsResponseDoNothing() {
                HTTPResponse<String> responseAfterMap = failed.map((status, headers, b) -> fail("ooops...this should not be happened"));

                InternalServerError exception = assertThrows(InternalServerError.class, responseAfterMap.body()::unsafe);

                assertSame(failure, exception);
            }

            @Test
            @DisplayName("lazy, failure HTTP response doesn't have to do nothing on success")
            void onSuccessDoNothing(@Mock Consumer<String> consumer) {
                HTTPResponse<String> returned = failed.onSuccess(consumer);

                assertSame(failed, returned);

                verify(consumer, never()).accept(any());
            }

            @Test
            @DisplayName("lazy, failure HTTP response doesn't have to do nothing on success")
            void onSuccessWithFullResponseDoNothing(@Mock HTTPResponseConsumer<String> consumer) {
                HTTPResponse<String> returned = failed.onSuccess(consumer);

                assertSame(failed, returned);

                verify(consumer, never()).accept(any(), any(), any());
            }

            @Test
            @DisplayName("subscribe to failure should pass a HTTPResponseException")
            void subscribe(@Mock Subscriber<String> subscriber) {
                HTTPResponse<String> returned = failed.subscribe(subscriber);

                assertTrue(returned instanceof LazyHTTPResponse);

                verify(subscriber).failure(failure);
                verify(subscriber).done();
                verify(subscriber, never()).success(any());
            }

            @Test
            @DisplayName("subscribe to failure with all HTTP response data should just pass a HTTPResponseException")
            void subscribeToFullResponseDoNothing(@Mock HTTPResponseSubscriber<String> subscriber) {
                HTTPResponse<String> returned = failed.subscribe(subscriber);

                assertTrue(returned instanceof LazyHTTPResponse);

                verify(subscriber).failure(failure);
                verify(subscriber).done();
                verify(subscriber, never()).success(any(), any(), any());
            }

            @Test
            @DisplayName("just call failure callback passing HTTPResponseException")
            void onFailureDoNothing(@Mock Consumer<Exception> consumer) {
                HTTPResponse<String> returned = failed.onFailure(consumer);

                assertTrue(returned instanceof LazyHTTPResponse);

                verify(consumer).accept(failure);
            }

            @Test
            @DisplayName("a lazy, failure HTTP response can recover for a successful one")
            void onRecover() {
                HTTPResponse<String> returned = failed.recover(e -> "i'm recovered");

                assertEquals("i'm recovered", returned.body().unsafe());
            }

            @Test
            @DisplayName("a lazy, failure HTTP response can recover for a successful one using a exception predicate")
            void onRecoverUsingPredicate() {
                HTTPResponse<String> returned = failed
                        .recover(e -> e instanceof InternalServerError, e -> "i'm recovered from an InternalServerError");

                assertEquals("i'm recovered from an InternalServerError", returned.body().unsafe());
            }

            @Test
            @DisplayName("a lazy, failure HTTP response can't recover for a successful one when the exception predicate doesn't match")
            void onRecoverUsingPredicateThatDontMatchDoNothing() {
                HTTPResponse<String> returned = failed
                        .recover(e -> e instanceof GatewayTimeout, e -> "i'm recovered from a GatewayTimeout");

                InternalServerError exception = assertThrows(InternalServerError.class, returned.body()::unsafe);

                assertSame(failure, exception);
            }

            @Test
            @DisplayName("a lazy, failure HTTP response can recover for a successful one using an exception type")
            void onRecoverUsingExceptionType() {
                HTTPResponse<String> returned = failed
                        .recover(InternalServerError.class, e -> "i'm recovered from an Internal Server Error");

                assertEquals("i'm recovered from an Internal Server Error", returned.body().unsafe());
            }

            @Test
            @DisplayName("a lazy, failure HTTP response can't recover for a successful one when the expected exception type doesn't match")
            void onRecoverUsingExceptionTypeThatDontMatchDoNothing() {
                HTTPResponse<String> returned = failed
                        .recover(GatewayTimeout.class, e -> "i'm recovered from a GatewayTimeout");

                InternalServerError exception = assertThrows(InternalServerError.class, returned.body()::unsafe);

                assertSame(failure, exception);
            }

            @Test
            @DisplayName("a lazy, failure HTTP response can recover for a successful one using a HTTP status code")
            void onRecoverUsingStatusCode() {
                HTTPResponse<String> returned = failed
                        .recover(HTTPStatusCode.INTERNAL_SERVER_ERROR, (status, headers, bodyAsBytes) -> "i'm recovered from an Internal Server Error");

                assertEquals("i'm recovered from an Internal Server Error", returned.body().unsafe());
            }

            @Test
            @DisplayName("a lazy, failure HTTP response can't recover for a successful one when the expected HTTP status code doesn't match")
            void onRecoverUsingStatusCodeThatDontMatchDoNothing() {
                HTTPResponse<String> returned = failed
                        .recover(HTTPStatusCode.GATEWAY_TIMEOUT, (status, headers, bodyAsBytes) -> "i'm recovered from a GatewayTimeout");

                assertSame(failed, returned);
            }
        }
    }
}
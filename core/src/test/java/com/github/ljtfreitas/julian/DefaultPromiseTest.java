package com.github.ljtfreitas.julian;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DefaultPromiseTest {

    @Nested
    class WhenSuccess {

        private final Promise<String> promise = new DefaultPromise<>(CompletableFuture.supplyAsync(() -> "hello"));

        @Test
        void onSuccess(@Mock Consumer<String> consumer) {
            CompletableFuture<String> future = promise.onSuccess(consumer).future();

            assertEquals("hello", future.join());

            verify(consumer).accept("hello");
        }

        @Test
        void then() {
            CompletableFuture<String> future = promise.then(value -> value + " world").future();

            assertEquals("hello world", future.join());
        }

        @Test
        void bind() {
            CompletableFuture<String> future = promise.bind(value -> Promise.done(value + " world")).future();

            assertEquals("hello world", future.join());
        }

        @Test
        void zip() {
            CompletableFuture<String> future = promise.zip(Promise.done(" world"), String::concat).future();

            assertEquals("hello world", future.join());
        }

        @Test
        void join() {
            assertEquals("hello", promise.join().unsafe());
        }

        @Test
        void subscribe() {
            Subscriber<String, Throwable> subscriber = spy(new Subscriber<String, Throwable>() {

                @Override
                public void success(String value) {
                    assertEquals("hello", value);
                }

                @Override
                public void failure(Throwable failure) {
                    fail("a success was expected...", failure);
                }

                @Override
                public void done() {
                }

            });

            promise.subscribe(subscriber).join();

            verify(subscriber).success("hello");
            verify(subscriber).done();
        }
    }

    @Nested
    class WhenFailed {

        private final Exception failure = new RuntimeException("oops");
        private final Promise<String> promise = new DefaultPromise<>(CompletableFuture.failedFuture(failure));

        @Test
        void onFailure(@Mock Consumer<Throwable> consumer) {
            promise.onFailure(consumer).join();

            verify(consumer).accept(failure);
        }

        @Test
        void failure() {
            CompletableFuture<String> future = promise
                    .failure(e -> new RuntimeException(e.getMessage() + "...an error occurred"))
                    .future()
                    .exceptionally(e -> e.getCause().getMessage());

            assertThat(future.join(), endsWith("oops...an error occurred"));
        }

        @Test
        void recover() {
            CompletableFuture<String> future = new DefaultPromise<String>(CompletableFuture.failedFuture(new RuntimeException("oops")))
                    .recover(e -> e.getMessage() + "...but i'm recovered")
                    .future();

            assertThat(future.join(), endsWith("oops...but i'm recovered"));
        }

        @Test
        void subscribe() {
            Subscriber<String, Throwable> subscriber = spy(new Subscriber<String, Throwable>() {

                @Override
                public void success(String value) {
                    fail("a failure was expected...instead, it was the value " + value);
                }

                @Override
                public void failure(Throwable e) {
                    assertEquals(failure, e);
                }

                @Override
                public void done() {
                }
            });

            promise.subscribe(subscriber).join();

            verify(subscriber).failure(failure);
        }

        @Nested
        class UnrecoverableCases {

            @Test
            void unrecoverableBecauseExceptionClassIsDifferentThatExpected() {
                Promise<Object> promise = new DefaultPromise<>(CompletableFuture.failedFuture(new IllegalArgumentException("ops")))
                        .recover(IllegalStateException.class, Throwable::getMessage);

                assertThrows(IllegalArgumentException.class, promise.join()::unsafe);
            }

            @Test
            void unrecoverableBecauseExceptionDoesNotMatchThePredicate() {
                Promise<Object> promise = new DefaultPromise<>(CompletableFuture.failedFuture(new IllegalArgumentException("ops")))
                        .recover(e -> e instanceof IllegalStateException, Throwable::getMessage);

                assertThrows(IllegalArgumentException.class, promise.join()::unsafe);
            }
        }
    }

    @Nested
    class WithThreadPool {

        @Test
        void sample(TestReporter reporter) {
            Executor executor = Executors.newSingleThreadExecutor();

            reporter.publishEntry("running on thread " + Thread.currentThread() + "...");

            String result = Promise.pending(() -> "hello", executor)
                    .onSuccess(value -> reporter.publishEntry("running 'onSucess' on thread " + Thread.currentThread() + "..."))
                    .then(value -> {
                        reporter.publishEntry("running 'then' on thread " + Thread.currentThread() + "...");
                        return value + " world";
                    }).join().unsafe();

            assertEquals("hello world", result);
        }
    }
}
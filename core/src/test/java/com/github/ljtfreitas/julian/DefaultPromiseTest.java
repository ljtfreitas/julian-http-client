package com.github.ljtfreitas.julian;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DefaultPromiseTest {

    @Nested
    class WhenSuccess {

        private final Promise<String, Exception> promise = new DefaultPromise<>(CompletableFuture.supplyAsync(() -> "hello"));

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
        void join() {
            assertEquals("hello", promise.join().unsafe());
        }
    }

    @Nested
    class WhenFailed {

        private final Exception failure = new RuntimeException("oops");
        private final Promise<String, Exception> promise = new DefaultPromise<>(CompletableFuture.failedFuture(failure));

        @Test
        void onFailure(@Mock Consumer<Exception> consumer) {
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
            CompletableFuture<String> future = new DefaultPromise<String, Exception>(CompletableFuture.failedFuture(new RuntimeException("oops")))
                    .recover(e -> e.getMessage() + "...but i'm recovered")
                    .future();

            assertThat(future.join(), endsWith("oops...but i'm recovered"));
        }
    }
}
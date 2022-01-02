package com.github.ljtfreitas.julian;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DonePromiseTest {

    private final Promise<String, Exception> promise = new DonePromise<>("hello");

    @Test
    void onSuccess(@Mock Consumer<String> consumer) {
        CompletableFuture<String> future = promise.onSuccess(consumer).future();

        assertEquals("hello", future.join());

        verify(consumer).accept("hello");
    }

    @Test
    void onFailure(@Mock Consumer<Exception> consumer) {
        CompletableFuture<String> future = promise.onFailure(consumer).future();

        verify(consumer, never()).accept(any());
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
    void failure() {
        CompletableFuture<String> future = promise
                .failure(e -> new RuntimeException(e.getMessage() + "...an error occurred"))
                .future();

        assertEquals("hello", future.join());
    }

    @Test
    void recover() {
        CompletableFuture<String> future = promise
                .recover(e -> "oops...but i'm recovered")
                .future();

        assertEquals("hello", future.join());
    }

    @Test
    void join() {
        assertEquals("hello", promise.join().unsafe());
    }
}
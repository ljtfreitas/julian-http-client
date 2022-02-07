package com.github.ljtfreitas.julian;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FailedPromiseTest {

    private final Exception failure = new RuntimeException("oops");
    private final Promise<String> promise = new FailedPromise<>(failure);

    @Test
    void onSuccess(@Mock Consumer<String> consumer) {
        promise.onSuccess(consumer);
        verify(consumer, never()).accept(any());
    }

    @Test
    void onFailure(@Mock Consumer<Exception> consumer) {
        promise.onFailure(consumer);
        verify(consumer).accept(any());
    }

    @Test
    void then() {
        CompletableFuture<String> future = promise.then(value -> value + " world")
                .future()
                .exceptionally(Throwable::getMessage);

        assertEquals(failure.getMessage(), future.join());
    }

    @Test
    void bind() {
        CompletableFuture<String> future = promise.bind(value -> Promise.done(value + " world"))
                .future()
                .exceptionally(Throwable::getMessage);

        assertEquals(failure.getMessage(), future.join());
    }

    @Test
    void failure() {
        CompletableFuture<String> future = promise
                .failure(e -> new RuntimeException(e.getMessage() + "...an error occurred"))
                .future()
                .exceptionally(Throwable::getMessage);

        assertEquals("oops...an error occurred", future.join());
    }

    @Test
    void recover() {
        CompletableFuture<String> future = promise
                .recover(e -> "oops...but i'm recovered")
                .future()
                .exceptionally(Throwable::getMessage);

        assertEquals("oops...but i'm recovered", future.join());
    }

    @Test
    void join() {
        RuntimeException exception = assertThrows(RuntimeException.class, promise.join()::unsafe);
        assertSame(failure, exception);
    }
}
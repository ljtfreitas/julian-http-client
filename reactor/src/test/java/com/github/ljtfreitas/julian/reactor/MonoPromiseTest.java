package com.github.ljtfreitas.julian.reactor;

import com.github.ljtfreitas.julian.Subscriber;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.function.Consumer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MonoPromiseTest {

    @Nested
    class WhenSuccess {

        private final MonoPromise<String> promise = new MonoPromise<>(Mono.just("hello"));

        @Test
        void onSuccess(@Mock Consumer<String> consumer) {
            Mono<String> result = promise.onSuccess(consumer).mono();

            StepVerifier.create(result)
                    .expectNext("hello")
                    .expectComplete()
                    .verify();

            verify(consumer).accept("hello");
        }

        @Test
        void then() {
            Mono<String> result = promise.then(value -> value + " world").mono();

            StepVerifier.create(result)
                    .expectNext("hello world")
                    .expectComplete()
                    .verify();
        }

        @Test
        void bind() {
            Mono<String> result = promise.bind(value -> new MonoPromise<>(Mono.just(value + " world"))).mono();

            StepVerifier.create(result)
                    .expectNext("hello world")
                    .expectComplete()
                    .verify();
        }

        @Test
        void join() {
            assertEquals("hello", promise.join().unsafe());
        }

        @Test
        void subscribe() {
            Subscriber<String> subscriber = spy(new Subscriber<>() {

                @Override
                public void success(String value) {
                    assertEquals("hello", value);
                }

                @Override
                public void failure(Exception failure) {
                    fail("a success was expected...", failure);
                }

                @Override
                public void done() {}
            });

            Mono<String> result = promise.subscribe(subscriber).mono();

            StepVerifier.create(result)
                    .expectSubscription()
                    .expectNext("hello")
                    .expectComplete()
                    .verify();

            verify(subscriber).success("hello");
            verify(subscriber).done();
        }
    }

    @Nested
    class WhenFailed {

        private final Exception failure = new RuntimeException("oops");
        private final MonoPromise<String> promise = new MonoPromise<>(Mono.error(failure));

        @Test
        void onFailure(@Mock Consumer<Exception> consumer) {
            Mono<String> result = promise.onFailure(consumer).mono();

            StepVerifier.create(result)
                    .expectError()
                    .verify();

            verify(consumer).accept(failure);
        }

        @Test
        void failure() {
            Mono<String> result = promise
                    .failure(e -> new RuntimeException(e.getMessage() + "...an error occurred"))
                    .mono();


            StepVerifier.create(result)
                    .consumeErrorWith(e -> assertThat(e.getMessage(), endsWith("oops...an error occurred")))
                    .verify();
        }

        @Test
        void recover() {
            Mono<String> result = new MonoPromise<String>(Mono.error(new RuntimeException("oops")))
                    .recover(e -> e.getMessage() + "...but i'm recovered")
                    .mono();

            StepVerifier.create(result)
                    .consumeNextWith(value -> assertEquals("oops...but i'm recovered", value))
                    .verifyComplete();
        }

        @Test
        void subscribe() {
            Subscriber<String> subscriber = spy(new Subscriber<>() {

                @Override
                public void success(String value) {
                    fail("a failure was expected...instead, it was the value " + value);
                }

                @Override
                public void failure(Exception e) {
                    assertEquals(failure, e);
                }

                @Override
                public void done() {}
            });

            Mono<String> result = promise.subscribe(subscriber).mono();

            StepVerifier.create(result)
                    .expectError()
                    .verify();

            verify(subscriber).failure(failure);
        }
    }
}
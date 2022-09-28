package com.github.ljtfreitas.julian;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublisherResponseTTest {

    @Mock
    private Endpoint endpoint;

    private final PublisherResponseT responseT = new PublisherResponseT();

    @Nested
    class Predicates {

        @Test
        void supported() {
            when(endpoint.returnType()).thenReturn(JavaType.parameterized(Publisher.class, String.class));

            assertTrue(responseT.test(endpoint));
        }

        @Test
        void unsupported() {
            when(endpoint.returnType()).thenReturn(JavaType.valueOf(String.class));

            assertFalse(responseT.test(endpoint));
        }
    }

    @Nested
    class Adapted {

        @Test
        void parameterized() {
            when(endpoint.returnType()).thenReturn(JavaType.parameterized(Publisher.class, String.class));

            assertEquals(JavaType.valueOf(String.class), responseT.adapted(endpoint));
        }

        @Test
        void simple() {
            when(endpoint.returnType()).thenReturn(JavaType.object());

            assertEquals(JavaType.object(), responseT.adapted(endpoint));
        }
    }

    @Test
    void compose(@Mock ResponseFn<String, Object> fn, @Mock Promise<Response<String, Throwable>> response, TestReporter reporter) throws InterruptedException {
        Arguments arguments = Arguments.empty();

        when(fn.run(response, arguments)).thenReturn(Promise.pending(CompletableFuture.supplyAsync(() -> "expected",
                CompletableFuture.delayedExecutor(1000, TimeUnit.MILLISECONDS))));

        when(fn.run(response, arguments)).thenReturn(Promise.done("expected"));

        Publisher<Object> publisher = responseT.bind(endpoint, fn).join(response, arguments);

        publisher.subscribe(new Subscriber<>() {

            @Override
            public void onSubscribe(Subscription subscription) {
                reporter.publishEntry("onSubscribe");
                subscription.request(1);
            }

            @Override
            public void onNext(Object value) {
                reporter.publishEntry("onNext: " + Thread.currentThread());
                assertEquals("expected", value);
            }

            @Override
            public void onError(Throwable throwable) {
                reporter.publishEntry("onError");
                fail(throwable.getMessage());
            }

            @Override
            public void onComplete() {
                reporter.publishEntry("onComplete");
            }
        });

        Thread.sleep(1500);
    }
}
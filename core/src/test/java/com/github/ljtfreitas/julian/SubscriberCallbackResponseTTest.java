package com.github.ljtfreitas.julian;

import com.github.ljtfreitas.julian.Endpoint.Parameter;
import com.github.ljtfreitas.julian.Endpoint.Parameters;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriberCallbackResponseTTest {

    @Mock
    private Endpoint endpoint;

    private final SubscriberCallbackResponseT responseT = new SubscriberCallbackResponseT();

    @Nested
    class Predicates {

        @Test
        void supported() {
            when(endpoint.parameters()).thenReturn(Parameters.create(Parameter.callback(0, "subscriber", JavaType.parameterized(Subscriber.class, String.class))));
            when(endpoint.returnType()).thenReturn(JavaType.none());

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
            when(endpoint.parameters()).thenReturn(Parameters.create(Parameter.callback(0, "success", JavaType.parameterized(Subscriber.class, String.class))));

            assertEquals(JavaType.parameterized(Publisher.class, String.class), responseT.adapted(endpoint));
        }

        @Test
        void unsupported() {
            when(endpoint.parameters()).thenReturn(Parameters.empty());

            assertEquals(JavaType.parameterized(Publisher.class, Object.class), responseT.adapted(endpoint));
        }
    }

    @Test
    void compose(@Mock Endpoint endpoint, TestReporter reporter) throws InterruptedException {
        Promise<Response<String, Throwable>> response = Promise.pending(CompletableFuture.supplyAsync(() -> new DoneResponse<>("it works!"),
                CompletableFuture.delayedExecutor(1000, TimeUnit.MILLISECONDS)));

        Subscriber<String> subscriber = new Subscriber<>() {
            @Override
            public void onSubscribe(Subscription subscription) {
                reporter.publishEntry("onSubscribe");
                subscription.request(1);
            }

            @Override
            public void onNext(String item) {
                reporter.publishEntry("onNext: '" + item + "' on " + Thread.currentThread());
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
        };

        when(endpoint.parameters()).thenReturn(Parameters.create(Parameter.callback(0, "subscriber", JavaType.parameterized(Subscriber.class, String.class))));
        Arguments arguments = Arguments.create(subscriber);

        ObjectResponseT<Object> objectResponseT = new ObjectResponseT<>();

        PublisherResponseT publisherResponseT = new PublisherResponseT();
        ResponseFn<String, Publisher<Object>> publisherFn = publisherResponseT.bind(endpoint, objectResponseT.bind(endpoint, null));

        responseT.bind(endpoint, publisherFn).join(response, arguments);

        Thread.sleep(1500);
    }
}
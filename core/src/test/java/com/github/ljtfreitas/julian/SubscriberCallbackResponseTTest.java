package com.github.ljtfreitas.julian;

import com.github.ljtfreitas.julian.Endpoint.Parameter;
import com.github.ljtfreitas.julian.Endpoint.Parameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriberCallbackResponseTTest {

    @Mock
    private Endpoint endpoint;

    private final SubscriberCallbackResponseT<String> responseT = new SubscriberCallbackResponseT<>();

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
        void parameterized() throws Exception {
            when(endpoint.parameters()).thenReturn(Parameters.create(Parameter.callback(0, "success", JavaType.parameterized(Subscriber.class, String.class))));

            assertEquals(JavaType.parameterized(Publisher.class, String.class), responseT.adapted(endpoint));
        }

        @Test
        void unsupported() throws Exception {
            when(endpoint.parameters()).thenReturn(Parameters.empty());

            assertEquals(JavaType.parameterized(Publisher.class, Object.class), responseT.adapted(endpoint));
        }
    }

    @Test
    void compose(@Mock Endpoint endpoint, TestReporter reporter) throws InterruptedException {
        RequestIO<String> request = new StringRequest();

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

        ObjectResponseT<String> objectResponseT = new ObjectResponseT<>();

        PublisherResponseT<String> publisherResponseT = new PublisherResponseT<>();
        ResponseFn<Publisher<String>, String> publisherFn = publisherResponseT.comp(endpoint, objectResponseT.comp(endpoint, null));

        responseT.comp(endpoint, publisherFn).join(request, arguments);

        Thread.sleep(1500);
    }

    private class StringRequest implements RequestIO<String> {

        @Override
        public Promise<? extends Response<String>> execute() {
            return Promise.pending(CompletableFuture.supplyAsync(() -> new DoneResponse<>("it works!"),
                    CompletableFuture.delayedExecutor(1000, TimeUnit.MILLISECONDS)));
        }
    }
}
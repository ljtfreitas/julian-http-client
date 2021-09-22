package com.github.ljtfreitas.julian.http;

import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.http.client.HTTPClientResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Map;

import static com.github.ljtfreitas.julian.http.HTTPResponseFailure.empty;
import static com.github.ljtfreitas.julian.http.HTTPStatusCode.INTERNAL_SERVER_ERROR;
import static com.github.ljtfreitas.julian.http.HTTPStatusCode.NOT_FOUND;
import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConditionalHTTPResponseFailureTest {

    @Test
    void notFound(@Mock HTTPClientResponse failed) {
        ConditionalHTTPResponseFailure subject = new ConditionalHTTPResponseFailure(Map.of(NOT_FOUND, empty()));

        when(failed.status()).thenReturn(HTTPStatus.valueOf(NOT_FOUND));

        HTTPResponse<Void> handled = subject.apply(failed, JavaType.none());

        assertThat(handled, instanceOf(EmptyHTTPResponse.class));
    }

    @Test
    void anyOther(@Mock HTTPClientResponse failed, @Mock HTTPResponseFailure failure, @Mock HTTPResponse<Void> other) {
        ConditionalHTTPResponseFailure subject = new ConditionalHTTPResponseFailure(emptyMap(), failure);

        when(failed.status()).thenReturn(HTTPStatus.valueOf(INTERNAL_SERVER_ERROR));
        when(failure.<Void> apply(same(failed), any(JavaType.class))).thenReturn(other);

        HTTPResponse<Void> handled = subject.apply(failed, JavaType.none());

        assertThat(handled, equalTo(other));

        verify(failure).apply(failed, JavaType.none());
    }
}
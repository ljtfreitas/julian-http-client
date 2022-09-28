package com.github.ljtfreitas.julian.http;

import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.http.HTTPClientFailureResponseException.BadRequest;
import com.github.ljtfreitas.julian.http.codec.HTTPResponseReaderException;
import com.github.ljtfreitas.julian.http.codec.HTTPResponseReaders;
import com.github.ljtfreitas.julian.http.codec.StringHTTPMessageCodec;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RecoverableHTTPResponseTest {

    private final HTTPResponseReaders readers = new HTTPResponseReaders(List.of(StringHTTPMessageCodec.get()));

    @Test
    void weAreAbleToRecoverAFailureResponse() {
        HTTPHeaders headers = new HTTPHeaders(List.of(new HTTPHeader(HTTPHeader.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)));

        Promise<byte[]> failureResponseBody = Promise.done("i am a string".getBytes());

        FailureHTTPResponse<String> failure = new FailureHTTPResponse<>(new BadRequest(headers, failureResponseBody));

        RecoverableHTTPResponse<String> recoverable = new RecoverableHTTPResponse<>(failure, readers);

        HTTPResponse<String> recovered = recoverable.recover(String.class);

        // if we try to get the failure response body from the original response, we should get an exception
        assertThrows(BadRequest.class, failure.body()::unsafe);

        // but it's safe to get from the recovered response
        assertEquals("i am a string", recovered.body().unsafe());
    }

    @Test
    void weCantRecoverIfTheResponseDoesNotHaveTheContentTypeHeader() {
        HTTPHeaders headers = HTTPHeaders.empty();

        FailureHTTPResponse<String> failure = new FailureHTTPResponse<>(new BadRequest(headers, Promise.done("".getBytes())));

        RecoverableHTTPResponse<String> recoverable = new RecoverableHTTPResponse<>(failure, readers);

        HTTPResponse<String> recovered = recoverable.recover(String.class);

        UnrecoverableHTTPResponseException unrecoverableHTTPResponseException = assertThrows(UnrecoverableHTTPResponseException.class, recovered.body()::unsafe);

        Throwable cause = unrecoverableHTTPResponseException.getCause();

        assertThat(cause, instanceOf(IllegalStateException.class));
    }

    @Test
    void weCantRecoverIfThereIsNoAReaderAbleToConvertToTheDesiredType() {
        HTTPHeaders headers = new HTTPHeaders(List.of(new HTTPHeader(HTTPHeader.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)));

        FailureHTTPResponse<String> failure = new FailureHTTPResponse<>(new BadRequest(headers, Promise.done("{\"message\":\"i am an error\"}".getBytes())));

        RecoverableHTTPResponse<String> recoverable = new RecoverableHTTPResponse<>(failure, new HTTPResponseReaders(Collections.emptyList()));

        HTTPResponse<String> recovered = recoverable.recover(String.class);

        UnrecoverableHTTPResponseException unrecoverableHTTPResponseException = assertThrows(UnrecoverableHTTPResponseException.class, recovered.body()::unsafe);

        Throwable cause = unrecoverableHTTPResponseException.getCause();

        assertThat(cause, instanceOf(HTTPResponseReaderException.class));
    }
}
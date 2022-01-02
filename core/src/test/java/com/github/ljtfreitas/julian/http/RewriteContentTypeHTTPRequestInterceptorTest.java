package com.github.ljtfreitas.julian.http;

import com.github.ljtfreitas.julian.JavaType;
import com.github.ljtfreitas.julian.Promise;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.Optional;

import static com.github.ljtfreitas.julian.http.HTTPHeader.CONTENT_TYPE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RewriteContentTypeHTTPRequestInterceptorTest {

    private final RewriteContentTypeHTTPRequestInterceptor interceptor = new RewriteContentTypeHTTPRequestInterceptor();

    @Test
    void rewriteContentTypeHeaderWhenBodyIsPresent(@Mock HTTPRequestBody body) {
        HTTPHeaders headers = HTTPHeaders.create(new HTTPHeader(CONTENT_TYPE, "application/json"));

        MediaType bodyContentType = MediaType.valueOf("application/json; whatever=value");
        when(body.contentType()).thenReturn(Optional.of(bodyContentType));

        HTTPRequest<Void> request = new SimpleHTTPRequest(headers, body);

        Promise<HTTPRequest<Void>, HTTPException> promise = interceptor.intercepts(Promise.done(request));

        HTTPRequest<Void> newRequest = promise.join().unsafe();

        assertThat(newRequest.headers(), contains(new HTTPHeader(CONTENT_TYPE, bodyContentType.toString())));
    }

    @Test
    void dontTouchTheContentTypeHeaderWhenRequestBodyContentTypeIsMissing(@Mock HTTPRequestBody body) {
        HTTPHeader header = new HTTPHeader(CONTENT_TYPE, "application/json");
        HTTPHeaders headers = HTTPHeaders.create(header);

        when(body.contentType()).thenReturn(Optional.empty());

        HTTPRequest<Void> request = new SimpleHTTPRequest(headers, body);

        Promise<HTTPRequest<Void>, HTTPException> promise = interceptor.intercepts(Promise.done(request));

        HTTPRequest<Void> newRequest = promise.join().unsafe();

        assertThat(newRequest.headers(), contains(header));
    }

    @Test
    void dontTouchTheContentTypeHeaderWhenThereIsNotBody() {
        HTTPHeader contentType = new HTTPHeader(CONTENT_TYPE, "application/json");

        HTTPHeaders headers = HTTPHeaders.create(contentType);

        HTTPRequest<Void> request = new SimpleHTTPRequest(headers, null);

        Promise<HTTPRequest<Void>, HTTPException> promise = interceptor.intercepts(Promise.done(request));

        HTTPRequest<Void> newRequest = promise.join().unsafe();

        assertThat(newRequest.headers(), contains(contentType));
    }

    private class SimpleHTTPRequest implements HTTPRequest<Void> {

        private final HTTPHeaders headers;
        private final HTTPRequestBody body;

        private SimpleHTTPRequest(HTTPHeaders headers, HTTPRequestBody body) {
            this.headers = headers;
            this.body = body;
        }

        @Override
        public JavaType returnType() {
            return null;
        }

        @Override
        public HTTPRequest<Void> path(URI path) {
            return null;
        }

        @Override
        public HTTPRequest<Void> method(HTTPMethod method) {
            return null;
        }

        @Override
        public HTTPRequest<Void> headers(HTTPHeaders headers) {
            return new SimpleHTTPRequest(headers, body);
        }

        @Override
        public HTTPRequest<Void> body(HTTPRequestBody body) {
            return null;
        }

        @Override
        public URI path() {
            return null;
        }

        @Override
        public HTTPMethod method() {
            return null;
        }

        @Override
        public HTTPHeaders headers() {
            return headers;
        }

        @Override
        public Optional<HTTPRequestBody> body() {
            return Optional.ofNullable(body);
        }

        @Override
        public Promise<HTTPResponse<Void>, HTTPException> execute() {
            return null;
        }
    }
}
package com.github.ljtfreitas.julian.http.client;

import com.github.ljtfreitas.julian.http.DefaultHTTPRequestBody;
import com.github.ljtfreitas.julian.http.HTTPHeader;
import com.github.ljtfreitas.julian.http.HTTPHeaders;
import com.github.ljtfreitas.julian.http.HTTPMethod;
import com.github.ljtfreitas.julian.http.HTTPRequest;
import com.github.ljtfreitas.julian.http.MediaType;
import com.github.ljtfreitas.julian.http.codec.StringHTTPMessageCodec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.junit.jupiter.MockServerSettings;

import java.net.URI;
import java.net.http.HttpRequest.BodyPublishers;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.MediaType.TEXT_PLAIN;

@ExtendWith(MockitoExtension.class)
@ExtendWith(MockServerExtension.class)
@MockServerSettings(ports = 8090)
class DebugHTTPClientTest {

    private final MockServerClient mockServer;

    DebugHTTPClientTest(MockServerClient mockServer) {
        this.mockServer = mockServer;
    }

    @Test
    void show(@Mock HTTPRequest<String> httpRequest) {
        mockServer.when(request("/debug")
                        .withMethod("GET")
                        .withBody("request body"))
                .respond(response()
                        .withStatusCode(200)
                        .withContentType(TEXT_PLAIN)
                        .withHeader("x-some-header", "some-content")
                        .withBody("it works!"));

        DebugHTTPClient debugHTTPClient = new DebugHTTPClient(new DefaultHTTPClient());

        when(httpRequest.path()).thenReturn(URI.create("http://localhost:8090/debug"));
        when(httpRequest.method()).thenReturn(HTTPMethod.GET);
        when(httpRequest.headers()).thenReturn(new HTTPHeaders(List.of(new HTTPHeader("X-Some-Header", "some-content"),
                                                                       new HTTPHeader("Accept", "text/plain"))));
        when(httpRequest.body()).thenReturn(Optional.of(new DefaultHTTPRequestBody(MediaType.TEXT_PLAIN, () -> BodyPublishers.ofString("request body"))));

        HTTPClientRequest intercepted = debugHTTPClient.request(httpRequest);

        intercepted.execute().join().unsafe();

        verify(httpRequest, atLeastOnce()).path();
        verify(httpRequest, atLeastOnce()).headers();
        verify(httpRequest, atLeastOnce()).method();
    }
}
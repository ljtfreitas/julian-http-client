## http-client-reactor-netty

This module provides to use [Reactor Netty](https://projectreactor.io/docs/netty/release/reference/index.html) as reactive, non-blocking HTTP client implementation.

## Install

### Maven
```xml
<dependency>
    <groupId>com.github.ljtfreitas.julian-http-client</groupId>
    <artifactId>julian-http-client-http-client-reactor-netty</artifactId>
    <version>${julian-http-client-version}</version>
</dependency>
```

### Gradle
```kotlin
dependencies {
    implementation("com.github.ljtfreitas.julian-http-client:julian-http-client-reactor-netty:$julianHttpClientVersion")
}
```

## Usage

We need to configure the `ProxyBuilder` to use the implementation provided for this module:

```java
import com.github.ljtfreitas.julian.http.client.reactor.ReactorNettyHTTPClient;

interface MyApi {}

ReactorNettyHTTPClient reactorNettyHTTPClient = new ReactorNettyHTTPClient();

MyApi myApi = new ProxyBuilder()
    .http()
        .client()
            .with(reactorNettyHTTPClient)
        .and()
    .build(MyApi.class, "http://my.api.com");
```

`ReactorNettyHTTPClient` uses a default [HttpClient](https://projectreactor.io/docs/netty/release/api/reactor/netty/http/client/HttpClient.html), but we can customize it:

```java
import com.github.ljtfreitas.julian.http.client.reactor.ReactorNettyHTTPClient;
import reactor.netty.http.client.HttpClient;

interface MyApi {}

HttpClient httpClient = HttpClient
    .create()
    // other HttpClient options here...

ReactorNettyHTTPClient reactorNettyHTTPClient = new ReactorNettyHTTPClient(httpClient);

MyApi myApi = new ProxyBuilder()
    .http()
        .client()
            .with(reactorNettyHTTPClient)
        .and()
    .build(MyApi.class, "http://my.api.com");
```

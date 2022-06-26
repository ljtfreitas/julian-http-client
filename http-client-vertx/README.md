## http-client-vertx

This module provides to use [Vert.x Web Client](https://vertx.io/docs/vertx-web-client/java/) as reactive, non-blocking HTTP client implementation.

## Install

### Maven
```xml
<dependency>
    <groupId>com.github.ljtfreitas.julian-http-client</groupId>
    <artifactId>julian-http-client-http-client-vertx</artifactId>
    <version>${julian-http-client-version}</version>
</dependency>
```

### Gradle
```kotlin
dependencies {
    implementation("com.github.ljtfreitas.julian-http-client:julian-http-client-vertx:$julianHttpClientVersion")
}
```

## Usage

We need to configure the `ProxyBuilder` to use the implementation provided for this module:

```java
import com.github.ljtfreitas.julian.http.client.vertx.VertxHTTPClient;
import io.vertx.core.Vertx;

interface MyApi {}

Vertx vertx = // ...

VertxHTTPClient vertxHTTPClient = new VertxHTTPClient(vertx);

MyApi myApi = new ProxyBuilder()
    .http()
        .client()
            .with(vertxHTTPClient)
        .and()
    .build(MyApi.class, "http://my.api.com");
```

`VertxHTTPClient` creates a Vertx's `WebClient` instance using default options, but we can customize it:

```java
import com.github.ljtfreitas.julian.http.client.vertx.VertxHTTPClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.Vertx;

interface MyApi {}

Vertx vertx = // ...

WebClientOptions options = new WebClientOptions();
// configure WebClientOptions...

VertxHTTPClient vertxHTTPClient = new VertxHTTPClient(vertx, options);

MyApi myApi = new ProxyBuilder()
    .http()
        .client()
            .with(vertxHTTPClient)
        .and()
    .build(MyApi.class, "http://my.api.com");
```
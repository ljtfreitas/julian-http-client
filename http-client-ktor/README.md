## http-client-ktor

This module provides to use [Ktor Client](https://ktor.io/docs/getting-started-ktor-client.html) as non-blocking, coroutine-based HTTP client implementation.

## Install

### Maven
```xml
<dependency>
    <groupId>com.github.ljtfreitas.julian-http-client</groupId>
    <artifactId>julian-http-client-http-client-ktor</artifactId>
    <version>${julian-http-client-version}</version>
</dependency>
```

### Gradle
```kotlin
dependencies {
    implementation("com.github.ljtfreitas.julian-http-client:julian-http-client-ktor:$julianHttpClientVersion")
}
```

## Usage

We need to configure the `ProxyBuilder` to use the implementation provided for this module:

```kotlin
interface MyApi {}

val ktorHttpClient = KtorHTTPClient()

val myApi = proxy<MyApi>("http://my.api.com") {
    http {
        client {
            with(ktorHttpClient)
        }
    }
}
```

In order to configure the client, we can pass an additional block to the constructor:

```kotlin
val ktorHttpClient = KtorHTTPClient {
    followRedirects = true

    //any other configuration here
}
```

Or install plugins:

```kotlin
val ktorHttpClient = KtorHTTPClient {
    install(HttpTimeout) {
        requestTimeoutMillis = 2000
    }

    //any configuration here
}
```

The engine used by default is [CIO](https://ktor.io/docs/http-client-engines.html#cio), a fully asynchronous coroutine-based engine, with default options. We can customize it, of course:

```kotlin
import io.ktor.client.engine.cio.CIO

val ktorHttpClient = KtorHTTPClient(CIO) {
    engine {
        // specific CIO options; see https://ktor.io/docs/http-client-engines.html#cio

    }
}
```

And we can change the engine, if we want. For instance, [Apache](https://ktor.io/docs/http-client-engines.html#apache) engine

```kotlin
import io.ktor.client.engine.apache.*

val ktorHttpClient = KtorHTTPClient(Apache) {
    engine {
        // specific Apache engine options; see https://ktor.io/docs/http-client-engines.html#apache

    }
}
```

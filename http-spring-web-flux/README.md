## http-spring-web-flux

This module provides to use [Spring's WebClient](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/reactive/function/client/WebClient.html) as reactive, non-blocking HTTP implementation.

## Install

### Maven
```xml
<dependency>
    <groupId>com.github.ljtfreitas.julian-http-client</groupId>
    <artifactId>julian-http-client-http-spring-web-flux</artifactId>
    <version>${julian-http-client-version}</version>
</dependency>
```

### Gradle
```kotlin
dependencies {
    implementation("com.github.ljtfreitas.julian-http-client:julian-http-client-http-spring-web-flux:$julianHttpClientVersion")
}
```

## Usage

We just need to configure the `ProxyBuilder` to use the implementation provided for this module:

```java
import com.github.ljtfreitas.julian.http.spring.webflux.WebClientHTTP;

interface MyApi {}

WebClientHTTP webClientHttp = new WebClientHTTP();

MyApi myApi = new ProxyBuilder()
    .http()
        .with(webClientHttp)
    .build(SimpleApi.class, "http://my.api.com");
```

`WebClientHTTP` uses a default `WebClient`, but we can customize it:

```java
import com.github.ljtfreitas.julian.http.spring.webflux.WebClientHTTP;
import org.springframework.web.reactive.function.client.WebClient;

interface MyApi {}

WebClient webClient = WebClient.builder()
    //WebClient customizations...
    .build();

WebClientHTTP webClientHttp = new WebClientHTTP(webClient);

MyApi myApi = new ProxyBuilder()
    .http()
        .client()
            .with(webClientHttp)
        .and()
    .build(SimpleApi.class, "http://my.api.com");
```

`WebClient` has built-in support for interceptors, media types and error handling. So, all these concerns are delegated for it; in other words, `julian-http-client` doesn't apply default serialization/deserialization behaviors and interceptors-chain when using `WebClientHTTP`. All these stuff are done by `WebClient`.

In case of failures (IO errors or 4xx/5xx responses), `WebClient` exceptions wiil be translated to `julian-http-client` ones, so the error handling on the client side can keep the same.
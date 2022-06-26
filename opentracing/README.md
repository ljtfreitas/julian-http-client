## opentracing

This module provides support to use [OpenTracing API for Java](https://github.com/opentracing/opentracing-java) with `julian-http-client`.

## Install

### Maven
```xml
<dependency>
    <groupId>com.github.ljtfreitas.julian-http-client</groupId>
    <artifactId>julian-http-client-http-opentracing</artifactId>
    <version>${julian-http-client-version}</version>
</dependency>
```

### Gradle
```kotlin
dependencies {
    implementation("com.github.ljtfreitas.julian-http-client:julian-http-client-opentracing:$julianHttpClientVersion")
}
```

## Usage

`julian-http-client` support for OpenTracing uses [interceptors](../README.md#http-request-interceptors). The original request is modified to include trace-related headers.

```java
import com.github.ljtfreitas.julian.http.opentracing.TracingHTTPRequestInterceptor;
import io.opentracing.Tracer;

interface MyApi {}

Tracer tracer = // ...

TracingHTTPRequestInterceptor tracingInterceptor = new TracingHTTPRequestInterceptor(tracer);

MyApi myApi = new ProxyBuilder()
    .http()
        .interceptors()
            .add(tracingInterceptor)
        .and()
    .and()
    .build(MyApi.class, "http://my.api.com");
```

And that's it.

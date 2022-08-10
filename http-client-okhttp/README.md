## http-client-okhttp

This module provides to use [OkHTTP](https://square.github.io/okhttp/) as HTTP client implementation.

## Install

### Maven
```xml
<dependency>
    <groupId>com.github.ljtfreitas.julian-http-client</groupId>
    <artifactId>julian-http-client-http-client-okhttp</artifactId>
    <version>${julian-http-client-version}</version>
</dependency>
```

### Gradle
```kotlin
dependencies {
    implementation("com.github.ljtfreitas.julian-http-client:julian-http-client-okhttp:$julianHttpClientVersion")
}
```

## Usage

We need to configure the `ProxyBuilder` to use the implementation provided for this module:

```java
import com.github.ljtfreitas.julian.http.client.okhttp.OkHTTPClient;

interface MyApi {}

OkHTTPClient okHTTPClient = new OkHTTPClient();

MyApi myApi = new ProxyBuilder()
    .http()
        .client()
            .with(okHTTPClient)
        .and()
    .build(MyApi.class, "http://my.api.com");
```

`OkHTTPClient` creates a [OkHttpClient](https://square.github.io/okhttp/4.x/okhttp/okhttp3/-ok-http-client/) instance using default options, but we can customize it:

```java
import com.github.ljtfreitas.julian.http.client.okhttp.OkHTTPClient;
import okhttp3.OkHttpClient;

interface MyApi {}

OkHttpClient client = new OkHttpClient.Builder()
        // ...
        .build();

OkHTTPClient okHTTPClient = new OkHTTPClient(client);

MyApi myApi = new ProxyBuilder()
    .http()
        .client()
            .with(okHTTPClient)
        .and()
    .build(MyApi.class, "http://my.api.com");
```

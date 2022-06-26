## json-jsonb

This module provides support to `application/json` using [json-b](https://javaee.github.io/jsonb-spec/), a Java spec to convert objects to/from json.

## Install

### Maven
```xml
<dependency>
    <groupId>com.github.ljtfreitas.julian-http-client</groupId>
    <artifactId>julian-http-client-json-jsonb</artifactId>
    <version>${julian-http-client-version}</version>
</dependency>
```

### Gradle
```kotlin
dependencies {
    implementation("com.github.ljtfreitas.julian-http-client:julian-http-client-json-jsonb:$julianHttpClientVersion")
}
```

And that's it. HTTP requests and responses with `application/json` media type will be serialized or deserialized using `json-b`.

## Usage

The main object from `json-b` API is [Jsonb](https://javadoc.io/static/javax.json.bind/javax.json.bind-api/1.0/javax/json/bind/Jsonb.html). `julian-http-client` creates an instance of it using default options, but we can customize that easily if we want:

```java
import com.github.ljtfreitas.julian.ProxyBuilder;
import com.github.ljtfreitas.julian.http.codec.json.jsonb.JsonBHTTPMessageCodec;

import javax.json.bind.JsonBuilder;
import javax.json.bind.Jsonb;

Jsonb myJsonb = JsonBuilder.newBuilder()
        // json-b configurations...
        .build();

MyApi myApi = new ProxyBuilder()
    .codecs()
        .add(new JsonBHTTPMessageCodec(myJsonb))
    .and()
    .build(MyApi.class);
```

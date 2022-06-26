## json-gson

This module provides support to `application/json` using [Gson](https://github.com/google/gson) library.

## Install

### Maven
```xml
<dependency>
    <groupId>com.github.ljtfreitas.julian-http-client</groupId>
    <artifactId>julian-http-client-json-gson</artifactId>
    <version>${julian-http-client-version}</version>
</dependency>
```

### Gradle
```kotlin
dependencies {
    implementation("com.github.ljtfreitas.julian-http-client:julian-http-client-json-gson:$julianHttpClientVersion")
}
```

And that's it. HTTP requests and responses with `application/json` media type will be serialized or deserialized using `gson`.

## Usage

The default implementation uses a [Gson](https://javadoc.io/doc/com.google.code.gson/gson/latest/com.google.gson/com/google/gson/Gson.html) instance with default options. In case you want to customize `gson` with features/options, you can build your own Gson and configure a `ProxyBuilder` to use it:

```java
import com.github.ljtfreitas.julian.ProxyBuilder;
import com.github.ljtfreitas.julian.http.codec.json.gson.GsonJsonHTTPMessageCodec;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

 Gson myGson = new GsonBuilder()
     // a lot of gson custom options...
     .create();

PersonApi personApi = new ProxyBuilder()
    .codecs()
        .add(new GsonJsonHTTPMessageCodec(myGson))
    .and()
    .build(PersonApi.class);
```

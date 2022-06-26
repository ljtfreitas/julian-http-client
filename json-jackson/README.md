## json-jackson

This module provides support to `application/json` using [Jackson](https://github.com/FasterXML/jackson) library.

## Install

### Maven
```xml
<dependency>
    <groupId>com.github.ljtfreitas.julian-http-client</groupId>
    <artifactId>julian-http-client-json-jackson</artifactId>
    <version>${julian-http-client-version}</version>
</dependency>
```

### Gradle
```kotlin
dependencies {
    implementation("com.github.ljtfreitas.julian-http-client:julian-http-client-json-jackson:$julianHttpClientVersion")
}
```

And that's it.

## Usage

We can use any Java object that could be handled by `jackson` as a body parameter or method return.

```java
import com.fasterxml.jackson.annotation.JsonProperty;

import com.github.ljtfreitas.julian.contract.Body;
import com.github.ljtfreitas.julian.contract.GET;
import com.github.ljtfreitas.julian.contract.POST;
import com.github.ljtfreitas.julian.contract.Path;

class Person {

    @JsonProperty
    String name;

    @JsonProperty
    int age;
}

class PersonResponse {

    @JsonProperty
    int id;

    @JsonProperty
    String name;

    @JsonProperty
    int age;
}

@Path("/person")
interface PersonApi {

    @POST
    PersonResponse create(@Body("application/json") Person person); // a body with application/json content-type will be serialized by jackson

    @GET("/{personId}")
    PersonResponse get(@Path int personId); // a response with application/json content-type will be deserialized by jackson
}
```

The default implementation uses an `ObjectMapper` with default options. In case you want to customize `jackson` with features/options, you can build your own ObjectMapper and configure a `ProxyBuilder` to use it:

```java
import com.github.ljtfreitas.julian.ProxyBuilder;
import com.github.ljtfreitas.julian.http.codec.json.jackson.JacksonJsonHTTPMessageCodec;
import com.fasterxml.jackson.databind.ObjectMapper;

ObjectMapper myObjectMapper = // ...

PersonApi personApi = new ProxyBuilder()
    .codecs()
        .add(new JacksonJsonHTTPMessageCodec(myObjectMapper))
    .and()
    .build(PersonApi.class);
```

## json-jsonp

This module provides support to `application/json` using [json-p](https://javaee.github.io/jsonp/), a Java spec to process json content.

`json-p` does not serialize/deserialize JSON to/from POJOs like `json-b` or `jackson`; instead, `json-p` has high-level objects to abstract over a JSON structure.

## Install

### Maven
```xml
<dependency>
    <groupId>com.github.ljtfreitas.julian-http-client</groupId>
    <artifactId>julian-http-client-json-jsonp</artifactId>
    <version>${julian-http-client-version}</version>
</dependency>
```

### Gradle
```kotlin
dependencies {
    implementation("com.github.ljtfreitas.julian-http-client:julian-http-client-json-jsonp:$julianHttpClientVersion")
}
```

## Usage

With the dependency in place, we can use a [JsonObject](https://javadoc.io/doc/javax.json/javax.json-api/latest/javax/json/JsonObject.html) or a [JsonArray](https://javadoc.io/doc/javax.json/javax.json-api/latest/javax/json/JsonArray.html) to write and read json content.

```java
import javax.json.Json;
import javax.json.JsonObject;

import com.github.ljtfreitas.julian.contract.Path;
import com.github.ljtfreitas.julian.contract.GET;
import com.github.ljtfreitas.julian.contract.POST;
import com.github.ljtfreitas.julian.contract.Body;

@Path("/dogs")
interface DogsApi {

    @POST
    void create(@Body("application/json") JsonObject dogAsJson);

    @GET
    JsonArray allDogs();
}

DogsApi dogsApi = new ProxyBuilder().build(DogsApi.class, "http://my.dogs.api");

JsonObject dogAsJson = Json.createObjectBuilder()
        .add("name", "Falco")
        .add("age", 3)
        .build();

dogsApi.create(dogAsJson);

JsonArray allDogs = dogsApi.allDogs();
allDogs.forEach(dog-> // ...);
```

julian-http-client uses [JsonWriter](https://javadoc.io/doc/javax.json/javax.json-api/latest/javax/json/JsonWriter.html) and [JsonReader](https://javadoc.io/doc/javax.json/javax.json-api/latest/javax/json/JsonReader.html) in order to write and read json, and these objects are created using default configurations. Of course, we can customize both, building our own [JsonWriterFactory](https://javadoc.io/doc/javax.json/javax.json-api/latest/javax/json/JsonWriterFactory.html) and [JsonReaderFactory](https://javadoc.io/doc/javax.json/javax.json-api/latest/javax/json/JsonReaderFactory.html):

```java
import com.github.ljtfreitas.julian.ProxyBuilder;
import com.github.ljtfreitas.julian.http.codec.json.jsonp.JsonPHTTPMessageCodec;

import javax.json.Json;
import javax.json.JsonWriterFactory;
import javax.json.JsonReaderFactory;

Map<String, ?> configurations = //...

JsonWriterFactory jsonWriterFactory = Json.createWriterFactory(configurations);
JsonReaderFactory jsonReaderFactory = Json.createWriterFactory(configurations);

MyApi myApi = new ProxyBuilder()
    .codecs()
        .add(new JsonPHTTPMessageCodec(jsonReaderFactory,jsonWriterFactory))
    .and()
    .build(MyApi.class);
```

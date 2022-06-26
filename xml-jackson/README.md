## xml-jackson

This module provides support to `application/xml` using [jackson](https://github.com/FasterXML/jackson-dataformat-xml).

## Install

### Maven
```xml
<dependency>
    <groupId>com.github.ljtfreitas.julian-http-client</groupId>
    <artifactId>julian-http-client-xml-jackson</artifactId>
    <version>${julian-http-client-version}</version>
</dependency>
```

### Gradle
```kotlin
dependencies {
    implementation("com.github.ljtfreitas.julian-http-client:julian-http-client-xml-jackson:$julianHttpClientVersion")
}
```

## Usage

```java
import com.github.ljtfreitas.julian.contract.Body;
import com.github.ljtfreitas.julian.contract.GET;
import com.github.ljtfreitas.julian.contract.POST;
import com.github.ljtfreitas.julian.contract.Path;

class Person {

    String name;

    int age;
}

class PersonResponse {

    int id;

    String name;

    int age;
}

@Path("/person")
interface PersonApi {

    @POST
    PersonResponse create(@Body("application/xml") Person person); // a body with application/xml content-type will be serialized by jackson

    @GET("/{personId}")
    PersonResponse get(@Path int personId); // a response with application/xml content-type will be deserialized by jackson
}
```

`jackson` provides a `XmlMapper` object to handle xml read/write operations. In order to customize it, configure a `JacksonXMLHTTPMessageCodec` custom codec:

```java
import com.github.ljtfreitas.julian.ProxyBuilder;
import com.github.ljtfreitas.julian.http.codec.xml.jackson.JacksonXMLHTTPMessageCodec;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

XmlMapper myXmlMapper = //...

PersonApi personApi = new ProxyBuilder()
    .codecs()
        .add(new JacksonXMLHTTPMessageCodec(myXmlMapper))
    .and()
    .build(PersonApi.class);
```

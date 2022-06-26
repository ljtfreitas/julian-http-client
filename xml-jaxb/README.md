## xml-jaxb

This module provides support to `application/xml` using [jax-b](https://javaee.github.io/jaxb-v2/), a Java API to process XML content.

## Install

### Maven
```xml
<dependency>
    <groupId>com.github.ljtfreitas.julian-http-client</groupId>
    <artifactId>julian-http-client-xml-jaxb</artifactId>
    <version>${julian-http-client-version}</version>
</dependency>
```

### Gradle
```kotlin
dependencies {
    implementation("com.github.ljtfreitas.julian-http-client:julian-http-client-xml-jaxb:$julianHttpClientVersion")
}
```

## Usage

```java
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "person")
class Person {

    @XmlElement
    String name;

    @XmlElement
    int age;

    Person() {
    }

    Person(String name, int age) {
        this.name = name;
        this.age = age;
    }
}

@XmlRootElement(name = "person")
class PersonResponse {

    @XmlElement
    int id;

    @XmlElement
    String name;

    @XmlElement
    int age;
}

@Path("/person")
interface PersonApi {

    @POST
    PersonResponse create(@Body("application/xml") Person person); // a body with application/xml content-type will be serialized by jax-b

    @GET("/{personId}")
    PersonResponse get(@Path int personId); // a response with application/xml content-type will be deserialized by jax-b
}
```

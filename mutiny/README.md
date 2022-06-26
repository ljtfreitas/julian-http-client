## mutiny

This module provides support to use [Mutiny](https://smallrye.io/smallrye-mutiny/) classes with `julian-http-client`. Mutiny is a event driven, reactive library for Java.

## Install

### Maven
```xml
<dependency>
    <groupId>com.github.ljtfreitas.julian-http-client</groupId>
    <artifactId>julian-http-client-mutiny</artifactId>
    <version>${julian-http-client-version}</version>
</dependency>
```

### Gradle
```kotlin
dependencies {
    implementation("com.github.ljtfreitas.julian-http-client:julian-http-client-mutiny:$julianHttpClientVersion")
}
```

That's it. So, let's the code.

## Usage

`julian-http-client` supports [Uni](https://smallrye.io/smallrye-mutiny/getting-started/creating-unis) and [Multi](https://smallrye.io/smallrye-mutiny/getting-started/creating-multis) types. 

```java
import com.github.ljtfreitas.julian.contract.Body;
import com.github.ljtfreitas.julian.contract.GET;
import com.github.ljtfreitas.julian.contract.POST;
import com.github.ljtfreitas.julian.contract.Path;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.Multi;

@Path("/person")
interface PersonApi {

    @POST
    Uni<Void> create(@Body("text/plain") String bodyAsText);

    @GET("/{personId}")
    Unit<Person> get(@Path int personId);

    @GET
    Multi<Person> getAllPersons();
}
```

Just be careful to use `Multi` because deserialization process will try to read the response body as a collection of values. So, if we want to get a single value from HTTP response (a single json document, for example), we must to use `Uni`; in case we expect a list of values (a json array, for example), we must to use `Multi`.

Failures can be handled using the regular `Uni` and `Multi` methods.
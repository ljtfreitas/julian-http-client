## reactor

This module provides support to use [Reactor](https://projectreactor.io/) classes with `julian-http-client`. Reactor is a event driven, reactive library for Java.

## Install

### Maven
```xml
<dependency>
    <groupId>com.github.ljtfreitas.julian-http-client</groupId>
    <artifactId>julian-http-client-reactor</artifactId>
    <version>${julian-http-client-version}</version>
</dependency>
```

### Gradle
```kotlin
dependencies {
    implementation("com.github.ljtfreitas.julian-http-client:julian-http-client-reactor:$julianHttpClientVersion")
}
```

That's it. So, let's to the code.

## Usage

`julian-http-client` supports [Mono](https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Mono.html) and [Flux](https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Flux.html) types. 

```java
import com.github.ljtfreitas.julian.contract.Body;
import com.github.ljtfreitas.julian.contract.GET;
import com.github.ljtfreitas.julian.contract.POST;
import com.github.ljtfreitas.julian.contract.Path;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Path("/person")
interface PersonApi {

    @POST
    Mono<Void> create(@Body("text/plain") String bodyAsText);

    @GET("/{personId}")
    Mono<Person> get(@Path int personId);

    @GET
    Flux<Person> getAllPersons();
}
```

Just be careful to use `Flux` because deserialization process will try to read the response body as a collection of values. So, if we want to get a single value from HTTP response (a single json document, for example), we must to use `Mono`; in case we expect a list of values (a json array, for example), we must to use `Flux`.

Failures can be handled using the regular `Mono` and `Flux` methods.
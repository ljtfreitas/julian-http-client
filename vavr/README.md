## vavr

This module provides support to use [Vavr](https://www.vavr.io/) classes with `julian-http-client`. Vavr is a functional library for Java.

## Install

### Maven
```xml
<dependency>
    <groupId>com.github.ljtfreitas.julian-http-client</groupId>
    <artifactId>julian-http-client-vavr</artifactId>
    <version>${julian-http-client-version}</version>
</dependency>
```

### Gradle
```kotlin
dependencies {
    implementation("com.github.ljtfreitas.julian-http-client:julian-http-client-vavr:$julianHttpClientVersion")
}
```

Now, let's to the code.

## Usage

`julian-http-client` supports all main `vavr` types. 

Like `vavr` collections:

```java
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.contract.Callback;
import com.github.ljtfreitas.julian.contract.GET;
import com.github.ljtfreitas.julian.contract.Path;

import io.vavr.collection.Array;
import io.vavr.collection.IndexedSeq;
import io.vavr.collection.LinearSeq;
import io.vavr.collection.List;
import io.vavr.collection.Queue;
import io.vavr.collection.Seq;
import io.vavr.collection.Set;
import io.vavr.collection.Traversable;

@Path("/person")
interface PersonApi {

    @GET
    Array<Person> getAllPersonsAsArray();

    @GET
    IndexedSeq<Person> getAllPersonsAsIndexedSeq();

    @GET
    LinearSeq<Person> getAllPersonsAsLinearSeq();

    @GET
    List<Person> getAllPersonsAsList();

    @GET
    Queue<Person> getAllPersonsAsQueue();

    @GET
    Queue<Person> getAllPersonsAsQueue();

    @GET
    Seq<Person> getAllPersonsAsSeq();

    @GET
    Set<Person> getAllPersonsAsSet();

    @GET
    Traversable<Person> getAllPersonsAsTraversable();

    @GET
    Vector<Person> getAllPersonsAsVector();

    // of course, all these types works as expected in async responses
    @GET
    Promise<List<Person>> getAllPersonsAsAsyncList();

    @GET
    CompletableFuture<List<Person>> getAllPersonsAsAsyncCompletableList();

    // or can be used with @Callback
    @GET
    void getAllPersonsAsListOnCallback(@Callback Consumer<List<Person>>);

}
```

And `vavr` monadic types:

```java
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.contract.Callback;
import com.github.ljtfreitas.julian.contract.GET;
import com.github.ljtfreitas.julian.contract.Path;
import com.github.ljtfreitas.julian.http.HTTPResponseException;

import io.vavr.Lazy;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;

@Path("/person")
interface PersonApi {

    /// Either will block the request until get a result;
    // in case of failure, the exception will be checked against the "left" type argument from Either
    @GET("/{personId}")
    Either<HTTPResponseException, Person> getPersonAsEither(@Path Long personId);

    // Option will block the request too
    @GET("/{personId}")
    Option<Person> getPersonAsOption(@Path Long personId);

    // Try will block the request too, until get a result (success or failure)
    @GET("/{personId}")
    Try<Person> getPersonAsTry(@Path Long personId);

    // of course, in case we don't want to block the request, we can just get this values inside an async wrapper
    @GET("/{personId}")
    Promise<Either<HTTPResponseException, Person>> getPersonAsAsyncEither(@Path Long personId);

    // @Callback works as expected as well
    @GET("/{personId}")
    void getPersonAsEitherOnCallback(@Path Long personId, @Callback Consumer<Either<HTTPResponseException, Person>>);

    // the vavr's Lazy type is supported too
    @GET("/{personId}")
    Lazy<Person> getPersonAsLazy(@Path Long personId);
}
```

`vavr` has a [Future](https://docs.vavr.io/#_future) type to abstract over async computations. It is supported as well:


```java
import com.github.ljtfreitas.julian.contract.GET;
import com.github.ljtfreitas.julian.contract.Path;

import io.vavr.concurrent.Future;

@Path("/person")
interface PersonApi {

    @GET("/{personId}")
    Future<Person> getPersonAsFuture(@Path Long personId);

}
```

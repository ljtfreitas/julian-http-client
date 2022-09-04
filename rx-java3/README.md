## rx-java-3

This module provides support to use [RxJava 3](https://github.com/ReactiveX/RxJava) classes with `julian-http-client`. RxJava is a Java implementation of Reactive Extensions.

## Install

### Maven
```xml
<dependency>
    <groupId>com.github.ljtfreitas.julian-http-client</groupId>
    <artifactId>julian-http-client-rx-java3</artifactId>
    <version>${julian-http-client-version}</version>
</dependency>
```

### Gradle
```kotlin
dependencies {
    implementation("com.github.ljtfreitas.julian-http-client:julian-http-client-rx-java3:$julianHttpClientVersion")
}
```

That's it. So, let's to the code.

## Usage

`julian-http-client` supports [Completable](http://reactivex.io/RxJava/3.x/javadoc/io/reactivex/rxjava3/core/Completable.html), [Flowable](http://reactivex.io/RxJava/3.x/javadoc/io/reactivex/rxjava3/core/Flowable.html), [Maybe](http://reactivex.io/RxJava/3.x/javadoc/io/reactivex/rxjava3/core/Maybe.html), [Observable](http://reactivex.io/RxJava/3.x/javadoc/io/reactivex/rxjava3/core/Observable.html), and [Single](http://reactivex.io/RxJava/3.x/javadoc/io/reactivex/rxjava3/core/Single.html) types. 

```java
import com.github.ljtfreitas.julian.contract.Body;
import com.github.ljtfreitas.julian.contract.GET;
import com.github.ljtfreitas.julian.contract.POST;
import com.github.ljtfreitas.julian.contract.Path;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

@Path("/person")
interface PersonApi {

    @POST
    Completable create(@Body("text/plain") String bodyAsText);

    @GET("/{personId}")
    Single<Person> getAsSingle(@Path int personId);

    @GET("/{personId}")
    Maybe<Person> getAsMaybe(@Path int personId);

    @GET
    Flowable<Person> getAllPersonsAsFlowable();

    @GET
    Observable<Person> getAllPersonsAsObservable();
}
```

Just be careful to use `Flowable` and `Observable` because deserialization process will try to read the response body as a collection of values. So, if we want to get a single value from HTTP response (a single json document, for example), we must to use `Single` or `Maybe`; in case we expect a list of values (a json array, for example), we must to use `Flowable` or `Observable`.

`Completable` is good for cases when we don't need a return value; we just need to know if the operation was completed (success or failure).

Failures can be handled using the regular reactive operators as well.
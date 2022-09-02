## arrow

This module provides support to use [Arrow.kt][https://arrow-kt.io/] core types with `julian-http-client`. Arrow is a functional library for Kotlin.

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

And that's it, all the required things will be registered in a transparent way. Now, let's to the code.

## Usage

`julian-http-client` supports [Either](https://arrow-kt.io/docs/apidocs/arrow-core/arrow.core/-either/), [Option](https://arrow-kt.io/docs/apidocs/arrow-core/arrow.core/-option/), [Eval](https://arrow-kt.io/docs/apidocs/arrow-core/arrow.core/-eval/) and [NonEmptyList](https://arrow-kt.io/docs/apidocs/arrow-core/arrow.core/-non-empty-list/) as return types.

```kotlin
import arrow.core.Either
import arrow.core.Option
import arrow.core.Eval
import arrow.core.NonEmptyList

@Path("/person")
interface PersonApi {

    // Either will block the request until get a result;
    // in case of failure, the exception will be checked against the "left" type argument from Either
    @GET("/{personId}")
    fun getPersonAsEither(@Path personId: Long): Either<HTTPResponseException, Person>

    // Option will block the request too
    @GET("/{personId}")
    fun getPersonAsOption(@Path personId: Long): Option<Person>

    // Eval will defer the HTTP request until you try to get the result
    @GET
    fun getPersonAsEval(@Path personId: Long): Eval<Person>

    // of course, in case we don't want to block the request, we can just get this values inside an async wrapper...
    @GET("/{personId}")
    fun getPersonAsAsyncEither(@Path personId: Long): Promise<Either<HTTPResponseException, Person>> 

    // or just use a suspend function
    @GET("/{personId}")
    suspend fun getPersonAsAsyncEither(@Path personId: Long): Either<HTTPResponseException, Person>>

    // NonEmptyList is supported too, but be careful to use.
    // the deserialization will try to read the HTTP response value as a list of values;
    // in case the list is empty (an empty json array, for example), an exception will be throw because an empty list cannot be converted to a NonEmptyList
    @GET
    fun getAllPersons(): NonEmptyList<Person>

}
```

Another type supported is [Effect](https://arrow-kt.io/docs/next/apidocs/arrow-core/arrow.core.continuations/-effect/). Using `Effect`, the HTTP request will be executed inside a coroutine.

```kotlin
import arrow.core.continuations.Effect

@Path("/person")
interface PersonApi {

    // a function returning an Effect *does not* block;
    // instead, the HTTP request will be executed inside a coroutine and you need to handle the result (success of failure) inside a coroutine too, because all Effect operations are suspendable.
    // in case of failure, the exception will be checked against the short-circuit argument from Either
    @GET("/{personId}")
    fun getPersonAsEffect(@Path personId: Long): Effect<HTTPResponseException, Person>
}
```

Some extension functions are available too:

```kotlin
import com.github.ljtfreitas.julian.Promise
import com.github.ljtfreitas.julian.Attempt

import com.github.ljtfreitas.julian.k.arrow.either

import arrow.core.Either

val promise: Promise<MyExpectedReturnType> = // ...get a HTTP response as a Promise

val promiseAsEither: Either<Throwable, MyExpectedReturnType> = promise.either() // convert a Promise to an Either

val attempt: Attempt<MyExpectedReturnType> = promise.join()

val attemptAsEither: Either<Throwable, MyExpectedReturnType> = attempt.either() // convert an Attempt to an Either
```

```kotlin
import com.github.ljtfreitas.julian.Promise
import com.github.ljtfreitas.julian.Attempt

import com.github.ljtfreitas.julian.k.arrow.fx.effect

import arrow.core.Either
import arrow.core.continuations.Effect

val promise: Promise<MyExpectedReturnType> = // ...get a HTTP response as a Promise

val promiseAsEffect: Effect<Throwable, MyExpectedReturnType> = promise.effect() // convert a Promise to an Either

// Promise.effectAsEither() is a suspend function
val either: Either<Throwable, MyExpectedReturnType> = promise.effectAsEither() // convert a Promise to an Either in a non-blocking manner
```

## Arrow CircuitBreaker

`julian-http-client` provides support to use Arrow's [CircuitBreaker](https://arrow-kt.io/docs/apidocs/arrow-fx-coroutines/arrow.fx.coroutines/-circuit-breaker/), in order to protect HTTP requests.

```kotlin
import com.github.ljtfreitas.julian.k.proxy
import com.github.ljtfreitas.julian.k.http.arrow.CircuitBreakerHTTPRequestInterceptor

import arrow.fx.coroutines.CircuitBreaker

@Path("/person")
interface PersonApi {

    @GET("/{personId}")
    fun getPerson(@Path personId: Long): Person
}

val circuitBreaker = CircuitBreaker.of(maxFailures = 5) // configure circuit breaker...

val personApi = proxy<PersonApi>(endpoint = "http://my.person.api.com") {
    http {
        interceptors {
            add(CircuitBreakerHTTPRequestInterceptor(circuitBreaker))
        }
    }
}

val person = personApi.getPerson(personId = 1) // the HTTP request will be wrapped inside the circuit breaker
```

By default, any non-2xx response will be counted as a failure in the circuit breaker. We can customize that, too:

```kotlin
val circuitBreaker = CircuitBreaker.of(maxFailures = 5) // configure circuit breaker...

val personApi = proxy<PersonApi>(endpoint = "http://my.person.api.com") {
    http {
        interceptors {
            // the last argument on CircuitBreakerHTTPRequestInterceptor constructor is a predicate;
            // we can check if the response will be handled as a success (true) or a failure (false)
            add(CircuitBreakerHTTPRequestInterceptor(circuitBreaker)) { httpResponse ->
                // in this example, just 5xx responses are failures;
                // so 4xx responses will not be counted as failures in the circuit breaker metrics.
                // it's useful in cases we need a fine control over what kind of responses are counted in order to open the circuit
                !httpResponse.status().isServerError
            }
        }
    }
}

val person = personApi.getPerson(personId = 1) // the HTTP request will be wrapped inside the circuit breaker
```

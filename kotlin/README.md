## kotlin

This module provides support for use `julian-http-client` with Kotlin language.

## Install

### Maven
```xml
<dependency>
    <groupId>com.github.ljtfreitas.julian-http-client</groupId>
    <artifactId>julian-http-client-kotlin</artifactId>
    <version>${julian-http-client-version}</version>
</dependency>
```

### Gradle
```kotlin
dependencies {
    implementation("com.github.ljtfreitas.julian-http-client:julian-http-client-kotlin:$julianHttpClientVersion")
}
```

## Usage

### ProxyBuilder

This module adds a new extension method on `ProxyBuilder`, which enables Kotlin-specific additional features:

```kotlin
interface MyApi {}

val myApi = ProxyBuilder()
    .enableKotlinExtensions()
    // other ProxyBuilder stuff...
    .build(MyApi::class.java, "http://my.api.com")
```

`enableKotlinExtensions` adds:

- support to use [kotlinx.serialization with json](#kotlinxserialization-json-support)
- support to [suspend functions/coroutines](#suspend-functions-coroutines)
- support to use [some Kotlin types as function return](#supported-kotlin-types)
- support to use [regular Kotlin functions as callback arguments](#kotlin-functions-as-callbacks)

Other extension functions are available too:

```kotlin
interface MyApi {}

val myApi = ProxyBuilder()
    .enableKotlinExtensions()
    .build<MyApi>("http://my.api.com") // inferred type
```

```kotlin
interface MyApi {}

// instead use ProxyBuilder, we can use the "proxy" function that takes a URL and a more idiomatic Kotlin builder.
// "proxy" function will include Kotlin extensions too
val myApi = proxy<MyApi>("http://my.api.com") {
    
    // the same ProxyBuilder options. for instance, to customize request timeout
    http {
        client {
            configure {
                requestTimeout(1000)
            }
        }
    }
}
```

### kotlinx.serialization-json support

[kotlinx.serialization-json](https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/json.md) support is included by default.

```kotlin
import com.github.ljtfreitas.julian.contract.Body
import com.github.ljtfreitas.julian.contract.GET
import com.github.ljtfreitas.julian.contract.POST
import com.github.ljtfreitas.julian.http.HTTPStatusCode

import kotlinx.serialization.Serializable

@Path("/person")
interface PersonApi {

    // request body will be serialized using kotlinx.serialization-json
    @POST
    fun create(@Body("application/json") person: Person): HTTPStatusCode

    // response body will be deserialized using kotlinx.serialization-json
    @GET("/{personId})
    fun get(@Path("personId") personId: Long): Person
}

@Serializable
data class Person(val name: String, val age: Int)
```

In case we **don't** want to use `kotlinx.serialization` for json content, we need to add any other `julian-http-client` json module (`jackson`, `gson`, etc).

### suspend functions (coroutines)

`julian-http-client` supports Kotlin's suspend functions/coroutines. It just works as expected:

```kotlin
@Path("/person")
interface PersonApi {

    // it will run in a coroutine
    @GET("/{personId}")
    suspend fun get(@Path("personId") personId: Long): Person
}
```

### Supported Kotlin types

A few specific Kotlin types are supported as function return:

```kotlin
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow

@Path("/person")
interface PersonApi {

    // Deferred, Job and Flow values only can be used inside a coroutine.
    // For Sequence and Flow returns, deserialization process will assume that response body is a collection-like value (for example, a json array)

    @GET
    fun getAllAsSequence(): Sequence<Person>

    @GET
    fun getAllAsFlow(): Flow<Person>
    
    @GET("/{personId}")
    fun get(@Path("personId") personId: Long): Deferred<Person>

    @POST
    fun create(@Body("application/json") person: Person): Job
}
```

### Kotlin functions as callbacks

Regular Kotlin function are supported as callback:

```kotlin
@Path("/person")
interface PersonApi {

    // a success callback
    @GET("/{personId}")
    fun get(@Path("personId") personId: Long, @Callback success: (Person) -> Unit): Unit

    // a failure callback
    @GET("/{personId}")
    fun get(@Path("personId") personId: Long, @Callback failure: (Throwable) -> Unit): Unit

    // we can use both...
    @GET("/{personId}")
    fun get(@Path("personId") personId: Long, @Callback success: (Person) -> Unit, @Callback failure: (Throwable) -> Unit): Unit

    // or we can get a Result as callback argument
    @GET("/{personId}")
    fun get(@Path("personId") personId: Long, @Callback result: (Result<Person>) -> Unit): Unit
}
```

## Ktor HTTP client

`julian-http-client` provides support to use [Ktor](https://ktor.io/docs/getting-started-ktor-client.html) as HTTP client. Check out the [docs](../http-client-ktor/README.md)
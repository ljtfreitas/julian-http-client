# julian-http-client (former [java-restify](https://github.com/ljtfreitas/java-restify))

Simple, annotation-based HTTP client for Java, inspired by [Feign](https://github.com/OpenFeign/feign), [Retrofit](https://github.com/square/retrofit), and [RESTEasy](https://docs.jboss.org/resteasy/docs/6.0.1.Final/userguide/html/RESTEasy_Client_Framework.html#proxies) projects.

The main goal is to be a good option to build HTTP requests, using regular Java abstractions in order to model an API on the client-side.

- [Requirements](#requirements)
- [Usage](#usage)
    - [Available annotations](#available-annotations)
    - [Building the client](#building-the-client)
    - [Supported media types](#supported-media-types)
        - [wildcard](#wildcard-default)
        - [application/form-url-encoded](#applicationform-url-encoded)
        - [application/json](#applicationjson)
        - [application/xml](#applicationxml)
        - [application/octet-stream](#applicationoctet-stream)
    - [Supported Java objects](#supported-java-objects)
        - [Additional plugins](#additional-plugins)
    - [HTTP client](#http-client)
        - [HTTP interceptors](#http-request-interceptors)
            - [Authentication](#authentication)
        - [HTTP response failures](#http-response-failures)
        - [Additional HTTP client implementations](#custom-http-client-implementations)
    - [Error handling](#error-handling)
    - [Kotlin support](#kotlin-support)
    - [Additional stuff](#additional-stuff)
        - [Resilience4j](#resilience4j)
        - [OpenTracing](#opentracing)

## Current version

0.0.1-SNAPSHOT

## Requirements

Java >= 11

## Install

> First, to use Maven Central Snapshots repository, add:
>
> ### Maven
> ```xml
> <repositories>
>     <repository>
>         <id>oss.sonatype.org-snapshot</id>
>         <url>https://oss.sonatype.org/content/repositories/snapshots</url>
>         <releases>
>             <enabled>false</enabled>
>         </releases>
>         <snapshots>
>             <enabled>true</enabled>
>         </snapshots>
>     </repository>
> </repositories>
> ```
>
> ### Gradle
> ```kotlin
> repositories {
> 
>     maven {
>         url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
>         mavenContent {
>             snapshotsOnly()
>         }
>     }
> }
> ```

### Maven
```xml
<dependency>
    <groupId>com.github.ljtfreitas.julian-http-client</groupId>
    <artifactId>julian-http-client-core</artifactId>
    <version>${version}</version>
</dependency>
```

### Gradle
```kotlin
dependencies {
    implementation("com.github.ljtfreitas.julian-http-client:julian-http-client-core:$version")
}
```

No additional dependencies will be added to classpath; an implementation principle from `julian-http-client` is to use only classes available on Java's API.

Of course, some additional features require additional libraries (for instance, `jackson` for JSON processing). In these cases, features and extensions are available through optional plugins.

## Usage

`julian-http-client` relies on **interface proxies** and a small collection of **annotations**.

We need a Java interface to act as some kind of abstraction over the API that we want to consume. Then, using annotations, we specify details about the API contract and expected requests/responses.

- [available annotations](#available-annotations)
- [building the client](#building-the-client)
- [supported media types](#supported-media-types)
- [supported Java objects](#supported-java-objects)

### Available annotations

| annotation                                                                  | target                     | details
| --------------------------------------------------------------------------- | -------------------------- | --------------------------- |
| `@Path`                                                                     | interface/method/parameter | [@Path annotation](#path)
| `@GET`, `@POST`, `@PUT`, `@DELETE`, `@PATCH`, `@HEAD`, `@TRACE`, `@OPTIONS` | method                     | [HTTP method annotations](#http-method-annotations)
| `@QueryParameter`                                                           | method/parameter           | [@QueryParameter annotation](#queryparameter)
| `@Header`                                                                   | interface/method/parameter | [@Header annotation](#header)
| `@Cookie`                                                                   | interface/method/parameter | [@Cookie annotation](#cookie)
| `@Body`                                                                     | parameter                  | [@Body annotation](#body)
| `@Callback`                                                                 | parameter                  | [@Callback annotation](#callback)
| shortcut annotations                                                        | interface/method/parameter | [shortcut annotations](#shortcut-annotations)

#### @Path

The `@Path` annotation is used to define the endpoint path. It can be used on interface level to define a common path for all methods, or optionally on the method level.

```java
import com.github.ljtfreitas.julian.contract.Path;
import com.github.ljtfreitas.julian.contract.GET;

@Path("/base/api") // a common path, applied to all methods (optional)
public interface MyApi {

    @Path("/resource")
    @GET                   // a HTTP method annotation is required; see below
    String someResource(); // path will be "/base/api/resource"

}
```

```java
// no common path - it's valid as well
public interface MyApi {

    @Path("/resource")
    @GET
    String someResource(); // path will be just "/resource"

}
```

Paths can have placeholders, dynamically replaced by method parameters. @Path-related arguments **must** be annotated with `@Path` too, and the argument's names should match with the placeholder's names; in case we want to use a different name, we can use the `@Path` annotation to customize it:

```java
import com.github.ljtfreitas.julian.contract.Path;
import com.github.ljtfreitas.julian.contract.GET;

public interface MyApi {

    @Path("/resource/{param}")
    @GET
    String someResource(@Path String param);

    @Path("/resource/{param}")
    @GET
    String someResource(@Path(name = "param") String anotherName);
}
```

> to use parameter names, you must compile your code with the `-parameters` flag.

#### HTTP method annotations

A HTTP method annotation is **required**. Also, we can use this same annotation to define the path:

```java
import com.github.ljtfreitas.julian.contract.GET;
import com.github.ljtfreitas.julian.contract.POST;
import com.github.ljtfreitas.julian.contract.PUT;
import com.github.ljtfreitas.julian.contract.PATCH;
import com.github.ljtfreitas.julian.contract.DELETE;
import com.github.ljtfreitas.julian.contract.OPTIONS;
import com.github.ljtfreitas.julian.contract.HEAD;
import com.github.ljtfreitas.julian.contract.TRACE;

public interface MyApi {

    @GET("/resource") // an alias to "@Path("/resource) @GET". the examples below keep the same idea
    String get();

    @POST("/resource")
    String post();

    @PUT("/resource")
    String put();

    @PATCH("/resource")
    String patch();

    @DELETE("/resource")
    String delete();

    @OPTIONS("/resource")
    String options();

    @HEAD("/resource")
    String head();

    @TRACE("/resource")
    String trace();
}
```

```java
@Path("/base/api")
public interface MyApi {

    @GET("/resource")
    String get(); // same rules about @Path on top of interface - the final path will be "/base/api/resource"
}
```

```java
public interface MyApi {

    @GET("/resource/{param}")       // placeholders can be used, too
    String get(@Path String param); // same rules about arguments annotated with @Path
}
```

#### @QueryParameter

We can pass query parameters in several ways. The simpler one, of course, it's just put on the path:

```java
public interface MyApi {

    @GET("/resource?param=value")
    String get();
}
```

Another option is to use `@QueryParameter` annotation to define static key-value parameters:

```java
public interface MyApi {

    @GET("/resource")
    @QueryParameter(name = "param", value = "value") // the final path is /resource?param=value
    String someResource();

    @GET("/resource")
    @QueryParameter(name = "param1", value = "value1")
    @QueryParameter(name = "param2", value = "value2") // @QueryParameter is repeatable; the final path is /resource?param1=value1&param2=value2
    String someResource();
}
```

```java
@QueryParameter(name = "param", value = "value") // common query parameters can be defined here
public interface MyApi {

    @GET("/resource") // the final path is /resource?param=value (parameters on top are inherited for all methods)
    String someResource();
}
```

Query parameters can be dynamic too, using method arguments:

```java
public interface MyApi {

    @GET("/resource")
    String someResource(@QueryParameter String name); // /resource?name={argument value}

    @GET("/resource")
    String someResource(@QueryParameter("parameter-name") String name); // /resource?parameter-name={argument value}

    @GET("/resource")
    String someResource(@QueryParameter(name = "parameter-name") String name); // /resource?parameter-name={argument value}

    @GET("/resource")
    String someResource(@QueryParameter("param") Collection<String> values); // /resource?param={collection-item-0}&param={collection-item-1}...

    @GET("/resource")
    String someResource(@QueryParameter("param") String[] values); // /resource?param={array-item-0}&param={array-item-1}...

    @GET("/resource")
    String someResource(@QueryParameter Map<String, String> values); // map keys will be used as parameter names

    @GET("/resource")
    String someResource(@QueryParameter com.github.ljtfreitas.julian.QueryParameters values); // QueryParameters is an immutable map-like object
}
```

#### @Header

HTTP headers can be defined using `@Header` annotation:

```java
import com.github.ljtfreitas.julian.contract.GET;
import com.github.ljtfreitas.julian.contract.Header;

public interface MyApi {

    @GET("/resource")
    @Header(name = "x-header", value = "header-value")
    String someResource();

    @GET("/resource")
    @Header(name = "x-header", value = {"header-value", "another-header-value"}) // multiple values are acceptable
    @Header(name = "x-other-header", value = "header-value") // @Header is repeatable  
    String someResource();
}
```

```java
import com.github.ljtfreitas.julian.contract.GET;
import com.github.ljtfreitas.julian.contract.Header;

@Header(name = "x-header", value = "header-value") // common headers can be defined here
public interface MyApi {

    @GET("/resource")
    String someResource(); // x-header will be sent on HTTP request (headers on top are inherited for all methods)
}
```

Headers can be dynamic:

```java
public interface MyApi {

    @GET("/resource")
    String someResource(@Header("x-header") String value);

    @GET("/resource")
    String someResource(@Header(name = "x-header") String value);

    @GET("/resource")
    String someResource(@Header("x-header") Collection<String> values); // all collection values will be sent, in a comma-separared list

    @GET("/resource")
    String someResource(@Header("x-header") String[] values); // all array values will be sent, in a comma-separared list

    @GET("/resource")
    String someResource(@Header Map<String, String> values); // map keys will be used as header names

    @GET("/resource")
    String someResource(@Header com.github.ljtfreitas.julian.Headers headers); // Headers is an immutable collection of Header objects
}
```

#### @Cookie

Cookies can be defined using `@Cookie` annotation (the content will be sent in the `Cookie` header):

```java
import com.github.ljtfreitas.julian.contract.GET;
import com.github.ljtfreitas.julian.contract.Cookie;

public interface MyApi {

    @GET("/resource")
    @Cookie(name = "my-cookie", value = "cookie-value")
    String someResource();

    @GET("/resource")
    @Header(name = "my-cookie", value = {"cookie-value", "another-cookie-value"}) // multiple values are acceptable
    @Header(name = "another-cookie", value = "cookie-value") // @Cookie is repeatable  
    String someResource();
}
```

```java
import com.github.ljtfreitas.julian.contract.GET;
import com.github.ljtfreitas.julian.contract.Cookie;

@Header(name = "my-cookie", value = "cookie-value") // common cookies can be defined here
public interface MyApi {

    @GET("/resource")
    String someResource(); // my-cookie will be sent on Cookie header (cookies on top are inherited for all methods)
}
```

Cookies, of course, can be dynamic as well:

```java
public interface MyApi {

    @GET("/resource")
    String someResource(@Cookie("my-cookie") String value);

    @GET("/resource")
    String someResource(@Cookie(name = "my-cookie") String value);

    @GET("/resource")
    String someResource(@Cookie("my-cookie") Collection<String> values); // all collection values will be sent

    @GET("/resource")
    String someResource(@Cookie("my-cookie") String[] values); // all array values will be sent

    @GET("/resource")
    String someResource(@Cookie Map<String, String> values); // map keys will be used as cookie names

    @GET("/resource")
    String someResource(@Cookie com.github.ljtfreitas.julian.Cookies cookies); // Cookies is an immutable collection of Cookie objects
}
```

#### @Body

We can sent an object to be used as HTTP request body, using the `@Body` on the argument:

```java
import com.github.ljtfreitas.julian.contract.Body;

public interface MyApi {

    @POST("/resource")
    String create(@Body String bodyAsString);
}
```

There are two details to pay attention here: 

- we need to transform the argument value to the *content-type* format which we want to use
- we need to serialize the content to a binary stream. 

These low-level details are all handled by `julian-http-client`; the only requirement is to set a `Content-Type` header.

This header can be defined using a `@Header` annotation or the same `@Body` annotation used on body argument.

```java
public interface MyApi {

    @POST("/resource")
    String create(@Body("text/plain") String bodyAsString); // text/plain will be used as Content-Type

    @POST("/resource")
    @Header(name = "Content-Type", value = "text/plain")   // or explicitly define the header
    String create(@Body String bodyAsString);
}
```

With the `Content-Type` in place, we need an instance of `HTTPRequestWriter` able to convert the argument value to the desired format; check out the docs about [HTTP request body serialization](#supported-media-types).

#### @Callback

In the examples above, we are using the method return to get the HTTP response body; another option is to use a callback style, with the `@Callback` annotation:

```java
import com.github.ljtfreitas.julian.contract.Callback;

public interface MyApi {

    @GET("/resource")
    void get(@Callback Consumer<String> success); // success callback

    @GET("/resource")
    void get(@Callback Consumer<Throwable> failure); // failure callback

    @GET("/resource")
    void get(@Callback Consumer<String> success, @Callback Consumer<Throwable> failure); // success or failure callbacks

    @GET("/resource")
    void get(@Callback BiConsumer<String, Throwable> callback); // success/failure in the same callback
}
```

The argument **must** be a `Consumer`, parameterized with the expected response type (`String`, in the examples above) or a `Throwable` (in this case, it will be a failure callback; check the docs about [error handling](#error-handling)); or a `BiConsumer`, parameterized in the same way (expected response type and a `Throwable`).

#### Shortcut annotations

There are some shortcut annotations to define contract details:

```java
import com.github.ljtfreitas.julian.contract.GET;
import com.github.ljtfreitas.julian.contract.Cookie;

public interface MyApi {

    @GET("/resource")
    @AcceptAll  // alias to @Header(name="Accept", value="*/*")
    String someResource();

    @GET("/resource")
    @AcceptJson // alias to @Header(name="Accept", value="application/json")
    String someResource();

    @GET("/resource")
    @AcceptXml  // alias to @Header(name="Accept", value="application/xml")
    String someResource();

    @POST("/resource")
    @FormUrlEncoded // alias to @Header(name="Content-Type", value="application/x-www-form-urlencoded")
    String someResource(@Body String urlEncodedBody);

    @POST("/resource")
    @JsonContent    // alias to @Header(name = "Content-Type", value="application/json")
    String someResource(@Body String jsonBody);

    @POST("/resource")
    @MultipartFormData  // alias to @Header(name = "Content-Type", value="multipart/form-data")
    String someResource(@Body String multipartBody);

    @POST("/resource")
    @SerializableContent    // alias to @Header(name="Content-Type", value="application/octet-stream")
    String someResource(@Body byte[] binaryBody);

    @POST("/resource")
    @XmlContent // alias to @Header(name = "Content-Type", value="application/xml")
    String someResource(@Body String xmlBody);

    @GET("/resource")
    String someResource(@Authorization String credentials); // alias to @Header(name="Authorization", value={argument value}"); (more details about authentication, see below)

    @GET("/resource")
    String someResource(@ContentType String contentType, @Body String body); // alias to @Header(name="Content-Type", value={argument value}")
}
```

### Building the client

Our next step is to build an instance from our interface. We will do that using the `ProxyBuilder` object:

```java
@Path("/base/api")
public interface MyApi {

    @GET("/resource")
    String get();
}
```

```java
import com.github.ljtfreitas.julian.ProxyBuilder;

MyApi myApi=new ProxyBuilder().build(MyApi.class,"http://my.api.com");
```

The base URL is optional, in case you prefer to define it on the interface:

```java
@Path("http://my.api.com/base/api")
public interface MyApi {

    @GET("/resource")
    String get();
}
```

```java
import com.github.ljtfreitas.julian.ProxyBuilder;

MyApi myApi=new ProxyBuilder().build(MyApi.class);
```

`ProxyBuilder` has several options to customize HTTP client behaviour, add support to additional media types and additional method return types. These options are explained in detail in the rest of this documentation.

### Supported media types

- [wildcard](#wildcard-default)
- [application/form-url-encodec](#applicationform-url-encoded)
- [application/json](#applicationjson)
- [application/xml](#applicationxml)
- [application/octet-stream](#applicationoctet-stream)
- [multipart/form-data](#multipartform-data)

Modern HTTP API's exchange data using a lot of formats, like json, xml, etc. So, we need to be able to serialize/deserialize Java values to/from these formats.

Of course, this is not new; in fact, a lot of libraries do that. `julian-http-client` handles this process in a transparent way using the `Content-Type` header; for request bodies, `Content-Type` is required, in order to say what is the desired format; for response bodies, `julian-http-client` uses the `Content-Type` header (from response) as well, to know what is the source media type and convert the body to the desired Java type.

The main abstraction around this work is `HTTPMessageCodec`. This interface has two specializations, `HTTPRequestWriter` and `HTTPResponseReader`. Implementations of these types need to say what media types they care about and, also, what Java types they are able to write to or read from.

#### wildcard (default)

By default, `julian-http-client` provides a few implementations that work with the "wildcard" media type (`*/*`), the most generic one. That means any content can be read or write using any specific mime-type ("text/plain", "application/json", etc) but, because these implementations are generic and does not run any special handling about the request/response bodies, there is some limits about what Java types we can use.

By default, these types are supported:

```java
public interface MyApi {

    // HTTP request bodies can be sent using these argument types (the Content-Type header is always required):

    @POST("/resource")
    void get(@Body("text/plain") String bodyAsString);

    // using binary types (byte[], InputStream or ByteBuffer) we can send data in any format ("text/plain" is just an example here; in fact, it could be any mime-type)

    @POST("/resource")
    void POST(@Body("text/plain") byte[] bodyAsBytes);

    @POST("/resource")
    void POST(@Body("text/plain") InputStream bodyAsStream);

    @POST("/resource")
    ByteBuffer POST(@Body("text/plain") ByteBuffer bodyAsStream);
}
```

```java
public interface MyApi {

    // HTTP response bodies from any content type can be read using these return types:

    @GET("/resource")
    String get();

    @GET("/resource")
    byte[] get();

    @GET("/resource")
    InputStream get();

    @GET("/resource")
    ByteBuffer get();
}
```

This is nice but not so useful, right? These objects are a bit low level and, except for specific use cases, we do not want to transform a byte stream or a string data in a Java object manually or vice-versa. 

`julian-http-client` is also able to do that (for requests and responses), because we know the source/target content type. With the suitable `HTTPMessageCoded` instance in place, we can read/write any content from/to any Java object that we want to.

#### application/form-url-encoded

`application/x-www-form-urlencoded` content type describes **form data** that is sent in a single block in the HTTP message body. 

Check out the docs: [aplication/form-url-encoded](/form-url-encoded-multipart/README.md)

#### application/json

`application/json` is a very common format in HTTP APIs. `julian-http-client` supports json using different implementations:

- [jackson](/json-jackson/README.md)
- [gson](/json-gson/README.md)
- [json-b](/json-jsonb/README.md)
- [json-p](/json-jsonp/README.md)

#### application/xml

Two implementations are provided to `application/xml`:

- [jackson-xml](/xml-jackson/README.md)
- [jax-b](/xml-jaxb/README.md)

#### application/octet-stream

`application/octet-stream` is the default mime-type for binary content (usually it means an "unknown" content or a binary file). For security reasons, be extremely careful to use it.

This codec is not registered by default. If you want to use it, you need to add explicitly:

```java
import com.github.ljtfreitas.julian.ProxyBuilder;
import com.github.ljtfreitas.julian.http.codec.OctetStreamHTTPMessageCodec;

MyApi myApi = new ProxyBuilder()
    .codecs()
        .add(new OctetStreamHTTPMessageCodec())
    .and()
    .build(MyApi.class);
```

The `OctetStreamHTTPMessageCodec` codec is able to serialize and deserialize any [Serializable](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/io/Serializable.html) value.

#### multipart/form-data

`multipart/form-data` content type is used to upload files; check out the [docs](/form-url-encoded-multipart/README.md).


### Supported Java objects

As we see above, `julian-http-client` attempts to deserialize the HTTP response body to the method return.

```java
interface MyApi {

    @GET
    String get(); // response body will be deserialized to a String
}
```

The same logic is applied for any other return type:

```java
interface MyApi {

    @GET
    MyResponseType get(); // response body will be deserialized to a MyResponseType object
}
```

But maybe we want to do it in another way. Let's suppose we want to get a `CompletableFuture` because we
want to run an async request:

```java
interface MyApi {

    @GET
    CompletableFuture<MyResponseType> get();
}
```

Another case: what if we want to handle an empty response using an `Optional`?

```java
interface MyApi {

    @GET
    Optional<MyResponseType> get();
}
```

Or, maybe we want to get an `Optional` inside an async request?

```java
interface MyApi {

    @GET
    CompletableFuture<Optional<MyResponseType>> get();
}
```

The examples above are all valid and work as expected. The response still will be deserialized to a `MyResponseType` object, despite the method return is a `CompletableFuture` or an `Optional`.

This works because `julian-http-client` handles two concerns here: the **deserialization target** from response and the method return. Sometimes they will be the same, sometimes will not. And `julian-http-client` is able to convert the deserialization target to the method return, using **response transformers**. It's just kinda an adapter between types.

`julian-http-client` registers several transformers, in order to support a big range of method signatures. If none of the registered adapters be able to handle a type, `julian-http-client` is going to use it to deserialize the response; in other words, it will assume the deserialization target and the method return type are the same.

By default these types are supported as method return:

```java
import java.util.Collection;
import java.util.Iterable;
import java.util.Iterator;
import java.util.Optional;
import java.io.InputStream;
import java.nio.ByteBuffer;

interface MyApi {

    // byte array
    @GET("/")
    byte[] byteArray();

    // InputStream
    @GET("/")
    InputStream inputStream();

    // ByteBuffer
    @GET("/")
    ByteBuffer byteBuffer();

    // scalar/primitive types (int, long, etc) - wrapper types are supported too
    // it assumes a text/plain response type
    @GET("/")
    int scalar();

    // String
    @GET("/")
    String string();

    // Collection<T> or subtypes (List<T>, Set<T>, etc)
    // it relies on a HTTPResponseReader able to read the response body as a collection of values
    @GET("/")
    Collection<YourType> collection();

    // Iterable<T> and Iterator<T> are supported too
    // again, it relies that HTTP response body can be read as a collection
    @GET("/")
    Iterable<YourType> iterable();

    @GET("/")
    Iterator<YourType> iterator();

    // Optional can be used to handle an empty response
    Optional<YourType> optional();
    
}
```

`julian-http-client` handles async requests in a transparent way; we just need to get an async value as return type.

These async types from Java's API are supported by default:

```java
import java.lang.Runnable;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

interface MyApi {

    @POST("/")
    Runnable runnable(@Body("text/plain") String bodyAsString);

    @GET("/")
    Callable<YourType> callable();

    @GET("/")
    CompletableFuture<YourType> completableFuture();

    @GET("/")
    Future<YourType> future();

    @GET("/")
    FutureTask<YourType> futureTask();
}
```

Of course, we can mix these signatures to handle different needs. An example, the target endpoint could return a JSON array, that we want get as a `Collection<SomeType>` inside an async result:

```java
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

interface MyApi {

    // it works as you would expect
    @GET("/")
    CompletableFuture<Collection<YourType>> asyncCollection();

}
```

`julian-http-client` provides a few types that can be useful as well:

```java
import com.github.ljtfreitas.julian.Attempt;
import com.github.ljtfreitas.julian.Headers;
import com.github.ljtfreitas.julian.Lazy;
import com.github.ljtfreitas.julian.Promise;
import com.github.ljtfreitas.julian.Response;

interface MyApi {

    // Attempt is a discriminated union that encapsulates a success or a failure (an Exception) value
    @GET("/")
    Attempt<YourType> attempt();

    // Lazy, like the name says, represents a lazy evaluated result (the result is an Attempt value)
    @GET("/")
    Lazy<YourType> lazy();

    // Promise is the main async abstraction from julian-http-client; it's just an wrapper around an async
    // evaluated result (success or failure). The default implementation is backed by a CompletableFuture
    @GET("/")
    Promise<YourType> promise();

    // Headers can be used to get headers response
    @HEAD("/")
    Headers headers();

    // Response is a basic representation from a execution response (handling success outcome or failure)
    @GET("/")
    Response<YourTYpe> response();
}
```

Next example shows some HTTP-specific types:

```java
import com.github.ljtfreitas.julian.http.HTTPHeaders;
import com.github.ljtfreitas.julian.http.HTTPResponse;
import com.github.ljtfreitas.julian.http.HTTPStatus;
import com.github.ljtfreitas.julian.http.HTTPStatusCode;

interface MyApi {

    // HTTPHeaders is a collection from headers; it can be used to get headers response
    @HEAD("/")
    HTTPHeaders headers();

    // HTTPResponse is a full representation from an HTTP response, including body, headers, and status code.
    // This object is able to handle fine-grained control about response details including error handling
    @GET("/")
    HTTPResponse<YourType> httpResponse();

    // the HTTP status from response (code and reason)
    @GET("/")
    HTTPStatus status();

    // the HTTP status code from response
    @GET("/")
    HTTPStatusCode statusCode();

    // again, we can get a method return in the way we want to; for example, we could get a HTTPResponse
    // in an async way 
    @GET("/")
    Promise<HTTPResponse<YourType>> asyncHttpResponse();
}
```

All examples above works as expected with callback responses as well:

```java
import com.github.ljtfreitas.julian.contract.Callback;

public interface MyApi {

    @GET("/resource")
    void get(@Callback Consumer<HTTPResponse<YourType>> success); // Consumer could be parameterized with any supported type
}
```

#### Additional plugins

`julian-http-client` provides additional support to some libraries:

- [Mutiny](./mutiny/README.md)
- [Reactor](./reactor/README.md)
- [RxJava 3](./rx-java3/README.md)
- [Vavr](./vavr/README.md)

### HTTP client

The first important detail to pay attention is **all** requests are async; `julian-http-client` is able to block until the request has been sent and the response has been received when it is required, but all requests will always run in an async, non-blocking way. This is by design.

```java
public interface MyApi {

    @GET("/resource")
    String get(); // blocks until receive the response

    @GET("/resource")
    CompletableFuture<String> get(); // for async types (as CompletableFuture) blocking is not required
}
```

Also, all [response transformations](#supported-java-objects) run in an async way.

The main abstraction around HTTP requests is `HTTP`:

```java
package com.github.ljtfreitas.julian.http.HTTP;

public interface HTTP {

    <T> Promise<HTTPResponse<T>> run(HTTPEndpoint request);

}
```

This object is intended to be some kind of bridge between I/O work and request/response handling; the default implementation uses a second abstraction to run the HTTP request, `HTTPClient`:

```java
package com.github.ljtfreitas.julian.http.client;

public interface HTTPClient {

	HTTPClientRequest request(HTTPRequestDefinition request);

}

public interface HTTPClientRequest {

	Promise<HTTPClientResponse> execute();

}
```

`HTTPClient` and `HTTPClientRequest` are responsible to run a HTTP request and get a HTTP response; the default implementation uses [HttpClient](https://docs.oracle.com/en/java/javase/11/docs/api/java.net.http/java/net/http/HttpClient.html).

Both `HTTP` and `HTTPClient` can be overrided, as well default implementations can be configured (request and response handling and specific HTTP client details):

```java
MyApi myApi = new ProxyBuilder()
    .http()
        .interceptors()
            .add(myHTTPRequestInterceptor) // add request interceptors - see details below
            .and()
        .failure()
            .when(HTTPStatusCode.INTERNAL_SERVER_ERROR, (response, expectedJavaType) -> ...) // custom error handling - see details below
            .and()
        .encoding()
            .with("UTF-8") // charset for requests and responses; UTF-8 is the default value
            .and()
        .client() // HTTP client configurations
            .with(myHTTPClientImplementation) // optionally, we can just override the HTTPClient implementation
            .configure() // specific configurations for the default HTTP client implementation
                .connectionTimeout(/* value in milliseconds */)
                .requestTimeout(/* value in milliseconds */)
                .charset("")
                .proxyAddress(/* an InetSocketAddress object */)
                .redirects()
                    .follow() // follow 302 responses (default)
                    .nofollow() // or, no follow, if you do not want :)
                    .and()
                .ssl()
                    .context(/* sets a SSLContext; more details: https://docs.oracle.com/en/java/javase/11/docs/api/java.net.http/java/net/http/HttpClient.Builder.html#sslContext(javax.net.ssl.SSLContext) */)
                    .parameters(/* sets a SSLParameters; more details: https://docs.oracle.com/en/java/javase/11/docs/api/java.net.http/java/net/http/HttpClient.Builder.html#sslParameters(javax.net.ssl.SSLParameters) */)
                    .and()
                .executor(myThreadPool) // custom Executor object used by HttpClient
            .and()
        .and()
    .async()
        // custom Executor object used by async operations
        .with(myThreadPool)
        .and()
    .build(MyApi.class, "http://my.api.com");
```

#### HTTP request interceptors

We can add `interceptors` in the HTTP request pipeline in order to change/add details in the request. 

We just need to implement a new `HTTPRequestInterceptor` object:

```java
import com.github.ljtfreitas.julian.http.HTTPRequestInterceptor;

public class MyHTTPRequestInterceptor implements HTTPRequestInterceptor {

    @Override
    public <T> Promise<HTTPRequest<T>> intercepts(Promise<HTTPRequest<T>> request) {
        // HTTPRequest is an immutable object. 
        // the "headers" method takes a new Headers object and returns a new HTTPRequest
        return request.then(r -> r.headers(r.headers.join("X-My-Custom-Header", "whatever")));
    }
}
```

The `intercepts` method receives a `Promise<HTTPRequest>` as argument, so the request handling will run in an async way.

Now, we just need to add our interceptor to the proxy:

```java
MyApi myApi = new ProxyBuilder()
    .http()
        .interceptors()
            .add(new MyHTTPRequestInterceptor())
            .and()
        .and()
    .build(MyApi.class, "http://my.api.com");
```

##### Authentication

`julian-http-client` provides a built-in interceptor to add an *Authorization* header:

```java
import com.github.ljtfreitas.julian.http.auth.Authentication;
import com.github.ljtfreitas.julian.http.auth.BasicAuthentication;
import com.github.ljtfreitas.julian.http.auth.HTTPAuthenticationInterceptor;

Authentication basicAuthentication = new BasicAuthentication("user", "password");

MyApi myApi = new ProxyBuilder()
    .http()
        .interceptors()
            .add(new HTTPAuthenticationInterceptor(basicAuthentication))
            .and()
        .and()
    .build(MyApi.class, "http://my.api.com");
```

`HTTPAuthenticationInterceptor` requires an `Authentication` object, and `julian-http-client` provides `BasicAuthentication` and `BearerAuthentication` implementations; in case we want to use any other kind of authorization mechanism, we just need to implement a new `Authentication` object.

#### HTTP response failures

Check out the docs about [error handling](#error-handling).

#### Additional HTTP client implementations

Currently, there is a few additional implementations:

- [Spring WebClient](./http-spring-web-flux/README.md)
- [ReactorNetty](./http-client-reactor-netty/README.md)
- [Vert.x HTTP Client](./http-client-vertx/README.md)

### Error handling

Error handling can be done in different ways. Of course, the simplest one is using a simple `try/catch` block:

```java
import com.github.ljtfreitas.julian.http.HTTPException;
import com.github.ljtfreitas.julian.http.HTTPResponseException;
import com.github.ljtfreitas.julian.http.HTTPClientFailureResponseException;
import com.github.ljtfreitas.julian.http.HTTPServerFailureResponseException;
import com.github.ljtfreitas.julian.http.client.HTTPClientException;
import com.github.ljtfreitas.julian.http.codec.HTTPMessageException;

interface MyApi {

    @GET("/some-path")
    String get();
}


MyApi myApi = new ProxyBuilder().build(MyApi.class, "http://my.api.com");

try {
    String response = myApi.get();

} catch (HTTPClientException e) { // http client/network/IO exceptions
    
    
} catch (HTTPMessageException e) { // serialization/deserialization related exceptions
    /* HTTPMessageException has two sub-exceptions: 
    
       - HTTPRequestWriterException (serialization exceptions)
       - HTTPResponseReaderException (deserialization exceptions)
    */

} catch (HTTPResponseException e) { // failure responses (4xx or 5xx status codes)
    /* HTTPResponseException has two sub-exceptions:

       - HTTPClientFailureResponseException (4xx responses)
       - HTTPClientFailureResponseException (5xx responses)

       Also, HTTPResponseException provides full access to HTTP response.
    */

    HTTPStatus status = e.status();
    HTTPHeaders headers = e.headers();
    String bodyAsString = e.bodyAsString();
    byte[] bodyAsBytes = e.bodyAsBytes();


} catch (HTTPClientFailureResponseException e) { // just 4xx responses (client errors)
    /* HTTPClientFailureResponseException has several sub-exceptions, for each 4xx error.
       for a fine-grained error handling, we can use them instead:

       try {
           // ...
       } catch (HTTPClientFailureResponseException.NotFound e) { // 404 Not Found responses

       } catch (HTTPClientFailureResponseException.BadRequest e) { // 400 Bad Request responses

       } catch (// any other HTTPClientFailureResponseException subtype) {

       }
    */

} catch (HTTPServerFailureResponseException e) { // just 5xx responses (server errors)
    /* HTTPServerFailureResponseException has several sub-exceptions, for each 5xx error.
       for a fine-grained error handling, we can use them instead:

       try {
           // ...
       } catch (HTTPServerFailureResponseException.InternalServerError e) { // 500 Internal Server Error responses

       } catch (HTTPServerFailureResponseException.GatewayTimeout e) { // 504 Gateway Timeout responses

       } catch (// any other HTTPServerFailureResponseException subtype) {

       } 
    */

} catch (HTTPException e) { // the most generic one; it's the parent for all exceptions above

}
```

An importante note about `HTTPResponseException`: HTTP response failures (4xx or 5xx) are not handled as exceptions; instead are just regular responses. `julian-http-client` will throw a HTTPResponseException just in case we try to access the **response body**, because the body just can be deserialized in case of successful responses.

We can use more declarative approaches as well, using different return types:

```java
import com.github.ljtfreitas.julian.http.HTTPResponse;

interface MyApi {

    @GET("/some-path")
    HTTPResponse<String> get();
}

MyApi myApi = new ProxyBuilder().build(MyApi.class, "http://my.api.com");

HTTPResponse<String> response = myApi.get();

// HTTPResponse.recover functions allow us to build a new response value when a failure happens

String result = response.recover(exception -> /* ... */) // takes a function which receives an exception and build a new value 
        
                        .recover(exception -> exception instanceof HTTPClientFailureResponseException.NotFound, exception -> /* ... */) // takes a predicate to check agains an exception and a function to build a new value, in case the predicate matches the exception
        
                        .recover(HTTPClientFailureResponseException.NotFound.class, exception -> /* ... */ ) // takes a Class to check against the exception and a function to build a new value, in case the exception is compatible with the expected Class argument

                        .recover(HTTPStatusCode.NOT_FOUND, (status, headers, bodyAsBytes) -> /* ... */); // takes a HTTPStatusCode to check against the status code from response, and a function to build a new value, in case the failure status code is the same as expected

                        .body() // HTTPResponse.body() returns an Attempt instance (because the expected response body can or cannot be available)
                        
                        .unsafe() // this method is "unsafe" because will get either get the successful value or throws the original exception.
```

`Attempt` is a type which abstracts over a computation that could get a successful value or an exception; we can use `Attempt` as return value and use several methods to mapping over the expected value or recovering from an exception.

```java
import com.github.ljtfreitas.julian.Attempt;

interface MyApi {

    @GET("/some-path")
    Attempt<String> get();
}

MyApi myApi = new ProxyBuilder().build(MyApi.class, "http://my.api.com");

Attempt<String> response = myApi.get();

// Attempt.recover functions allow us to build a new response value when a failure happens
// Attempt.failure functions allow us to map over a failure; it can be useful to transform the original exception in a more domain-specific one

String result = response.failure(exception -> /* ... */) // takes a function which receives an exception and map to another

                        .failure(exception -> exception instanceof HTTPResponseException, exception -> /* ... */) // takes a predicate to check agains an exception and a function to map the failure to a new one, in case the predicate matches the exception

                        .failure(HTTPClientFailureResponseException.NotFound.class, exception -> /* ... */ ) // takes a Class to check against the exception and a function to map the failure to a new one, in case the exception is compatible with the expected Class argument

                        .recover(exception -> /* ... */) // takes a function which receives an exception and build a new value 
        
                        .recover(exception -> exception instanceof HTTPResponseException, exception -> /* ... */) // takes a predicate to check agains an exception and a function to build a new value, in case the predicate matches the exception
        
                        .recover(HTTPClientFailureResponseException.NotFound.class, exception -> /* ... */ ) // takes a Class to check against the exception and a function to build a new value, in case the exception is compatible with the expected Class argument

                        .recover(HTTPStatusCode.NOT_FOUND, (status, headers, bodyAsBytes) -> /* ... */); // takes a HTTPStatusCode to check against the status code from response, and a function to build a new value, in case the failure status code is the same as expected

                        .unsafe() // this method is "unsafe" because will get either get the successful value or throws the original exception.
```

For async requests, `Promise` has recover methods as well:

```java
import com.github.ljtfreitas.julian.http.HTTPResponse;
import com.github.ljtfreitas.julian.http.client.HTTPClientException;

interface MyApi {

    @GET("/some-path")
    Promise<String> get();
}

MyApi myApi = new ProxyBuilder().build(MyApi.class, "http://my.api.com");

Promise<String> response = myApi.get();

String result = response.recover(exception -> /* ... */) // takes a function which receives an exception and build a new value 
        
                        .recover(exception -> exception instanceof HTTPClientException, exception -> /* ... */) // takes a predicate to check agains an exception and a function to build a new value, in case the predicate matches the exception
        
                        .recover(HTTPClientException.class, exception -> /* ... */ ) // takes a Class to check against the exception and a function to build a new value, in case the exception is compatible with the expected Class argument

                        .failure(exception -> /* ... */) // takes a function which receives an exception and map to another; it can be useful to transform the original failure in a more domain-specific one

                        .join() // Promise.join() returns an Attempt instance (because the expected content can or cannot be available); just remember this method will block the async thread!
                        
                        .unsafe(); // this method is "unsafe" because will get either get the successful value or throws the exception.
```

### Kotlin support

`julian-http-client` has dedicated supported for Kotlin language. Check out the [docs](./kotlin/README.md).

### Additional stuff

#### Resilience4j

Check out the [docs](./resilience4j/README.md) about Resilience4j support.

#### OpenTracing

Check out the [docs](./opentracing/README.md) about OpenTracing support.
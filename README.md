# julian-http-client (former [java-restify](https://github.com/ljtfreitas/java-restify))

Simple, annotation-based HTTP client for Java, inspired by Feign, Retrofit, and RESTClient projects.

The main goal of the project is to help you to build HTTP requests using regular Java abstractions, and the same objects
that you would use to model an API on the client-side.

- [requirements](#requirements)
- [usage](#usage)
    - [available annotations](#available-annotations)
    - [using the proxy builder](#using-the-proxy-builder)
    - [about media types](#about-media-types)
    - [about return types](#about-return-types)
    - [HTTP client](#http-client)
    - [error handling](#error-handling)

## current version

0.0.1-SNAPSHOT

## requirements

Java >= 11

## install

> First, to use Maven Central Snapshots repository, add:
>
> ### maven
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
> ### gradle
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

### maven

```xml

<dependency>
    <groupId>com.github.ljtfreitas.julian-http-client</groupId>
    <artifactId>julian-http-client-core</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

### gradle

```kotlin
dependencies {
    implementation("com.github.ljtfreitas.julian-http-client:julian-http-client-core:0.0.1-SNAPSHOT")
}
```

No additional dependencies will be added to classpath; an implementation principle from `julian-http-client` is to use
only classes available on Java's API.

Of course, some additional features require additional libraries (for instance, `jackson` for JSON processing). In these
cases, features and extensions are available through optional plugins.

## usage

`julian-http-client` works based on **interface proxies** and a small collection of **annotations**.

You need just a Java interface to act as the API that we want to consume. Then, using annotations, you specify details
about API contract and HTTP requests.

- [available annotations](#available-annotations)
- [using the proxy builder](#using-the-proxy-builder)
- [about media types](#about-media-types)

### available annotations

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

#### endpoint definition

##### @Path

The `@Path` annotation is used to define the endpoint path. It can be used on interface level to define a common path
for all methods, or optionally on the method level.

```java
import com.github.ljtfreitas.julian.contract.Path;
import com.github.ljtfreitas.julian.contract.GET;

@Path("/base/api") //a common path, applied to all methods
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

Paths can have placeholders, dynamically replaced by method parameters. @Path-related arguments **must** be annotated
with `@Path` too, and the argument's name should match with the placeholder's name; in case you want to use a different
name, just use the `@Path` annotation value (or `name` attribute) on the argument.

```java
import com.github.ljtfreitas.julian.contract.Path;
import com.github.ljtfreitas.julian.contract.GET;

public interface MyApi {

    @Path("/resource/{param}")
    @GET
    String someResource(@Path String param); // param

    @Path("/resource/{param}")
    @GET
    String someResource(@Path(name = "param") String anotherName); // using a different name
}
```

> to use parameter names, you must compile your code with the `-parameters` flag.

#### HTTP method annotations

A HTTP method annotation is **required**. Also, it's possible to use the same annotation to define the path:

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

    @GET("/resource") // an alias to "@Path("/resource) @GET". the examples below follow the same idea
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

##### @QueryParameter

You can pass query parameters in several ways. The simpler one, of course, it's just put on the path:

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

    @GET("/resource") // the final path is /resource?param=value (parameters on top are inherited)
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
    String someResource(@QueryParameter("param") Collection<String> values); // /resource?param={collection-item-1}&param={collection-item-2}...

    @GET("/resource")
    String someResource(@QueryParameter("param") String[] values); // /resource?param={array-item-1}&param={array-item-2}...

    @GET("/resource")
    String someResource(@QueryParameter Map<String, String> values); // map keys will be used as parameter names

    @GET("/resource")
    String someResource(@QueryParameter com.github.ljtfreitas.julian.QueryParameters values); // QueryParameters is an immutable map-like object
}
```

##### @Header

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
    String someResource(); // x-header will be passed on HTTP request (headers on top are inherited)
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
    String someResource(@Header("x-header") Collection<String> values); // all collection values will be passed, in a comma-separared list

    @GET("/resource")
    String someResource(@Header("x-header") String[] values); // all array values will be passed, in a comma-separared list

    @GET("/resource")
    String someResource(@Header Map<String, String> values); // map keys will be used as header names

    @GET("/resource")
    String someResource(@Header com.github.ljtfreitas.julian.Headers headers); // Headers is an immutable collection of header objects
}
```

##### @Cookie

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
    String someResource(); // my-cookie will be passed on Cookie header (cookies on top are inherited)
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
    String someResource(@Cookie("my-cookie") Collection<String> values); // all collection values will be passed

    @GET("/resource")
    String someResource(@Cookie("my-cookie") String[] values); // all array values will be passed

    @GET("/resource")
    String someResource(@Cookie Map<String, String> values); // map keys will be used as cookie names

    @GET("/resource")
    String someResource(@Cookie com.github.ljtfreitas.julian.Cookies cookies); // Cookies is an immutable collection of cookie objects
}
```

##### @Body

You can pass an object to be used as HTTP request body. Just use `@Body` on the method argument:

```java
import com.github.ljtfreitas.julian.contract.POST;
import com.github.ljtfreitas.julian.contract.Cookie;

public interface MyApi {

    @POST("/resource")
    String create(@Body String bodyAsString);
}
```

There are two points to pay attention to: we need to transform the argument value to the format which we want to use as *
content-type*, and we need to serialize this content to a binary stream. These low-level details are handled by `julian-http-client`, but
you *must* set a `Content-Type` header.

```java
import com.github.ljtfreitas.julian.contract.POST;
import com.github.ljtfreitas.julian.contract.Cookie;

public interface MyApi {

    @POST("/resource")
    String create(@Body("text/plain") String bodyAsString); // text/plain will be used as Content-TYpe

    @POST("/resource")
    @Header(name = "Content-Type", value = "text/plain")   // or explicitly define a header
    String create(@Body String bodyAsString);
}
```

With the `Content-Type` in place, we need an instance of `HTTPRequestWriter` able to convert the argument value; check
the docs about [HTTP request body serialization](#about-media-types).

##### @Callback

In the examples above, we are using the method return to get the HTTP response body; another option is to use a callback
style, with the `@Callback` annotation:

```java
import com.github.ljtfreitas.julian.contract.GET;
import com.github.ljtfreitas.julian.contract.Cookie;

public interface MyApi {

    @GET("/resource")
    void create(@Callback Consumer<String> success); // success callback

    @GET("/resource")
    void create(@Callback Consumer<String> success, @Callback Consumer<Throwable> callback); // success or failure callbacks

    @GET("/resource")
    void create(@Callback BiConsumer<String, Throwable> callback); // success or failure in the same callback
}
```

The argument must be a `Consumer`, parameterized with the expected response type (`String`, in the examples above) or
a `Throwable` (which will be a failure callback; check the docs about [error handling](#error-handling)); or a `BiConsumer`, parameterized in the
same way (expected response type and a `Throwable`).

##### shortcut annotations

There are some shortcut annotations to define some contract details:

```java
import com.github.ljtfreitas.julian.contract.GET;
import com.github.ljtfreitas.julian.contract.Cookie;

public interface MyApi {

    @GET("/resource")
    @AcceptAll  // header "Accept: */*"
    String someResource();

    @GET("/resource")
    @AcceptJson // header "Accept: application/json"
    String someResource();

    @GET("/resource")
    @AcceptXml  // header "Accept: application/xml"
    String someResource();

    @POST("/resource")
    @FormUrlEncoded // header "Content-Type: application/x-www-form-urlencoded"
    String someResource(@Body String urlEncodedBody);

    @POST("/resource")
    @JsonContent    // header "Content-Type: application/json"
    String someResource(@Body String jsonBody);

    @POST("/resource")
    @MultipartFormData  // header "Content-Type: multipart/form-data"
    String someResource(@Body String multipartBody);

    @POST("/resource")
    @SerializableContent    // header "Content-Type: application/octet-stream"
    String someResource(@Body byte[] binaryBody);

    @POST("/resource")
    @XmlContent // header "Content-Type: application/xml"
    String someResource(@Body String xmlBody);

    @GET("/resource")
    String someResource(@Authorization String credentials); // header "Authorization: {argument value}" (more details about authentication, see below)

    @GET("/resource")
    String someResource(@ContentType String contentType, @Body String body); // header "Content-Type: {argument value}"
}
```

### using the proxy builder

Our next step is to build an instance from our interface. We can do that with the `ProxyBuilder` object:

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

The base URL is optional, in case you want to define it on the interface:

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

`ProxyBuilder` has several options to customize HTTP client behaviour, add support to additional media types and
additional method return types. These options are explained in detail in the rest of this documentation.

### about media types

- [supported media types](#supported-media-types)
    - [application/json](#applicationjson)
    - [application/xml](#applicationxml)
    - [application/octet-stream](#applicationoctet-stream)

`julian-http-client` enables us to use high-level Java objects as request or response bodies. But modern HTTP API's
exchange data using a lot of formats, like json, xml, etc. So, we need to be able to serialize/deserialize Java values
to/from these formats.

Of course, this is not new; in fact, a lot of libraries do that. `julian-http-client` handles this process in a
transparent way, using the `Content-Type` header; for request bodies, `Content-Type` is required, in order to say what
is the desired format; for response bodies, `julian-http-client` uses the `Content-Type` header as well, to know what is
the source media type, and convert the body to the desired Java type.

The main abstraction around this work is `HTTPMessageCodec`; this interface has two specializations, `HTTPRequestWriter`
and `HTTPResponseReader`. Implementations of these types need to say what media types they care about and, also, what
Java types they are able to write to or read from.

#### supported media types

##### wildcard (default)

By default, `julian-http-client` provides a few implementations that work with the "wildcard" media type (`*/*`). That
means any content can be read or write using any specific mime-type ("text/plain", "application/json", etc), but this
forces a limit about what types we can use to write to or read from a HTTP message body.

By default, these Java types are supported:

```java
public interface MyApi {

    // HTTP request bodies can be sent using these method argument types (the Content-Type header is required):

    @POST("/resource")
    void get(@Body("text/plain") String bodyAsString);

    // using binary types (byte[], InputStream or ByteBuffer) we can send data in any format ("text/plain" is just an example; it could be any mime-type)

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

This is nice but not so useful, right? These objects are a bit low level and, except for specific use cases, we do not
want to transform a byte stream or a string data in a Java object manually or vice-versa. `julian-http-client` is able to do that (for
requests and responses), because we know the source/target content type.

##### application/json

A common format used in HTTP APIs is `application/json`. `julian-http-client` supports json using three different
implementations:

###### jackson

[jackson](https://github.com/FasterXML/jackson) is a well-known Java library to serialize and deserialize json using Java objects.
`julian-http-client`'s support to `jackson` lives in an additional dependency:

###### # maven

```xml

<dependency>
    <groupId>com.github.ljtfreitas.julian-http-client</groupId>
    <artifactId>julian-http-client-json-jackson</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

###### # gradle

```kotlin
dependencies {
    implementation("com.github.ljtfreitas.julian-http-client:julian-http-client-json-jackson:0.0.1-SNAPSHOT")
}
```

And that's it. Now, we can use any Java object that could be handled by `jackson` as a body parameter or method return.

```java
import com.fasterxml.jackson.annotation.JsonProperty;

import com.github.ljtfreitas.julian.contract.Body;
import com.github.ljtfreitas.julian.contract.GET;
import com.github.ljtfreitas.julian.contract.POST;
import com.github.ljtfreitas.julian.contract.Path;

class Person {

    @JsonProperty
    String name;

    @JsonProperty
    int age;
}

class PersonResponse {

    @JsonProperty
    int id;

    @JsonProperty
    String name;

    @JsonProperty
    int age;
}

@Path("/person")
interface PersonApi {

    @POST
    PersonResponse create(@Body("application/json") Person person); // a body with application/json content-type will be serialized by jackson

    @GET("/{personId}")
    PersonResponse get(@Path int personId); // a response with application/json content-type will be deserialized by jackson
}
```

The implementation uses a default `ObjectMapper`; in case you want to customize `jackson` with features/options, you can
build your own `ObjectMapper` and configure your proxy to use it. `ProxyBuilder` has an option to configure custom *codecs*:

```java
import com.github.ljtfreitas.julian.ProxyBuilder;
import com.github.ljtfreitas.julian.http.codec.json.jackson.JacksonJsonHTTPMessageCodec;
import com.fasterxml.jackson.databind.ObjectMapper;

ObjectMapper myObjectMapper= //...

PersonApi personApi = new ProxyBuilder()
    .codecs()
        .add(new JacksonJsonHTTPMessageCodec<Object>(myObjectMapper))
    .and()
    .build(PersonApi.class);
```

###### json-b

[json-b (JSON Binding)](https://javaee.github.io/jsonb-spec/) is a Java spec to convert objects to/from json.

To use it with `julian-http-client`, just add this dependency:

###### # maven

```xml

<dependency>
    <groupId>com.github.ljtfreitas.julian-http-client</groupId>
    <artifactId>julian-http-client-json-jsonb</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

###### # gradle

```kotlin
dependencies {
    implementation("com.github.ljtfreitas.julian-http-client:julian-http-client-json-jsonb:0.0.1-SNAPSHOT")
}
```

And that's it.

```java
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.ljtfreitas.julian.contract.Path;
import com.github.ljtfreitas.julian.contract.GET;
import com.github.ljtfreitas.julian.contract.POST;
import com.github.ljtfreitas.julian.contract.Body;

public class Dog {

    public String name;

    public int age;

    public boolean bitable;
}

class DogResponse {

    public int id;

    public String name;

    public int age;

    public boolean bitable;
}

@Path("/dogs")
interface DogsApi {

    @POST
    DogResponse create(@Body("application/json") Dog dog); // a body with application/json content-type will be serialized by json-b

    @GET("/{dogId}")
    PersonResponse get(@Path int dogId); // a response with application/json content-type will be deserialized by json-b
}
```

The main object from `json-b` API
is [Jsonb](https://javadoc.io/static/javax.json.bind/javax.json.bind-api/1.0/javax/json/bind/Jsonb.html).
`julian-http-client` creates an instance of it using default options, but you can customize the codec with you want:

```java
import com.github.ljtfreitas.julian.ProxyBuilder;
import com.github.ljtfreitas.julian.http.codec.json.jsonb.JsonBHTTPMessageCodec;

import javax.json.bind.JsonBuilder;
import javax.json.bind.Jsonb;

Jsonb myJsonb=JsonBuilder.newBuilder()
        //...
        .build();

PersonApi personApi = new ProxyBuilder()
    .codecs()
        .add(new JsonBHTTPMessageCodec<Object>(myJsonb))
    .and()
    .build(DogsApi.class);
```

###### json-p

[json-p (JSON processing)](https://javaee.github.io/jsonp/) is a Java API to process JSON. json-p does not
serialize/deserialize JSON to/from POJOs like `json-b` or `jackson`; instead, `json-p` has just some high-level objects
to abstract over a JSON structure.

To use it with `julian-http-client`, just add this dependency:

###### # maven

```xml

<dependency>
    <groupId>com.github.ljtfreitas.julian-http-client</groupId>
    <artifactId>julian-http-client-json-jsonp</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

###### # gradle

```kotlin
dependencies {
    implementation("com.github.ljtfreitas.julian-http-client:julian-http-client-json-jsonp:0.0.1-SNAPSHOT")
}
```

With this dependency in place, we can use
a [JsonObject](https://javadoc.io/doc/javax.json/javax.json-api/latest/javax/json/JsonObject.html)
or a [JsonArray](https://javadoc.io/doc/javax.json/javax.json-api/latest/javax/json/JsonArray.html) to write and read
json content.

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

`julian-http-client` uses [JsonWriter](https://javadoc.io/static/javax.json/javax.json-api/1.1.4/javax/json/JsonWriter.html)
and [JsonReader](https://javadoc.io/static/javax.json/javax.json-api/1.1.4/javax/json/JsonReader.html), and these
objects are created using default configurations.

Of course, we can customize both building our
own [JsonWriterFactory](https://javadoc.io/static/javax.json/javax.json-api/1.1.4/javax/json/JsonWriterFactory.html)
and [JsonReaderFactory](https://javadoc.io/static/javax.json/javax.json-api/1.1.4/javax/json/JsonReaderFactory.html):

```java
import com.github.ljtfreitas.julian.ProxyBuilder;
import com.github.ljtfreitas.julian.http.codec.json.jsonp.JsonPHTTPMessageCodec;

import javax.json.Json;
import javax.json.JsonWriterFactory;
import javax.json.JsonReaderFactory;

Map<String, ?> configurations = //...

JsonWriterFactory jsonWriterFactory = Json.createWriterFactory(configurations);
JsonReaderFactory jsonReaderFactory = Json.createWriterFactory(configurations);

PersonApi personApi = new ProxyBuilder()
    .codecs()
        .add(new JsonPHTTPMessageCodec<Object>(jsonReaderFactory,jsonWriterFactory))
    .and()
    .build(DogsApi.class);
```

#### application/xml

`julian-http-client` supports xml using two different implementations:

##### jackson

`julian-http-client` supports `jackson` to serialize/deserialize xml as well (more info about `jackson-xml`
here: <https://github.com/FasterXML/jackson-dataformat-xml>):

###### # maven

```xml

<dependency>
    <groupId>com.github.ljtfreitas.julian-http-client</groupId>
    <artifactId>julian-http-client-xml-jackson</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

###### # gradle

```kotlin
dependencies {
    implementation("com.github.ljtfreitas.julian-http-client:julian-http-client-xml-jackson:0.0.1-SNAPSHOT")
}
```

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

`jackson` provides a `XmlMapper` object to handle xml read/write operations. In order to customize it, configure
a `JacksonXMLHTTPMessageCodec` instance:

```java
import com.github.ljtfreitas.julian.ProxyBuilder;
import com.github.ljtfreitas.julian.http.codec.xml.jackson.JacksonXMLHTTPMessageCodec;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

XmlMapper myXmlMapper = //...

PersonApi personApi = new ProxyBuilder()
    .codecs()
        .add(new JacksonXMLHTTPMessageCodec<Object>(myXmlMapper))
    .and()
    .build(PersonApi.class);
```

##### jax-b

Another option to use `xml` with `julian-http-client` is [jax-b](https://javaee.github.io/jaxb-v2/):

###### # maven

```xml

<dependency>
    <groupId>com.github.ljtfreitas.julian-http-client</groupId>
    <artifactId>julian-http-client-xml-jaxb</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

###### # gradle

```kotlin
dependencies {
    implementation("com.github.ljtfreitas.julian-http-client:julian-http-client-xml-jaxb:0.0.1-SNAPSHOT")
}
```

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

#### application/octet-stream

`application/octet-stream` is the default mime-type for binary content (but usually it means an "unknown" binary file).
Be extremely careful to use it.

This codec is not registered by default. If you want to use it, you need to add explicitly:

```java
import com.github.ljtfreitas.julian.ProxyBuilder;
import com.github.ljtfreitas.julian.http.codec.OctetStreamHTTPMessageCodec;

MyApi myApi = new ProxyBuilder()
    .codecs()
        .add(new OctetStreamHTTPMessageCodec<Serializable>())
    .and()
    .build(MyApi.class);
```

The `OctetStreamHTTPMessageCodec` codec is able to serialize and deserialize
any [Serializable](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/io/Serializable.html) value.

### about return types

As we see above, `julian-http-client` attempts to deserialize the HTTP response body to the method return.

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

Or, if we want to handle an empty response using an `Optional`:

```java
interface MyApi {

    @GET
    Optional<MyResponseType> get();
}
```

Or, maybe we want to get an `Optional` with an async request:

```java
interface MyApi {

    @GET
    CompletableFuture<Optional<MyResponseType>> get();
}
```

The examples above are all valid and work as expected. The response still will be deserialized to a `MyResponseType` object, despite the
method return is a `CompletableFuture` or an `Optional`.

This works because `julian-http-client` handles two concerns here: the **target deserialization** from response and
the method return. Sometimes they will be the same, sometimes will not. And `julian-http-client` is able to convert the
target deserialization object to the method return, using **response transformers**. It's just kinda an adapter between
types.

`julian-http-client` registers several transformers, in order to support a big range of method signatures. If none of
these adapters are able to handle a type, `julian-http-client` is going to use it to deserialize the response.

By default, these types are supported as method return:

- `byte[]`
- scalar/primitive types (`int`, `long`, etc)
- `java.lang.String`
- `java.util.Collection` (including subtypes)
- `java.util.Iterable`
- `java.util.Iterator`
- `java.util.Optional<T>`
- `java.io.InputStream`
- `java.nio.ByteBuffer`
- `java.util.concurrent.Callable<T>`
- `java.util.concurrent.CompletableFuture<T>`
- `java.util.concurrent.Future<T>`
- `java.util.concurrent.FutureTask<T>`
- `java.lang.Runnable`
- `com.github.ljtfreitas.julian.Except<T>`
- `com.github.ljtfreitas.julian.Headers`
- `com.github.ljtfreitas.julian.Lazy<T>`
- `com.github.ljtfreitas.julian.Promise<T>`
- `com.github.ljtfreitas.julian.Response<T>`
- `com.github.ljtfreitas.julian.http.HTTPResponse<T>`
- `com.github.ljtfreitas.julian.http.HTTPHeaders`
- `com.github.ljtfreitas.julian.http.HTTPStatus`
- `com.github.ljtfreitas.julian.http.HTTPStatusCode`

### HTTP client

TODO

### error handling

TODO
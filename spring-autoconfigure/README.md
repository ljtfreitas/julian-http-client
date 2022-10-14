## spring-autoconfigure

A SpringBoot autoconfiguration for `julian-http-client`.

## Install

The recommended way is to install the [Spring starter](../spring-starter/README.md). But the autoconfiguration can be directly imported as well.

It depends of SpringBoot >= 2.7.2.

### Maven
```xml
<dependency>
    <groupId>com.github.ljtfreitas.julian-http-client</groupId>
    <artifactId>julian-http-client-spring-autoconfigure</artifactId>
    <version>${julian-http-client-version}</version>
</dependency>
```

### Gradle
```kotlin
dependencies {
    implementation("com.github.ljtfreitas.julian-http-client:julian-http-client-spring-autoconfigure:$julianHttpClientVersion")
}
```

## Usage

The autoconfiguration provides a new annotation, `@JulianHTTPClient`, to mark interfaces that should be processed. 

These objects will be exposed as regular Spring beans and can be injected in a regular way.

```java
import com.github.ljtfreitas.julian.spring.autoconfigure.JulianHTTPClient;

@JulianHTTPClient(baseURL = "http://my.api.com")
interface MyApi {

   @GET("/my-get-endpoint") 
   String someGet(); 
}

@Component
class MySpringBean {
    
    @Autowired
    MyApi myApi;

}
```

The base URL can be externalized, using Spring facilities (yaml, properties, env vars, etc):

```java
import com.github.ljtfreitas.julian.spring.autoconfigure.JulianHTTPClient;

@JulianHTTPClient(name = "myApi")
interface MyApi {

   @GET("/my-get-endpoint") 
   String someGet(); 
}
```
```properties
# in the properties file:
julian-http-client.myApi.base-url=http://my.api.com
```
```yaml
# or, a yaml file:
julian-http-client:
  myApi:
    base-url: http://my.api.com
```

The `debug mode` can be enabled with properties as well:

```properties
# in the properties file:
julian-http-client.myApi.base-url=http://my.api.com
```

In case we need a fine-grained control over the generated `julian-http-client`, we can customize it with a customizer object:

```
```
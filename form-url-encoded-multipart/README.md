## form-url-encoded-multipart

This module provides support to `application/x-www-form-urlencoded` and `multipart/form-data` media types.

## Install

### Maven
```xml
<dependency>
    <groupId>com.github.ljtfreitas.julian-http-client</groupId>
    <artifactId>julian-http-client-form-url-encoded-multipart</artifactId>
    <version>${julian-http-client-version}</version>
</dependency>
```

### Gradle
```kotlin
dependencies {
    implementation("com.github.ljtfreitas.julian-http-client:julian-http-client-form-url-encoded-multipart:$julianHttpClientVersion")
}
```

Then, the required codecs will be registered automatically. Let's move to code :)

## Usage

### application/x-www-form-urlencoded

`application/x-www-form-urlencoded` can be used to send form data in a request body. This module enables some ways to do that:

```java
import com.github.ljtfreitas.julian.Form;

interface MyApi {

    // @FormUrlEncoded is an alias to @Body("application/x-www-form-urlencoded")
    
    @POST("/resource")
    @FormUrlEncoded
    void sendAsForm(@Body Form form);

    @POST("/resource")
    @FormUrlEncoded
    void sendAsMap(@Body Map<String, String> map);

    @POST("/resource")
    @FormUrlEncoded
    void sendAsMultiMap(@Body Map<String, Collection<String>> map);
}

MyApi myApi = new ProxyBuilder()
        .build(MyApi.class, "http://my.api.com");

// Form is a multi-map like, immutable object
Form form = new Form()
        .join("name", "Tiago de Freitas Lima")
        .join("age", "36")
        .join("pets", "Zulu", "Puka", "Fiona"); // multiple values for the same field are valid

myApi.sendAdForm(form);

// We can use a plain Map too
Map<String, String> map = new HashMap<>();
map.put("name", "Tiago de Freitas Lima");
map.put("age", "36");

myApi.sendAsMap(map);

// Or we can use a map with multiple values
Map<String, Collection<String>> multiMap = new HashMap<>();
multiMap.put("name", "Tiago de Freitas Lima");
multiMap.put("age", "36");
multiMap.put("pets", List.of("Zulu", "Puka, "Fiona""));

myApi.sendAsMultiMap(map);
```

### multipart/form-data

`multipart/form-data` is the media type used to upload files as well other fields as a form.

We can send a multipart form using a `MultipartForm` object or a `Map`:

```java
import com.github.ljtfreitas.julian.multipart.MultipartForm;

import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.file.Paths;

interface MyApi {

    // @MultipartFormData is an alias to @Body("multipart/form-data")

    @POST("/resource")
    @MultipartFormData
    void uploadAsForm(@Body MultipartForm form);

    @POST("/resource")
    @MultipartFormData
    void sendAsMap(@Body Map<String, Object> map);
}

MyApi myApi = new ProxyBuilder()
        .build(MyApi.class, "http://my.api.com");

// MultipartForm is a multi-map like, immutable object
Form form = new MultipartForm()
        .join(MultipartForm.Part.create("name", "Tiago de Freitas Lima"))
        .join(MultipartForm.Part.create("age", "36"))
        .join(MultipartForm.Part.create("picture", new File("/path/to/file.jpg"))) // we can send a java.io.File
        .join(MultipartForm.Part.create("picture", Paths.get("/path/to/file.jpg"), "file.jpg")) // or a java.nio.file.Path
        .join(MultipartForm.Part.create("picture", new FileInputStream("/path/to/file.jpg"), "file.jpg")) // or a java.io.InputStream
        .join(MultipartForm.Part.create("picture", new byte[...], "file.jpg")) // or a byte array with the file content
        .join(MultipartForm.Part.create("picture", ByteBuffer.wrap(new byte[...]), "file.jpg")); // or a java.nio.ByteBuffer with the file content

myApi.sendAdForm(form);

// We can use a plain Map too
Map<String, Object> map = new HashMap<>();
map.put("name", "Tiago de Freitas Lima");
map.put("age", "36");
map.put("picture", new File("/path/to/file.jpg")); // the same types are supported: File, Path, etc...

myApi.sendAsMap(map);
```
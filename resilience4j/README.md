## resilience4j

This module provides support to use [Resilience4j](https://resilience4j.readme.io/) with `julian-http-client`. Resilience4j is a fault tolerance library.

## Install

### Maven
```xml
<dependency>
    <groupId>com.github.ljtfreitas.julian-http-client</groupId>
    <artifactId>julian-http-client-http-client-resilience4j</artifactId>
    <version>${julian-http-client-version}</version>
</dependency>
```

### Gradle
```kotlin
dependencies {
    implementation("com.github.ljtfreitas.julian-http-client:julian-http-client-resilience4j:$julianHttpClientVersion")
}
```

## Usage

`julian-http-client` provides support for several features from Resilience4j, using [interceptors](../README.md#http-request-interceptors).

### Circuit Breaker

`CircuitBreakerHTTPRequestInterceptor` wraps the HTTP request inside a [CircuitBreaker](https://resilience4j.readme.io/docs/circuitbreaker).

```java
import com.github.ljtfreitas.julian.http.resilience4j.CircuitBreakerHTTPRequestInterceptor;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;

interface MyApi {}

CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("my-circuit-breaker");

CircuitBreakerHTTPRequestInterceptor circuitBreakerInterceptor = new CircuitBreakerHTTPRequestInterceptor(circuitBreaker);

MyApi myApi = new ProxyBuilder()
    .http()
        .interceptors()
            .add(circuitBreakerInterceptor)
        .and()
    .and()
    .build(MyApi.class, "http://my.api.com");
```

2xx HTTP responses will be counted as success and 4xx/5xx responses will be counted as errors. We can customize this behavior passing a predicate in order to implement a fine control over what HTTP response codes are counted as an "error":

```java
import com.github.ljtfreitas.julian.http.resilience4j.CircuitBreakerHTTPRequestInterceptor;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;

interface MyApi {}

CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("my-circuit-breaker");

Predicate<HTTPResponse<?>> myPredicate = r -> r.status().isSuccess() || r.status().is(HTTPStatusCode.NOT_FOUND); // 404 (NotFound) responses will not be counted as errors on circuit breaker

CircuitBreakerHTTPRequestInterceptor circuitBreakerInterceptor = new CircuitBreakerHTTPRequestInterceptor(circuitBreaker);

MyApi myApi = new ProxyBuilder()
    .http()
        .interceptors()
            .add(circuitBreakerInterceptor)
        .and()
    .and()
    .build(MyApi.class, "http://my.api.com");
```

With the circuit breaker in place, we can just call our interface method. In case circuit breaker is OPEN, an exception will be throw, so we just need to be careful about error handling:

```java
import com.github.ljtfreitas.julian.Attempt;
import com.github.ljtfreitas.julian.http.resilience4j.CircuitBreakerHTTPRequestInterceptor;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;

interface MyApi {

    @GET("/get-something")
    Attempt<String> get();
}

CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("my-circuit-breaker");

CircuitBreakerHTTPRequestInterceptor circuitBreakerInterceptor = new CircuitBreakerHTTPRequestInterceptor(circuitBreaker);

MyApi myApi = new ProxyBuilder()
    .http()
        .interceptors()
            .add(circuitBreakerInterceptor)
        .and()
    .and()
    .build(MyApi.class, "http://my.api.com");

Attempt<String> result = myApi.get(); // success or failure
```

### Rate limiter

`RateLimiterHTTPRequestInterceptor` wraps the HTTP request inside a [RateLimiter](https://resilience4j.readme.io/docs/ratelimiter).

```java
import com.github.ljtfreitas.julian.http.resilience4j.RateLimiterHTTPRequestInterceptor;
import io.github.resilience4j.ratelimiter.RateLimiter;

interface MyApi {}

RateLimiter rateLimiter = RateLimiter.ofDefaults("my-rate-limiter");

RateLimiterHTTPRequestInterceptor rateLimiterInterceptor = new RateLimiterHTTPRequestInterceptor(rateLimiter);

MyApi myApi = new ProxyBuilder()
    .http()
        .interceptors()
            .add(rateLimiterInterceptor)
        .and()
    .and()
    .build(MyApi.class, "http://my.api.com");
```

In case of `RateLimiter` thows a `RequestNotPermitted` exception, `julian-http-client` will return a 409 (Too Many Requests) HTTP response.

### Retry

`RetryHTTPRequestInterceptor` wraps the HTTP request inside a [Retry](https://resilience4j.readme.io/docs/retry) component.

Because `julian-http-client` requests are async and don't block the main thread, we need to run retries using a [ScheduledExecutorService](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/concurrent/ScheduledExecutorService.html).

```java
import com.github.ljtfreitas.julian.http.resilience4j.RetryHTTPRequestInterceptor;
import io.github.resilience4j.retry.Retry;

interface MyApi {}

ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

Retry retry = Retry.ofDefaults("my-retry");

RetryHTTPRequestInterceptor retryInterceptor = new RetryHTTPRequestInterceptor(retry, scheduler);

MyApi myApi = new ProxyBuilder()
    .http()
        .interceptors()
            .add(retryInterceptor)
        .and()
    .and()
    .build(MyApi.class, "http://my.api.com");
```

By default, just exceptions are retried, and `julian-http-client` doesn't handle HTTP response failures (4xx or 5xx) as exceptions. In case we need to retry this kind of response too, we can configure `Retry`:

```java
import com.github.ljtfreitas.julian.http.resilience4j.RetryHTTPRequestInterceptor;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;

interface MyApi {}

ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

Retry retry = Retry.of("my-retry", RetryConfig.<HTTPResponse<String>> custom()
    .retryOnResult(r -> r.status().is(HTTPStatusGroup.SERVER_ERROR)) // retry for any 5xx (server errors) responses
    .build());

RetryHTTPRequestInterceptor retryInterceptor = new RetryHTTPRequestInterceptor(retry, scheduler);

MyApi myApi = new ProxyBuilder()
    .http()
        .interceptors()
            .add(retryInterceptor)
        .and()
    .and()
    .build(MyApi.class, "http://my.api.com");
```

For exceptions, we can choose what errors should be retried:

```java
import com.github.ljtfreitas.julian.http.resilience4j.RetryHTTPRequestInterceptor;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;

interface MyApi {}

ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

Retry retry = Retry.of("my-retry", RetryConfig.<HTTPResponse<String>> custom()
    .retryOnException(e -> e instanceof HTTPClientException) // just retry HTTPClientException failures 
    .build());

RetryHTTPRequestInterceptor retryInterceptor = new RetryHTTPRequestInterceptor(retry, scheduler);

MyApi myApi = new ProxyBuilder()
    .http()
        .interceptors()
            .add(retryInterceptor)
        .and()
    .and()
    .build(MyApi.class, "http://my.api.com");
```

### TimeLimiter

`TimeLimiterHTTPRequestInterceptor` wraps the HTTP request inside a [TimeLimter](https://resilience4j.readme.io/docs/timeout) component.

Because `julian-http-client` requests are async and don't block the main thread, we need to use a [ScheduledExecutorService](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/concurrent/ScheduledExecutorService.html) in order to schedule a timeout.

```java
import com.github.ljtfreitas.julian.http.resilience4j.TimeLimiterHTTPRequestInterceptor;
import io.github.resilience4j.timelimiter.TimeLimiter;

interface MyApi {}

ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

TimeLimiter timeLimiter = TimeLimiter.of(Duration.ofMillis(2000));

TimeLimiterHTTPRequestInterceptor timeLimiter = new TimeLimiterHTTPRequestInterceptor(timeLimiter, scheduler);

MyApi myApi = new ProxyBuilder()
    .http()
        .interceptors()
            .add(timeLimiter)
        .and()
    .and()
    .build(MyApi.class, "http://my.api.com");
```

In case of `TimeLimiter` timeout is exceeded, a `java.util.concurrent.TimeoutException` will be throw.

package com.github.ljtfreitas.julian.k.http.arrow

import arrow.fx.coroutines.CircuitBreaker
import com.github.ljtfreitas.julian.Attempt
import com.github.ljtfreitas.julian.Promise
import com.github.ljtfreitas.julian.http.HTTPClientFailureResponseException.NotFound
import com.github.ljtfreitas.julian.http.HTTPHeaders
import com.github.ljtfreitas.julian.http.HTTPRequest
import com.github.ljtfreitas.julian.http.HTTPResponse
import com.github.ljtfreitas.julian.http.HTTPServerFailureResponseException.InternalServerError
import com.github.ljtfreitas.julian.http.HTTPStatus
import com.github.ljtfreitas.julian.http.HTTPStatusCode
import io.kotest.assertions.fail
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.throwable.shouldHaveCauseInstanceOf
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class CircuitBreakerHTTPRequestInterceptorTest : DescribeSpec({

    describe("Arrow CircuitBreaker as HTTP request interceptor") {

        describe("HTTP requests should be protected inside a circuit breaker") {

            describe("Closed to Open") {

                val circuitBreaker = CircuitBreaker.of(
                    maxFailures = 1,
                    resetTimeout = 5000.milliseconds
                )

                val interceptor = CircuitBreakerHTTPRequestInterceptor(circuitBreaker)

                it("success requests should work...and the circuit breaker should keep Closed") {
                    val request = mockk<HTTPRequest<String>> {
                        every { execute() } returns Promise.done(
                            HTTPResponse.success(
                                HTTPStatus.valueOf(HTTPStatusCode.OK),
                                HTTPHeaders(),
                            "hello"
                            )
                        )
                    }

                    val protectedResponse = interceptor.intercepts(Promise.done(request))
                        .bind(HTTPRequest<String>::execute)
                        .join()
                        .unsafe()

                    protectedResponse.body().unsafe() shouldBe "hello"

                    circuitBreaker.state().shouldBeInstanceOf<CircuitBreaker.State.Closed>()
                }

                it("a failed request should return an exception and open the circuit breaker (the circuit breaker state should be Open)") {
                    val request = mockk<HTTPRequest<String>> {
                        every { execute() } returns Promise.done(
                            HTTPResponse.failed(
                                InternalServerError(HTTPHeaders(), Promise.done(ByteArray(size = 0)))
                            )
                        )
                    }

                    val protectedResponse = interceptor.intercepts(Promise.done(request))
                        .bind(HTTPRequest<String>::execute)
                        .join()

                    shouldThrow<InternalServerError> { protectedResponse.unsafe().body() }

                    circuitBreaker.state().shouldBeInstanceOf<CircuitBreaker.State.Open>()
                }

                it("...and when the circuit breaker is open, a request should short-circuit with a ExecutionRejection failure") {
                    val request = mockk<HTTPRequest<String>>()

                    val protectedResponse = interceptor.intercepts(Promise.done(request))
                        .bind(HTTPRequest<String>::execute)
                        .onFailure { it.shouldBeInstanceOf<CircuitBreaker.ExecutionRejected>() }
                        .join()

                    val failure = shouldThrow<Attempt.FailureException> { protectedResponse.unsafe().body() }

                    failure.shouldHaveCauseInstanceOf<CircuitBreaker.ExecutionRejected>()
                }

            }

            describe("Open to Closed") {

                val circuitBreaker = CircuitBreaker.of(
                    maxFailures = 1,
                    resetTimeout = 5000.milliseconds
                )

                val interceptor = CircuitBreakerHTTPRequestInterceptor(circuitBreaker)

                it("a failed request should return an exception and open the circuit breaker (the circuit breaker state should be Open)") {
                    val request = mockk<HTTPRequest<String>> {
                        every { execute() } returns Promise.done(
                            HTTPResponse.failed(
                                InternalServerError(HTTPHeaders(), Promise.done(ByteArray(size = 0)))
                            )
                        )
                    }

                    val protectedResponse = interceptor.intercepts(Promise.done(request))
                        .bind(HTTPRequest<String>::execute)
                        .join()

                    shouldThrow<InternalServerError> { protectedResponse.unsafe().body() }

                    circuitBreaker.state().shouldBeInstanceOf<CircuitBreaker.State.Open>()
                }

                it("...while the state is Open, any call should fail") {
                    val request = mockk<HTTPRequest<String>> {
                        every { execute() } returns Promise.done(
                            HTTPResponse.success(
                                HTTPStatus.valueOf(HTTPStatusCode.OK),
                                HTTPHeaders(),
                                "hello"
                            )
                        )
                    }

                    val protectedResponse = interceptor.intercepts(Promise.done(request))
                        .bind(HTTPRequest<String>::execute)
                        .onFailure { it.shouldBeInstanceOf<CircuitBreaker.ExecutionRejected>() }
                        .join()

                    val failure = shouldThrow<Attempt.FailureException> { protectedResponse.unsafe().body() }

                    failure.shouldHaveCauseInstanceOf<CircuitBreaker.ExecutionRejected>()
                }

                it("...and a successful request should change the state to Closed") {
                    delay(timeMillis = 5000)

                    val request = mockk<HTTPRequest<String>> {
                        every { execute() } returns Promise.done(
                            HTTPResponse.success(
                                HTTPStatus.valueOf(HTTPStatusCode.OK),
                                HTTPHeaders(),
                                "hello"
                            )
                        )
                    }

                    val protectedResponse = interceptor.intercepts(Promise.done(request))
                        .bind(HTTPRequest<String>::execute)
                        .join()
                        .unsafe()

                    protectedResponse.body().unsafe() shouldBe "hello"

                    circuitBreaker.state().shouldBeInstanceOf<CircuitBreaker.State.Closed>()
                }
            }

            describe("Failures and exceptions") {

                it("any exception should be handled as a failure") {
                    val circuitBreaker = CircuitBreaker.of(
                        maxFailures = 1,
                        resetTimeout = 1000.milliseconds
                    )

                    val interceptor = CircuitBreakerHTTPRequestInterceptor(circuitBreaker)

                    val failure = RuntimeException("oops")

                    val request = mockk<HTTPRequest<String>> {
                        every { execute() } returns Promise.failed(failure)
                    }

                    val protectedResponse = interceptor.intercepts(Promise.done(request))
                        .bind(HTTPRequest<String>::execute)
                        .join()

                    protectedResponse
                        .onSuccess { fail("a success response was not expected here :(") }
                        .onFailure { e -> e shouldBe failure }

                    circuitBreaker.state().shouldBeInstanceOf<CircuitBreaker.State.Open>()
                }

                it("we can choose which HTTP responses are handled as failure") {
                    val circuitBreaker = CircuitBreaker.of(
                        maxFailures = 1,
                        resetTimeout = 1000.milliseconds
                    )

                    val interceptor = CircuitBreakerHTTPRequestInterceptor(circuitBreaker, predicate = { r -> !r.status().isServerError })

                    val request = mockk<HTTPRequest<String>> {
                        every { execute() } returns Promise.done(
                            HTTPResponse.failed(
                                NotFound(HTTPHeaders(), Promise.done(ByteArray(size = 0)))
                            )
                        )
                    }

                    val protectedResponse = interceptor.intercepts(Promise.done(request))
                        .bind(HTTPRequest<String>::execute)
                        .join()

                    protectedResponse
                        .onSuccess { r -> r.status() shouldBe HTTPStatus(HTTPStatusCode.NOT_FOUND) }
                        .onFailure { fail("a failed response was not expected here :(") }

                    circuitBreaker.state().shouldBeInstanceOf<CircuitBreaker.State.Closed>()
                }
            }
        }
    }
})
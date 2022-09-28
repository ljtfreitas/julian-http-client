package com.github.ljtfreitas.julian.k.arrow

import arrow.core.Either
import com.github.ljtfreitas.julian.Arguments
import com.github.ljtfreitas.julian.Endpoint
import com.github.ljtfreitas.julian.ObjectResponseT
import com.github.ljtfreitas.julian.Promise
import com.github.ljtfreitas.julian.Response
import com.github.ljtfreitas.julian.Subscriber
import com.github.ljtfreitas.julian.http.FailureHTTPResponse
import com.github.ljtfreitas.julian.http.HTTPClientFailureResponseException.BadRequest
import com.github.ljtfreitas.julian.http.HTTPHeader
import com.github.ljtfreitas.julian.http.HTTPHeaders
import com.github.ljtfreitas.julian.http.HTTPResponse
import com.github.ljtfreitas.julian.http.RecoverableHTTPResponse
import com.github.ljtfreitas.julian.http.UnrecoverableHTTPResponseException
import com.github.ljtfreitas.julian.http.codec.HTTPResponseReaders
import com.github.ljtfreitas.julian.http.codec.StringHTTPMessageCodec
import com.github.ljtfreitas.julian.k.javaType
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.fail
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import java.io.IOException

class RecoverableEitherResponseTTest : DescribeSpec({

    val subject = RecoverableEitherResponseT

    val endpoint = mockk<Endpoint>()

    describe("a ResponseT instance for Either<L, R> values (left is a recovered value!)") {

        describe("predicates") {

            it("supports Either<Left, Right> as function return type") {
                every { endpoint.returnType() } returns javaType<Either<String, String>>()

                subject.test(endpoint) shouldBe true
            }

            it("it does not support Throwable (or sub-exceptions) as left argument.") {
                every { endpoint.returnType() } returns javaType<Either<Throwable, String>>()

                subject.test(endpoint) shouldBe false
            }

            it("sub-exceptions does not supported as left argument, too") {
                every { endpoint.returnType() } returns javaType<Either<IOException, String>>()

                subject.test(endpoint) shouldBe false
            }
        }

        describe("adapt to expected type") {

            it("we must to adapt to Right argument from either (Either<Left, Right> -> Right)") {
                every { endpoint.returnType() } returns javaType<Either<Any, String>>()

                subject.adapted(endpoint) shouldBe javaType<String>()
            }
        }

        describe("bind") {

            every { endpoint.returnType() } returns javaType<Either<String, String>>()

            val fn = subject.bind<String>(endpoint = endpoint, next = ObjectResponseT<Any>().bind(endpoint, null))

            it("bind a value T to Either<String, T>") {
                val expected = "hello"

                val either = fn.join(Promise.done(Response.done(expected)), Arguments.empty())

                either shouldBeRight expected
            }

            it("we are able to recover a response failure to Either<String, T>") {
                val expected = "oops"

                val badRequest = BadRequest(
                    HTTPHeaders(listOf(HTTPHeader(HTTPHeader.CONTENT_TYPE, "text/plain"))),
                    Promise.done(expected.toByteArray())
                )

                val failure = FailureHTTPResponse<String>(badRequest)

                val response = Promise.done<HTTPResponse<String>>(
                    RecoverableHTTPResponse(
                        failure,
                        HTTPResponseReaders(listOf(StringHTTPMessageCodec()))
                    )
                )

                val either = fn.join(response, Arguments.empty())

                either shouldBeLeft expected
            }

            it("in case it's impossible to convert the response to the desired recovered value, we get a failed Promise") {
                every { endpoint.returnType() } returns javaType<Either<MyFailure, String>>()

                val badRequest = BadRequest(
                    HTTPHeaders(listOf(HTTPHeader(HTTPHeader.CONTENT_TYPE, "text/plain"))),
                    Promise.done("oops".toByteArray())
                )

                val failure = FailureHTTPResponse<String>(badRequest)

                val codec = StringHTTPMessageCodec()  // isn't able to convert String to MyFailure

                val response = Promise.done<HTTPResponse<String>>(RecoverableHTTPResponse(failure, HTTPResponseReaders(listOf(codec))))

                val promise = fn.run(response, Arguments.empty())

                promise.subscribe(object : Subscriber<Either<Any, Any>, Throwable> {

                    override fun success(value: Either<Any, Any>?) {
                        fail("a success value was not expected here...")
                    }

                    override fun failure(failure: Throwable) {
                        failure.shouldBeInstanceOf<UnrecoverableHTTPResponseException>()
                    }

                })
            }
        }
    }
})

class MyFailure
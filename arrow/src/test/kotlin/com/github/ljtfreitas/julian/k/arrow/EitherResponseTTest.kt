package com.github.ljtfreitas.julian.k.arrow

import arrow.core.Either
import com.github.ljtfreitas.julian.Arguments
import com.github.ljtfreitas.julian.Endpoint
import com.github.ljtfreitas.julian.ObjectResponseT
import com.github.ljtfreitas.julian.Promise
import com.github.ljtfreitas.julian.Response
import com.github.ljtfreitas.julian.Subscriber
import com.github.ljtfreitas.julian.k.javaType
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.fail
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.io.IOException

class EitherResponseTTest : DescribeSpec({

    val subject = EitherResponseT

    val endpoint = mockk<Endpoint>()

    describe("a ResponseT instance for Either<Exception, T> values") {

        describe("predicates") {

            it("supports Either<Exception, T> as function return type") {
                every { endpoint.returnType() } returns javaType<Either<Exception, String>>()

                subject.test(endpoint) shouldBe true
            }

            it("supports just Exception (or sub-exceptions) as left argument.") {
                every { endpoint.returnType() } returns javaType<Either<String, String>>()

                subject.test(endpoint) shouldBe false
            }

            it("supports sub-exceptions as left argument, too") {
                every { endpoint.returnType() } returns javaType<Either<IOException, String>>()

                subject.test(endpoint) shouldBe true
            }

            it("Throwable as left-argument is not supported") {
                every { endpoint.returnType() } returns javaType<Either<Throwable, String>>()

                subject.test(endpoint) shouldBe false
            }
        }

        describe("adapt to expected type") {

            it("we must to adapt to Right argument from either (Either<Left, Right> -> Right)") {
                every { endpoint.returnType() } returns javaType<Either<Exception, String>>()

                subject.adapted(endpoint) shouldBe javaType<String>()
            }
        }

        describe("bind") {

            every { endpoint.returnType() } returns javaType<Either<Exception, String>>()

            val fn = subject.bind<String>(endpoint = endpoint, next = ObjectResponseT<Any>().bind(endpoint, null))

            it("bind a value T to Either<Exception, T>") {
                val expected = "hello"

                val either = fn.join(Promise.done(Response.done(expected)), Arguments.empty())

                either shouldBeRight expected
            }

            it("bind a response failure to Either<Exception, T>") {
                val failure = RuntimeException("oops")

                val either = fn.join(Promise.failed(failure), Arguments.empty())

                either shouldBeLeft failure
            }

            it("bind a response failure to Either<L, T> when the expected left value is compatible") {
                every { endpoint.returnType() } returns javaType<Either<IllegalArgumentException, String>>()

                val failure = IllegalArgumentException("oops")

                val either = fn.join(Promise.failed(failure), Arguments.empty())

                either shouldBeLeft failure
            }

            it("we shouldn't bind a response failure to Either<L, T> when the expected left value isn't compatible with the failure") {
                every { endpoint.returnType() } returns javaType<Either<IllegalArgumentException, String>>()

                val failure = IllegalStateException("another kind of problem...")

                val promise: Promise<Either<Exception, Any>> = fn.run(Promise.failed(failure), Arguments.empty())

                promise.subscribe(object : Subscriber<Either<Exception, Any>> {

                    override fun success(value: Either<Exception, Any>) {
                        fail("it was expected a failure, not a successful value :(")
                    }

                    override fun failure(exception: Exception) {
                        exception shouldBe failure
                    }
                })
            }
        }
    }
})

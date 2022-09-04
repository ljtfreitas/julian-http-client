package com.github.ljtfreitas.julian.k.arrow.fx

import arrow.core.continuations.Effect
import com.github.ljtfreitas.julian.Arguments
import com.github.ljtfreitas.julian.Endpoint
import com.github.ljtfreitas.julian.ObjectResponseT
import com.github.ljtfreitas.julian.Promise
import com.github.ljtfreitas.julian.Response
import com.github.ljtfreitas.julian.k.javaType
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.io.IOException

class EffectResponseTTest : DescribeSpec({

    val subject = EffectResponseT

    val endpoint = mockk<Endpoint>()

    describe("a ResponseT instance for Effect<Exception, T> values") {

        describe("predicates") {

            it("supports Effect<Exception, T> as function return type") {
                every { endpoint.returnType() } returns javaType<Effect<Exception, String>>()

                subject.test(endpoint) shouldBe true
            }

            it("supports just Exception (or sub-exceptions) as left/short-circuit argument.") {
                every { endpoint.returnType() } returns javaType<Effect<String, String>>()

                subject.test(endpoint) shouldBe false
            }

            it("supports sub-exceptions as left/short-circuit argument, too") {
                every { endpoint.returnType() } returns javaType<Effect<IOException, String>>()

                subject.test(endpoint) shouldBe true
            }

            it("Throwable as left-argument is not supported") {
                every { endpoint.returnType() } returns javaType<Effect<Throwable, String>>()

                subject.test(endpoint) shouldBe false
            }
        }

        describe("adapt to expected type") {

            it("we must to adapt to A argument from effect (Effect<R, A> -> A)") {
                every { endpoint.returnType() } returns javaType<Effect<Exception, String>>()

                subject.adapted(endpoint) shouldBe javaType<String>()
            }
        }

        describe("bind") {

            every { endpoint.returnType() } returns javaType<Effect<Exception, String>>()

            val fn = subject.bind<String>(endpoint = endpoint, next = ObjectResponseT<Any>().bind(endpoint, null))

            it("bind a value T to Effect<Exception, T>") {
                val expected = "hello"

                val effect = fn.join(Promise.done(Response.done(expected)), Arguments.empty())

                val either = effect.toEither()

                either shouldBeRight expected
            }

            it("bind a response failure to Effect<Exception, T>") {
                val failure = RuntimeException("oops")

                val effect = fn.join(Promise.failed(failure), Arguments.empty())

                val either = effect.toEither()

                either shouldBeLeft failure
            }

            it("bind a response failure to Effect<R, A> when the expected left/short-circuit (exceptional) value is compatible") {
                every { endpoint.returnType() } returns javaType<Effect<IllegalArgumentException, String>>()

                val failure = IllegalArgumentException("oops")

                val effect = fn.join(Promise.failed(failure), Arguments.empty())

                val either = effect.toEither()

                either shouldBeLeft failure
            }

            it("we shouldn't bind a response failure to Effect<R, A> when the expected left/short-circuit value isn't compatible with the failure") {
                every { endpoint.returnType() } returns javaType<Effect<IllegalArgumentException, String>>()

                val failure = IllegalStateException("another kind of problem...")

                val effect = fn.join(Promise.failed(failure), Arguments.empty())

                val exception = shouldThrowAny { effect.toEither() }

                exception shouldBe failure
            }
        }
    }
})

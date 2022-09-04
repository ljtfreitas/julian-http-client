package com.github.ljtfreitas.julian.k.arrow

import arrow.core.Eval
import com.github.ljtfreitas.julian.Arguments
import com.github.ljtfreitas.julian.Endpoint
import com.github.ljtfreitas.julian.ObjectResponseT
import com.github.ljtfreitas.julian.Promise
import com.github.ljtfreitas.julian.Response
import com.github.ljtfreitas.julian.k.javaType
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.every
import io.mockk.mockk

class EvalResponseTTest : DescribeSpec({

    val subject = EvalResponseT

    val endpoint = mockk<Endpoint>()

    describe("a ResponseT instance for Eval<T> values") {

        describe("predicates") {

            it("supports Eval<T> as function return type") {
                every { endpoint.returnType() } returns javaType<Eval<String>>()

                subject.test(endpoint) shouldBe true
            }

            it("it doesn't support any other return type") {
                every { endpoint.returnType() } returns javaType<String>()

                subject.test(endpoint) shouldBe false
            }
        }

        describe("adapt to expected type") {

            it("we must to adapt to Eval argument (Eval<T> -> T)") {
                every { endpoint.returnType() } returns javaType<Eval<String>>()

                subject.adapted(endpoint) shouldBe javaType<String>()
            }
        }

        describe("bind") {

            every { endpoint.returnType() } returns javaType<Eval<String>>()

            val fn = subject.bind<String>(endpoint = endpoint, next = ObjectResponseT<Any>().bind(endpoint, null))

            it("bind a value T to Eval<T>") {
                val expected = "hello"

                val eval = fn.join(Promise.done(Response.done(expected)), Arguments.empty())

                eval.value() shouldBe expected
            }

            it("bind a failure response...for what?") {
                val failure = RuntimeException("oops")

                val eval = fn.join(Promise.failed(failure), Arguments.empty())

                val exception = shouldThrowAny { eval.value() }

                exception shouldBeSameInstanceAs failure
            }
        }
    }
})

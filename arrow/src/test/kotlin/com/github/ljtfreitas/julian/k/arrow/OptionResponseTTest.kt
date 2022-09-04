package com.github.ljtfreitas.julian.k.arrow

import arrow.core.Option
import com.github.ljtfreitas.julian.Arguments
import com.github.ljtfreitas.julian.Endpoint
import com.github.ljtfreitas.julian.ObjectResponseT
import com.github.ljtfreitas.julian.Promise
import com.github.ljtfreitas.julian.Response
import com.github.ljtfreitas.julian.k.javaType
import io.kotest.assertions.arrow.core.shouldBeNone
import io.kotest.assertions.arrow.core.shouldBeSome
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class OptionResponseTTest : DescribeSpec({

    val subject = OptionResponseT

    val endpoint = mockk<Endpoint>()

    describe("a ResponseT instance for Option<T> values") {

        describe("predicates") {

            it("supports Option<T> as function return type") {
                every { endpoint.returnType() } returns javaType<Option<String>>()

                subject.test(endpoint) shouldBe true
            }

            it("it doesn't support any other return type") {
                every { endpoint.returnType() } returns javaType<String>()

                subject.test(endpoint) shouldBe false
            }
        }

        describe("adapt to expected type") {

            it("we must to adapt to Option argument (Option<T> -> T)") {
                every { endpoint.returnType() } returns javaType<Option<String>>()

                subject.adapted(endpoint) shouldBe javaType<String>()
            }
        }

        describe("bind") {

            every { endpoint.returnType() } returns javaType<Option<String>>()

            val fn = subject.bind<String>(endpoint = endpoint, next = ObjectResponseT<Any>().bind(endpoint, null))

            it("bind a value T to Option<T>") {
                val expected = "hello"

                val option = fn.join(Promise.done(Response.done(expected)), Arguments.empty())

                option shouldBeSome expected
            }

            it("bind a nullable value of type T for a empty Option (a None)") {
                val option = fn.join(Promise.done(Response.empty()), Arguments.empty())

                option.shouldBeNone()
            }
        }
    }
})

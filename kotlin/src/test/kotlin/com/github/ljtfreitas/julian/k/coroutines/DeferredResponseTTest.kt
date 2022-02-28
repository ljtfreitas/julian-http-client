package com.github.ljtfreitas.julian.k.coroutines

import com.github.ljtfreitas.julian.Arguments
import com.github.ljtfreitas.julian.Endpoint
import com.github.ljtfreitas.julian.JavaType
import com.github.ljtfreitas.julian.JavaType.Wildcard
import com.github.ljtfreitas.julian.ObjectResponseT
import com.github.ljtfreitas.julian.Promise
import com.github.ljtfreitas.julian.Response
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Deferred

class DeferredResponseTTest : DescribeSpec({

    val subject = DeferredResponseT

    val endpoint = mockk<Endpoint>()

    describe("a ResponseT instance for Deferred<T> values") {

        describe("predicates") {

            it("a Deferred<T> return is acceptable") {
                every { endpoint.returnType() } returns JavaType.parameterized(Deferred::class.java, Wildcard.lower(String::class.java))

                subject.test(endpoint) shouldBe true
            }

            it("any other return type is unacceptable") {
                every { endpoint.returnType() } returns JavaType.valueOf(String::class.java)

                subject.test(endpoint) shouldBe false
            }
        }

        describe("adapt to expected type") {

            it("adapt to Deferred<T> argument") {
                every { endpoint.returnType() } returns JavaType.parameterized(Deferred::class.java, Wildcard.lower(String::class.java))

                subject.adapted(endpoint) shouldBe JavaType.valueOf(String::class.java)
            }
        }

        describe("bind") {

            it("bind to a successful Deferred<T> value") {
                val fn = subject.bind<Any>(endpoint, fn = ObjectResponseT<Any>().bind(endpoint, null))

                val actual = fn.join(Promise.done(Response.done("hello")), Arguments.empty())

                actual.await() shouldBe "hello"
            }

            it("bind to a failure Deferred<T> value") {
                val fn = subject.bind<Any>(endpoint, fn = ObjectResponseT<Any>().bind(endpoint, null))

                val failure = RuntimeException("oops")

                val actual = fn.join(Promise.failed(failure), Arguments.empty())

                val exception = shouldThrow<RuntimeException> { actual.await() }

                exception shouldBe failure
            }
        }
    }
})



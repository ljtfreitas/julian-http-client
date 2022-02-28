package com.github.ljtfreitas.julian.k.coroutines

import com.github.ljtfreitas.julian.Arguments
import com.github.ljtfreitas.julian.Endpoint
import com.github.ljtfreitas.julian.JavaType
import com.github.ljtfreitas.julian.ObjectResponseT
import com.github.ljtfreitas.julian.Promise
import com.github.ljtfreitas.julian.Response
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Job

class JobResponseTTest : DescribeSpec({

    val subject = JobResponseT

    val endpoint = mockk<Endpoint>()

    describe("a ResponseT instance for Job values") {

        describe("predicates") {

            it("a Job return is acceptable") {
                every { endpoint.returnType() } returns JavaType.valueOf(Job::class.java)

                subject.test(endpoint) shouldBe true
            }

            it("any other return type is unacceptable") {
                every { endpoint.returnType() } returns JavaType.valueOf(String::class.java)

                subject.test(endpoint) shouldBe false
            }
        }

        describe("adapt to expected type") {

            it("Job requests should be adapted to Void") {
                subject.adapted(endpoint) shouldBe JavaType.none()
            }
        }

        it("bind to a Job value") {
            val fn = subject.bind<Unit>(endpoint, fn = ObjectResponseT<Unit>().bind(endpoint, null))

            val actual = fn.join(Promise.done(Response.done(null)), Arguments.empty())

            actual.join() shouldBe Unit
        }

        it("bind a failed Promise to a Job") {
            val fn = subject.bind<Unit>(endpoint, fn = ObjectResponseT<Unit>().bind(endpoint, null))

            val actual = fn.join(Promise.failed(RuntimeException("ue")), Arguments.empty())

            actual.join() shouldBe Unit
        }
    }
})


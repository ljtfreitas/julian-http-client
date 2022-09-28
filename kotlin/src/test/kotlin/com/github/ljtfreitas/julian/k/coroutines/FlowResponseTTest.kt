package com.github.ljtfreitas.julian.k.coroutines

import com.github.ljtfreitas.julian.Arguments
import com.github.ljtfreitas.julian.CollectionResponseT
import com.github.ljtfreitas.julian.Endpoint
import com.github.ljtfreitas.julian.JavaType
import com.github.ljtfreitas.julian.JavaType.Wildcard
import com.github.ljtfreitas.julian.ObjectResponseT
import com.github.ljtfreitas.julian.Promise
import com.github.ljtfreitas.julian.Response
import com.github.ljtfreitas.julian.StreamResponseT
import com.github.ljtfreitas.julian.k.javaType
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import java.util.stream.Stream

class FlowResponseTTest : DescribeSpec({

    val subject = FlowResponseT

    val endpoint = mockk<Endpoint>()

    describe("a ResponseT instance for Flow<T> values") {

        describe("predicates") {

            it("supports Flow<T> as function return type") {
                every { endpoint.returnType() } returns javaType<Flow<String>>()

                subject.test(endpoint) shouldBe true
            }

            it("doesn't support any other return type") {
                every { endpoint.returnType() } returns javaType<String>()

                subject.test(endpoint) shouldBe false
            }
        }

        describe("adapt to expected type") {
            it("adapts a Flow<T> to Stream<T>") {
                every { endpoint.returnType() } returns javaType<Flow<String>>()

                subject.adapted(endpoint) shouldBe JavaType.parameterized(Stream::class.java, String::class.java)
            }
        }

        describe("bind") {

            it("bind a Stream<T> to a Flow<T>") {
                val fn = subject.bind<Collection<Any>>(endpoint, next = StreamResponseT()
                    .bind(endpoint, CollectionResponseT()
                        .bind(endpoint, ObjectResponseT<Collection<Any>>()
                            .bind(endpoint, null))))

                val flow = fn.join(Promise.done(Response.done(listOf("one", "two", "three"))), Arguments.empty())

                flow.toList(mutableListOf()) shouldContainExactly listOf("one", "two", "three")
            }

        }
    }
})

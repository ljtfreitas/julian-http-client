package com.github.ljtfreitas.julian.k

import com.github.ljtfreitas.julian.Arguments
import com.github.ljtfreitas.julian.CollectionResponseT
import com.github.ljtfreitas.julian.Endpoint
import com.github.ljtfreitas.julian.JavaType
import com.github.ljtfreitas.julian.JavaType.Wildcard
import com.github.ljtfreitas.julian.ObjectResponseT
import com.github.ljtfreitas.julian.Promise
import com.github.ljtfreitas.julian.Response
import com.github.ljtfreitas.julian.StreamResponseT
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.sequences.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.util.stream.Stream

class SequenceResponseTTest : DescribeSpec({

    val subject = SequenceResponseT

    val endpoint = mockk<Endpoint>()

    describe("a ResponseT instance for Sequence<T> values") {

        describe("predicates") {
            it("supports Sequence<T> as function return type") {
                every { endpoint.returnType() } returns JavaType.parameterized(
                    Sequence::class.java,
                    Wildcard.lower(String::class.java)
                )

                subject.test(endpoint) shouldBe true
            }

            it("doesn't support any other return type") {
                every { endpoint.returnType() } returns JavaType.valueOf(String::class.java)

                subject.test(endpoint) shouldBe false
            }
        }

        describe("adapt to expected type") {
            it("adapts a Sequence<T> to Stream<T>") {
                every { endpoint.returnType() } returns JavaType.parameterized(
                    Sequence::class.java,
                    Wildcard.lower(String::class.java)
                )

                subject.adapted(endpoint) shouldBe JavaType.parameterized(Stream::class.java, String::class.java)
            }
        }

        describe("bind") {

            it("bind a Stream<T> to a Sequence<T>") {
                val fn = subject.bind<Collection<Any>>(endpoint, fn = StreamResponseT()
                    .bind(endpoint, CollectionResponseT()
                        .bind(endpoint, ObjectResponseT<Collection<Any>>()
                            .bind(endpoint, null))))

                val sequence = fn.join(Promise.done(Response.done(listOf("one", "two", "three"))), Arguments.empty())

                sequence shouldContainExactly sequenceOf("one", "two", "three")
            }

        }
    }
})

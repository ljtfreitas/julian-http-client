package com.github.ljtfreitas.julian.k.arrow

import arrow.core.NonEmptyList
import com.github.ljtfreitas.julian.Arguments
import com.github.ljtfreitas.julian.CollectionResponseT
import com.github.ljtfreitas.julian.Endpoint
import com.github.ljtfreitas.julian.JavaType
import com.github.ljtfreitas.julian.ObjectResponseT
import com.github.ljtfreitas.julian.Promise
import com.github.ljtfreitas.julian.Response
import com.github.ljtfreitas.julian.k.javaType
import io.kotest.assertions.arrow.core.shouldContainAll
import io.kotest.assertions.fail
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.every
import io.mockk.mockk

class NonEmptyListResponseTTest : DescribeSpec({

    val subject = NonEmptyListResponseT()

    val endpoint = mockk<Endpoint>()

    describe("a ResponseT instance for NonEmptyList<T> values") {

        describe("predicates") {
            it("supports NonEmptyList<T> as function return type") {
                every { endpoint.returnType() } returns javaType<NonEmptyList<String>>()

                subject.test(endpoint) shouldBe true
            }

            it("doesn't support any other return type") {
                every { endpoint.returnType() } returns javaType<String>()

                subject.test(endpoint) shouldBe false
            }
        }

        describe("adapt to expected type") {
            it("adapts a NonEmptyList<T> to Collection<T>") {
                every { endpoint.returnType() } returns javaType<NonEmptyList<String>>()

                subject.adapted(endpoint) shouldBe JavaType.parameterized(Collection::class.java, String::class.java)
            }
        }

        describe("bind") {

            every { endpoint.returnType() } returns javaType<Collection<String>>()

            it("bind a Collection<T> to a NonEmptyList<T>") {
                val fn = subject.bind<Collection<Any>>(endpoint, next = CollectionResponseT()
                        .bind(endpoint, ObjectResponseT<Collection<Any>>()
                            .bind(endpoint, null)))

                val nonEmptyList = fn.join(Promise.done(Response.done(listOf("one", "two", "three"))), Arguments.empty())

                nonEmptyList.shouldContainAll("one", "two", "three")
            }

            it("we can't convert a empty collection to a NonEmptyList") {
                val fn = subject.bind<Collection<Any>>(endpoint, next = CollectionResponseT()
                    .bind(endpoint, ObjectResponseT<Collection<Any>>()
                        .bind(endpoint, null)))

                val promise = fn.run(Promise.done(Response.done(emptyList())), Arguments.empty())

                promise.onSuccess { fail("we wasn't expecting a sucess here...:sad:") }
                        .onFailure { it.shouldBeTypeOf<IndexOutOfBoundsException>() }
            }
        }
    }
})

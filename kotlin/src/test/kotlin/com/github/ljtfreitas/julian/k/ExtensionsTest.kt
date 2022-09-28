package com.github.ljtfreitas.julian.k

import com.github.ljtfreitas.julian.Attempt
import com.github.ljtfreitas.julian.JavaType
import com.github.ljtfreitas.julian.Promise
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.result.shouldBeFailure
import io.kotest.matchers.result.shouldBeSuccess
import io.kotest.matchers.shouldBe

class ExtensionsTest : DescribeSpec({

    describe("Kotlin extensions") {

        describe("Promise<T> extensions") {

            describe("composed Promises") {

                it("a composition from two Promises should return a Pair") {
                    val one = Promise.done("one")
                    val two = Promise.done("two")

                    val composed = one + two

                    val pair: Pair<String, String> = composed.join().unsafe()

                    val (a, b) = pair

                    a shouldBe "one"
                    b shouldBe "two"
                }

                it("a composition from three Promises should return a Triple") {
                    val one = Promise.done("one")
                    val two = Promise.done("two")
                    val three = Promise.done("three")

                    val composed = one + two + three

                    val triple: Triple<String, String, String> = composed.join().unsafe()

                    val (a, b, c) = triple

                    a shouldBe "one"
                    b shouldBe "two"
                    c shouldBe "three"
                }

                it("a composition from four Promises should return an Array") {
                    val one = Promise.done("one")
                    val two = Promise.done("two")
                    val three = Promise.done("three")
                    val four = Promise.done("four")

                    val composed = one + two + three + four

                    val array: Array<*> = composed.join().unsafe()

                    val (a, b, c, d) = array

                    a shouldBe "one"
                    b shouldBe "two"
                    c shouldBe "three"
                    d shouldBe "four"
                }

                it("a composition from five (or more) Promises should return an Array as well") {
                    val one = Promise.done("one")
                    val two = Promise.done("two")
                    val three = Promise.done("three")
                    val four = Promise.done("four")
                    val five = Promise.done("five")

                    val composed = one + two + three + four + five

                    val array: Array<*> = composed.join().unsafe()

                    val (a, b, c, d, e) = array

                    a shouldBe "one"
                    b shouldBe "two"
                    c shouldBe "three"
                    d shouldBe "four"
                    e shouldBe "five"
                }

                it("a failed Promise should break composition") {
                    val failure = RuntimeException("i'm failed...")

                    val one = Promise.done("one")
                    val two = Promise.done("two")

                    val three = Promise.failed<String, RuntimeException>(failure)

                    val composed = one + two + three

                    val actual = shouldThrow<RuntimeException> { composed.join().unsafe() }

                    actual shouldBe failure
                }

            }
        }

        describe("Attempt<T> extensions") {

            it("a successful Attempt -> a successful Result") {
                val result = Attempt.success("success").result()
                result shouldBeSuccess "success"
            }

            it("a failed Attempt -> a failed Result") {
                val failure = RuntimeException("oops")
                val result = Attempt.failed<Unit>(failure).result()
                result.shouldBeFailure { it shouldBe failure }
            }
        }

        describe("JavaType extensions") {

            it("building a JavaType using a reified argument") {
                val javaType = javaType<List<String>>()

                javaType shouldBe JavaType.parameterized(List::class.java, String::class.java)
            }
        }
    }
})
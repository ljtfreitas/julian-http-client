package com.github.ljtfreitas.julian.k

import com.github.ljtfreitas.julian.Except
import com.github.ljtfreitas.julian.Promise
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.result.shouldBeFailure
import io.kotest.matchers.result.shouldBeSuccess
import io.kotest.matchers.shouldBe

class ExtensionsTest : DescribeSpec({

    describe("Kotlin extensions") {

        describe("Promise<T> extensions") {

            describe("Promise<T> to Result<T>") {

                it("a successful Promise -> a successful Result") {
                    val result = Promise.done("success").result()
                    result shouldBeSuccess "success"
                }

                it("a failed Promise -> a failed Result") {
                    val failure = RuntimeException("oops")
                    val result = Promise.failed<Unit, RuntimeException>(failure).result()
                    result.shouldBeFailure { it shouldBe failure }
                }
            }

            describe("composed Promises") {

                it("composed, dependent successful Promises") {
                    val one = Promise.done("one")
                    val two = Promise.done("two")
                    val three = Promise.done("three")

                    val composed = one + two + three

                    val (a, b, c) = composed.join().unsafe()

                    a shouldBe "one"
                    b shouldBe "two"
                    c shouldBe "three"
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

        describe("Except<T> extensions") {

            it("a successful Except -> a successful Except") {
                val result = Except.success("success").result()
                result shouldBeSuccess "success"
            }

            it("a failed Promise -> a failed Result") {
                val failure = RuntimeException("oops")
                val result = Except.failed<Unit>(failure).result()
                result.shouldBeFailure { it shouldBe failure }
            }
        }
    }
})
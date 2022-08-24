package com.github.ljtfreitas.julian.k.arrow

import com.github.ljtfreitas.julian.Attempt
import com.github.ljtfreitas.julian.JavaType
import com.github.ljtfreitas.julian.Promise
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.result.shouldBeFailure
import io.kotest.matchers.result.shouldBeSuccess
import io.kotest.matchers.shouldBe

class ExtensionsTest : DescribeSpec({

    describe("Arrow extensions") {

        describe("Promise<T> extensions") {

            it("a successful Promise -> a right Either") {
                val result = Promise.done("success").either()
                result shouldBeRight "success"
            }

            it("a failed Promise -> a left Either") {
                val failure = RuntimeException("oops")
                val result = Promise.failed<Unit, RuntimeException>(failure).either()
                result shouldBeLeft failure
            }

        }

        describe("Attempt<T> extensions") {

            it("a successful Attempt -> a right Either") {
                val result = Attempt.success("success").either()
                result shouldBeRight "success"
            }

            it("a failed Attempt -> a left Either") {
                val failure = RuntimeException("oops")
                val result = Attempt.failed<Unit>(failure).either()
                result shouldBeLeft failure
            }
        }
    }
})
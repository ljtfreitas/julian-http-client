package com.github.ljtfreitas.julian.k.coroutines

import com.github.ljtfreitas.julian.Promise
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.result.shouldBeFailure
import io.kotest.matchers.result.shouldBeSuccess
import io.kotest.matchers.shouldBe
import java.util.concurrent.CompletableFuture

class CoroutinesExtensionsTest : DescribeSpec({

    describe("Coroutines extensions") {

        it("Promise<T> -> Deferred<T>") {
            val deferred = Promise.pending(CompletableFuture.supplyAsync { "hello" }).deferred()

            deferred.await() shouldBe "hello"
        }

        it("a awaitable Promise<T>") {
            val promise = Promise.pending(CompletableFuture.supplyAsync { "hello" })

            promise.await() shouldBe "hello"
        }

        it("a failed Promise<T> should throw the cause exception when we try to await...") {
            val cause = RuntimeException("oops")

            val promise = Promise.pending(CompletableFuture.failedFuture<Unit>(cause))

            val exception = shouldThrow<RuntimeException> { promise.await() }

            exception shouldBe cause
        }

        describe("we can run a Promise<T> inside a coroutine") {

            it("a successful Promise<T>") {
                val promise = promise { "hello" }

                promise.join().unsafe() shouldBe "hello"
            }

            it("a failed Promise<T>") {
                val failure = RuntimeException("oops")

                val promise = promise { throw failure }

                val exception = shouldThrow<RuntimeException> { promise.join().unsafe() }

                exception shouldBe failure
            }
        }

        describe("Promise<T> to Result<T>, non-blocking") {

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

        describe("Promise<T> to Attempt<T>, non-blocking") {

            it("a successful Promise -> a successful Attempt") {
                val attempt = Promise.done("success").attempt()
                attempt.unsafe() shouldBe "success"
            }

            it("a failed Promise -> a failed Attempt") {
                val failure = RuntimeException("oops")
                val attempt = Promise.failed<Unit, RuntimeException>(failure).attempt()
                val exception = shouldThrow<RuntimeException> { attempt.unsafe() }
                exception shouldBe failure
            }
        }
    }
})
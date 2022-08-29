package com.github.ljtfreitas.julian.k.coroutines

import com.github.ljtfreitas.julian.Promise
import io.kotest.assertions.failure
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
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
    }
})
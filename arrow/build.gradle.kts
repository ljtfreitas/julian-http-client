import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/*
 * Copyright (C) 2021 Tiago de Freitas Lima
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

plugins {
    modules
    kotlin("jvm") version "1.6.10"
}

description = "Arrow.kt extensions for julian-http-client"

tasks.jar.configure {
    archiveBaseName.set("julian-http-client-arrow")
}

val kotlinCompileAttributes: KotlinCompile.() -> Unit = {
    sourceCompatibility = "11"
    targetCompatibility = "11"

    kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
}

tasks.compileKotlin.configure(kotlinCompileAttributes)
tasks.compileTestKotlin.configure(kotlinCompileAttributes)

dependencies {
    implementation(project(":core"))
    implementation(project(":kotlin"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.6.4")
    api("io.arrow-kt:arrow-core-jvm:1.1.2")
    api("io.arrow-kt:arrow-fx-coroutines:1.1.2")
    testImplementation("io.kotest:kotest-runner-junit5:5.4.2")
    testImplementation("io.kotest:kotest-assertions-core-jvm:5.4.2")
    testImplementation("io.kotest.extensions:kotest-assertions-arrow:1.2.5")
    testImplementation("io.mockk:mockk:1.12.7")
}


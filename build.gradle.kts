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
    // Apply the java-library plugin to add support for Java Library
    `java-library`
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {

    // Use JUnit Jupiter API for testing.
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.7.1")
    testImplementation("org.mockito:mockito-core:3.8.0")
    testImplementation("org.mockito:mockito-junit-jupiter:3.8.0")
    testImplementation("org.mockito:mockito-inline:3.8.0")
    testImplementation("org.mock-server:mockserver-junit-jupiter:5.11.1")
    testImplementation("ch.qos.logback:logback-classic:1.2.3")
    testImplementation("org.hamcrest:hamcrest:2.2")

    // Use JUnit Jupiter Engine for testing.
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.0")
}

val test by tasks.getting(Test::class) {
    useJUnitPlatform()
}

tasks.compileJava.configure {
    options.compilerArgs.add("-parameters")
}

tasks.compileTestJava.configure {
    options.compilerArgs.add("-parameters")
}
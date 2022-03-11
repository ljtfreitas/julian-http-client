import net.researchgate.release.GitAdapter
import net.researchgate.release.GitAdapter.GitConfig
import net.researchgate.release.ReleaseExtension
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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
    `java-library`
    `maven-publish`
    signing
    id("net.researchgate.release")
}

java {
    modularity.inferModulePath.set(true)
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    // Use JUnit Jupiter API for testing.
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.7.0")
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

    testLogging.showStandardStreams = true
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.jar {
    archiveBaseName.set("julian-http-client-${project.name}")

    manifest {
        val systemProperties = System.getProperties()

        val manifest = File("${project.projectDir}/src/main/resources/META-INF/MANIFEST.MF")

        if (manifest.exists()) {
            from(manifest.toString())
        }

        attributes(
            "Build-By" to systemProperties["user.name"],
            "Build-JDK" to "${systemProperties["java.version"]} (${systemProperties["java.version"]} ${systemProperties["java.vm.version"]})",
            "Build-OS" to "${systemProperties["os.name"]} ${systemProperties["os.arch"]} ${systemProperties["os.version"]}",
            "Build-Timestamp" to LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            "Implementation-Title" to project.name,
            "Implementation-Group" to project.group,
            "Implementation-Version" to project.version,
            "Created-By" to "Gradle ${gradle.gradleVersion}"
        )
    }
}

//java {
//    withJavadocJar()
//    withSourcesJar()
//}

tasks {
    val sourcesJar by creating(Jar::class) {
        dependsOn(JavaPlugin.CLASSES_TASK_NAME)
        archiveClassifier.set("sources")
        archiveBaseName.set("julian-http-client-${project.name}")
        from(sourceSets["main"].allSource)
    }

    val javadocJar by creating(Jar::class) {
        dependsOn(JavaPlugin.JAVADOC_TASK_NAME)
        archiveClassifier.set("javadoc")
        archiveBaseName.set("julian-http-client-${project.name}")
        from(tasks["javadoc"])
    }

    artifacts {
        add("archives", sourcesJar)
        add("archives", javadocJar)

    }
}

tasks.javadoc { (options as StandardJavadocDocletOptions).addBooleanOption("html5", true) }

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = tasks.jar.get().archiveBaseName.get()
            from(components["java"])

            pom {
                name.set(artifactId)
                url.set("https://github.com/ljtfreitas/julian-http-client")
                packaging = "jar"

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("ljtfreitas")
                        name.set("Tiago de Freitas Lima")
                        email.set("ljtfreitas@gmail.com")
                    }
                }
                issueManagement {
                    system.set("GitHub issues")
                    url.set("https://github.com/ljtfreitas/julian-http-client/issues")
                }
                scm {
                    connection.set("scm:git:git@github.com:ljtfreitas/julian-http-client.git")
                    developerConnection.set("scm:git:git@github.com:ljtfreitas/julian-http-client.git")
                    url.set("https://github.com/ljtfreitas/julian-http-client")
                    tag.set("HEAD")
                }
            }
        }
    }
    repositories {
        maven {
            name = "maven-central-nexus"

            val releasesRepoUrl = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotsRepoUrl = uri("https://oss.sonatype.org/content/repositories/snapshots")
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl

            credentials {
                val nexusUserName: String? by project
                val nexusPassword: String? by project

                username = nexusUserName
                password = nexusPassword
            }
        }
    }
}

signing {
    sign(publishing.publications["maven"])
}

release {
    git {
        requireBranch = "main"
    }
}

fun ReleaseExtension.git(configure: GitConfig.() -> Unit) = (getProperty("git") as GitConfig).configure()
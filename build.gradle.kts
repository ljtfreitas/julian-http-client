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

    // Use JUnit Jupiter Engine for testing.
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.0")
}

val test by tasks.getting(Test::class) {
    useJUnitPlatform()
}

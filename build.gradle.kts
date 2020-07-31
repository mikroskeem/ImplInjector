plugins {
    `java-library`
    id("net.minecrell.licenser") version "0.4.1"
    `maven-publish`
}

group = "eu.mikroskeem"
version = "0.0.3-SNAPSHOT"

repositories {
    mavenCentral()
}

val checkerQualVersion = "3.1.1"
val junitVersion = "5.5.2"

dependencies {
    compileOnly("org.checkerframework:checker-qual:$checkerQualVersion")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

val test by tasks.getting(Test::class) {
    useJUnitPlatform()
}

license {
    header = rootProject.file("etc/HEADER")
    filter.include("**/*.java")
}

val sourcesJar by tasks.creating(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets["main"].allJava)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "eu.mikroskeem"
            artifactId = "implinjector"
            from(components["java"])
            artifact(sourcesJar)
        }
    }
    repositories {
        mavenLocal()
        if (rootProject.hasProperty("wutee.repository.deploy.username") && rootProject.hasProperty("wutee.repository.deploy.password")) {
            maven("https://repo.wut.ee/repository/mikroskeem-repo") {
                credentials {
                    username = rootProject.properties["wutee.repository.deploy.username"]!! as String
                    password = rootProject.properties["wutee.repository.deploy.password"]!! as String
                }
            }
        }
    }
}
plugins {
    id("io.micronaut.build.internal.security-module")
    id("io.micronaut.build.internal.aot-module")
}

micronautBuild {
    aot {
        version.set("2.0.0-M3")
    }
}

dependencies {
    annotationProcessor(platform(mn.micronaut.core.bom))
    compileOnly(platform(mn.micronaut.core.bom))
    implementation(projects.micronautSecurityOauth2)
    implementation(projects.micronautSecurityJwt)
    testImplementation(platform(mn.micronaut.core.bom))
    testImplementation(mn.micronaut.http.server.netty)
    testImplementation(mnTest.micronaut.test.spock)
}

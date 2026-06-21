package com.garun

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*

fun Application.module() {
    val adminUsername = System.getenv("ADMIN_USERNAME")
        ?: environment.config.propertyOrNull("admin.username")?.getString()
        ?: "admin"
    val adminPassword = System.getenv("ADMIN_PASSWORD")
        ?: environment.config.propertyOrNull("admin.password")?.getString()
        ?: "changeme"
    val corsHost = System.getenv("CORS_HOST")
        ?: environment.config.propertyOrNull("cors.host")?.getString()
        ?: "microtize.ru"

    install(ContentNegotiation) {
        json()
    }

    install(CORS) {
        allowHost(corsHost, schemes = listOf("https", "http"))
        allowHost("www.$corsHost", schemes = listOf("https", "http"))
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
    }

    install(Authentication) {
        basic("admin-auth") {
            realm = "Admin Panel"
            validate { credentials ->
                if (credentials.name == adminUsername && credentials.password == adminPassword) {
                    UserIdPrincipal(credentials.name)
                } else null
            }
        }
    }

    DatabaseFactory.init()
    configureRouting()
}

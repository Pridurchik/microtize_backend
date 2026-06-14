package com.garun

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.contentnegotiation.*

fun Application.module() {
    val adminUsername = environment.config.property("admin.username").getString()
    val adminPassword = environment.config.property("admin.password").getString()

    install(ContentNegotiation) {
        json()
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

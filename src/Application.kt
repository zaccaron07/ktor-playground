package com.example

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.UserIdPrincipal
import io.ktor.auth.authenticate
import io.ktor.auth.jwt.jwt
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.routing.routing
import java.util.*

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    val simpleJWT = SimpleJWT("my-super-secret")
    install(ContentNegotiation) {
        gson()
    }
    install(Authentication) {
        jwt {
            verifier(simpleJWT.verifier)
            validate {
                UserIdPrincipal(it.payload.getClaim("name").asString())
            }
        }
    }

    routing {
        get("/") {
            call.respondText("Hello from Ktor, Docker and Heroku!")
        }

        route("/login") {
            post {
                val post = call.receive<LoginRegister>()
                val user = users.getOrPut(post.user) {
                    User(post.user, post.password)
                }

                if (user.password != post.password) error("Invalid credentials")
                call.respond(mapOf("token" to simpleJWT.sign(user.name)))
                //call.respondText("Hello from Ktor, Docker and Heroku!")
            }
        }

        route("/users") {
            get {
                call.respond(mapOf("users" to "user1"))
            }

            authenticate {
                get("/authenticated") {
                    call.respond(mapOf("users" to "user2"))
                }

                post {
                    call.respond(mapOf("status" to "is logged"))
                }
            }
        }
    }
}

open class SimpleJWT(val secret: String) {
    private val algorithm = Algorithm.HMAC256(secret)
    val verifier = JWT.require(algorithm).build()
    fun sign(name: String): String = JWT.create().withClaim("name", name).sign(algorithm)
}

class User(val name: String, val password: String)

val users = Collections.synchronizedMap(
    listOf(User("test", "test"))
        .associateBy { it.name }
        .toMutableMap()
)

class LoginRegister(val user: String, val password: String)
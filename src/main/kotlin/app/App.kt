package app

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.principal
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

fun main() = startServer()

fun startServer(port: Int = 8008) {
    embeddedServer(Netty, port) {
        install(ContentNegotiation) {
            jackson {
                enable(SerializationFeature.INDENT_OUTPUT)
                disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                disable(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES)
            }
        }

        install(Authentication) {
            jwt {
                verifier(JWT.require(Algorithm.RSA256(InMemoryJwtService.keyProvider)).build())
                validate { JWTPrincipal(it.payload) }
            }
        }

        routing {

            get("/.well-known/jwks.json") {
                call.respond(InMemoryJwtService.jwksMap)
            }

            post("/jwt") {
                call.respond(InMemoryJwtService.createJwt(call.receive()))
            }

            // end point that requires JWT
            authenticate {
                get("/secure_api") {
                    val payload = call.principal<JWTPrincipal>()!!.payload
                    call.respond(
                        mapOf(
                            "authorities" to payload.claims["authorities"]!!.`as`(JsonNode::class.java),
                            "identity" to InMemoryJwtService.payloadToUser(payload)
                        )
                    )
                }
            }
        }
    }.start(true)
}

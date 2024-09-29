package app

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.RSAKeyProvider
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.commons.codec.binary.Base64
import org.junit.BeforeClass
import org.junit.Test
import java.math.BigInteger
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.RSAPublicKeySpec
import java.time.Instant
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class IntegrationTest {

    companion object {
        private val objectMapper = ObjectMapper()
        private const val port = 39114
        private const val apiUrl = "http://localhost:$port"
        private val client = HttpClient.newHttpClient()
        private val rsaKeyFactory = KeyFactory.getInstance("RSA")
        private const val jwksPath = "/.well-known/jwks.json"

        @JvmStatic
        @BeforeClass
        fun beforeAll() {
            Thread { startServer(port) }.start()
            var ok = false
            while (!ok) {

                val startupCheckRequest = HttpRequest.newBuilder()
                    .uri(URI.create("$apiUrl$jwksPath"))
                    .header("Content-Type", "application/json")
                    .GET()
                    .build()

                try {
                    val response = client.send(startupCheckRequest, HttpResponse.BodyHandlers.ofString())
                    if (response.statusCode() == 200) {
                        ok = true
                    }
                } catch (_: Exception) {

                }

            }
        }
    }

    @Test
    fun jwks() {
        val jwksResponse = object : TypeReference<Map<String, List<Map<String, String>>>>() {}

        val jwksRequest = HttpRequest.newBuilder()
            .uri(URI.create("$apiUrl$jwksPath"))
            .header("Content-Type", "application/json")
            .GET()
            .build()

        val rawResponse = client.send(jwksRequest, HttpResponse.BodyHandlers.ofString())
        val response = objectMapper.readValue(rawResponse.body(), jwksResponse)

        assertEquals(1, response.size)
        assertEquals(setOf("keys"), response.keys)
        val keys = response["keys"]
        assertEquals(1, keys!!.size)
        val key = keys.first()
        assertEquals(setOf("alg", "kty", "use", "kid", "e", "n"), key.keys)
        assertEquals("KEY_ID", key["kid"])
        val rsaPublicKey = rsaKeyFactory.generatePublic(
            RSAPublicKeySpec(
                BigInteger(Base64.decodeBase64(key["n"])),
                BigInteger(Base64.decodeBase64(key["e"]))
            )
        ) as RSAPublicKey
        assertNotNull(rsaPublicKey)

    }


    @Test
    fun user_Jwt() {

        val client = HttpClient.newBuilder().build()

        val jsonBody = """
                    {
                        "authorities": ["shopper", "admin"],
                        "user": { 
                            "id": "test-user",
                            "email": "user@test"
                        },
                        "expireAfterSeconds": 6
                    }
    """.trimIndent()

        val jwtRequest = HttpRequest.newBuilder()
            .uri(URI.create("$apiUrl/jwt"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build()

        val response = client.send(jwtRequest, HttpResponse.BodyHandlers.ofString())
        val responseTypeReference = object : TypeReference<Map<String, String>>() {}
        val responseValue = objectMapper.readValue(response.body(), responseTypeReference)

        assertEquals(1, responseValue.size)
        assertEquals(setOf("jwt"), responseValue.keys)
        val jwt = responseValue["jwt"]

        val authResponseTypeReference = object : TypeReference<Map<String, Any>>() {}

        val authRequest = HttpRequest.newBuilder()
            .uri(URI.create("$apiUrl/secure_api"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer $jwt")
            .GET()
            .build()

        val authRawResponse = client.send(authRequest, HttpResponse.BodyHandlers.ofString())
        val authResponse = objectMapper.readValue(authRawResponse.body(), authResponseTypeReference)

        assertEquals(setOf("authorities", "identity"), authResponse.keys)
        val authorities = authResponse["authorities"] as List<*>
        assertTrue(authorities.contains("shopper"))
        assertEquals(2, authorities.size)

        /**
         *
         */

        // Present a service jwt signed by an unknown key
        // This mock auth service does not validate the signature of the presented jwt
        val now = Instant.now()
        val serviceJwt = JWT.create().withIssuedAt(Date.from(now))
            .withExpiresAt(Date.from(now.plusSeconds(10)))
            .withJWTId(UUID.randomUUID().toString())
            .withClaim("identityType", "service")
            .withClaim("serviceName", "mock-jwt")
            .sign(Algorithm.RSA256(newKeyProvider()))

        val failingAuthRequest = HttpRequest.newBuilder()
            .uri(URI.create("$apiUrl/secure_api"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer $serviceJwt")
            .GET()
            .build()

        val failingAuthResponse = client.send(failingAuthRequest, HttpResponse.BodyHandlers.ofString())
        assertEquals(401, failingAuthResponse.statusCode())

    }
}

private fun newKeyProvider(): RSAKeyProvider {
    val keyFactory = KeyPairGenerator.getInstance("RSA").also { it.initialize(2048) }
    val keyPair = keyFactory.genKeyPair()
    return object : RSAKeyProvider {
        override fun getPrivateKeyId(): String = UUID.randomUUID().toString()
        override fun getPrivateKey(): RSAPrivateKey = keyPair.private as RSAPrivateKey
        override fun getPublicKeyById(keyId: String?): RSAPublicKey = keyPair.public as RSAPublicKey
    }
}

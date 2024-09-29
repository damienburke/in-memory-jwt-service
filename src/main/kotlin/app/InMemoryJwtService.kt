package app

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTCreator
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.Payload
import com.auth0.jwt.interfaces.RSAKeyProvider
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.time.Instant
import java.util.*


internal class InMemoryJwtService {

    companion object {
        const val KID = "KEY_ID"
        private val keyFactory = KeyPairGenerator.getInstance("RSA").also { it.initialize(2048) }
        private val keyPair = keyFactory.genKeyPair()
        internal val keyProvider = object : RSAKeyProvider {
            override fun getPrivateKeyId(): String = KID
            override fun getPrivateKey(): RSAPrivateKey = keyPair.private as RSAPrivateKey
            override fun getPublicKeyById(keyId: String?): RSAPublicKey = keyPair.public as RSAPublicKey
        }

        internal val jwksMap = mapOf(
            "keys" to listOf(
                mapOf(
                    "alg" to "RS256",
                    "kty" to "RSA",
                    "use" to "sig",
                    "kid" to KID,
                    "e" to keyProvider.getPublicKeyById(KID).publicExponent.toByteArray().base64EncodeToString(),
                    "n" to keyProvider.getPublicKeyById(KID).modulus.toByteArray().base64EncodeToString()
                )
            )
        )

        internal fun createJwt(userJwtRequest: JwtRequest): Map<String, String> {
            return mapOf(
                "jwt" to userJwtRequest.generateJwt {
                    with(userJwtRequest.user) {
                        withClaim("id", id)
                        withClaim("email", email)
                    }
                }
            )
        }

        private fun JwtRequest.generateJwt(block: JWTCreator.Builder.() -> Unit): String {
            val now = Instant.now()
            return JWT.create().withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(now.plusSeconds(expireAfterSeconds)))
                .withArrayClaim("authorities", authorities.toTypedArray())
                .withJWTId(UUID.randomUUID().toString())
                .apply(block)
                .sign(Algorithm.RSA256(keyProvider))
        }

        fun payloadToUser(payload: Payload): User {
            return User(
                payload.claims["id"]!!.asString(),
                payload.claims["email"]!!.asString()
            )
        }
    }

}

data class User(
    val id: String,
    val email: String
)

data class JwtRequest(
    val authorities: Set<String>,
    val user: User,
    val expireAfterSeconds: Long = 360
)

private fun ByteArray.base64EncodeToString() = Base64.getEncoder().encodeToString(this)
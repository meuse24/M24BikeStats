package info.meuse24.m24bikestats.auth

import info.meuse24.m24bikestats.domain.repository.AuthRepository
import info.meuse24.m24bikestats.api.BoschEndpoint
import info.meuse24.m24bikestats.api.FetchBoschDataUseCase
import org.json.JSONObject
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.Locale

data class OidcCertificateInfoUiModel(
    val tokenKeyId: String?,
    val keyId: String,
    val matchesCurrentToken: Boolean,
    val keyType: String?,
    val algorithm: String?,
    val usage: String?,
    val subject: String?,
    val issuer: String?,
    val validFrom: String?,
    val validUntil: String?,
    val sha1Thumbprint: String?,
    val sha256Thumbprint: String?,
    val certificateChainEntries: Int,
)

interface OidcCertificateInfoProvider {
    suspend fun loadCurrentCertificate(): OidcCertificateInfoUiModel?
}

class LiveOidcCertificateInfoProvider(
    private val fetchBoschData: FetchBoschDataUseCase,
    private val authRepository: AuthRepository,
) : OidcCertificateInfoProvider {
    override suspend fun loadCurrentCertificate(): OidcCertificateInfoUiModel? {
        val response = fetchBoschData(BoschEndpoint.OIDC_CERTS).getOrNull() ?: return null
        return parseOidcCertificateInfo(response, authRepository.getAccessToken())
    }
}

internal fun parseOidcCertificateInfo(
    response: String,
    accessToken: String?,
): OidcCertificateInfoUiModel? {
    val jsonBody = response.substringAfter("\n\n", response).trim()
    if (!jsonBody.startsWith("{")) return null

    val keys = JSONObject(jsonBody).optJSONArray("keys") ?: return null
    if (keys.length() == 0) return null

    val tokenKeyId = extractJwtKeyId(accessToken)
    val selectedKey = (0 until keys.length())
        .mapNotNull(keys::optJSONObject)
        .firstOrNull { key -> tokenKeyId != null && key.optString("kid") == tokenKeyId }
        ?: keys.optJSONObject(0)
        ?: return null

    val keyId = selectedKey.optString("kid").ifBlank { return null }
    val x5c = selectedKey.optJSONArray("x5c")
    val certificate = x5c
        ?.optString(0)
        ?.takeIf { it.isNotBlank() }
        ?.let(::decodeX509Certificate)

    return OidcCertificateInfoUiModel(
        tokenKeyId = tokenKeyId,
        keyId = keyId,
        matchesCurrentToken = tokenKeyId == keyId,
        keyType = selectedKey.optString("kty").ifBlank { null },
        algorithm = selectedKey.optString("alg").ifBlank { null },
        usage = selectedKey.optString("use").ifBlank { null },
        subject = certificate?.subjectX500Principal?.name,
        issuer = certificate?.issuerX500Principal?.name,
        validFrom = certificate?.notBefore?.toInstant()?.atZone(ZoneId.systemDefault())?.format(CERT_DATE_FORMATTER),
        validUntil = certificate?.notAfter?.toInstant()?.atZone(ZoneId.systemDefault())?.format(CERT_DATE_FORMATTER),
        sha1Thumbprint = selectedKey.optString("x5t").ifBlank { null },
        sha256Thumbprint = selectedKey.optString("x5t#S256").ifBlank { null },
        certificateChainEntries = x5c?.length() ?: 0,
    )
}

private fun decodeX509Certificate(encodedCertificate: String): X509Certificate? =
    runCatching {
        val bytes = Base64.getDecoder().decode(encodedCertificate)
        CertificateFactory.getInstance("X.509")
            .generateCertificate(bytes.inputStream()) as X509Certificate
    }.getOrNull()

internal fun extractJwtKeyId(accessToken: String?): String? {
    val headerPart = accessToken?.split('.')?.getOrNull(0) ?: return null
    val decodedHeader = runCatching {
        val paddedHeader = headerPart.padEnd((headerPart.length + 3) / 4 * 4, '=')
        String(Base64.getUrlDecoder().decode(paddedHeader))
    }.getOrNull() ?: return null

    return Regex(""""kid"\s*:\s*"([^"]+)"""")
        .find(decodedHeader)
        ?.groupValues
        ?.getOrNull(1)
}

private val CERT_DATE_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale.US)

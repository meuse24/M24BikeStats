package info.meuse24.m24bikestats.domain.model

data class BoschApiRequest(
    val label: String,
    val baseUrl: String,
    val path: String,
) {
    val url: String
        get() = "$baseUrl$path"
}

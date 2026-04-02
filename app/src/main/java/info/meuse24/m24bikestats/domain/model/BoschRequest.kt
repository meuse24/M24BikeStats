package info.meuse24.m24bikestats.domain.model

data class BoschRequest(
    val label: String,
    val baseUrl: String,
    val path: String,
    val isLocalOnly: Boolean = false,
) {
    val url: String
        get() = "$baseUrl$path"
}

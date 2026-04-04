package info.meuse24.m24bikestats.api

data class BoschRequest(
    val label: String,
    val baseUrl: String,
    val path: String,
    val debugName: String = label,
    val isLocalOnly: Boolean = false,
    val absoluteUrl: String? = null,
) {
    val url: String
        get() = absoluteUrl ?: "$baseUrl$path"
}

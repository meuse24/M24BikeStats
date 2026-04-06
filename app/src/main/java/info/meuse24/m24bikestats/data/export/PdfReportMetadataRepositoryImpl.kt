package info.meuse24.m24bikestats.data.export

import info.meuse24.m24bikestats.auth.OidcDiscoveryInfoProvider
import info.meuse24.m24bikestats.auth.OidcUserInfoProvider
import info.meuse24.m24bikestats.domain.model.PdfReportDiscoveryInfo
import info.meuse24.m24bikestats.domain.model.PdfReportUserInfo
import info.meuse24.m24bikestats.domain.repository.PdfReportMetadataRepository

class PdfReportMetadataRepositoryImpl(
    private val userInfoProvider: OidcUserInfoProvider,
    private val discoveryInfoProvider: OidcDiscoveryInfoProvider,
) : PdfReportMetadataRepository {
    override suspend fun getCurrentUserInfo(): Result<PdfReportUserInfo?> = runCatching {
        userInfoProvider.loadCurrentUserInfo()?.let { userInfo ->
            PdfReportUserInfo(
                email = userInfo.email,
                username = userInfo.username,
                subject = userInfo.subject,
            )
        }
    }

    override suspend fun getCurrentDiscoveryInfo(): Result<PdfReportDiscoveryInfo?> = runCatching {
        discoveryInfoProvider.loadCurrentDiscovery()?.let { discoveryInfo ->
            PdfReportDiscoveryInfo(
                issuer = discoveryInfo.issuer,
                authorizationEndpoint = discoveryInfo.authorizationEndpoint,
                tokenEndpoint = discoveryInfo.tokenEndpoint,
                userInfoEndpoint = discoveryInfo.userInfoEndpoint,
            )
        }
    }
}

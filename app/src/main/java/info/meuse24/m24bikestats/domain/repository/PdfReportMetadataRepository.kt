package info.meuse24.m24bikestats.domain.repository

import info.meuse24.m24bikestats.domain.model.PdfReportDiscoveryInfo
import info.meuse24.m24bikestats.domain.model.PdfReportUserInfo

interface PdfReportMetadataRepository {
    suspend fun getCurrentUserInfo(): Result<PdfReportUserInfo?>
    suspend fun getCurrentDiscoveryInfo(): Result<PdfReportDiscoveryInfo?>
}

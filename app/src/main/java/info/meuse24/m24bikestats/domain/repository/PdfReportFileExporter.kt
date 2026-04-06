package info.meuse24.m24bikestats.domain.repository

import info.meuse24.m24bikestats.domain.model.PdfReportData
import java.io.File

interface PdfReportFileExporter {
    fun generate(
        reportData: PdfReportData,
        fileName: String,
    ): File
}

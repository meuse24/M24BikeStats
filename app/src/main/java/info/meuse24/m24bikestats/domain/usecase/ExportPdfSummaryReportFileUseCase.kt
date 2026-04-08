package info.meuse24.m24bikestats.domain.usecase

import info.meuse24.m24bikestats.domain.repository.PdfReportFileExporter
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class ExportedPdfSummaryReport(
    val fileName: String,
    val filePath: String,
)

class ExportPdfSummaryReportFileUseCase(
    private val exportPdfSummaryReportUseCase: ExportPdfSummaryReportUseCase,
    private val pdfReportFileExporter: PdfReportFileExporter,
    private val dateProvider: () -> LocalDate = LocalDate::now,
) {
    suspend operator fun invoke(): Result<ExportedPdfSummaryReport> =
        exportPdfSummaryReportUseCase().mapCatching { reportData ->
            val fileName = buildPdfFileName(dateProvider())
            val file = pdfReportFileExporter.generate(reportData, fileName)
            ExportedPdfSummaryReport(
                fileName = fileName,
                filePath = file.absolutePath,
            )
        }

    internal fun buildPdfFileName(date: LocalDate): String =
        "m24-bericht-${date.format(FILE_DATE_FORMATTER)}.pdf"

    private companion object {
        private val FILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }
}

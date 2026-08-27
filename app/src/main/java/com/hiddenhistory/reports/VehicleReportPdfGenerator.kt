package com.hiddenhistory.reports

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.hiddenhistory.models.MotTest
import com.hiddenhistory.models.SymptomReport
import com.hiddenhistory.viewmodel.SavedVehicleRecord
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Generates the professional shareable PDF version of a saved
 * Hidden History vehicle report.
 *
 * IMPORTANT:
 *
 * This class does NOT recalculate or alter report data.
 *
 * It reads the exact information already stored in
 * SavedVehicleRecord and presents it in a clean PDF format.
 */
object VehicleReportPdfGenerator {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842

    private const val LEFT_MARGIN = 42f
    private const val RIGHT_MARGIN = 42f
    private const val TOP_MARGIN = 48f
    private const val BOTTOM_MARGIN = 48f

    private const val CONTENT_WIDTH =
        PAGE_WIDTH - LEFT_MARGIN - RIGHT_MARGIN

    private val jsonParser =
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }

    /**
     * Creates the PDF file and returns it.
     */
    fun generate(
        context: Context,
        record: SavedVehicleRecord
    ): File {

        val vehicle =
            parseObject(record.vehicle_json)

        val advert =
            record.advert_json
                ?.takeIf { it.isNotBlank() }
                ?.let(::parseObject)

        val crossCheck =
            record.cross_check_json
                ?.takeIf { it.isNotBlank() }
                ?.let(::parseObject)

        val motTests =
            decodeMotTests(vehicle)

        val symptoms =
            decodeSymptoms(vehicle)

        val safeRegistration =
            record.registration
                .replace(
                    Regex("[^A-Za-z0-9_-]"),
                    "_"
                )

        val fileName =
            "Hidden_History_Vehicle_Report_$safeRegistration.pdf"

        val outputDirectory =
            File(context.cacheDir, "vehicle_reports")

        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs()
        }

        val outputFile =
            File(
                outputDirectory,
                fileName
            )

        if (outputFile.exists()) {
            outputFile.delete()
        }

        val document =
            PdfDocument()

        val writer =
            PdfWriter(document)

        try {

            writer.startPage()

            drawHeader(
                writer.canvas,
                record,
                vehicle
            )

            writer.space(22f)

            drawVehicleOverview(
                writer,
                vehicle,
                advert
            )

            drawAttentionSection(
                writer,
                crossCheck
            )

            drawReportSummary(
                writer,
                record.report_summary
            )

            drawAdvertAnalysis(
                writer,
                advert
            )

            drawCrossCheck(
                writer,
                crossCheck
            )

            drawMileageHistory(
                writer,
                motTests
            )

            drawMotHistory(
                writer,
                motTests
            )

            drawSellerClaims(
                writer,
                advert
            )

            drawVerificationItems(
                writer,
                advert,
                crossCheck
            )

            drawSellerQuestions(
                writer,
                advert
            )

            drawMissingInformation(
                writer,
                advert
            )

            drawSymptoms(
                writer,
                symptoms
            )

            drawFooter(
                writer
            )

            writer.finishPage()

            document.writeTo(
                FileOutputStream(outputFile)
            )

        } finally {
            document.close()
        }

        return outputFile
    }

    /**
     * Creates a content URI for the generated PDF.
     */
    fun getShareUri(
        context: Context,
        pdfFile: File
    ) =
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

    private fun drawHeader(
        canvas: Canvas,
        record: SavedVehicleRecord,
        vehicle: JsonObject?
    ) {

        val registration =
            record.registration
                .uppercase()
                .replace(" ", "")

        val make =
            vehicle?.string("make")
                .orEmpty()

        val model =
            vehicle?.string("model")
                .orEmpty()

        val year =
            vehicle?.string("yearOfManufacture")
                ?: vehicle?.string("year")
                ?: ""

        val title =
            listOf(
                make,
                model
            )
                .filter { it.isNotBlank() }
                .joinToString(" ")
                .ifBlank {
                    "Vehicle Report"
                }

        canvas.drawRect(
            0f,
            0f,
            PAGE_WIDTH.toFloat(),
            142f,
            Paint().apply {
                color =
                    android.graphics.Color.rgb(
                        24,
                        32,
                        44
                    )
                style =
                    Paint.Style.FILL
            }
        )

        val brandPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color =
                    android.graphics.Color.WHITE
                textSize =
                    15f
                typeface =
                    Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.BOLD
                    )
            }

        canvas.drawText(
            "HIDDEN HISTORY",
            LEFT_MARGIN,
            38f,
            brandPaint
        )

        val titlePaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color =
                    android.graphics.Color.WHITE
                textSize =
                    25f
                typeface =
                    Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.BOLD
                    )
            }

        canvas.drawText(
            title,
            LEFT_MARGIN,
            76f,
            titlePaint
        )

        val registrationPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color =
                    android.graphics.Color.rgb(
                        110,
                        190,
                        255
                    )
                textSize =
                    21f
                typeface =
                    Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.BOLD
                    )
            }

        canvas.drawText(
            registration,
            LEFT_MARGIN,
            108f,
            registrationPaint
        )

        val meta =
            listOf(
                year,
                "Vehicle Intelligence Report"
            )
                .filter { it.isNotBlank() }
                .joinToString(" • ")

        val metaPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color =
                    android.graphics.Color.LTGRAY
                textSize =
                    10f
            }

        canvas.drawText(
            meta,
            LEFT_MARGIN,
            129f,
            metaPaint
        )

        canvas.drawText(
            "Generated ${formatSavedDate(record.created_at)}",
            PAGE_WIDTH - RIGHT_MARGIN - 155f,
            38f,
            metaPaint
        )
    }

    private fun drawVehicleOverview(
        writer: PdfWriter,
        vehicle: JsonObject?,
        advert: JsonObject?
    ) {

        if (vehicle == null) {
            return
        }

        writer.sectionTitle(
            "VEHICLE OVERVIEW"
        )

        val make =
            vehicle.string("make")

        val model =
            vehicle.string("model")

        val year =
            vehicle.string("yearOfManufacture")
                ?: vehicle.string("year")

        val registration =
            vehicle.string("registrationNumber")
                ?: vehicle.string("registration")

        val colour =
            vehicle.string("primaryColour")
                ?: vehicle.string("colour")

        val engineSize =
            vehicle.string("engineSize")

        val engineCapacity =
            vehicle.int("engineCapacity")
                ?.let {
                    "${it}cc"
                }

        val fuel =
            vehicle.string("fuelType")

        val transmission =
            advert?.string("transmission")

        val motStatus =
            vehicle.string("motStatus")

        val motExpiry =
            vehicle.string("motExpiryDate")
                ?.let(::formatDate)

        val taxStatus =
            vehicle.string("taxStatus")

        val taxDue =
            vehicle.string("taxDueDate")
                ?.let(::formatDate)

        writer.twoColumnField(
            "Make",
            make
        )

        writer.twoColumnField(
            "Model",
            model
        )

        writer.twoColumnField(
            "Year",
            year
        )

        writer.twoColumnField(
            "Registration",
            registration
        )

        writer.twoColumnField(
            "Colour",
            colour
        )

        writer.twoColumnField(
            "Engine Size",
            engineSize
        )

        writer.twoColumnField(
            "Engine Capacity",
            engineCapacity
        )

        writer.twoColumnField(
            "Fuel Type",
            fuel
        )

        writer.twoColumnField(
            "Transmission",
            transmission
        )

        writer.twoColumnField(
            "MOT Status",
            motStatus
        )

        writer.twoColumnField(
            "MOT Expiry",
            motExpiry
        )

        writer.twoColumnField(
            "Tax Status",
            taxStatus
        )

        writer.twoColumnField(
            "Tax Due",
            taxDue
        )
    }

    private fun drawAttentionSection(
        writer: PdfWriter,
        crossCheck: JsonObject?
    ) {

        val warnings =
            crossCheck?.stringList(
                "warnings"
            )
                ?: emptyList()

        if (warnings.isEmpty()) {
            return
        }

        writer.sectionTitle(
            "ATTENTION REQUIRED"
        )

        writer.warningBox(
            "${warnings.size} stored finding" +
                if (warnings.size == 1) {
                    " requires attention."
                } else {
                    "s require attention."
                }
        )

        warnings.forEach {
            writer.bullet(it)
        }
    }

    private fun drawReportSummary(
        writer: PdfWriter,
        summary: String?
    ) {

        if (summary.isNullOrBlank()) {
            return
        }

        writer.sectionTitle(
            "REPORT SUMMARY"
        )

        writer.paragraph(summary)
    }

    private fun drawAdvertAnalysis(
        writer: PdfWriter,
        advert: JsonObject?
    ) {

        if (advert == null) {
            return
        }

        writer.sectionTitle(
            "ADVERT ANALYSIS"
        )

        val score =
            advert.int("conditionScore")

        if (score != null) {

            writer.scoreBox(
                score
            )
        }

        writer.twoColumnField(
            "Advertised price",
            advert.string("price")
        )

        writer.twoColumnField(
            "Advertised mileage",
            advert.string("mileage")
        )

        writer.twoColumnField(
            "Engine size",
            advert.string("engineSize")
        )

        writer.twoColumnField(
            "Fuel type",
            advert.string("fuelType")
        )

        writer.twoColumnField(
            "Transmission",
            advert.string("transmission")
        )

        val risks =
            advert.stringList("riskFlags")

        if (risks.isNotEmpty()) {

            writer.subTitle(
                "Risk flags"
            )

            risks.forEach {
                writer.bullet(it)
            }
        }

        val inconsistencies =
            advert.stringList(
                "inconsistencies"
            )

        if (inconsistencies.isNotEmpty()) {

            writer.subTitle(
                "Advert inconsistencies"
            )

            inconsistencies.forEach {
                writer.bullet(it)
            }
        }

        val insights =
            advert.stringList(
                "keyInsights"
            )

        if (insights.isNotEmpty()) {

            writer.subTitle(
                "Advert insights"
            )

            insights.forEach {
                writer.bullet(it)
            }
        }

        val summary =
            advert.string(
                "professionalSummary"
            )
                ?: advert.string(
                    "overallSummary"
                )

        if (!summary.isNullOrBlank()) {

            writer.subTitle(
                "Advert assessment"
            )

            writer.paragraph(summary)
        }
    }

    private fun drawCrossCheck(
        writer: PdfWriter,
        crossCheck: JsonObject?
    ) {

        if (crossCheck == null) {
            return
        }

        val warnings =
            crossCheck.stringList(
                "warnings"
            )

        val confirmations =
            crossCheck.stringList(
                "confirmations"
            )

        val verificationItems =
            crossCheck.stringList(
                "verificationItems"
            )

        if (
            warnings.isEmpty() &&
            confirmations.isEmpty() &&
            verificationItems.isEmpty()
        ) {
            return
        }

        writer.sectionTitle(
            "ADVERT ↔ OFFICIAL CROSS-CHECK"
        )

        if (warnings.isNotEmpty()) {

            writer.subTitle(
                "Findings requiring attention"
            )

            warnings.forEach {
                writer.bullet(it)
            }
        }

        if (confirmations.isNotEmpty()) {

            writer.subTitle(
                "Confirmed report facts"
            )

            confirmations.forEach {
                writer.bullet(it)
            }
        }

        if (verificationItems.isNotEmpty()) {

            writer.subTitle(
                "Verification actions"
            )

            verificationItems.forEach {
                writer.bullet(it)
            }
        }
    }

    private fun drawMileageHistory(
        writer: PdfWriter,
        tests: List<MotTest>
    ) {

        val validTests =
            tests.filter {
                !it.odometerValue.isNullOrBlank()
            }

        if (validTests.isEmpty()) {
            return
        }

        writer.sectionTitle(
            "MILEAGE HISTORY"
        )

        validTests.forEach { test ->

            val date =
                test.completedDate
                    ?.let {
                        it.take(10)
                    }
                    ?: "MOT"

            val mileage =
                listOfNotNull(
                    test.odometerValue,
                    test.odometerUnit
                )
                    .joinToString(" ")

            writer.twoColumnField(
                date,
                mileage
            )
        }
    }

    private fun drawMotHistory(
        writer: PdfWriter,
        tests: List<MotTest>
    ) {

        if (tests.isEmpty()) {
            return
        }

        writer.sectionTitle(
            "MOT HISTORY"
        )

        tests.forEachIndexed { index, test ->

            val result =
                test.testResult
                    ?.uppercase()
                    ?: "UNKNOWN"

            val date =
                test.completedDate
                    ?.let(::formatDate)
                    ?: "Date unavailable"

            val mileage =
                listOfNotNull(
                    test.odometerValue,
                    test.odometerUnit
                )
                    .joinToString(" ")

            writer.subTitle(
                "MOT Test ${index + 1} — $result"
            )

            writer.twoColumnField(
                "Date",
                date
            )

            writer.twoColumnField(
                "Mileage",
                mileage
            )

            val defects =
                test.defects
                    .mapNotNull {
                        it.text
                    }
                    .filter {
                        it.isNotBlank()
                    }

            if (defects.isNotEmpty()) {

                writer.subTitle(
                    "Defects"
                )

                defects.forEach {
                    writer.bullet(it)
                }
            }
        }
    }

    private fun drawSellerClaims(
        writer: PdfWriter,
        advert: JsonObject?
    ) {

        val claims =
            advert?.stringList(
                "claimsMadeBySeller"
            )
                ?: emptyList()

        if (claims.isEmpty()) {
            return
        }

        writer.sectionTitle(
            "SELLER CLAIMS"
        )

        writer.warningBox(
            "Seller claims — not independently verified."
        )

        claims.forEach {
            writer.bullet(it)
        }
    }

    private fun drawVerificationItems(
        writer: PdfWriter,
        advert: JsonObject?,
        crossCheck: JsonObject?
    ) {

        val advertItems =
            advert?.stringList(
                "thingsWorthVerifying"
            )
                ?: emptyList()

        val crossCheckItems =
            crossCheck?.stringList(
                "verificationItems"
            )
                ?: emptyList()

        val items =
            (advertItems + crossCheckItems)
                .distinct()

        if (items.isEmpty()) {
            return
        }

        writer.sectionTitle(
            "WHAT YOU SHOULD VERIFY"
        )

        writer.paragraph(
            "Before buying this vehicle:"
        )

        items.forEach {
            writer.bullet(it)
        }
    }

    private fun drawSellerQuestions(
        writer: PdfWriter,
        advert: JsonObject?
    ) {

        val questions =
            advert?.stringList(
                "questionsTheBuyerShouldAsk"
            )
                ?: emptyList()

        if (questions.isEmpty()) {
            return
        }

        writer.sectionTitle(
            "QUESTIONS FOR THE SELLER"
        )

        questions.forEachIndexed { index, question ->

            writer.numberedItem(
                index + 1,
                question
            )
        }
    }

    private fun drawMissingInformation(
        writer: PdfWriter,
        advert: JsonObject?
    ) {

        val missing =
            advert?.stringList(
                "missingInformation"
            )
                ?: emptyList()

        if (missing.isEmpty()) {
            return
        }

        writer.sectionTitle(
            "MISSING FROM ADVERT"
        )

        writer.paragraph(
            "${missing.size} item" +
                if (missing.size == 1) {
                    " was"
                } else {
                    "s were"
                } +
                " not supplied by the advert."
        )

        missing.forEach {
            writer.bullet(it)
        }
    }

    private fun drawSymptoms(
        writer: PdfWriter,
        symptoms: List<SymptomReport>
    ) {

        if (symptoms.isEmpty()) {
            return
        }

        writer.sectionTitle(
            "ACTIVE SYMPTOMS"
        )

        symptoms.forEach { symptom ->

            writer.bullet(
                symptom.userDescription
                    ?: "Unspecified symptom"
            )
        }
    }

    private fun drawFooter(
        writer: PdfWriter
    ) {

        writer.space(18f)

        writer.drawDivider()

        writer.space(10f)

        writer.smallText(
            "Generated by Hidden History"
        )

        writer.smallText(
            "This report presents saved vehicle, advert and MOT information."
        )

        writer.smallText(
            "Seller statements remain claims until independently verified."
        )
    }

    private fun parseObject(
        value: String
    ): JsonObject? =
        runCatching {
            jsonParser
                .parseToJsonElement(value)
                .jsonObject
        }.getOrNull()

    private fun decodeMotTests(
        vehicle: JsonObject?
    ): List<MotTest> {

        if (vehicle == null) {
            return emptyList()
        }

        return vehicle["motTests"]
            ?.jsonArray
            ?.mapNotNull { element ->

                runCatching {
                    jsonParser.decodeFromJsonElement(MotTest.serializer(), element)
                }.getOrNull()
            }
            ?: emptyList()
    }

    private fun decodeSymptoms(
        vehicle: JsonObject?
    ): List<SymptomReport> {

        if (vehicle == null) {
            return emptyList()
        }

        val element =
            vehicle["symptoms"]
                ?: vehicle["activeSymptoms"]
                ?: return emptyList()

        return element
            .jsonArray
            .mapNotNull { item ->

                runCatching {
                    jsonParser.decodeFromJsonElement(SymptomReport.serializer(), item)
                }.getOrNull()
            }
    }

    private fun formatDate(
        value: String
    ): String {

        val candidates =
            listOf(
                "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX",
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ss.SSSSSSX",
                "yyyy-MM-dd'T'HH:mm:ssX",
                "yyyy-MM-dd"
            )

        for (pattern in candidates) {

            runCatching {

                val date =
                    SimpleDateFormat(
                        pattern,
                        Locale.UK
                    ).apply {
                        timeZone =
                            TimeZone.getTimeZone(
                                "UTC"
                            )
                    }.parse(value)

                if (date != null) {

                    return SimpleDateFormat(
                        "dd MMM yyyy",
                        Locale.UK
                    ).format(date)
                }
            }
        }

        return value
    }

    private fun formatSavedDate(
        value: String?
    ): String {

        if (value.isNullOrBlank()) {
            return "Unknown date"
        }

        val candidates =
            listOf(
                "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX",
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ssX"
            )

        for (pattern in candidates) {

            runCatching {

                val date =
                    SimpleDateFormat(
                        pattern,
                        Locale.UK
                    ).apply {
                        timeZone =
                            TimeZone.getTimeZone(
                                "UTC"
                            )
                    }.parse(value)

                if (date != null) {

                    return SimpleDateFormat(
                        "dd MMM yyyy • HH:mm",
                        Locale.UK
                    ).format(date)
                }
            }
        }

        return value
    }

    private fun JsonObject.string(
        key: String
    ): String? =
        this[key]
            ?.jsonPrimitive
            ?.contentOrNull
            ?.replace("$", "")

    private fun JsonObject.int(
        key: String
    ): Int? =
        this[key]
            ?.jsonPrimitive
            ?.intOrNull

    private fun JsonObject.stringList(
        key: String
    ): List<String> =
        this[key]
            ?.jsonArray
            ?.mapNotNull {
                it.jsonPrimitive
                    .contentOrNull
                    ?.replace("$", "")
                    ?.takeIf(
                        String::isNotBlank
                    )
            }
            ?: emptyList()

    /**
     * Internal PDF drawing helper.
     *
     * Handles:
     * - page creation
     * - page breaks
     * - text wrapping
     * - sections
     * - bullets
     * - fields
     */
    private class PdfWriter(
        private val document: PdfDocument
    ) {

        var canvas: Canvas
            private set

        private var page:
            PdfDocument.Page

        // Starts below the 142f header box so content doesn't overlap the header
        private var y = 165f

        private val bodyPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color =
                    android.graphics.Color.rgb(
                        45,
                        45,
                        50
                    )
                textSize =
                    10.5f
                typeface =
                    Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.NORMAL
                    )
            }

        private val smallPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color =
                    android.graphics.Color.GRAY
                textSize =
                    8.5f
            }

        private val labelPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color =
                    android.graphics.Color.DKGRAY
                textSize =
                    9f
                typeface =
                    Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.BOLD
                    )
            }

        private val valuePaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color =
                    android.graphics.Color.rgb(
                        35,
                        35,
                        40
                    )
                textSize =
                    10f
                typeface =
                    Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.BOLD
                    )
            }

        private val sectionPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color =
                    android.graphics.Color.rgb(
                        35,
                        65,
                        95
                    )
                textSize =
                    14f
                typeface =
                    Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.BOLD
                    )
            }

        private val subTitlePaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color =
                    android.graphics.Color.rgb(
                        50,
                        50,
                        55
                    )
                textSize =
                    10.5f
                typeface =
                    Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.BOLD
                    )
            }

        init {

            val pageInfo =
                PdfDocument.PageInfo.Builder(
                    PAGE_WIDTH,
                    PAGE_HEIGHT,
                    1
                ).create()

            page =
                document.startPage(
                    pageInfo
                )

            canvas =
                page.canvas
        }

        fun startPage() {
            // First page is already created.
        }

        fun finishPage() {
            document.finishPage(page)
        }

        private fun newPage() {

            document.finishPage(page)

            val pageNumber =
                document.pages.size + 1

            val pageInfo =
                PdfDocument.PageInfo.Builder(
                    PAGE_WIDTH,
                    PAGE_HEIGHT,
                    pageNumber
                ).create()

            page =
                document.startPage(
                    pageInfo
                )

            canvas =
                page.canvas

            y =
                TOP_MARGIN
        }

        private fun ensureSpace(
            requiredHeight: Float
        ) {

            if (
                y + requiredHeight >
                PAGE_HEIGHT - BOTTOM_MARGIN
            ) {
                newPage()
            }
        }

        fun space(
            amount: Float
        ) {

            ensureSpace(amount)

            y += amount
        }

        fun sectionTitle(
            title: String
        ) {

            ensureSpace(45f)

            y += 8f

            canvas.drawText(
                title,
                LEFT_MARGIN,
                y,
                sectionPaint
            )

            y += 8f

            drawDivider()

            y += 12f
        }

        fun subTitle(
            title: String
        ) {

            ensureSpace(28f)

            y += 5f

            canvas.drawText(
                title,
                LEFT_MARGIN,
                y,
                subTitlePaint
            )

            y += 16f
        }

        fun paragraph(
            text: String
        ) {

            val cleanText = text.replace("$", "")
            if (cleanText.isBlank()) {
                return
            }

            val lines =
                wrapText(
                    cleanText,
                    bodyPaint,
                    CONTENT_WIDTH
                )

            lines.forEach { line ->

                ensureSpace(17f)

                canvas.drawText(
                    line,
                    LEFT_MARGIN,
                    y,
                    bodyPaint
                )

                y += 14f
            }

            y += 5f
        }

        fun bullet(
            text: String
        ) {

            val cleanText = text.replace("$", "")
            val bulletWidth =
                14f

            val lines =
                wrapText(
                    cleanText,
                    bodyPaint,
                    CONTENT_WIDTH - bulletWidth
                )

            lines.forEachIndexed { index, line ->

                ensureSpace(17f)

                canvas.drawText(
                    if (index == 0) {
                        "•"
                    } else {
                        ""
                    },
                    LEFT_MARGIN,
                    y,
                    bodyPaint
                )

                canvas.drawText(
                    line,
                    LEFT_MARGIN + bulletWidth,
                    y,
                    bodyPaint
                )

                y += 14f
            }

            y += 2f
        }

        fun numberedItem(
            number: Int,
            text: String
        ) {

            val cleanText = text.replace("$", "")
            val prefix =
                "$number."

            val prefixWidth =
                22f

            val lines =
                wrapText(
                    cleanText,
                    bodyPaint,
                    CONTENT_WIDTH - prefixWidth
                )

            lines.forEachIndexed { index, line ->

                ensureSpace(17f)

                canvas.drawText(
                    if (index == 0) {
                        prefix
                    } else {
                        ""
                    },
                    LEFT_MARGIN,
                    y,
                    bodyPaint
                )

                canvas.drawText(
                    line,
                    LEFT_MARGIN + prefixWidth,
                    y,
                    bodyPaint
                )

                y += 14f
            }

            y += 2f
        }

        fun twoColumnField(
            label: String,
            value: String?
        ) {

            val cleanValue = value?.replace("$", "")
            if (cleanValue.isNullOrBlank()) {
                return
            }

            ensureSpace(22f)

            canvas.drawText(
                label,
                LEFT_MARGIN,
                y,
                labelPaint
            )

            val valueX =
                LEFT_MARGIN + 165f

            val lines =
                wrapText(
                    cleanValue,
                    valuePaint,
                    CONTENT_WIDTH - 165f
                )

            lines.forEachIndexed { index, line ->

                if (index > 0) {
                    ensureSpace(15f)
                    y += 14f
                }

                canvas.drawText(
                    line,
                    valueX,
                    y,
                    valuePaint
                )
            }

            y += 17f
        }

        fun scoreBox(
            score: Int
        ) {

            ensureSpace(60f)

            val boxPaint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color =
                        when {
                            score >= 70 ->
                                android.graphics.Color.rgb(
                                    215,
                                    242,
                                    224
                                )

                            score >= 50 ->
                                android.graphics.Color.rgb(
                                    245,
                                    235,
                                    190
                                )

                            else ->
                                android.graphics.Color.rgb(
                                    250,
                                    220,
                                    220
                                )
                        }

                    style =
                        Paint.Style.FILL
                }

            canvas.drawRoundRect(
                LEFT_MARGIN,
                y,
                LEFT_MARGIN + 135f,
                y + 48f,
                12f,
                12f,
                boxPaint
            )

            val scorePaint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color =
                        when {
                            score >= 70 ->
                                android.graphics.Color.rgb(
                                    25,
                                    125,
                                    65
                                )

                            score >= 50 ->
                                android.graphics.Color.rgb(
                                    130,
                                    105,
                                    25
                                )

                            else ->
                                android.graphics.Color.rgb(
                                    175,
                                    45,
                                    45
                                )
                        }

                    textSize =
                        21f

                    typeface =
                        Typeface.DEFAULT_BOLD
                }

            canvas.drawText(
                "$score / 100",
                LEFT_MARGIN + 15f,
                y + 30f,
                scorePaint
            )

            y += 62f
        }

        fun warningBox(
            text: String
        ) {

            val cleanText = text.replace("$", "")
            ensureSpace(42f)

            val paint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color =
                        android.graphics.Color.rgb(
                            255,
                            235,
                            235
                        )

                    style =
                        Paint.Style.FILL
                }

            val lines =
                wrapText(
                    cleanText,
                    bodyPaint,
                    CONTENT_WIDTH - 24f
                )

            val height =
                18f +
                    (lines.size * 14f)

            canvas.drawRoundRect(
                LEFT_MARGIN,
                y - 12f,
                PAGE_WIDTH - RIGHT_MARGIN,
                y + height,
                10f,
                10f,
                paint
            )

            lines.forEach { line ->

                canvas.drawText(
                    line,
                    LEFT_MARGIN + 12f,
                    y + 5f,
                    bodyPaint
                )

                y += 14f
            }

            y += 15f
        }

        fun drawDivider() {

            val paint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color =
                        android.graphics.Color.LTGRAY
                    strokeWidth =
                        1f
                }

            canvas.drawLine(
                LEFT_MARGIN,
                y,
                PAGE_WIDTH - RIGHT_MARGIN,
                y,
                paint
            )
        }

        fun smallText(
            text: String
        ) {

            val cleanText = text.replace("$", "")
            val lines =
                wrapText(
                    cleanText,
                    smallPaint,
                    CONTENT_WIDTH
                )

            lines.forEach { line ->

                ensureSpace(14f)

                canvas.drawText(
                    line,
                    LEFT_MARGIN,
                    y,
                    smallPaint
                )

                y += 12f
            }
        }

        private fun wrapText(
            text: String,
            paint: Paint,
            maxWidth: Float
        ): List<String> {

            val result =
                mutableListOf<String>()

            val paragraphs =
                text
                    .replace(
                        "\r\n",
                        "\n"
                    )
                    .split("\n")

            paragraphs.forEach { paragraph ->

                if (paragraph.isBlank()) {

                    result.add("")

                } else {

                    val words =
                        paragraph.split(
                            Regex("\\s+")
                        )

                    var current =
                        StringBuilder()

                    words.forEach { word ->

                        val candidate =
                            if (
                                current.isEmpty()
                            ) {
                                word
                            } else {
                                "$current $word"
                            }

                        if (
                            paint.measureText(
                                candidate
                            ) <= maxWidth
                        ) {

                            current =
                                StringBuilder(
                                    candidate
                                )

                        } else {

                            if (
                                current.isNotEmpty()
                            ) {
                                result.add(
                                    current.toString()
                                )
                            }

                            current =
                                StringBuilder(
                                    word
                                )
                        }
                    }

                    if (
                        current.isNotEmpty()
                    ) {
                        result.add(
                            current.toString()
                        )
                    }
                }
            }

            return result
        }
    }
}

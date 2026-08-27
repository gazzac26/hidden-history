package com.hiddenhistory.engine.advert.crosscheck

import com.hiddenhistory.models.MotTest
import java.time.LocalDate
import java.time.LocalDateTime

internal enum class MileageUnit {
    MILES,
    KILOMETRES,
    UNKNOWN
}

internal data class AdvertMileage(
    val originalMileage: Int,
    val originalUnit: MileageUnit,
    val miles: Int
)

internal data class OfficialMileageReading(
    val originalMileage: Int,
    val originalUnit: MileageUnit,
    val miles: Int,
    val date: String?,
    val resultType: String?,
    val testResult: String?,
    val test: MotTest
)

internal data class MileageRegression(
    val previous: OfficialMileageReading,
    val current: OfficialMileageReading,
    val difference: Int
)

internal data class ParsedDateTime(
    val raw: String,
    val date: LocalDate?,
    val dateTime: LocalDateTime?
)

internal data class DefectEvidence(
    val test: MotTest,
    val text: String?,
    val type: String?,
    val dangerous: Boolean
)
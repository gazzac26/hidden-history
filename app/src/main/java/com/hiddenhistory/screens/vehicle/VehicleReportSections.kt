package com.hiddenhistory.screens.vehicle

import com.hiddenhistory.models.MotTest
import com.hiddenhistory.models.SymptomReport

sealed class UiSection {
    data class Header(
        val title: String,
        val items: List<Pair<String, String>>
    ) : UiSection()

    data class MotTests(
        val tests: List<MotTest>
    ) : UiSection()

    data class Symptoms(
        val symptoms: List<Any>
    ) : UiSection()
}

object VehicleSearchSectionParser {
    fun parse(uiState: List<Any>): List<UiSection> {
        val sections = mutableListOf<UiSection>()
        var currentHeaderTitle: String? = null
        val currentAttributes = mutableListOf<Pair<String, String>>()
        val motTestsList = mutableListOf<MotTest>()
        val symptomsList = mutableListOf<Any>()
        var mode = "NONE"

        for (item in uiState) {
            when (item) {
                is String -> {
                    when (item) {
                        "MOT History" -> {
                            if (currentHeaderTitle != null) {
                                sections.add(UiSection.Header(currentHeaderTitle, currentAttributes.toList()))
                                currentHeaderTitle = null
                                currentAttributes.clear()
                            }
                            mode = "MOT"
                        }
                        "Active Symptoms" -> {
                            if (currentHeaderTitle != null) {
                                sections.add(UiSection.Header(currentHeaderTitle, currentAttributes.toList()))
                                currentHeaderTitle = null
                                currentAttributes.clear()
                            }
                            mode = "SYMPTOMS"
                        }
                        else -> {
                            if (currentHeaderTitle != null) {
                                sections.add(UiSection.Header(currentHeaderTitle, currentAttributes.toList()))
                                currentAttributes.clear()
                            }
                            currentHeaderTitle = item
                            mode = "HEADER"
                        }
                    }
                }
                is Pair<*, *> -> {
                    val key = item.first?.toString() ?: ""
                    val value = item.second?.toString() ?: ""
                    if (mode == "HEADER") {
                        currentAttributes.add(key to value)
                    }
                }
                is MotTest -> motTestsList.add(item)
                is SymptomReport -> symptomsList.add(item)
                else -> {
                    if (mode == "SYMPTOMS") symptomsList.add(item)
                }
            }
        }

        if (currentHeaderTitle != null && currentAttributes.isNotEmpty()) {
            sections.add(UiSection.Header(currentHeaderTitle, currentAttributes.toList()))
        }
        if (motTestsList.isNotEmpty()) {
            sections.add(UiSection.MotTests(motTestsList))
        }
        if (symptomsList.isNotEmpty()) {
            sections.add(UiSection.Symptoms(symptomsList))
        }

        return sections
    }
}

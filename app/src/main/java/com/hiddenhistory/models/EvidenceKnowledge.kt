package com.hiddenhistory.models

data class EvidenceKnowledge(

    val source: String,

    val sourceType: String,

    val confidence: Float,

    val verified: Boolean,

    val lastUpdated: Long

)
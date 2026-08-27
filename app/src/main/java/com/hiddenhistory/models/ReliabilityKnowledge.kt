package com.hiddenhistory.models

data class ReliabilityKnowledge(

    val strengths: List<String>,

    val weaknesses: List<String>,

    val commonFaults: List<String>,

    val commonRepairs: List<String>,

    val reliabilityRating: Float?

)
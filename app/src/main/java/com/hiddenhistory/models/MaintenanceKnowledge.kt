package com.hiddenhistory.models

data class MaintenanceKnowledge(

    val serviceIntervals: List<String>,

    val oilSpecification: String?,

    val oilCapacity: Double?,

    val coolant: String?,

    val brakeFluid: String?,

    val timingBeltInterval: String?,

    val tyreSizes: List<String>

)
package com.hiddenhistory.models

data class ManufacturerKnowledge(

    val generation: String?,

    val trim: String?,

    val engineFamily: String?,

    val horsepower: Int?,

    val torque: Int?,

    val drivetrain: String?,

    val gearbox: String?,

    val towingCapacity: Int?,

    val fuelEconomy: Double?,

    val co2: Int?,

    val serviceSchedule: List<String>,

    val recalls: List<String>

)
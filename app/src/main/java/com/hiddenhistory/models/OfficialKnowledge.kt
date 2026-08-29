package com.hiddenhistory.models

data class OfficialKnowledge(

    val registration: String?,

    val vin: String?,

    val make: String?,

    val model: String?,

    val year: Int?,

    val colour: String?,

    val bodyType: String?,

    val fuelType: String?,

    val transmission: String?,

    val engineCapacity: Int?,

    val taxStatus: String?,

    val taxDueDate: String?,

    val motStatus: String?,

    val motExpiryDate: String?,

    val previousKeepers: Int?,

    val recalls: List<String>

)
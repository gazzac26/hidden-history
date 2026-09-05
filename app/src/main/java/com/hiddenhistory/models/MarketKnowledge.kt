package com.hiddenhistory.models

data class MarketKnowledge(

    val averageDealerPrice: Double?,

    val averagePrivatePrice: Double?,

    val lowPrice: Double?,

    val highPrice: Double?,

    val marketTrend: String?,

    val popularity: Float?

)
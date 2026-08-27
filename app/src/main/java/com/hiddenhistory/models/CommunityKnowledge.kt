package com.hiddenhistory.models

data class CommunityKnowledge(

    val ownerRating: Float? = null,

    val comfortRating: Float? = null,

    val performanceRating: Float? = null,

    val economyRating: Float? = null,

    val favouriteFeatures: List<String> = emptyList(),

    val commonComplaints: List<String> = emptyList(),

    val ownerTips: List<String> = emptyList()

)
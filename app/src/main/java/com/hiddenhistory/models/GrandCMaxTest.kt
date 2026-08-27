package com.hiddenhistory.models

object GrandCMaxTest {

    /**
     * Fixed Grand C-Max test vehicle.
     *
     * This provides controlled vehicle data to the existing
     * vehicle analysis pipeline and advert analyzer.
     */
    
    val sampleAdvertText: String = """
        Ford Grand C-Max 1.6 TDCi Titanium 7 Seater - FM64BVS
        Price: £1,250
        
        Great family car, starts and drives spot on. 
        143k warranted miles with full service history and MOT valid until February 2027. 
        Very clean condition inside and out, nice alloys, parking sensors, dual-zone climate control, and sliding rear doors. 
        Engine and gearbox are smooth with no knocks or bangs. 
        HPI clear. Quick sale, no time wasters please.
    """.trimIndent()

    fun createVehicle(): Vehicle {

        return Vehicle(
            registrationNumber = "FM64BVS",
            registration = "FM64BVS",

            make = "FORD",
            model = "GRAND C-MAX",
            year = 2015,

            fuelType = "DIESEL",
            engineCapacity = 1560,
            engineSize = "1.6 TDCi",
            seats = 7,

            colour = "BLACK",

            price = 1250.0,

            taxStatus = "Unknown",
            motStatus = "Valid",
            motExpiryDate = "February 2027",

            hasOutstandingRecall = null,
            salvageCategory = null,
            vehicleTier = null,

            motTests = listOf(
                MotTest(
                    completedDate = "2026",
                    testResult = "PASSED",
                    odometerValue = "142000",
                    odometerUnit = "mi",
                    defects = emptyList()
                ),
                MotTest(
                    completedDate = "2025",
                    testResult = "PASSED",
                    odometerValue = "132000",
                    odometerUnit = "mi",
                    defects = emptyList()
                ),
                MotTest(
                    completedDate = "2024",
                    testResult = "PASSED",
                    odometerValue = "112000",
                    odometerUnit = "mi",
                    defects = emptyList()
                ),
                MotTest(
                    completedDate = "2023",
                    testResult = "PASSED",
                    odometerValue = "92000",
                    odometerUnit = "mi",
                    defects = emptyList()
                ),
                MotTest(
                    completedDate = "2022",
                    testResult = "PASSED",
                    odometerValue = "79000",
                    odometerUnit = "mi",
                    defects = emptyList()
                ),
                MotTest(
                    completedDate = "2021",
                    testResult = "PASSED",
                    odometerValue = "62000",
                    odometerUnit = "mi",
                    defects = emptyList()
                )
            ),

            activeSymptoms = emptyList()
        )
    }
}

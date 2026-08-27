package com.hiddenhistory.database

import android.content.Context
import android.util.Log
import com.hiddenhistory.database.util.CsvParserUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class GlobalVehicle(
    val id: String,
    val make: String,
    val model: String,
    val category: String,
    val variant: String,
    val yearStart: Int,
    val yearEnd: Int,
    val stockHp: Int
)

@Serializable
data class GlobalPart(
    val id: String,
    val vehicleId: String,
    val name: String,
    val category: String,
    val cost: Double,
    val hpGain: Int,
    val audioKey: String
)

class VehicleCatalogDao(private val context: Context) {
    private var cachedVehicles: List<GlobalVehicle>? = null
    private var cachedParts: List<GlobalPart>? = null

    private val json = Json { 
        ignoreUnknownKeys = true 
        isLenient = true
    }

    suspend fun getAllMakes(): List<String> = withContext(Dispatchers.IO) {
        loadVehicles().map { it.make }.distinct().sorted()
    }

    suspend fun getModelsForMake(make: String): List<GlobalVehicle> = withContext(Dispatchers.IO) {
        loadVehicles().filter { it.make.equals(make, ignoreCase = true) }
    }

    suspend fun searchVehicles(query: String, limit: Int = 50): List<GlobalVehicle> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        loadVehicles().asSequence()
            .filter { 
                it.make.contains(query, ignoreCase = true) || 
                it.model.contains(query, ignoreCase = true) ||
                it.variant.contains(query, ignoreCase = true)
            }
            .take(limit)
            .toList()
    }

    suspend fun getPartsForVehicle(vehicleId: String): List<GlobalPart> = withContext(Dispatchers.IO) {
        loadParts().filter { it.vehicleId == vehicleId || it.vehicleId == "universal" }
    }

    @Synchronized
    private fun loadVehicles(): List<GlobalVehicle> {
        cachedVehicles?.let { return it }

        val uniqueVehiclesMap = mutableMapOf<String, GlobalVehicle>()
        var idCounter = 1

        try {
            // Uses the built-in CsvRowReader wrapper to cleanly read rich data
            CsvParserUtil.parseCsvAsset(context, "vehicles.csv") { row ->
                
                val makeName = row.getString("make_name", "make", "manufacturer", "brand", default = "")
                if (makeName.isBlank() || makeName == "Unspecified") return@parseCsvAsset

                val modelName = row.getString("model_name", "model", "model_slug", default = "Standard")
                val variantName = row.getString("variant", "trim", "edition", default = "Standard")
                val bodyTypes = row.getString("body_types", "bodytype", "body_type", "category", default = "Car")

                val primaryCategory = bodyTypes.split("|").firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "Car"

                // Dynamically extract real years and horsepower using fuzzy matching & regex number parsing
                val year = row.getInt("year", "year_start", "production_year", "start_year", "date", default = 2015)
                val yearEndVal = row.getInt("year_end", "end_year", default = 2026)
                val hp = row.getInt("bhp", "hp", "horsepower", "power", "ps", default = 150)

                val uniqueKey = "$makeName|$modelName|$variantName".lowercase()

                if (!uniqueVehiclesMap.containsKey(uniqueKey)) {
                    uniqueVehiclesMap[uniqueKey] = GlobalVehicle(
                        id = "v_$idCounter",
                        make = makeName,
                        model = modelName,
                        category = primaryCategory,
                        variant = variantName,
                        yearStart = year,
                        yearEnd = yearEndVal,
                        stockHp = hp
                    )
                    idCounter++
                }
            }

            val vehicleList = uniqueVehiclesMap.values.toList()
            Log.d("VehicleCatalogDao", "Successfully loaded ${vehicleList.size} vehicles dynamically from vehicles.csv.")
            cachedVehicles = vehicleList
            return vehicleList
        } catch (e: Throwable) {
            Log.e("VehicleCatalogDao", "CRITICAL CSV ERROR: ${e.localizedMessage}", e)
        }

        return emptyList()
    }

    @Synchronized
    private fun loadParts(): List<GlobalPart> {
        cachedParts?.let { return it }
        try {
            context.assets.open("parts_catalog.json").use { inputStream ->
                val jsonString = inputStream.bufferedReader().use { it.readText() }
                val partsList = json.decodeFromString<List<GlobalPart>>(jsonString)
                cachedParts = partsList
                return partsList
            }
        } catch (e: Throwable) {
            Log.e("VehicleCatalogDao", "Failed to load parts catalog: ${e.localizedMessage}")
        }

        val defaultParts = listOf(
            GlobalPart(
                id = "default-exhaust",
                vehicleId = "universal",
                name = "Sport Exhaust",
                category = "EXHAUST",
                cost = 450.0,
                hpGain = 15,
                audioKey = "stock_inline6"
            ),
            GlobalPart(
                id = "default-tune",
                vehicleId = "universal",
                name = "Stage 1 ECU Tune",
                category = "ECU",
                cost = 600.0,
                hpGain = 35,
                audioKey = "stock_inline6"
            )
        )
        cachedParts = defaultParts
        return defaultParts
    }
}

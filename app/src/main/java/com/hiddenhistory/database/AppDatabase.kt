package com.hiddenhistory.database

import android.content.Context

class AppDatabase private constructor(context: Context) {
    private val appContext = context.applicationContext

    private val profileDaoInstance = ProfileDao(appContext)
    private val savedVehicleDaoInstance = SavedVehicleDao(appContext)
    private val personalVehicleDaoInstance = PersonalVehicleDao(appContext)
    
    // VehicleCatalogDao only needs the context argument
    private val vehicleCatalogDaoInstance = VehicleCatalogDao(appContext)

    fun profileDao(): ProfileDao = profileDaoInstance
    fun savedVehicleDao(): SavedVehicleDao = savedVehicleDaoInstance
    fun personalVehicleDao(): PersonalVehicleDao = personalVehicleDaoInstance
    fun vehicleCatalogDao(): VehicleCatalogDao = vehicleCatalogDaoInstance

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = AppDatabase(context)
                INSTANCE = instance
                instance
            }
        }
    }
}

package com.hiddenhistory.repository

import com.hiddenhistory.models.HiddenHistoryReportEntity
import com.hiddenhistory.models.HiddenHistoryReportInsert
import com.hiddenhistory.models.VehicleCacheEntity
import com.hiddenhistory.models.VehicleCacheInsert
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from

class HiddenHistoryRepository(
    private val supabase: SupabaseClient
) {

    suspend fun getVehicleCache(
        registrationNumber: String
    ): VehicleCacheEntity? {
        return supabase
            .from("hidden_history_vehicle_cache")
            .select {
                filter {
                    eq(
                        "registration",
                        registrationNumber.uppercase()
                    )
                }
            }
            .decodeSingleOrNull<VehicleCacheEntity>()
    }

    suspend fun insertVehicleCache(
        cache: VehicleCacheInsert
    ) {
        supabase
            .from("hidden_history_vehicle_cache")
            .insert(cache)
    }

    suspend fun updateVehicleCache(
        registrationNumber: String,
        cache: VehicleCacheInsert
    ) {
        supabase
            .from("hidden_history_vehicle_cache")
            .update(cache) {
                filter {
                    eq(
                        "registration",
                        registrationNumber.uppercase()
                    )
                }
            }
    }

    suspend fun getUserReports(
        userId: String
    ): List<HiddenHistoryReportEntity> {
        return supabase
            .from("hidden_history_reports")
            .select {
                filter {
                    eq("user_id", userId)
                }
            }
            .decodeList<HiddenHistoryReportEntity>()
    }

    suspend fun insertUserReport(
        report: HiddenHistoryReportInsert
    ) {
        supabase
            .from("hidden_history_reports")
            .insert(report)
    }

    suspend fun deleteUserReport(
        reportId: String
    ) {
        supabase
            .from("hidden_history_reports")
            .delete {
                filter {
                    eq("id", reportId)
                }
            }
    }
}

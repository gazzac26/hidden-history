package com.hiddenhistory.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hiddenhistory.data.SupabaseManager
import com.hiddenhistory.models.AdvertAnalysis
import com.hiddenhistory.repository.AdvertAnalysisRepository
import com.hiddenhistory.ui.debug.DebugStateHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AdvertAnalysisUiState {
    object Idle : AdvertAnalysisUiState
    object Analysing : AdvertAnalysisUiState
    data class Success(val analysis: AdvertAnalysis) : AdvertAnalysisUiState
    data class Error(val message: String) : AdvertAnalysisUiState
}

class AdvertAnalysisViewModel : ViewModel() {

    private val repository = AdvertAnalysisRepository(SupabaseManager.client)

    private val _uiState = MutableStateFlow<AdvertAnalysisUiState>(AdvertAnalysisUiState.Idle)
    val uiState: StateFlow<AdvertAnalysisUiState> = _uiState.asStateFlow()

    // Standard UK Vehicle Registration Regex Pattern
    private val ukRegPattern = Regex(
        pattern = "\\b(?:[A-Z]{2}[0-9]{2}\\s?[A-Z]{3}|[A-Z]{1}[0-9]{1,3}[A-Z]{3}|[A-Z]{3}[0-9]{1,3}[A-Z]{1}|[A-Z]{1,3}[0-9]{1,3}|[0-9]{1,4}[A-Z]{1,2}|[A-Z]{1,2}[0-9]{1,4}|[A-Z]{3}[0-9]{1,3}[A-Z])\\b",
        option = RegexOption.IGNORE_CASE
    )

    private fun extractRegistration(text: String): String? {
        val match = ukRegPattern.find(text)
        return match?.value?.uppercase()?.replace("\\s".toRegex(), "")
    }

    fun analyzeAdvert(advertText: String) {
        if (advertText.isBlank() || _uiState.value is AdvertAnalysisUiState.Analysing) return

        viewModelScope.launch {
            _uiState.value = AdvertAnalysisUiState.Analysing
            try {
                val rawResult = repository.analyzeAdvert(advertText)
                
                // Fallback / extract registration from text if not provided by backend model
                val detectedReg = rawResult.registrationNumber ?: extractRegistration(advertText)
                val result = rawResult.copy(registrationNumber = detectedReg)
                
                // Send the successful analysis result to the debug inspector screen
                DebugStateHolder.updateAdvert(result)
                
                _uiState.value = AdvertAnalysisUiState.Success(result)
            } catch (e: Throwable) {
                // Log the real error internally for debugging, but NEVER expose raw credentials or internal keys to the user
                Log.e("AdvertAnalysisVM", "Analysis failed securely", e)
                
                _uiState.value = AdvertAnalysisUiState.Error(
                    "Failed to analyze advert. Please check your network connection and try again."
                )
            }
        }
    }

    /**
     * Loads the Grand C-Max test advert text to test the analysis pipeline
     * offline, matching the test vehicle functionality in the vehicle search screen.
     */
    fun loadGrandCMaxTestAdvert(onTextChanged: (String) -> Unit) {
        val testAdvertText = """
            Ford Grand C-Max 1.6 TDCi Titanium 7 Seater - FM64BVS
            Good condition, starts and drives like new.
            No mechanical issues, well maintained.
            Full service history and V5 logbook present.
            143k warranted motorway miles.
            Valid MOT until February 2027 with a clean history.
            Nice alloys worth £500 alone, central locking, electric windows, dual-zone climate control, and CD player.
            No knocks or bangs from suspension or engine.
            HPI clear. Quick sale, no time wasters please. Price: £1,250
        """.trimIndent()

        onTextChanged(testAdvertText)
        analyzeAdvert(testAdvertText)
    }

    fun resetState() {
        if (_uiState.value !is AdvertAnalysisUiState.Analysing) {
            _uiState.value = AdvertAnalysisUiState.Idle
        }
    }
}

package com.hiddenhistory.billing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VehicleReportTokenViewModel :
    ViewModel() {

    private val tokenManager =
        VehicleReportTokenManager()

    private val _state =
        MutableStateFlow<VehicleReportTokenState>(
            VehicleReportTokenState.Idle
        )

    val state: StateFlow<VehicleReportTokenState> =
        _state.asStateFlow()

    fun reserveProSearchToken(
        onReserved: (String) -> Unit,
        onNoToken: () -> Unit
    ) {

        viewModelScope.launch {

            _state.value =
                VehicleReportTokenState.Checking

            try {

                val availableResult =
                    tokenManager
                        .getAvailableTokenCount()

                val available =
                    availableResult.getOrElse { 0 }

                if (available <= 0) {

                    _state.value =
                        VehicleReportTokenState.NoTokens(
                            availableCount =
                                available
                        )

                    onNoToken()

                    return@launch
                }

                _state.value =
                    VehicleReportTokenState.Reserving

                val tokenResult =
                    tokenManager
                        .reserveToken()

                val tokenId =
                    tokenResult.getOrThrow()

                _state.value =
                    VehicleReportTokenState.Reserved(
                        tokenId =
                            tokenId
                    )

                onReserved(
                    tokenId
                )

            } catch (
                e: Exception
            ) {

                _state.value =
                    VehicleReportTokenState.Error(
                        e.message
                            ?: "Unable to reserve the Pro Vehicle Search."
                    )
            }
        }
    }

    fun consumeToken(
        tokenId: String,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) {

        viewModelScope.launch {

            try {

                val consumedResult =
                    tokenManager
                        .consumeToken(
                            tokenId
                        )

                val consumed =
                    consumedResult.getOrElse { false }

                if (consumed) {

                    onComplete()

                } else {

                    onError(
                        "The Pro Vehicle Search token could not be completed."
                    )
                }

            } catch (
                e: Exception
            ) {

                onError(
                    e.message
                        ?: "Unable to complete the Pro Vehicle Search."
                )
            }
        }
    }

    fun refundToken(
        tokenId: String
    ) {

        viewModelScope.launch {

            try {

                tokenManager
                    .refundToken(
                        tokenId
                    )

            } catch (
                _: Exception
            ) {

                /*
                 * The backend remains authoritative.
                 * Failure here is intentionally not allowed
                 * to crash or interrupt the application.
                 */
            }
        }
    }
}

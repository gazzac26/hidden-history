package com.hiddenhistory.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hiddenhistory.data.SupabaseManager
import com.hiddenhistory.viewmodel.ProfileViewModel
import io.github.jan.supabase.auth.auth

@Composable
fun UserProfileScreen(
    viewModel: ProfileViewModel,
    onNavigateToEditProfile: () -> Unit
) {
    // 1. Get the current user ID
    val userId = SupabaseManager.client.auth.currentUserOrNull()?.id

    // 2. Observe the profile flow reactively
    // This will automatically re-render the screen when the database updates
    val profile by if (userId != null) {
        viewModel.getProfileFlow(userId).collectAsStateWithLifecycle(initialValue = null)
    } else {
        remember { mutableStateOf(null) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()), // Makes the screen scrollable
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("User Profile", style = MaterialTheme.typography.headlineMedium)

        if (profile == null) {
            // Show loading or empty state
            CircularProgressIndicator()
        } else {
            // Use the non-null profile object
            val p = profile!!

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Account Details", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Name: ${p.firstName ?: ""} ${p.lastName ?: ""}")
                    Text("Username: ${p.username ?: "N/A"}")
                    Text("Email: ${p.email ?: "N/A"}")
                    Text("Phone: ${p.phoneNumber ?: "Not provided"}")
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Vehicle Preferences", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Owned Vehicle: ${p.ownedVehicle ?: "N/A"}")
                    Text("Preferred Vehicle: ${p.prefVehicle ?: "N/A"}")
                }
            }
        }

        Button(
            onClick = { onNavigateToEditProfile() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Edit Profile")
        }
    }
}

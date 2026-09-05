package com.hiddenhistory.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.hiddenhistory.data.SupabaseManager
import com.hiddenhistory.viewmodel.ProfileViewModel
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch

@Composable
fun UserProfileEditScreen(
    viewModel: ProfileViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userId = SupabaseManager.client.auth.currentUserOrNull()?.id ?: return
    val profile by viewModel.getProfileFlow(userId).collectAsStateWithLifecycle(initialValue = null)

    // State for essential fields
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var prefVehicle by remember { mutableStateOf("") }
    var ownedVehicle by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var signedImageUrl by remember { mutableStateOf<String?>(null) }

    // Load data
    LaunchedEffect(profile) {
        profile?.let {
            username = it.username ?: ""
            email = it.email ?: ""
            firstName = it.firstName ?: ""
            lastName = it.lastName ?: ""
            phoneNumber = it.phoneNumber ?: ""
            prefVehicle = it.prefVehicle ?: ""
            ownedVehicle = it.ownedVehicle ?: ""
            imageUrl = it.profileImageUrl ?: ""
        }
    }

    // Refresh Image
    LaunchedEffect(imageUrl) {
        if (imageUrl.isNotEmpty()) signedImageUrl = viewModel.getSignedUrl(imageUrl)
    }

    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let {
            scope.launch {
                val bytes = context.contentResolver.openInputStream(it)?.use { stream -> stream.readBytes() }
                if (bytes != null) {
                    val path = "$userId/profile_${System.currentTimeMillis()}.jpg"
                    SupabaseManager.client.storage.from("profile-images").upload(path, bytes)
                    imageUrl = path
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Image UI
        AsyncImage(
            model = signedImageUrl,
            contentDescription = "Profile Photo",
            modifier = Modifier
                .size(100.dp)
                .align(Alignment.CenterHorizontally)
        )
        Button(
            onClick = { pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Change Photo")
        }

        // Essential Fields
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = firstName,
            onValueChange = { firstName = it },
            label = { Text("First Name") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = lastName,
            onValueChange = { lastName = it },
            label = { Text("Last Name") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            label = { Text("Phone") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = prefVehicle,
            onValueChange = { prefVehicle = it },
            label = { Text("Preferred Vehicle") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = ownedVehicle,
            onValueChange = { ownedVehicle = it },
            label = { Text("Owned Vehicle") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            onClick = {
                val updated = profile?.copy(
                    username = username,
                    email = email,
                    firstName = firstName,
                    lastName = lastName,
                    phoneNumber = phoneNumber,
                    profileImageUrl = imageUrl,
                    prefVehicle = prefVehicle,
                    ownedVehicle = ownedVehicle
                )
                updated?.let { viewModel.updateProfile(it) }
                Toast.makeText(context, "Profile Saved", Toast.LENGTH_SHORT).show()
                onNavigateBack()
            }
        ) {
            Text("Save Profile")
        }
    }
}

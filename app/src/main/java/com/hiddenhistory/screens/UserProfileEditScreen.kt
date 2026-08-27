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

    // State for all fields
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var address1 by remember { mutableStateOf("") }
    var address2 by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var county by remember { mutableStateOf("") }
    var postcode by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var licenseNumber by remember { mutableStateOf("") }
    var licencePassDate by remember { mutableStateOf("") }
    var occupation by remember { mutableStateOf("") }
    var prefVehicle by remember { mutableStateOf("") }
    var ownedVehicle by remember { mutableStateOf("") }
    var drivingConfidence by remember { mutableStateOf("") }
    var vehicleKnowledge by remember { mutableStateOf("") }
    var primaryUse by remember { mutableStateOf("") }
    var budget by remember { mutableStateOf("") }
    var performance by remember { mutableStateOf("") }
    var economy by remember { mutableStateOf("") }
    var insurance by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var signedImageUrl by remember { mutableStateOf<String?>(null) }

    // Load data
    LaunchedEffect(profile) {
        profile?.let {
            username = it.username ?: ""; email = it.email ?: ""; firstName = it.firstName ?: ""
            lastName = it.lastName ?: ""; phoneNumber = it.phoneNumber ?: ""; address1 = it.addressLine1 ?: ""
            address2 = it.addressLine2 ?: ""; city = it.city ?: ""; county = it.county ?: ""
            postcode = it.postcode ?: ""; country = it.country ?: ""; bio = it.bio ?: ""
            dob = it.dateOfBirth ?: ""; licenseNumber = it.licenseNumber ?: ""
            licencePassDate = it.licencePassDate ?: ""; occupation = it.occupation ?: ""
            prefVehicle = it.prefVehicle ?: ""; ownedVehicle = it.ownedVehicle ?: ""
            drivingConfidence = it.drivingConfidence ?: ""; vehicleKnowledge = it.vehicleKnowledgeLevel ?: ""
            primaryUse = it.primaryVehicleUse ?: "";
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

    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Image UI
        AsyncImage(model = signedImageUrl, contentDescription = "Profile Photo", modifier = Modifier.size(100.dp).align(Alignment.CenterHorizontally))
        Button(onClick = { pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Change Photo")
        }

        // All Fields
        OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = firstName, onValueChange = { firstName = it }, label = { Text("First Name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = lastName, onValueChange = { lastName = it }, label = { Text("Last Name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = phoneNumber, onValueChange = { phoneNumber = it }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = address1, onValueChange = { address1 = it }, label = { Text("Address Line 1") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = address2, onValueChange = { address2 = it }, label = { Text("Address Line 2") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("City") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = county, onValueChange = { county = it }, label = { Text("County") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = postcode, onValueChange = { postcode = it }, label = { Text("Postcode") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = country, onValueChange = { country = it }, label = { Text("Country") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = bio, onValueChange = { bio = it }, label = { Text("Bio") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = dob, onValueChange = { dob = it }, label = { Text("DOB") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = licenseNumber, onValueChange = { licenseNumber = it }, label = { Text("License Number") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = licencePassDate, onValueChange = { licencePassDate = it }, label = { Text("Pass Date") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = occupation, onValueChange = { occupation = it }, label = { Text("Occupation") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = prefVehicle, onValueChange = { prefVehicle = it }, label = { Text("Preferred Vehicle") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = ownedVehicle, onValueChange = { ownedVehicle = it }, label = { Text("Owned Vehicle") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = drivingConfidence, onValueChange = { drivingConfidence = it }, label = { Text("Driving Confidence") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = vehicleKnowledge, onValueChange = { vehicleKnowledge = it }, label = { Text("Vehicle Knowledge") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = primaryUse, onValueChange = { primaryUse = it }, label = { Text("Primary Use") }, modifier = Modifier.fillMaxWidth())


        Button(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            onClick = {
                val updated = profile?.copy(
                    username = username, email = email, firstName = firstName, lastName = lastName,
                    phoneNumber = phoneNumber, addressLine1 = address1, addressLine2 = address2,
                    city = city, county = county, postcode = postcode, country = country,
                    dateOfBirth = dob, licenseNumber = licenseNumber, licencePassDate = licencePassDate,
                    bio = bio, profileImageUrl = imageUrl, occupation = occupation,
                    prefVehicle = prefVehicle, ownedVehicle = ownedVehicle,
                    drivingConfidence = drivingConfidence, vehicleKnowledgeLevel = vehicleKnowledge,
                    primaryVehicleUse = primaryUse,
                )
                updated?.let { viewModel.updateProfile(it) }
                onNavigateBack()
            }
        ) {
            Text("Save Profile")
        }
    }
}
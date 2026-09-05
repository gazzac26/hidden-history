package com.hiddenhistory.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hiddenhistory.data.SupabaseManager
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.functions.functions
import io.ktor.client.call.body
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteAccountScreen(
    onNavigateBack: () -> Unit,
    onAccountDeleted: () -> Unit
) {
    var confirmationText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val requiredText = "DELETE"
    val isConfirmed = confirmationText.trim() == requiredText
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Delete Account",
                        color = Color.White
                    )
                },
                navigationIcon = {
                    TextButton(
                        onClick = onNavigateBack,
                        enabled = !isLoading
                    ) {
                        Text(
                            "Back",
                            color = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black
                )
            )
        },
        containerColor = Color.Black
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.Start
        ) {

            Icon(
                imageVector = Icons.Rounded.WarningAmber,
                contentDescription = "Warning",
                tint = Color(0xFFFF5252),
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "This action is permanent and cannot be undone.",
                style = TextStyle(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Deleting your account will immediately remove all your personal information, uploaded files, preferences, and data history from our servers.",
                style = TextStyle(
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    lineHeight = 20.sp
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Type \"$requiredText\" below to confirm:",
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = confirmationText,
                onValueChange = {
                    confirmationText = it
                    errorMessage = null
                },
                singleLine = true,
                enabled = !isLoading,
                placeholder = {
                    Text(
                        requiredText,
                        color = Color.White.copy(alpha = 0.38f)
                    )
                },
                textStyle = TextStyle(
                    color = Color.White
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFF5252),
                    unfocusedBorderColor = Color.DarkGray,
                    focusedContainerColor = Color(0xFF121212),
                    unfocusedContainerColor = Color(0xFF121212),
                    disabledContainerColor = Color(0xFF121212),
                    disabledBorderColor = Color.DarkGray,
                    disabledTextColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            )

            /*
             * -------------------------------------------------------------
             * DELETE ERROR
             * -------------------------------------------------------------
             */

            errorMessage?.let { message ->

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = message,
                    color = Color(0xFFFF5252),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            /*
             * -------------------------------------------------------------
             * DELETE ACCOUNT
             * -------------------------------------------------------------
             */

            Button(
                onClick = {

                    if (!isConfirmed || isLoading) {
                        return@Button
                    }

                    coroutineScope.launch {

                        isLoading = true
                        errorMessage = null

                        try {

                            /*
                             * Make sure the user actually has an
                             * authenticated session before attempting
                             * account deletion.
                             */

                            val session =
                                SupabaseManager.client.auth.currentSessionOrNull()

                            if (session == null) {

                                errorMessage =
                                    "Your session has expired. Please sign in again and try again."

                                isLoading = false
                                return@launch
                            }

                            /*
                             * -------------------------------------------------
                             * CALL DELETE-ACCOUNT EDGE FUNCTION
                             * -------------------------------------------------
                             *
                             * The Supabase client handles the authenticated
                             * request. The user's current session JWT is used
                             * for the Authorization header.
                             *
                             * The service_role key NEVER exists here.
                             */

                            val response =
                                SupabaseManager.client.functions.invoke(
                                    function = "delete-account"
                                )

                            /*
                             * The Edge Function returning successfully means
                             * the server completed the deletion.
                             */

                            isLoading = false

                            /*
                             * Do NOT attempt to perform another database
                             * deletion from Android.
                             *
                             * The server-side Edge Function owns the
                             * destructive operation.
                             */

                            onAccountDeleted()

                        } catch (e: Exception) {

                            isLoading = false

                            errorMessage =
                                "We couldn't delete your account. Please check your connection and try again."

                        }
                    }
                },
                enabled = isConfirmed && !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF5252),
                    disabledContainerColor = Color(0xFF2C2C2C)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {

                if (isLoading) {

                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )

                } else {

                    Text(
                        text = "Permanently Delete Account",
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            }
        }
    }
}
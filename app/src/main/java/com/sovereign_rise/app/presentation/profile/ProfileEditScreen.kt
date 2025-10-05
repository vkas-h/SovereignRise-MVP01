package com.sovereign_rise.app.presentation.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.sovereign_rise.app.domain.model.User
import com.sovereign_rise.app.presentation.viewmodel.ProfileUiState
import com.sovereign_rise.app.presentation.viewmodel.ProfileViewModel
import com.sovereign_rise.app.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Profile edit screen allowing users to update their username and profile picture.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    navController: NavController,
    viewModel: ProfileViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    
    // Local state for form
    var username by remember { mutableStateOf("") }
    var photoUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var initialUser by remember { mutableStateOf<User?>(null) }
    var hasUpdated by remember { mutableStateOf(false) }
    
    // Initialize form with current user data
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is ProfileUiState.Success -> {
                if (initialUser == null) {
                    // First time loading - set initial values
                    initialUser = state.user
                    username = state.user.username
                    photoUrl = state.user.profileImageUrl
                }
                isLoading = false
            }
            is ProfileUiState.Loading -> {
                isLoading = true
            }
            is ProfileUiState.Error -> {
                isLoading = false
                snackbarHostState.showSnackbar(
                    message = state.message,
                    duration = SnackbarDuration.Short
                )
            }
            else -> {}
        }
    }
    
    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // TODO: Upload to Firebase Storage and get URL
            // For now, show a message that this feature needs implementation
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = "Photo upload requires Firebase Storage integration. Feature coming soon!",
                    duration = SnackbarDuration.Long
                )
            }
            // Temporarily use the URI string (won't work for backend)
            // photoUrl = it.toString()
        }
    }
    
    // Validation
    val isUsernameValid = username.length in 3..20
    val canSave = isUsernameValid && !isLoading
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundDark,
                    titleContentColor = TextPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BackgroundDark
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            // Profile Picture Section
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .semantics { contentDescription = "Profile picture" }
            ) {
                // Profile Image
                if (photoUrl != null) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        color = Primary.copy(alpha = 0.2f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Default Profile",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            tint = Primary
                        )
                    }
                }
                
                // Camera Button
                FilledIconButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(48.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Change Photo",
                        tint = TextPrimary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Username Section
            Text(
                text = "Username",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
            
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Enter username", color = TextMuted) },
                singleLine = true,
                isError = username.isNotBlank() && !isUsernameValid,
                supportingText = {
                    if (username.isNotBlank() && !isUsernameValid) {
                        Text(
                            text = "Username must be 3-20 characters",
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text(
                            text = "${username.length}/20 characters",
                            color = TextMuted
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = TextMuted,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                enabled = !isLoading
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Save Button
            Button(
                onClick = {
                    focusManager.clearFocus()
                    // Compare with initial user state
                    initialUser?.let { initial ->
                        // Only update changed fields
                        val newUsername = if (username.trim() != initial.username && username.trim().isNotEmpty()) {
                            username.trim()
                        } else null
                        
                        val newPhotoUrl = if (photoUrl != initial.profileImageUrl) {
                            photoUrl
                        } else null
                        
                        if (newUsername != null || newPhotoUrl != null) {
                            hasUpdated = true
                            viewModel.updateProfile(
                                username = newUsername,
                                photoUrl = newPhotoUrl
                            )
                        } else {
                            // No changes made
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "No changes to save",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = canSave,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    contentColor = TextPrimary
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = TextPrimary
                    )
                } else {
                    Text("Save Changes")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Cancel Button
            OutlinedButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isLoading,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TextMuted
                )
            ) {
                Text("Cancel")
            }
            
            // Navigate back on successful update
            LaunchedEffect(uiState, hasUpdated) {
                if (hasUpdated && uiState is ProfileUiState.Success && !isLoading) {
                    // Show success message
                    snackbarHostState.showSnackbar(
                        message = "Profile updated successfully!",
                        duration = SnackbarDuration.Short
                    )
                    // Small delay before navigating back
                    kotlinx.coroutines.delay(500)
                    navController.popBackStack()
                }
            }
        }
    }
}


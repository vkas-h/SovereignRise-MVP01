package com.sovereign_rise.app.presentation.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.sovereign_rise.app.presentation.components.LoadingOverlay
import com.sovereign_rise.app.presentation.navigation.Screen
import com.sovereign_rise.app.presentation.viewmodel.AuthUiState
import com.sovereign_rise.app.presentation.viewmodel.AuthViewModel
import com.sovereign_rise.app.ui.theme.Primary
import com.sovereign_rise.app.ui.theme.Success
import com.sovereign_rise.app.ui.theme.TextMuted
import com.sovereign_rise.app.ui.theme.TextPrimary

/**
 * Screen for upgrading guest accounts to full accounts.
 */
@Composable
fun UpgradeAccountScreen(
    navController: NavController,
    viewModel: AuthViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    
    // Validation states
    val passwordsMatch = password == confirmPassword
    val isUsernameValid = username.length in 3..20
    val isEmailValid = email.contains("@")
    val isPasswordValid = password.length >= 8
    val canUpgrade = isUsernameValid && isEmailValid && isPasswordValid && passwordsMatch
    
    // Show Snackbar for errors and navigation
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is AuthUiState.Error -> {
                val result = snackbarHostState.showSnackbar(
                    message = state.message,
                    actionLabel = "Retry",
                    duration = SnackbarDuration.Long
                )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.resetState()
                }
            }
            is AuthUiState.Success -> {
                snackbarHostState.showSnackbar(
                    message = "Account upgraded successfully!",
                    duration = SnackbarDuration.Short
                )
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            }
            else -> {}
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
                    .imePadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
            // Title
            Text(
                text = "Upgrade Your Account",
                style = MaterialTheme.typography.headlineLarge,
                color = Primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Unlock all features and sync across devices",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Benefits Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Account upgrade benefits" },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "✨ Benefits you'll gain:",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    BenefitItem(
                        icon = "🏰",
                        text = "Join guilds and compete on leaderboards"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    BenefitItem(
                        icon = "🤝",
                        text = "Find accountability partners"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    BenefitItem(
                        icon = "☁️",
                        text = "Sync progress across devices"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    BenefitItem(
                        icon = "🏆",
                        text = "Participate in seasons and events"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    BenefitItem(
                        icon = "🔒",
                        text = "Secure account with password protection"
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Username TextField
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                supportingText = {
                    Text("${username.length}/20", color = TextMuted)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                isError = username.isNotBlank() && !isUsernameValid,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = TextMuted,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState !is AuthUiState.Loading
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Email TextField
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                isError = email.isNotBlank() && !isEmailValid,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = TextMuted,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState !is AuthUiState.Loading
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Password TextField
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                supportingText = {
                    Text("At least 8 characters", color = TextMuted)
                },
                singleLine = true,
                visualTransformation = if (passwordVisible) 
                    VisualTransformation.None 
                else 
                    PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) 
                                Icons.Filled.Visibility 
                            else 
                                Icons.Filled.VisibilityOff,
                            contentDescription = "Toggle password visibility"
                        )
                    }
                },
                isError = password.isNotBlank() && !isPasswordValid,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = TextMuted,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState !is AuthUiState.Loading
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Confirm Password TextField
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
                singleLine = true,
                visualTransformation = if (confirmPasswordVisible) 
                    VisualTransformation.None 
                else 
                    PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { 
                        if (canUpgrade) {
                            viewModel.upgradeAccount(username, email, password)
                        }
                    }
                ),
                trailingIcon = {
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(
                            imageVector = if (confirmPasswordVisible) 
                                Icons.Filled.Visibility 
                            else 
                                Icons.Filled.VisibilityOff,
                            contentDescription = "Toggle password visibility"
                        )
                    }
                },
                isError = confirmPassword.isNotBlank() && !passwordsMatch,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = TextMuted,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState !is AuthUiState.Loading
            )
            
            if (confirmPassword.isNotBlank() && !passwordsMatch) {
                Text(
                    text = "Passwords do not match",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Upgrade Button
            Button(
                onClick = { viewModel.upgradeAccount(username, email, password) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .semantics { contentDescription = "Upgrade account button" },
                enabled = uiState !is AuthUiState.Loading && canUpgrade,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    contentColor = TextPrimary
                )
            ) {
                Text("Upgrade Account")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Maybe Later Button
            TextButton(
                onClick = { navController.popBackStack() },
                enabled = uiState !is AuthUiState.Loading,
                modifier = Modifier.semantics { contentDescription = "Cancel and return" }
            ) {
                Text("Maybe Later", color = TextMuted)
            }
            }
            
            // Loading Overlay
            LoadingOverlay(
                isLoading = uiState is AuthUiState.Loading,
                message = "Upgrading account..."
            )
        }
    }
}

@Composable
private fun BenefitItem(icon: String, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Success
        )
    }
}

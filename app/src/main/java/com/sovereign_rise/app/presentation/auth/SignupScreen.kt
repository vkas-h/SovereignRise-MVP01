package com.sovereign_rise.app.presentation.auth

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.sovereign_rise.app.presentation.components.*
import com.sovereign_rise.app.presentation.navigation.Screen
import com.sovereign_rise.app.presentation.viewmodel.AuthUiState
import com.sovereign_rise.app.presentation.viewmodel.AuthViewModel
import com.sovereign_rise.app.ui.theme.*
import com.sovereign_rise.app.util.observeAsState

/**
 * Signup screen for creating new accounts.
 */
@Composable
fun SignupScreen(
    navController: NavController,
    viewModel: AuthViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val activity = LocalContext.current as Activity
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    
    // Observe connectivity
    val connectivityObserver = remember { com.sovereign_rise.app.di.AppModule.provideConnectivityObserver(context) }
    val connectivityStatus by connectivityObserver.observeAsState()
    val isOnline = connectivityStatus == com.sovereign_rise.app.util.ConnectivityStatus.AVAILABLE
    
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    
    // Reset auth state when screen is first displayed
    LaunchedEffect(Unit) {
        // Always check actual auth status from repository to ensure we have the latest state
        viewModel.checkAuthStatus()
    }
    
    // Validation states
    val passwordsMatch = password == confirmPassword
    val isUsernameValid = username.length in 3..20
    val isEmailValid = email.contains("@")
    val isPasswordValid = password.length >= 8
    val canSignup = isUsernameValid && isEmailValid && isPasswordValid && passwordsMatch
    
    // Password strength calculation
    val passwordStrength = remember(password) {
        calculatePasswordStrength(password)
    }
    
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
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            }
            else -> {}
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        BackgroundPrimary,
                        Secondary.copy(alpha = 0.1f),
                        BackgroundSecondary
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.huge)
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Title
            Text(
                text = "Create Account",
                style = Typography.displaySmall,
                fontFamily = ManropeFontFamily,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            
            Spacer(modifier = Modifier.height(Spacing.small))
            
            Text(
                text = "Join the rise to sovereignty",
                style = Typography.bodyLarge,
                color = TextSecondary
            )
            
            Spacer(modifier = Modifier.height(Spacing.massive))
            
            // Offline warning banner
            if (!isOnline) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Warning.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CloudOff,
                            contentDescription = "Offline",
                            tint = Warning,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "No internet connection. Account registration requires internet.",
                            style = Typography.bodySmall,
                            color = TextPrimary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(Spacing.default))
            }
            
            // Username TextField
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username", style = Typography.bodyMedium) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                },
                supportingText = {
                    Text(
                        text = if (username.isNotBlank() && !isUsernameValid) {
                            "Username must be 3-20 characters"
                        } else {
                            "${username.length}/20"
                        },
                        style = Typography.bodySmall.copy(fontFamily = MonoFontFamily),
                        color = if (username.isNotBlank() && !isUsernameValid) Error else TextTertiary
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                isError = username.isNotBlank() && !isUsernameValid,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = BorderMedium,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedLabelColor = Primary,
                    unfocusedLabelColor = TextSecondary
                ),
                textStyle = Typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp),
                enabled = uiState !is AuthUiState.Loading
            )
            
            Spacer(modifier = Modifier.height(Spacing.default))
            
            // Email TextField
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email", style = Typography.bodyMedium) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Email,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                isError = email.isNotBlank() && !isEmailValid,
                supportingText = {
                    if (email.isNotBlank() && !isEmailValid) {
                        Text("Please enter a valid email address", style = Typography.bodySmall, color = Error)
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = BorderMedium,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedLabelColor = Primary,
                    unfocusedLabelColor = TextSecondary
                ),
                textStyle = Typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp),
                enabled = uiState !is AuthUiState.Loading
            )
            
            Spacer(modifier = Modifier.height(Spacing.default))
            
            // Password TextField
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password", style = Typography.bodyMedium) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                },
                supportingText = {
                    Text("At least 8 characters", style = Typography.bodySmall, color = TextTertiary)
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
                    IconButton(
                        onClick = { passwordVisible = !passwordVisible },
                        modifier = Modifier.semantics { contentDescription = "Toggle password visibility" }
                    ) {
                        Icon(
                            imageVector = if (passwordVisible) 
                                Icons.Outlined.Visibility 
                            else 
                                Icons.Outlined.VisibilityOff,
                            contentDescription = "Toggle password visibility"
                        )
                    }
                },
                isError = password.isNotBlank() && !isPasswordValid,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = BorderMedium,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedLabelColor = Primary,
                    unfocusedLabelColor = TextSecondary
                ),
                textStyle = Typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .semantics { contentDescription = "Password input" },
                enabled = uiState !is AuthUiState.Loading
            )
            
            // Password Strength Indicator
            if (password.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.small))
                AnimatedProgressBar(
                    progress = passwordStrength.progress,
                    height = 8.dp,
                    gradientColors = when {
                        passwordStrength.progress < 0.3f -> listOf(Error, Warning)
                        passwordStrength.progress < 0.7f -> listOf(Warning, AccentAmber)
                        else -> listOf(Success, Primary)
                    },
                    showShimmer = passwordStrength.progress >= 0.7f
                )
            }
            
            Spacer(modifier = Modifier.height(Spacing.default))
            
            // Confirm Password TextField
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password", style = Typography.bodyMedium) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                },
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
                        if (canSignup) {
                            viewModel.register(username, email, password)
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
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = BorderMedium,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedLabelColor = Primary,
                    unfocusedLabelColor = TextSecondary
                ),
                textStyle = Typography.bodyLarge,
                supportingText = if (confirmPassword.isNotBlank() && !passwordsMatch) {
                    { Text("Passwords do not match", style = Typography.bodySmall, color = Error) }
                } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp),
                enabled = uiState !is AuthUiState.Loading
            )
            
            Spacer(modifier = Modifier.height(Spacing.large))
            
            // Create Account Button
            GradientButton(
                text = "Create Account",
                onClick = { viewModel.register(username, email, password) },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState !is AuthUiState.Loading && canSignup && isOnline,
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.PersonAdd,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            )
            
            Spacer(modifier = Modifier.height(Spacing.default))
            
            // Google Sign-Up Button
            OutlinedButton(
                onClick = { viewModel.loginWithGoogle(activity) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = uiState !is AuthUiState.Loading && isOnline,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TextPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.Psychology,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.small))
                Text("Sign up with Google", style = Typography.labelLarge)
            }
            
            Spacer(modifier = Modifier.height(Spacing.default))
            
            // Login Link
            TextButton(
                onClick = { navController.popBackStack() },
                enabled = uiState !is AuthUiState.Loading
            ) {
                Text("Already have an account? Log in", style = Typography.bodyMedium, color = Primary)
            }
        }
            
        // Loading Overlay
        LoadingOverlay(
            isLoading = uiState is AuthUiState.Loading,
            message = "Creating account..."
        )
        
        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(Spacing.default)
        )
    }
}

@Composable
private fun PasswordStrengthIndicator(strength: PasswordStrength) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Password strength",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
            Text(
                text = strength.label,
                style = MaterialTheme.typography.labelSmall,
                color = strength.color
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { strength.progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = strength.color
        )
    }
}

private data class PasswordStrength(
    val label: String,
    val progress: Float,
    val color: Color
)

private fun calculatePasswordStrength(password: String): PasswordStrength {
    if (password.isEmpty()) {
        return PasswordStrength("None", 0f, Color.Gray)
    }
    
    var score = 0
    
    // Length check
    when {
        password.length >= 12 -> score += 3
        password.length >= 8 -> score += 2
        password.length >= 6 -> score += 1
    }
    
    // Character variety checks
    if (password.any { it.isUpperCase() }) score++
    if (password.any { it.isLowerCase() }) score++
    if (password.any { it.isDigit() }) score++
    if (password.any { !it.isLetterOrDigit() }) score++
    
    return when {
        score >= 7 -> PasswordStrength("Strong", 1f, Success)
        score >= 5 -> PasswordStrength("Good", 0.75f, Info)
        score >= 3 -> PasswordStrength("Fair", 0.5f, Warning)
        else -> PasswordStrength("Weak", 0.25f, Danger)
    }
}

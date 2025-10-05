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
 * Login screen for email/password, Google Sign-In, and guest mode.
 */
@Composable
fun LoginScreen(
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
    
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showGuestDialog by remember { mutableStateOf(false) }
    
    // Reset auth state when screen is first displayed to prevent auto-navigation after logout
    LaunchedEffect(Unit) {
        // Always check actual auth status from repository to ensure we have the latest state
        viewModel.checkAuthStatus()
    }
    
    // Show Snackbar for errors
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
            is AuthUiState.GuestMode -> {
                showGuestDialog = true
            }
            else -> {}
        }
    }
    
    // Show guest mode dialog
    if (showGuestDialog && uiState is AuthUiState.GuestMode) {
        GuestModeDialog(
            onDismiss = { showGuestDialog = false },
            onUpgrade = {
                showGuestDialog = false
                navController.navigate(Screen.UpgradeAccount.route)
            },
            onContinue = {
                showGuestDialog = false
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            }
        )
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        BackgroundPrimary,
                        Primary.copy(alpha = 0.1f),
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
            // App Logo and Title with modern typography
            Text(
                text = "Welcome Back",
                style = Typography.displaySmall,
                fontFamily = ManropeFontFamily,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            
            Spacer(modifier = Modifier.height(Spacing.small))
            
            Text(
                text = "Master yourself, rise above",
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
                            text = "No internet connection. Login requires internet for first-time users.",
                            style = Typography.bodySmall,
                            color = TextPrimary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(Spacing.default))
            }
            
            // Email TextField with modern styling
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email", style = Typography.bodyMedium) },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Email,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
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
                    .height(64.dp)
                    .semantics { contentDescription = "Email address input" },
                enabled = uiState !is AuthUiState.Loading
            )
            
            Spacer(modifier = Modifier.height(Spacing.default))
            
            // Password TextField with modern styling
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password", style = Typography.bodyMedium) },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                },
                visualTransformation = if (passwordVisible) 
                    VisualTransformation.None 
                else 
                    PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { viewModel.login(email, password) }
                ),
                trailingIcon = {
                    IconButton(
                        onClick = { passwordVisible = !passwordVisible },
                        modifier = Modifier.semantics { 
                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                        }
                    ) {
                        Icon(
                            imageVector = if (passwordVisible) 
                                Icons.Outlined.Visibility 
                            else 
                                Icons.Outlined.VisibilityOff,
                            contentDescription = if (passwordVisible) 
                                "Hide password" 
                            else 
                                "Show password"
                        )
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
                    .height(64.dp)
                    .semantics { contentDescription = "Password input" },
                enabled = uiState !is AuthUiState.Loading
            )
            
            Spacer(modifier = Modifier.height(Spacing.large))
            
            // Login Button with GradientButton
            GradientButton(
                text = "Sign In",
                onClick = { viewModel.login(email, password) },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Login button" },
                enabled = uiState !is AuthUiState.Loading && 
                         email.isNotBlank() && 
                         password.isNotBlank() &&
                         isOnline,
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.Login,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            )
            
            Spacer(modifier = Modifier.height(Spacing.default))
            
            // Google Sign-In Button
            OutlinedButton(
                onClick = { viewModel.loginWithGoogle(activity) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .semantics { contentDescription = "Sign in with Google button" },
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
                Text("Sign in with Google", style = Typography.labelLarge)
            }
            
            Spacer(modifier = Modifier.height(Spacing.default))
            
            // Guest Mode Button (works offline)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TextButton(
                    onClick = { viewModel.loginAsGuest() },
                    enabled = uiState !is AuthUiState.Loading,
                    modifier = Modifier.semantics { contentDescription = "Continue as guest" }
                ) {
                    Text("Continue as Guest", style = Typography.bodyMedium, color = TextSecondary)
                }
                
                if (!isOnline) {
                    Text(
                        "✓ Guest mode works offline",
                        style = Typography.bodySmall,
                        color = Success
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.small))
            
            // Sign Up Link
            TextButton(
                onClick = { navController.navigate(Screen.Signup.route) },
                enabled = uiState !is AuthUiState.Loading,
                modifier = Modifier.semantics { contentDescription = "Go to sign up" }
            ) {
                Text("Don't have an account? Sign up", style = Typography.bodyMedium, color = Primary)
            }
        }
            
        // Loading Overlay
        LoadingOverlay(
            isLoading = uiState is AuthUiState.Loading,
            message = "Signing in..."
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

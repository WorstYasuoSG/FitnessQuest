package com.example.fitnessquest

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AnimatedFitnessQuestLogo(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.05f, 
        animationSpec = infiniteRepeatable(animation = tween(1200, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse), label = "LogoPulse"
    )
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    Canvas(modifier = modifier.scale(scale)) {
        val w = size.width; val h = size.height
        drawCircle(Brush.linearGradient(listOf(primaryColor, secondaryColor), Offset(0f, 0f), Offset(w, h)), w / 2)
        drawCircle(tertiaryColor, w * 0.15f, Offset(w * 0.7f, h * 0.35f))
        val path = Path().apply {
            moveTo(w * 0.15f, h * 0.6f)
            lineTo(w * 0.35f, h * 0.6f)
            lineTo(w * 0.5f, h * 0.25f) 
            lineTo(w * 0.65f, h * 0.8f) 
            lineTo(w * 0.75f, h * 0.55f)
            lineTo(w * 0.85f, h * 0.55f)
        }
        drawPath(path, Color.White, style = Stroke(w * 0.08f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun LoginScreen(onAuthenticate: (username: String, email: String?, password: String, isSignUp: Boolean) -> Unit,
                externalError: String? = null,
                isLoading: Boolean = false) {
    var isLoginMode by remember { mutableStateOf(true) }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedFitnessQuestLogo(modifier = Modifier.size(120.dp))

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            "FitnessQuest",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            if (isLoginMode) "Welcome back, Adventurer!" else "Begin your journey today",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(48.dp))

        // USERNAME
        OutlinedTextField(
            value = username,
            onValueChange = { username = it; errorMessage = null },
            label = { Text(if (isLoginMode) "Email or Adventurer Name" else "Adventurer Name") },            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // EMAIL (ONLY FOR SIGNUP)
        if (!isLoginMode) {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; errorMessage = null },
                label = { Text("Email Address") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // PASSWORD
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; errorMessage = null },
            label = { Text("Secret Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(12.dp)
        )

        if (errorMessage != null) {
            Text(
                errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp).align(Alignment.Start)
            )
        }
        LaunchedEffect(externalError) {
            if (!externalError.isNullOrBlank()) errorMessage = externalError
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            if (isLoginMode) "Don't have an account? Sign up here"
            else "Already have an account? Log in here",
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .clickable {
                    isLoginMode = !isLoginMode
                    errorMessage = null
                }
                .padding(8.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                when {
                    username.isBlank() ->
                        errorMessage = "Please enter an Adventurer Name."

                    password.isBlank() ->
                        errorMessage = "Please enter a Password."

                    !isLoginMode && email.isBlank() ->
                        errorMessage = "Please enter an Email Address."

                    else -> {
                        onAuthenticate(
                            username.trim(),
                            if (isLoginMode) null else email.trim(),   // email only for signup
                            password,
                            !isLoginMode
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(0.dp)
        ) {
            Text(
                if (isLoginMode) "Login" else "Create Account",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
package com.boksl.running.ui.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import com.boksl.running.ui.common.AppPrimaryButton
import com.boksl.running.ui.common.AppSectionCard
import com.boksl.running.ui.common.AppUiTokens
import com.boksl.running.ui.common.appScreenModifier

@Composable
fun onboardingScreen(
    onStartClick: () -> Unit,
    modifier: Modifier = Modifier,
    uiState: OnboardingScreenState = OnboardingScreenState(),
) {
    Scaffold(
        modifier = modifier,
        containerColor = AppUiTokens.Background,
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .then(appScreenModifier()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AppSectionCard {
                Text(
                    text = "복슬달리기",
                    style = MaterialTheme.typography.headlineMedium,
                    color = AppUiTokens.TextPrimary,
                )
                Text(
                    text = uiState.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = AppUiTokens.TextSecondary,
                )
                AppPrimaryButton(
                    text = uiState.buttonLabel,
                    onClick = onStartClick,
                )
            }
        }
    }
}

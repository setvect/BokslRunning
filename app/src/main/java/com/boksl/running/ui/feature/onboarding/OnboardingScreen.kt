package com.boksl.running.ui.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class OnboardingUiState(
    val title: String = "러닝 경로/시간/페이스를 기록합니다",
    val buttonLabel: String = "시작하기",
)

@Composable
fun onboardingScreen(
    onStartClick: () -> Unit,
    modifier: Modifier = Modifier,
    uiState: OnboardingUiState = OnboardingUiState(),
) {
    Scaffold(modifier = modifier) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = uiState.title,
                style = MaterialTheme.typography.headlineSmall,
            )
            Button(
                onClick = onStartClick,
                modifier = Modifier.padding(top = 24.dp),
            ) {
                Text(text = uiState.buttonLabel)
            }
        }
    }
}

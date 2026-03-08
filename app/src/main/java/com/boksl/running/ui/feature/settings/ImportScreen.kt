package com.boksl.running.ui.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.boksl.running.domain.model.ImportProgress

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun importScreen(
    uiState: ImportUiState,
    actions: ImportScreenActions,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "가져오기") },
                navigationIcon = {
                    TextButton(onClick = actions.onNavigateUp) {
                        Text(text = "뒤로")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            importProgressContent(progress = uiState.progress, actions = actions)
        }
    }
}

@Composable
private fun importProgressContent(
    progress: ImportProgress,
    actions: ImportScreenActions,
) {
    when (progress) {
        ImportProgress.Idle -> importIdleContent(actions = actions)
        ImportProgress.BackingUp -> importRunningContent(message = "1/2 내부 백업 중…", actions = actions)
        ImportProgress.Importing -> importRunningContent(message = "2/2 데이터 병합 중…", actions = actions)
        is ImportProgress.Completed -> importCompletedContent(progress = progress, actions = actions)
        is ImportProgress.Error -> importErrorContent(progress = progress, actions = actions)
    }
}

@Composable
private fun importIdleContent(actions: ImportScreenActions) {
    importInfoCard()
    Button(onClick = actions.onStartImport, modifier = Modifier.fillMaxWidth()) {
        Text(text = "가져오기 시작")
    }
}

@Composable
private fun importRunningContent(
    message: String,
    actions: ImportScreenActions,
) {
    importInfoCard()
    progressCard(message = message)
    Button(onClick = actions.onCancelImport, modifier = Modifier.fillMaxWidth()) {
        Text(text = "취소")
    }
}

@Composable
private fun importCompletedContent(
    progress: ImportProgress.Completed,
    actions: ImportScreenActions,
) {
    importCompletedCard(progress = progress)
    Button(onClick = actions.onConfirm, modifier = Modifier.fillMaxWidth()) {
        Text(text = "확인")
    }
    Button(onClick = actions.onNavigateHome, modifier = Modifier.fillMaxWidth()) {
        Text(text = "홈")
    }
}

@Composable
private fun importCompletedCard(progress: ImportProgress.Completed) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (progress.result.wasDuplicateFile) {
                Text(
                    text = "이미 가져온 파일입니다",
                    style = MaterialTheme.typography.titleMedium,
                )
            } else {
                Text(
                    text = "가져오기 완료",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text =
                        "추가 ${progress.result.addedSessionCount}건, " +
                            "중복 ${progress.result.duplicateSessionCount}건",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (progress.result.appliedProfile) {
                    Text(
                        text = "프로필과 앱 설정도 함께 복원했습니다.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun importErrorContent(
    progress: ImportProgress.Error,
    actions: ImportScreenActions,
) {
    importInfoCard()
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "가져오기 실패",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
            )
            Text(
                text = progress.message,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
    Button(onClick = actions.onStartImport, modifier = Modifier.fillMaxWidth()) {
        Text(text = "다시 시도")
    }
    TextButton(onClick = actions.onNavigateUp, modifier = Modifier.fillMaxWidth()) {
        Text(text = "뒤로")
    }
}

@Composable
private fun importInfoCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "가져오기 전에 현재 데이터를 내부 백업합니다",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "정책: 기존 데이터 유지 + 가져온 데이터 병합",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun progressCard(message: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "선택한 JSON 파일을 검증하고 현재 데이터와 병합하고 있습니다.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

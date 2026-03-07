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
import com.boksl.running.domain.model.ExportProgress

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun exportScreen(
    uiState: ExportUiState,
    onNavigateUp: () -> Unit,
    onStartExport: () -> Unit,
    onCancelExport: () -> Unit,
    onShareExport: () -> Unit,
    onNavigateHome: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "전체 내보내기") },
                navigationIcon = {
                    TextButton(onClick = onNavigateUp) {
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
            when (val progress = uiState.progress) {
                ExportProgress.Idle -> {
                    exportInfoCard()
                    Button(onClick = onStartExport, modifier = Modifier.fillMaxWidth()) {
                        Text(text = "내보내기 시작")
                    }
                }

                is ExportProgress.Running -> {
                    exportInfoCard()
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "${progress.totalSessions}개 중 ${progress.completedSessions}개 생성 중…",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = "모든 러닝 기록, 프로필, 앱 설정을 JSON 파일로 만들고 있습니다.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    Button(onClick = onCancelExport, modifier = Modifier.fillMaxWidth()) {
                        Text(text = "취소")
                    }
                }

                is ExportProgress.Completed -> {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "내보내기 완료",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = "JSON 파일이 준비되었습니다. 공유 시트에서 파일 앱, 드라이브 등으로 저장할 수 있습니다.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    Button(onClick = onShareExport, modifier = Modifier.fillMaxWidth()) {
                        Text(text = "파일 공유/저장")
                    }
                    Button(onClick = onNavigateHome, modifier = Modifier.fillMaxWidth()) {
                        Text(text = "홈")
                    }
                    TextButton(onClick = onStartExport, modifier = Modifier.fillMaxWidth()) {
                        Text(text = "다시 내보내기")
                    }
                }

                is ExportProgress.Error -> {
                    exportInfoCard()
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "내보내기 실패",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                            Text(
                                text = progress.message,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    Button(onClick = onStartExport, modifier = Modifier.fillMaxWidth()) {
                        Text(text = "다시 시도")
                    }
                    TextButton(onClick = onNavigateUp, modifier = Modifier.fillMaxWidth()) {
                        Text(text = "뒤로")
                    }
                }
            }
        }
    }
}

@Composable
private fun exportInfoCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "모든 러닝 기록을 파일로 생성합니다",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "형식: bokslrunning_export_v1.json",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

package com.boksl.running.ui.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.boksl.running.domain.model.Gender

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun profileFormScreen(
    uiState: ProfileFormUiState,
    actions: ProfileFormActions,
    onNavigateUp: (() -> Unit)?,
) {
    var expanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = uiState.entryPoint.toTitle()) },
                navigationIcon = {
                    if (onNavigateUp != null) {
                        TextButton(onClick = onNavigateUp) {
                            Text(text = "뒤로")
                        }
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
                    .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "몸무게, 성별, 나이를 입력하면 기록과 칼로리 계산에 사용됩니다.",
                style = MaterialTheme.typography.bodyMedium,
            )
            profileWeightField(uiState = uiState, onWeightChanged = actions.onWeightChanged)
            profileGenderField(
                selectedGender = uiState.gender,
                expanded = expanded,
                onExpandedChange = { expanded = it },
                onGenderChanged = actions.onGenderChanged,
            )
            profileAgeField(uiState = uiState, onAgeChanged = actions.onAgeChanged)
            Button(
                onClick = actions.onSaveClick,
                enabled = uiState.isSaveEnabled,
            ) {
                Text(text = "저장")
            }
        }
    }
}

@Composable
private fun profileWeightField(
    uiState: ProfileFormUiState,
    onWeightChanged: (String) -> Unit,
) {
    OutlinedTextField(
        value = uiState.weightKg,
        onValueChange = onWeightChanged,
        label = { Text(text = "몸무게(kg)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        isError = uiState.weightError != null,
        supportingText = {
            uiState.weightError?.let { Text(text = it) }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun profileGenderField(
    selectedGender: Gender,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onGenderChanged: (Gender) -> Unit,
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
    ) {
        OutlinedTextField(
            value = selectedGender.toDisplayName(),
            onValueChange = {},
            readOnly = true,
            label = { Text(text = "성별") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier =
                Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            Gender.entries.forEach { gender ->
                DropdownMenuItem(
                    text = { Text(text = gender.toDisplayName()) },
                    onClick = {
                        onGenderChanged(gender)
                        onExpandedChange(false)
                    },
                )
            }
        }
    }
}

@Composable
private fun profileAgeField(
    uiState: ProfileFormUiState,
    onAgeChanged: (String) -> Unit,
) {
    OutlinedTextField(
        value = uiState.age,
        onValueChange = onAgeChanged,
        label = { Text(text = "나이") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        isError = uiState.ageError != null,
        supportingText = {
            uiState.ageError?.let { Text(text = it) }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun ProfileSetupEntryPoint.toTitle(): String =
    if (this == ProfileSetupEntryPoint.Settings) {
        "프로필 정보 수정"
    } else {
        "프로필 입력"
    }

private fun Gender.toDisplayName(): String =
    when (this) {
        Gender.MALE -> "남성"
        Gender.FEMALE -> "여성"
        Gender.OTHER -> "기타"
        Gender.UNSPECIFIED -> "선택 안 함"
    }

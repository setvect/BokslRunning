package com.boksl.running.ui.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.boksl.running.domain.model.Gender
import com.boksl.running.ui.common.AppPrimaryButton
import com.boksl.running.ui.common.AppScreenHeader
import com.boksl.running.ui.common.AppSectionCard
import com.boksl.running.ui.common.AppUiTokens
import com.boksl.running.ui.common.appScreenModifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun profileFormScreen(
    uiState: ProfileFormUiState,
    actions: ProfileFormActions,
    onNavigateUp: (() -> Unit)?,
) {
    var expanded by remember { mutableStateOf(false) }

    Scaffold(containerColor = AppUiTokens.Background) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .then(appScreenModifier())
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AppUiTokens.ScreenSpacing),
        ) {
            AppScreenHeader(
                title = uiState.entryPoint.toTitle(),
                onNavigateUp = onNavigateUp,
            )
            AppSectionCard {
                Text(
                    text = "입력한 프로필 정보는 칼로리 계산에 사용됩니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppUiTokens.TextSecondary,
                )
                profileWeightField(uiState = uiState, onWeightChanged = actions.onWeightChanged)
                profileGenderField(
                    selectedGender = uiState.gender,
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    onGenderChanged = actions.onGenderChanged,
                )
                profileAgeField(uiState = uiState, onAgeChanged = actions.onAgeChanged)
                AppPrimaryButton(
                    text = "저장",
                    onClick = actions.onSaveClick,
                    enabled = uiState.isSaveEnabled,
                )
            }
        }
    }
}

@Composable
private fun profileWeightField(
    uiState: ProfileFormUiState,
    onWeightChanged: (String) -> Unit,
) {
    AppOutlinedField(
        value = uiState.weightKg,
        onValueChange = onWeightChanged,
        label = "몸무게(kg)",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        error = uiState.weightError,
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
        AppOutlinedField(
            value = selectedGender.toDisplayName(),
            onValueChange = {},
            label = "성별",
            readOnly = true,
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
            listOf(Gender.MALE, Gender.FEMALE).forEach { gender ->
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
    AppOutlinedField(
        value = uiState.age,
        onValueChange = onAgeChanged,
        label = "나이",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        error = uiState.ageError,
    )
}

@Composable
private fun AppOutlinedField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    error: String? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        readOnly = readOnly,
        label = { Text(text = label) },
        trailingIcon = trailingIcon,
        keyboardOptions = keyboardOptions,
        isError = error != null,
        supportingText = {
            error?.let {
                Text(
                    text = it,
                    color = AppUiTokens.Error,
                )
            }
        },
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedContainerColor = AppUiTokens.SurfaceMuted,
                unfocusedContainerColor = AppUiTokens.SurfaceMuted,
                focusedBorderColor = AppUiTokens.Accent,
                unfocusedBorderColor = AppUiTokens.Divider,
                cursorColor = AppUiTokens.Accent,
                focusedTextColor = AppUiTokens.TextPrimary,
                unfocusedTextColor = AppUiTokens.TextPrimary,
                focusedLabelColor = AppUiTokens.TextSecondary,
                unfocusedLabelColor = AppUiTokens.TextSecondary,
                focusedTrailingIconColor = AppUiTokens.TextSecondary,
                unfocusedTrailingIconColor = AppUiTokens.TextSecondary,
            ),
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

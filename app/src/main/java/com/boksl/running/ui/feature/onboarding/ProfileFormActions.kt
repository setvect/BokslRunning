package com.boksl.running.ui.feature.onboarding

import com.boksl.running.domain.model.Gender

data class ProfileFormActions(
    val onWeightChanged: (String) -> Unit,
    val onGenderChanged: (Gender) -> Unit,
    val onAgeChanged: (String) -> Unit,
    val onSaveClick: () -> Unit,
)

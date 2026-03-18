package org.kalpeshbkundanani.burnmate.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import org.kalpeshbkundanani.burnmate.presentation.onboarding.OnboardingEvent
import org.kalpeshbkundanani.burnmate.presentation.onboarding.OnboardingField
import org.kalpeshbkundanani.burnmate.presentation.onboarding.OnboardingUiState
import org.kalpeshbkundanani.burnmate.ui.atoms.PrimaryButton
import org.kalpeshbkundanani.burnmate.ui.molecules.InputField
import org.kalpeshbkundanani.burnmate.ui.theme.BurnMateColors
import org.kalpeshbkundanani.burnmate.ui.theme.BurnMateTypography
import org.kalpeshbkundanani.burnmate.ui.theme.Spacing

@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    onEvent: (OnboardingEvent) -> Unit
) {
    Scaffold(
        containerColor = BurnMateColors.BackgroundPrimary
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BurnMateColors.BackgroundPrimary)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.Large),
            verticalArrangement = Arrangement.spacedBy(Spacing.Large)
        ) {
            Text(
                text = "Welcome to BURNMATE.",
                style = BurnMateTypography.displayMedium,
                color = BurnMateColors.TextPrimary
            )
            
            Text(
                text = "Let's personalize your metabolic engine.",
                style = BurnMateTypography.bodyLarge,
                color = BurnMateColors.TextSecondary
            )
            
            Spacer(modifier = Modifier.height(Spacing.Medium))
            
            val heightError = state.fieldErrors[OnboardingField.HEIGHT]
            InputField(
                value = state.heightInput,
                onValueChange = { onEvent(OnboardingEvent.HeightChanged(it)) },
                label = "HEIGHT (cm)",
                placeholder = "e.g. 180",
                keyboardType = KeyboardType.Number,
                isError = heightError != null,
                errorMessage = heightError?.message
            )
            
            val currentWeightError = state.fieldErrors[OnboardingField.CURRENT_WEIGHT]
            InputField(
                value = state.currentWeightInput,
                onValueChange = { onEvent(OnboardingEvent.CurrentWeightChanged(it)) },
                label = "CURRENT WEIGHT (kg)",
                placeholder = "e.g. 85.0",
                keyboardType = KeyboardType.Decimal,
                isError = currentWeightError != null,
                errorMessage = currentWeightError?.message
            )
            
            val goalWeightError = state.fieldErrors[OnboardingField.GOAL_WEIGHT]
            InputField(
                value = state.goalWeightInput,
                onValueChange = { onEvent(OnboardingEvent.GoalWeightChanged(it)) },
                label = "GOAL WEIGHT (kg)",
                placeholder = "e.g. 75.0",
                keyboardType = KeyboardType.Decimal,
                isError = goalWeightError != null,
                errorMessage = goalWeightError?.message,
                supportingMessage = state.goalWeightSuggestion?.message
            )
            
            if (state.submitError != null) {
                Text(
                    text = state.submitError.message,
                    style = BurnMateTypography.bodyMedium,
                    color = BurnMateColors.Error,
                    modifier = Modifier.padding(vertical = Spacing.Small)
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            PrimaryButton(
                text = if (state.isSubmitting) "SAVING..." else "COMPLETE PROFILE",
                onClick = { onEvent(OnboardingEvent.Submit) },
                enabled = state.isSubmitEnabled && !state.isSubmitting,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

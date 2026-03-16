package org.kalpeshbkundanani.burnmate.profile.domain

import kotlin.math.floor
import org.kalpeshbkundanani.burnmate.profile.model.BmiCategory
import org.kalpeshbkundanani.burnmate.profile.model.BmiSnapshot

class DefaultBmiCalculator : BmiCalculator {
    override fun calculate(heightCm: Double, weightKg: Double): BmiSnapshot {
        val heightMeters = heightCm / 100.0
        val rawBmi = weightKg / (heightMeters * heightMeters)
        val roundedBmi = floor((rawBmi * 10.0) + 0.5) / 10.0
        return BmiSnapshot(
            value = roundedBmi,
            category = classify(roundedBmi)
        )
    }

    override fun classify(bmiValue: Double): BmiCategory = when {
        bmiValue < 18.5 -> BmiCategory.UNDERWEIGHT
        bmiValue <= 24.9 -> BmiCategory.HEALTHY
        bmiValue <= 29.9 -> BmiCategory.OVERWEIGHT
        else -> BmiCategory.OBESE
    }
}

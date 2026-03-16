package org.kalpeshbkundanani.burnmate.profile.domain

import org.kalpeshbkundanani.burnmate.profile.model.BmiCategory
import org.kalpeshbkundanani.burnmate.profile.model.BmiSnapshot

interface BmiCalculator {
    fun calculate(heightCm: Double, weightKg: Double): BmiSnapshot
    fun classify(bmiValue: Double): BmiCategory
}

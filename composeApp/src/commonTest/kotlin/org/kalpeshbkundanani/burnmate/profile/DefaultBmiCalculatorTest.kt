package org.kalpeshbkundanani.burnmate.profile

import kotlin.test.Test
import kotlin.test.assertEquals
import org.kalpeshbkundanani.burnmate.profile.domain.DefaultBmiCalculator
import org.kalpeshbkundanani.burnmate.profile.model.BmiCategory

class DefaultBmiCalculatorTest {

    private val calculator = DefaultBmiCalculator()

    @Test
    fun bmiHelperReturnsDeterministicCurrentAndGoalValues() {
        val currentFirst = calculator.calculate(heightCm = 180.0, weightKg = 90.0)
        val currentSecond = calculator.calculate(heightCm = 180.0, weightKg = 90.0)
        val goalFirst = calculator.calculate(heightCm = 180.0, weightKg = 78.0)
        val goalSecond = calculator.calculate(heightCm = 180.0, weightKg = 78.0)

        assertEquals(27.8, currentFirst.value)
        assertEquals(27.8, currentSecond.value)
        assertEquals(24.1, goalFirst.value)
        assertEquals(24.1, goalSecond.value)
        assertEquals(currentFirst, currentSecond)
        assertEquals(goalFirst, goalSecond)
    }

    @Test
    fun bmiCategoriesMapCorrectlyAtThresholds() {
        assertEquals(BmiCategory.UNDERWEIGHT, calculator.classify(18.4))
        assertEquals(BmiCategory.HEALTHY, calculator.classify(18.5))
        assertEquals(BmiCategory.HEALTHY, calculator.classify(24.9))
        assertEquals(BmiCategory.OVERWEIGHT, calculator.classify(25.0))
        assertEquals(BmiCategory.OBESE, calculator.classify(30.0))
    }
}

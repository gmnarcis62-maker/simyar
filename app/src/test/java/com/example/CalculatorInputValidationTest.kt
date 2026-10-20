package com.example

import com.example.calc.CableSizingCalculator
import com.example.calc.VoltageDropCalculator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Regression tests for invalid calculator input handling. */
class CalculatorInputValidationTest {
    @Test
    fun cableSizingRejectsNonFiniteCurrent() {
        val result = CableSizingCalculator.calculateCableSize(
            CableSizingCalculator.CableSizingInput(designCurrentIbAmp = Double.NaN)
        )
        assertTrue(result.isFailure)
    }

    @Test
    fun cableSizingDoesNotSilentlyIgnoreEmptyCustomTable() {
        val result = CableSizingCalculator.calculateCableSize(
            CableSizingCalculator.CableSizingInput(
                designCurrentIbAmp = 10.0,
                useCustomCapacityTable = true,
                customCapacityTable = emptyList()
            )
        )
        assertTrue(result.isFailure)
    }

    @Test
    fun cableSizingRejectsInvalidCustomTableEntries() {
        val result = CableSizingCalculator.calculateCableSize(
            CableSizingCalculator.CableSizingInput(
                designCurrentIbAmp = 10.0,
                useCustomCapacityTable = true,
                customCapacityTable = listOf(
                    CableSizingCalculator.CableCapacityEntry(sectionMm2 = -2.5, currentAmp = 20.0)
                )
            )
        )
        assertTrue(result.isFailure)
    }

    @Test
    fun cableSizingRejectsNonPositiveGroupingCountInsteadOfSilentlyCoercing() {
        val result = CableSizingCalculator.calculateCableSize(
            CableSizingCalculator.CableSizingInput(designCurrentIbAmp = 10.0, groupedCircuitsCount = 0)
        )
        assertTrue(result.isFailure)
    }

    @Test
    fun voltageDropRejectsNonFiniteCurrent() {
        val result = VoltageDropCalculator.calculateVoltageDrop(
            VoltageDropCalculator.VoltageDropInput(
                currentAmp = Double.POSITIVE_INFINITY,
                lengthMeters = 30.0,
                crossSectionMm2 = 4.0
            )
        )
        assertTrue(result.isFailure)
    }

    @Test
    fun voltageDropRejectsInvalidCustomLimit() {
        val result = VoltageDropCalculator.calculateVoltageDrop(
            VoltageDropCalculator.VoltageDropInput(
                currentAmp = 10.0,
                lengthMeters = 30.0,
                crossSectionMm2 = 4.0,
                customAllowablePercent = Double.NaN
            )
        )
        assertTrue(result.isFailure)
    }

    @Test
    fun validVoltageDropInputStillProducesFiniteResult() {
        val result = VoltageDropCalculator.calculateVoltageDrop(
            VoltageDropCalculator.VoltageDropInput(
                currentAmp = 10.0,
                lengthMeters = 30.0,
                crossSectionMm2 = 4.0
            )
        )
        assertTrue(result.isSuccess)
        val value = result.getOrThrow()
        assertTrue(value.deltaUVolt.isFinite())
        assertTrue(value.deltaUPercent.isFinite())
        assertFalse(value.recommendedMinSectionMm2?.isFinite() == false)
    }
}

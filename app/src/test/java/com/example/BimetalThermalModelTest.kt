package com.example

import com.example.simulation.BimetalThermalModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BimetalThermalModelTest {
    @Test
    fun nonFiniteOverloadRatioDoesNotProduceTripTime() {
        assertNull(BimetalThermalModel.calculateIecTripTime(Float.POSITIVE_INFINITY, "10"))
        assertNull(BimetalThermalModel.calculateIecTripTime(Float.NaN, "10"))
    }

    @Test
    fun invalidTimeInputsDoNotAdvanceThermalState() {
        val cases = listOf(
            Pair(Float.NaN, 1f),
            Pair(Float.POSITIVE_INFINITY, 1f),
            Pair(-1f, 1f),
            Pair(1f, Float.NaN),
            Pair(1f, Float.POSITIVE_INFINITY),
            Pair(1f, -1f)
        )
        for ((delta, acceleration) in cases) {
            val (state, tripped) = BimetalThermalModel.stepThermalModel(
                currentThermalState = 0.4f,
                overloadRatio = 7.2f,
                tripClass = "10",
                deltaSeconds = delta,
                timeAccelerationFactor = acceleration
            )
            assertEquals("زمان یا شتاب نامعتبر نباید حرارت را جلو ببرد", 0.4f, state, 0.0001f)
            assertFalse("زمان یا شتاب نامعتبر نباید باعث تریپ جدید شود", tripped)
        }
    }

    @Test
    fun nonFiniteOverloadKeepsFiniteStateUnchanged() {
        val (state, tripped) = BimetalThermalModel.stepThermalModel(
            currentThermalState = 0.65f,
            overloadRatio = Float.POSITIVE_INFINITY,
            tripClass = "10",
            deltaSeconds = 5f
        )
        assertEquals(0.65f, state, 0.0001f)
        assertFalse(tripped)
    }

    @Test
    fun invalidStoredThermalStateIsSanitized() {
        val (state, tripped) = BimetalThermalModel.stepThermalModel(
            currentThermalState = Float.NaN,
            overloadRatio = 1.0f,
            tripClass = "10",
            deltaSeconds = 0f
        )
        assertTrue(state.isFinite())
        assertEquals(0f, state, 0.0001f)
        assertFalse(tripped)
    }
}

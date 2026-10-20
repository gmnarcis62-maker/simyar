package com.example

import com.example.model.CircuitComponent
import com.example.model.CircuitReferenceValidator
import com.example.model.ComponentType
import com.example.model.WireConnection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CircuitReferenceValidatorTest {
    private fun component(id: String) = CircuitComponent(
        id = id, type = ComponentType.PUSH_BUTTON_NO, label = id, x = 0f, y = 0f
    )

    @Test
    fun reportsMissingComponentAndTerminalWithoutChangingInput() {
        val wire = WireConnection(
            id = "w1", fromComponentId = "a", fromTerminalId = "13",
            toComponentId = "missing", toTerminalId = "14"
        )
        val original = listOf(wire)
        val issues = CircuitReferenceValidator.validate(listOf(component("a")), original)
        assertTrue(issues.any { it.type == CircuitReferenceValidator.IssueType.MISSING_TO_COMPONENT })
        assertEquals(original, listOf(wire))
    }

    @Test
    fun reportsDuplicateConnectionRegardlessOfDirection() {
        val forward = WireConnection("w1", "a", "13", "b", "14")
        val reverse = WireConnection("w2", "b", "14", "a", "13")
        val issues = CircuitReferenceValidator.validate(listOf(component("a"), component("b")), listOf(forward, reverse))
        assertTrue(issues.any { it.type == CircuitReferenceValidator.IssueType.DUPLICATE_CONNECTION })
    }

    @Test
    fun reportsSelfConnection() {
        val wire = WireConnection("w1", "a", "13", "a", "14")
        val issues = CircuitReferenceValidator.validate(listOf(component("a")), listOf(wire))
        assertTrue(issues.any { it.type == CircuitReferenceValidator.IssueType.SELF_CONNECTION })
    }
}

package com.example.model

/**
 * اعتبارسنجی غیرمخرب ارجاع‌های سیم‌کشی.
 * این ابزار فقط گزارش تولید می‌کند؛ داده ورودی را اصلاح یا حذف نمی‌کند.
 */
object CircuitReferenceValidator {
    enum class IssueType {
        DUPLICATE_COMPONENT_ID,
        DUPLICATE_WIRE_ID,
        MISSING_FROM_COMPONENT,
        MISSING_TO_COMPONENT,
        MISSING_FROM_TERMINAL,
        MISSING_TO_TERMINAL,
        SELF_CONNECTION,
        DUPLICATE_CONNECTION
    }

    data class Issue(
        val type: IssueType,
        val wireId: String? = null,
        val componentId: String? = null,
        val message: String
    )

    /**
     * بررسی ارجاع‌ها بدون حدس‌زدن ترمینال جایگزین.
     * اتصالات تکراری با جهت معکوس نیز یک اتصال تکراری محسوب می‌شوند.
     */
    fun validate(
        components: List<CircuitComponent>,
        wires: List<WireConnection>
    ): List<Issue> {
        val issues = mutableListOf<Issue>()
        val componentGroups = components.groupBy { it.id }
        componentGroups.filterValues { it.size > 1 }.keys.forEach { id ->
            issues += Issue(
                type = IssueType.DUPLICATE_COMPONENT_ID,
                componentId = id,
                message = "شناسه قطعه تکراری است: $id"
            )
        }

        val wireGroups = wires.groupBy { it.id }
        wireGroups.filterValues { it.size > 1 }.keys.forEach { id ->
            issues += Issue(
                type = IssueType.DUPLICATE_WIRE_ID,
                wireId = id,
                message = "شناسه سیم تکراری است: $id"
            )
        }

        val componentMap = components.groupBy { it.id }
        val seenConnections = mutableSetOf<Set<String>>()
        wires.forEach { wire ->
            val fromCandidates = componentMap[wire.fromComponentId].orEmpty()
            val toCandidates = componentMap[wire.toComponentId].orEmpty()

            if (fromCandidates.isEmpty()) {
                issues += Issue(IssueType.MISSING_FROM_COMPONENT, wire.id, wire.fromComponentId,
                    "قطعه مبدأ سیم ${wire.id} وجود ندارد")
            }
            if (toCandidates.isEmpty()) {
                issues += Issue(IssueType.MISSING_TO_COMPONENT, wire.id, wire.toComponentId,
                    "قطعه مقصد سیم ${wire.id} وجود ندارد")
            }
            if (fromCandidates.size == 1 && fromCandidates.single().getTerminals().none { it.id == wire.fromTerminalId }) {
                issues += Issue(IssueType.MISSING_FROM_TERMINAL, wire.id, wire.fromComponentId,
                    "ترمینال مبدأ ${wire.fromTerminalId} در سیم ${wire.id} وجود ندارد")
            }
            if (toCandidates.size == 1 && toCandidates.single().getTerminals().none { it.id == wire.toTerminalId }) {
                issues += Issue(IssueType.MISSING_TO_TERMINAL, wire.id, wire.toComponentId,
                    "ترمینال مقصد ${wire.toTerminalId} در سیم ${wire.id} وجود ندارد")
            }
            if (wire.fromComponentId == wire.toComponentId) {
                issues += Issue(IssueType.SELF_CONNECTION, wire.id, wire.fromComponentId,
                    "سیم ${wire.id} به همان قطعه متصل شده است")
            }

            val endpointA = "${wire.fromComponentId}:${wire.fromTerminalId}"
            val endpointB = "${wire.toComponentId}:${wire.toTerminalId}"
            val key = setOf(endpointA, endpointB)
            if (!seenConnections.add(key)) {
                issues += Issue(IssueType.DUPLICATE_CONNECTION, wire.id,
                    message = "اتصال تکراری برای سیم ${wire.id} شناسایی شد")
            }
        }
        return issues
    }
}

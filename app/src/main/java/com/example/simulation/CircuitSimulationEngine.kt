package com.example.simulation

import com.example.model.CircuitComponent
import com.example.model.ComponentType
import com.example.model.MotorDirection
import com.example.model.WireConnection
import java.util.LinkedList
import java.util.Queue

/**
 * موتور شبیه‌سازی پیشرفته و کاملاً مستقل از UI مدار برق صنعتی سیمیار:
 * - حل دقیق گراف تداوم جریان الکتریکی (Continuity BFS)
 * - انتشار مجزا و مستقل فازهای شبکه سه‌فاز (L1, L2, L3) و ولتاژهای تک‌فاز و DC
 * - تعیین جهت چرخش واقعی الکتروموتور از روی توالی فازهای U/V/W (جابجایی دو فاز = معکوس شدن جهت)
 * - تشخیص اتصال کوتاه فاز به نول و فاز به فاز (L1-L2, L2-L3, L1-L3)
 * - تشخیص اینترلاک متقابل حفاظتی منحصراً با فیلد interlockTag بدون شروط سخت‌کد
 * - گزارش دقیق پایانه‌های ناقص متصل شده (مدار باز واقعی بدون آلارم کاذب)
 * - شبیه‌سازی دقیق تاخیر در قطع (TOF) و حسگر مجاورتی القایی (Proximity Sensor)
 */
class CircuitSimulationEngine {

    enum class PhaseType {
        L1,
        L2,
        L3,
        DC_POS
    }

    data class SimStepResult(
        val updatedComponents: List<CircuitComponent>,
        val updatedWires: List<WireConnection>,
        val energizedCoilTags: Set<String>,
        val isShortCircuit: Boolean,
        val isInterlockViolation: Boolean,
        val isOpenCircuit: Boolean,
        val logMessage: String,
        val executionTimeMs: Long,
        val activePhaseL1Pins: Set<String> = emptySet(),
        val activePhaseL2Pins: Set<String> = emptySet(),
        val activePhaseL3Pins: Set<String> = emptySet(),
        val activeNeutralPins: Set<String> = emptySet(),
        val activeDcPosPins: Set<String> = emptySet(),
        val educationalDisclaimer: String = com.example.model.EDUCATIONAL_SIMULATION_DISCLAIMER
    )

    fun stepSimulation(
        components: List<CircuitComponent>,
        wires: List<WireConnection>,
        deltaSeconds: Float = 0.05f
    ): SimStepResult {
        val startTime = System.currentTimeMillis()
        // ورودی زمان می‌تواند از تنظیمات سرعت شبیه‌سازی یا وضعیت بازیابی‌شده بیاید.
        // زمان نامعتبر نباید پیشرفت تایمرها را به NaN/Infinity تبدیل کند یا تایمر را ناخواسته جلو ببرد.
        val safeDeltaSeconds = deltaSeconds.takeIf { it.isFinite() && it >= 0f } ?: 0f

        // ۱. شناسایی پایانه‌های منابع تغذیه
        val l1Sources = mutableListOf<String>()
        val l2Sources = mutableListOf<String>()
        val l3Sources = mutableListOf<String>()
        val dcPosSources = mutableListOf<String>()
        val neutralSources = mutableListOf<String>()

        for (comp in components) {
            when (comp.type) {
                ComponentType.THREE_PHASE_SOURCE -> {
                    l1Sources.add("${comp.id}:L1")
                    l2Sources.add("${comp.id}:L2")
                    l3Sources.add("${comp.id}:L3")
                }
                ComponentType.AC_PHASE -> {
                    // فاز متناوب تک‌فاز به عنوان L1 شبکه عمل می‌کند
                    l1Sources.add("${comp.id}:L")
                }
                ComponentType.DC_PLUS -> {
                    dcPosSources.add("${comp.id}:+24V")
                }
                ComponentType.AC_NEUTRAL -> {
                    neutralSources.add("${comp.id}:N")
                }
                ComponentType.DC_MINUS -> {
                    neutralSources.add("${comp.id}:0V")
                }
                else -> {}
            }
        }

        // ۲. ساخت گراف اتصالات پین به پین
        val adjacency = mutableMapOf<String, MutableList<String>>()

        fun addEdge(node1: String, node2: String) {
            adjacency.computeIfAbsent(node1) { mutableListOf() }.add(node2)
            adjacency.computeIfAbsent(node2) { mutableListOf() }.add(node1)
        }

        for (wire in wires) {
            val fromKey = "${wire.fromComponentId}:${wire.fromTerminalId}"
            val toKey = "${wire.toComponentId}:${wire.toTerminalId}"
            addEdge(fromKey, toKey)
        }

        // ۳. هدایت جریان از داخل قطعات الکتریکی و کلیدها
        for (comp in components) {
            when (comp.type) {
                ComponentType.PUSH_BUTTON_NO -> {
                    if (comp.isClosedOrPressed) addEdge("${comp.id}:13", "${comp.id}:14")
                }
                ComponentType.PUSH_BUTTON_NC -> {
                    if (!comp.isClosedOrPressed) addEdge("${comp.id}:21", "${comp.id}:22")
                }
                ComponentType.EMERGENCY_STOP -> {
                    if (!comp.isClosedOrPressed) addEdge("${comp.id}:1", "${comp.id}:2")
                }
                ComponentType.CIRCUIT_BREAKER_MCB, ComponentType.FUSE, ComponentType.SELECTOR_SWITCH_2POS -> {
                    if (comp.isClosedOrPressed) addEdge("${comp.id}:IN", "${comp.id}:OUT")
                }
                ComponentType.CIRCUIT_BREAKER_MCB_3P, ComponentType.CIRCUIT_BREAKER_MCCB_3P, ComponentType.FUSE_3P -> {
                    if (comp.isClosedOrPressed) {
                        addEdge("${comp.id}:1", "${comp.id}:2")
                        addEdge("${comp.id}:3", "${comp.id}:4")
                        addEdge("${comp.id}:5", "${comp.id}:6")
                        addEdge("${comp.id}:L1", "${comp.id}:T1")
                        addEdge("${comp.id}:L2", "${comp.id}:T2")
                        addEdge("${comp.id}:L3", "${comp.id}:T3")
                    }
                }
                ComponentType.RCD_4P -> {
                    if (comp.isClosedOrPressed) {
                        addEdge("${comp.id}:1", "${comp.id}:2")
                        addEdge("${comp.id}:3", "${comp.id}:4")
                        addEdge("${comp.id}:5", "${comp.id}:6")
                        addEdge("${comp.id}:N", "${comp.id}:N_OUT")
                        addEdge("${comp.id}:L1", "${comp.id}:T1")
                        addEdge("${comp.id}:L2", "${comp.id}:T2")
                        addEdge("${comp.id}:L3", "${comp.id}:T3")
                    }
                }
                ComponentType.CONTROL_RELAY_1CO -> {
                    val active = comp.isEnergized || comp.isClosedOrPressed
                    if (active) {
                        addEdge("${comp.id}:11", "${comp.id}:14")
                    } else {
                        addEdge("${comp.id}:11", "${comp.id}:12")
                    }
                }
                ComponentType.CONTROL_RELAY_2CO -> {
                    val active = comp.isEnergized || comp.isClosedOrPressed
                    if (active) {
                        addEdge("${comp.id}:11", "${comp.id}:14")
                        addEdge("${comp.id}:21", "${comp.id}:24")
                    } else {
                        addEdge("${comp.id}:11", "${comp.id}:12")
                        addEdge("${comp.id}:21", "${comp.id}:22")
                    }
                }
                ComponentType.PHASE_SEQUENCE_RELAY -> {
                    if (comp.isEnergized) {
                        addEdge("${comp.id}:15", "${comp.id}:18")
                    } else {
                        addEdge("${comp.id}:15", "${comp.id}:16")
                    }
                }
                ComponentType.BIMETAL_OVERLOAD -> {
                    // تا زمانی که بی‌متال تریپ نخورده، کنتاکت فرمان ۹۵-۹۶ بسته است
                    val isTripped = comp.isBimetalTripped || comp.isClosedOrPressed
                    if (!isTripped) {
                        // کنتاکت فرمان NC در حالت عادی بسته است.
                        addEdge("${comp.id}:95", "${comp.id}:96")
                        // مسیر قدرت ۳ فاز بی‌متال (در صورت سیم‌بندی)
                        addEdge("${comp.id}:1", "${comp.id}:2")
                        addEdge("${comp.id}:3", "${comp.id}:4")
                        addEdge("${comp.id}:5", "${comp.id}:6")
                        addEdge("${comp.id}:L1", "${comp.id}:T1")
                        addEdge("${comp.id}:L2", "${comp.id}:T2")
                        addEdge("${comp.id}:L3", "${comp.id}:T3")
                    } else {
                        // کنتاکت اعلام تریپ NO (97-98) فقط هنگام تریپ بسته می‌شود.
                        addEdge("${comp.id}:97", "${comp.id}:98")
                    }
                }
                ComponentType.CONTACTOR_NO_AUX, ComponentType.TIMER_NO_CONTACT -> {
                    if (comp.isClosedOrPressed) {
                        addEdge("${comp.id}:13", "${comp.id}:14")
                        addEdge("${comp.id}:17", "${comp.id}:18")
                    }
                }
                ComponentType.CONTACTOR_NC_AUX, ComponentType.TIMER_NC_CONTACT -> {
                    if (!comp.isClosedOrPressed) {
                        addEdge("${comp.id}:21", "${comp.id}:22")
                        addEdge("${comp.id}:15", "${comp.id}:16")
                    }
                }
                ComponentType.LIMIT_SWITCH_NO -> {
                    if (comp.isClosedOrPressed) addEdge("${comp.id}:IN", "${comp.id}:OUT")
                }
                ComponentType.LIMIT_SWITCH_NC -> {
                    if (!comp.isClosedOrPressed) addEdge("${comp.id}:IN", "${comp.id}:OUT")
                }
                ComponentType.MAIN_POWER_CONTACTS_3P -> {
                    if (comp.isClosedOrPressed) {
                        addEdge("${comp.id}:L1", "${comp.id}:T1")
                        addEdge("${comp.id}:L2", "${comp.id}:T2")
                        addEdge("${comp.id}:L3", "${comp.id}:T3")
                    }
                }
                ComponentType.PROXIMITY_SENSOR -> {
                    // سنسور القایی مجاورتی: اگر تحریک شود و سوئیچ فعال باشد، سیگنال خروجی را وصل می‌کند
                    if (comp.isClosedOrPressed) {
                        addEdge("${comp.id}:V+", "${comp.id}:OUT")
                    }
                }
                else -> {}
            }
        }

        // ۴. انتشار مجزا و مستقل فازها با BFS
        fun runBfs(roots: List<String>): Set<String> {
            val visited = mutableSetOf<String>()
            val queue: Queue<String> = LinkedList()
            for (r in roots) {
                visited.add(r)
                queue.add(r)
            }
            while (queue.isNotEmpty()) {
                val curr = queue.poll() ?: continue
                val neighbors = adjacency[curr] ?: emptyList()
                for (nb in neighbors) {
                    if (nb !in visited) {
                        visited.add(nb)
                        queue.add(nb)
                    }
                }
            }
            return visited
        }

        val nodesL1 = runBfs(l1Sources)
        val nodesL2 = runBfs(l2Sources)
        val nodesL3 = runBfs(l3Sources)
        val nodesDcPos = runBfs(dcPosSources)
        val neutralNodes = runBfs(neutralSources)

        val allLiveNodes = nodesL1 + nodesL2 + nodesL3 + nodesDcPos

        // ۵. تشخیص خطای اتصال کوتاه (اتصال کوتاه فاز-نول و فاز-فاز)
        var isShortCircuit = false
        var shortCircuitMessage = ""

        // الف) اتصال کوتاه مستقیم هر فاز به نول
        for (nNode in neutralNodes) {
            if (nNode in nodesL1 || nNode in nodesL2 || nNode in nodesL3 || nNode in nodesDcPos) {
                isShortCircuit = true
                shortCircuitMessage = "خطای بحرانی: اتصال کوتاه مستقیم فاز به نول رخ داد! کلید قطع شد."
                break
            }
        }

        // ب) اتصال کوتاه فاز به فاز (L1 با L2، یا L2 با L3، یا L1 با L3 در یک گره مشترک)
        if (!isShortCircuit) {
            val crossL1L2 = nodesL1.intersect(nodesL2)
            val crossL2L3 = nodesL2.intersect(nodesL3)
            val crossL1L3 = nodesL1.intersect(nodesL3)

            if (crossL1L2.isNotEmpty()) {
                isShortCircuit = true
                shortCircuitMessage = "خطای بحرانی: اتصال کوتاه شدید بین فاز L1 و فاز L2 رخ داد!"
            } else if (crossL2L3.isNotEmpty()) {
                isShortCircuit = true
                shortCircuitMessage = "خطای بحرانی: اتصال کوتاه شدید بین فاز L2 و فاز L3 رخ داد!"
            } else if (crossL1L3.isNotEmpty()) {
                isShortCircuit = true
                shortCircuitMessage = "خطای بحرانی: اتصال کوتاه شدید بین فاز L1 و فاز L3 رخ داد!"
            }
        }

        val newlyEnergizedCoils = mutableSetOf<String>()

        // ۶. فعال‌سازی بوبین‌ها و بارها
        val updatedComponents = components.map { comp ->
            when (comp.type) {
                ComponentType.CONTACTOR_COIL -> {
                    val p1 = "${comp.id}:A1"
                    val p2 = "${comp.id}:A2"
                    val active = (p1 in allLiveNodes && p2 in neutralNodes) ||
                            (p2 in allLiveNodes && p1 in neutralNodes)
                    if (active && comp.tagBinding.isNotBlank()) {
                        newlyEnergizedCoils.add(comp.tagBinding)
                    }
                    comp.copy(isEnergized = active)
                }

                ComponentType.TIMER_ON_DELAY_COIL -> {
                    val p1 = "${comp.id}:A1"
                    val p2 = "${comp.id}:A2"
                    val active = (p1 in allLiveNodes && p2 in neutralNodes) ||
                            (p2 in allLiveNodes && p1 in neutralNodes)
                    var newProgress = comp.currentTimerProgress
                    var isTimeout = comp.isEnergized
                    if (active) {
                        newProgress += safeDeltaSeconds
                        if (newProgress >= comp.delaySeconds) {
                            newProgress = comp.delaySeconds
                            isTimeout = true
                            if (comp.tagBinding.isNotBlank()) newlyEnergizedCoils.add(comp.tagBinding)
                        }
                    } else {
                        newProgress = 0f
                        isTimeout = false
                    }
                    comp.copy(isEnergized = isTimeout, currentTimerProgress = newProgress)
                }

                ComponentType.TIMER_OFF_DELAY_COIL -> {
                    // تایمر تاخیر در قطع (TOF) واقعی:
                    // با رسیدن ولتاژ فورا وصل می‌شود و پیشرفت تایمر صفر است
                    // با قطع ولتاژ ورودی، تا زمان اتمام delaySeconds روشن باقی می‌ماند
                    val p1 = "${comp.id}:A1"
                    val p2 = "${comp.id}:A2"
                    val activeInput = (p1 in allLiveNodes && p2 in neutralNodes) ||
                            (p2 in allLiveNodes && p1 in neutralNodes)

                    var newProgress = comp.currentTimerProgress
                    var isOutputEnergized: Boolean

                    if (activeInput) {
                        newProgress = 0f
                        isOutputEnergized = true
                        if (comp.tagBinding.isNotBlank()) newlyEnergizedCoils.add(comp.tagBinding)
                    } else {
                        if (comp.isEnergized && comp.delaySeconds > 0f) {
                            newProgress += safeDeltaSeconds
                            if (newProgress < comp.delaySeconds) {
                                isOutputEnergized = true
                                if (comp.tagBinding.isNotBlank()) newlyEnergizedCoils.add(comp.tagBinding)
                            } else {
                                newProgress = comp.delaySeconds
                                isOutputEnergized = false
                            }
                        } else {
                            newProgress = 0f
                            isOutputEnergized = false
                        }
                    }
                    comp.copy(isEnergized = isOutputEnergized, currentTimerProgress = newProgress)
                }

                ComponentType.PILOT_LAMP_GREEN, ComponentType.PILOT_LAMP_RED,
                ComponentType.PILOT_LAMP_YELLOW, ComponentType.ALARM_BUZZER -> {
                    val p1 = "${comp.id}:X1"
                    val p2 = "${comp.id}:X2"
                    val active = (p1 in allLiveNodes && p2 in neutralNodes) ||
                            (p2 in allLiveNodes && p1 in neutralNodes)
                    comp.copy(isEnergized = active)
                }

                ComponentType.MOTOR_3PHASE -> {
                    // موتور سه‌فاز فقط زمانی روشن است که مسیر قدرت واقعی به پایانه‌ها برسد
                    val uPin = "${comp.id}:U"
                    val vPin = "${comp.id}:V"
                    val wPin = "${comp.id}:W"

                    fun getPhaseForPin(pin: String): PhaseType? {
                        return when {
                            pin in nodesL1 -> PhaseType.L1
                            pin in nodesL2 -> PhaseType.L2
                            pin in nodesL3 -> PhaseType.L3
                            else -> null
                        }
                    }

                    val phaseU = getPhaseForPin(uPin)
                    val phaseV = getPhaseForPin(vPin)
                    val phaseW = getPhaseForPin(wPin)

                    var isRunning = false
                    var direction = MotorDirection.STOPPED

                    if (phaseU != null && phaseV != null && phaseW != null &&
                        setOf(phaseU, phaseV, phaseW).size == 3
                    ) {
                        // هر ۳ فاز L1, L2, L3 مستقل به موتور متصل شده‌اند
                        isRunning = true
                        // بررسی توالی فازها (توالی مستقیم = راستگرد، جابجایی دو فاز = چپگرد)
                        val isClockwiseSequence = (phaseU == PhaseType.L1 && phaseV == PhaseType.L2 && phaseW == PhaseType.L3) ||
                                (phaseU == PhaseType.L2 && phaseV == PhaseType.L3 && phaseW == PhaseType.L1) ||
                                (phaseU == PhaseType.L3 && phaseV == PhaseType.L1 && phaseW == PhaseType.L2)

                        direction = if (isClockwiseSequence) MotorDirection.CLOCKWISE else MotorDirection.COUNTER_CLOCKWISE
                    } else if (uPin in allLiveNodes && (wPin in neutralNodes || vPin in neutralNodes)) {
                        // اتصال تست تک‌فاز
                        isRunning = true
                        direction = MotorDirection.CLOCKWISE
                    } else if (wPin in allLiveNodes && (uPin in neutralNodes || vPin in neutralNodes)) {
                        isRunning = true
                        direction = MotorDirection.COUNTER_CLOCKWISE
                    }

                    comp.copy(isEnergized = isRunning, motorRotationDirection = direction)
                }

                ComponentType.MOTOR_1PHASE -> {
                    val p1 = "${comp.id}:L"
                    val p2 = "${comp.id}:N"
                    val isRunning = (p1 in allLiveNodes && p2 in neutralNodes) ||
                            (p2 in allLiveNodes && p1 in neutralNodes)
                    comp.copy(
                        isEnergized = isRunning,
                        motorRotationDirection = if (isRunning) MotorDirection.CLOCKWISE else MotorDirection.STOPPED
                    )
                }

                ComponentType.PROXIMITY_SENSOR -> {
                    // سنسور القایی با تغذیه مثبت و منفی روشن می‌شود
                    val isPowered = ("${comp.id}:V+" in allLiveNodes && "${comp.id}:GND" in neutralNodes)
                    comp.copy(isEnergized = isPowered && comp.isClosedOrPressed)
                }

                ComponentType.CONTROL_RELAY_1CO, ComponentType.CONTROL_RELAY_2CO -> {
                    val p1 = "${comp.id}:A1"
                    val p2 = "${comp.id}:A2"
                    val active = (p1 in allLiveNodes && p2 in neutralNodes) ||
                            (p2 in allLiveNodes && p1 in neutralNodes) ||
                            (p1 in nodesDcPos && p2 in neutralNodes) ||
                            (p2 in nodesDcPos && p1 in neutralNodes)
                    if (active && comp.tagBinding.isNotBlank()) {
                        newlyEnergizedCoils.add(comp.tagBinding)
                    }
                    comp.copy(isEnergized = active)
                }

                ComponentType.PHASE_SEQUENCE_RELAY -> {
                    val pL1 = "${comp.id}:L1"
                    val pL2 = "${comp.id}:L2"
                    val pL3 = "${comp.id}:L3"

                    fun getPhaseForPin(pin: String): PhaseType? {
                        return when {
                            pin in nodesL1 -> PhaseType.L1
                            pin in nodesL2 -> PhaseType.L2
                            pin in nodesL3 -> PhaseType.L3
                            else -> null
                        }
                    }

                    val ph1 = getPhaseForPin(pL1)
                    val ph2 = getPhaseForPin(pL2)
                    val ph3 = getPhaseForPin(pL3)

                    // تمام ۳ فاز باید حاضر باشند و توالی صحیح فاز (L1-L2-L3) برقرار باشد
                    val allPhasesPresent = (ph1 != null && ph2 != null && ph3 != null) &&
                            setOf(ph1, ph2, ph3).size == 3
                    val isCorrectSequence = allPhasesPresent && (
                            (ph1 == PhaseType.L1 && ph2 == PhaseType.L2 && ph3 == PhaseType.L3) ||
                            (ph1 == PhaseType.L2 && ph2 == PhaseType.L3 && ph3 == PhaseType.L1) ||
                            (ph1 == PhaseType.L3 && ph2 == PhaseType.L1 && ph3 == PhaseType.L2)
                    )

                    if (isCorrectSequence && comp.tagBinding.isNotBlank()) {
                        newlyEnergizedCoils.add(comp.tagBinding)
                    }
                    comp.copy(isEnergized = isCorrectSequence)
                }

                else -> comp
            }
        }

        // ۷. بررسی اینترلاک حفاظتی منحصراً با فیلد interlockTag
        var isInterlockViolation = false
        val violatingTags = mutableSetOf<String>()
        var interlockErrorMessage = ""

        for (comp in updatedComponents) {
            if (comp.isEnergized && comp.interlockTag.isNotBlank() && comp.interlockTag in newlyEnergizedCoils) {
                val conflictingComp = updatedComponents.find { it.tagBinding == comp.interlockTag && it.isEnergized }
                if (conflictingComp != null) {
                    isInterlockViolation = true
                    violatingTags.add(comp.tagBinding)
                    violatingTags.add(comp.interlockTag)
                    interlockErrorMessage = "خطای اینترلاک حفاظتی: بوبین‌های [${comp.tagBinding}] و [${conflictingComp.tagBinding}] همزمان برق‌دار شدند!"
                }
            }
        }

        // در صورت نقض اینترلاک، بوبین‌های درگیر خنثی می‌شوند
        val sanitizedEnergizedCoils = if (isInterlockViolation) {
            newlyEnergizedCoils.filter { it !in violatingTags }.toMutableSet()
        } else {
            newlyEnergizedCoils
        }

        // ۸. همگام‌سازی وضعیت تیغه‌ها با بوبین مربوطه
        val finalComponents = updatedComponents.map { comp ->
            if (isInterlockViolation && comp.type in listOf(ComponentType.CONTACTOR_COIL, ComponentType.TIMER_ON_DELAY_COIL, ComponentType.TIMER_OFF_DELAY_COIL)) {
                if (comp.tagBinding.isNotBlank() && comp.tagBinding in violatingTags) {
                    comp.copy(isEnergized = false)
                } else {
                    comp
                }
            } else {
                comp
            }
        }.map { comp ->
            when (comp.type) {
                ComponentType.CONTACTOR_NO_AUX, ComponentType.TIMER_NO_CONTACT, ComponentType.MAIN_POWER_CONTACTS_3P -> {
                    val parentActive = comp.tagBinding.isNotBlank() && comp.tagBinding in sanitizedEnergizedCoils
                    comp.copy(isClosedOrPressed = parentActive)
                }
                ComponentType.CONTACTOR_NC_AUX, ComponentType.TIMER_NC_CONTACT -> {
                    val parentActive = comp.tagBinding.isNotBlank() && comp.tagBinding in sanitizedEnergizedCoils
                    // تیغه بسته NC با برق‌دار شدن بوبین باز می‌شود
                    comp.copy(isClosedOrPressed = parentActive)
                }
                ComponentType.CONTROL_RELAY_1CO, ComponentType.CONTROL_RELAY_2CO -> {
                    val parentActive = comp.tagBinding.isNotBlank() && comp.tagBinding in sanitizedEnergizedCoils
                    val active = comp.isEnergized || parentActive
                    comp.copy(isEnergized = active, isClosedOrPressed = active)
                }
                else -> comp
            }
        }

        // ۹. به‌روزرسانی وضعیت برق‌دار بودن سیم‌ها
        val updatedWires = wires.map { wire ->
            val fromPin = "${wire.fromComponentId}:${wire.fromTerminalId}"
            val toPin = "${wire.toComponentId}:${wire.toTerminalId}"
            val energized = fromPin in allLiveNodes || toPin in allLiveNodes
            wire.copy(isEnergized = energized)
        }

        // ۱۰. تشخیص صحیح «مدار باز» بر اساس پایانه‌های ناقص سیم‌کشی شده
        // فقط در صورتی مدار باز است که باری دارای سیم ورودی باشد ولی ترمینال برگشت آن کاملاً بی‌اتصال رها شده باشد
        var isOpenCircuit = false
        var openCircuitMessage = ""

        if (!isShortCircuit && !isInterlockViolation) {
            for (comp in components) {
                if (comp.type in listOf(
                        ComponentType.CONTACTOR_COIL,
                        ComponentType.TIMER_ON_DELAY_COIL,
                        ComponentType.TIMER_OFF_DELAY_COIL,
                        ComponentType.PILOT_LAMP_GREEN,
                        ComponentType.PILOT_LAMP_RED,
                        ComponentType.PILOT_LAMP_YELLOW,
                        ComponentType.ALARM_BUZZER
                    )
                ) {
                    val p1 = "${comp.id}:" + if (comp.type in listOf(ComponentType.CONTACTOR_COIL, ComponentType.TIMER_ON_DELAY_COIL, ComponentType.TIMER_OFF_DELAY_COIL)) "A1" else "X1"
                    val p2 = "${comp.id}:" + if (comp.type in listOf(ComponentType.CONTACTOR_COIL, ComponentType.TIMER_ON_DELAY_COIL, ComponentType.TIMER_OFF_DELAY_COIL)) "A2" else "X2"

                    val hasP1Wire = wires.any {
                        (it.fromComponentId == comp.id && it.fromTerminalId == p1.substringAfter(":")) ||
                                (it.toComponentId == comp.id && it.toTerminalId == p1.substringAfter(":"))
                    }
                    val hasP2Wire = wires.any {
                        (it.fromComponentId == comp.id && it.fromTerminalId == p2.substringAfter(":")) ||
                                (it.toComponentId == comp.id && it.toTerminalId == p2.substringAfter(":"))
                    }

                    // اگر فقط یک سر بار سیم‌کشی شده باشد و سر دیگر آزاد باشد
                    if (hasP1Wire != hasP2Wire) {
                        isOpenCircuit = true
                        val missingPin = if (hasP1Wire) p2.substringAfter(":") else p1.substringAfter(":")
                        openCircuitMessage = "مدار باز: پایانه ${missingPin} در قطعه ${comp.label} بدون سیم‌کشی به مدار باقی مانده است."
                        break
                    }

                    // یا اگر یک سر ولتاژ فاز گرفته ولی سر دیگر به نول نمی‌رسد
                    val p1Live = p1 in allLiveNodes
                    val p2Live = p2 in allLiveNodes
                    val p1Neutral = p1 in neutralNodes
                    val p2Neutral = p2 in neutralNodes

                    if ((p1Live && !p2Neutral && hasP2Wire) || (p2Live && !p1Neutral && hasP1Wire)) {
                        val activeCoilsExist = finalComponents.any { it.isEnergized }
                        if (!activeCoilsExist && wires.isNotEmpty()) {
                            isOpenCircuit = true
                            openCircuitMessage = "مدار باز: مسیر بازگشت جریان نول برای قطعه ${comp.label} کامل نیست."
                            break
                        }
                    }
                }
            }
        }

        val log = when {
            isShortCircuit -> shortCircuitMessage
            isInterlockViolation -> interlockErrorMessage
            isOpenCircuit -> openCircuitMessage
            sanitizedEnergizedCoils.isNotEmpty() -> "بوبین‌های فعال: ${sanitizedEnergizedCoils.joinToString(", ")}"
            finalComponents.any { it.isEnergized } -> "مدار فعال و در حال کار"
            else -> "مدار در حالت نرمال و آماده فرمان"
        }

        val executionTime = System.currentTimeMillis() - startTime

        return SimStepResult(
            updatedComponents = finalComponents,
            updatedWires = updatedWires,
            energizedCoilTags = sanitizedEnergizedCoils,
            isShortCircuit = isShortCircuit,
            isInterlockViolation = isInterlockViolation,
            isOpenCircuit = isOpenCircuit,
            logMessage = log,
            executionTimeMs = executionTime
        )
    }
}

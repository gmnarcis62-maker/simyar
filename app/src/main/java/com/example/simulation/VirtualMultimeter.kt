package com.example.simulation

import com.example.model.CircuitComponent
import com.example.model.ComponentFault
import com.example.model.ComponentType
import com.example.model.EDUCATIONAL_SIMULATION_DISCLAIMER
import com.example.model.MultimeterMode
import com.example.model.MultimeterProbe
import com.example.model.MultimeterReading
import com.example.model.WireConnection
import java.util.LinkedList
import java.util.Queue

/**
 * مولتی‌متر مجازی جهت اندازه‌گیری ولتاژ AC، ولتاژ DC و تست بوق/پیوستگی پیوندها
 * برچسب الزامی: "شبیه‌سازی آموزشی؛ جایگزین محاسبات و تست میدانی نیست"
 */
object VirtualMultimeter {

    const val DISCLAIMER: String = EDUCATIONAL_SIMULATION_DISCLAIMER

    /**
     * انجام اندازه‌گیری بین دو پروب مولتی‌متر
     */
    fun measure(
        components: List<CircuitComponent>,
        wires: List<WireConnection>,
        probeA: MultimeterProbe?,
        probeB: MultimeterProbe?,
        mode: MultimeterMode
    ): MultimeterReading {
        if (probeA == null || probeB == null) {
            return MultimeterReading(
                displayValue = "--",
                unit = mode.unit,
                isBeeping = false,
                statusText = "هر دو پروب (قرمز و مشکی) را به ترمینال‌های مدار متصل کنید",
                educationalNote = DISCLAIMER
            )
        }

        val pinA = "${probeA.componentId}:${probeA.terminalId}"
        val pinB = "${probeB.componentId}:${probeB.terminalId}"

        if (pinA == pinB) {
            return MultimeterReading(
                displayValue = if (mode == MultimeterMode.CONTINUITY) "0.0" else "0.0",
                unit = mode.unit,
                isBeeping = mode == MultimeterMode.CONTINUITY,
                statusText = if (mode == MultimeterMode.CONTINUITY) "اتصال کوتاه مستقیم به همان ترمینال" else "اختلاف پتانسیل صفر",
                educationalNote = DISCLAIMER
            )
        }

        val compA = components.find { it.id == probeA.componentId }
        val compB = components.find { it.id == probeB.componentId }

        // بررسی خطای اتصال شل روی هر یک از پروب‌ها
        if (compA?.injectedFault == ComponentFault.LOOSE_CONNECTION || compB?.injectedFault == ComponentFault.LOOSE_CONNECTION) {
            return MultimeterReading(
                displayValue = if (mode == MultimeterMode.CONTINUITY) "O.L" else "0.0",
                unit = mode.unit,
                isBeeping = false,
                statusText = "اتصال شل / قطعی در ترمینال (مدار باز)",
                educationalNote = DISCLAIMER
            )
        }

        return when (mode) {
            MultimeterMode.CONTINUITY -> measureContinuity(components, wires, pinA, pinB)
            MultimeterMode.VOLTAGE_AC -> measureVoltageAc(components, wires, pinA, pinB)
            MultimeterMode.VOLTAGE_DC -> measureVoltageDc(components, wires, pinA, pinB)
        }
    }

    /**
     * تست بوق و پیوستگی (Continuity Test) با تحلیل گراف رسانایی در مدار بدون برق
     */
    private fun measureContinuity(
        components: List<CircuitComponent>,
        wires: List<WireConnection>,
        pinA: String,
        pinB: String
    ): MultimeterReading {
        // ایجاد گراف اتصالات فیزیکی سیم‌ها و کلیدهای بسته
        val adj = mutableMapOf<String, MutableList<String>>()
        fun addEdge(u: String, v: String) {
            adj.getOrPut(u) { mutableListOf() }.add(v)
            adj.getOrPut(v) { mutableListOf() }.add(u)
        }

        // سیم‌های مدار (سیم سالم هدایت دارد)
        for (w in wires) {
            val u = "${w.fromComponentId}:${w.fromTerminalId}"
            val v = "${w.toComponentId}:${w.toTerminalId}"
            addEdge(u, v)
        }

        // هدایت داخلی تجهیزات در حالت بسته یا سالم
        for (c in components) {
            // خطاهای قطعی و چسبیدگی
            val isWelded = c.injectedFault == ComponentFault.WELDED_CONTACT
            val isStuckOpen = c.injectedFault == ComponentFault.CONTACT_STUCK_OPEN
            val isBurnedCoil = c.injectedFault == ComponentFault.BURNED_COIL

            when (c.type) {
                ComponentType.PUSH_BUTTON_NO, ComponentType.CONTACTOR_NO_AUX, ComponentType.LIMIT_SWITCH_NO -> {
                    if ((c.isClosedOrPressed || isWelded) && !isStuckOpen) {
                        addEdge("${c.id}:13", "${c.id}:14")
                        addEdge("${c.id}:IN", "${c.id}:OUT")
                    }
                }
                ComponentType.PUSH_BUTTON_NC, ComponentType.CONTACTOR_NC_AUX, ComponentType.LIMIT_SWITCH_NC -> {
                    if ((!c.isClosedOrPressed || isWelded) && !isStuckOpen) {
                        addEdge("${c.id}:21", "${c.id}:22")
                        addEdge("${c.id}:IN", "${c.id}:OUT")
                    }
                }
                ComponentType.CIRCUIT_BREAKER_MCB, ComponentType.FUSE, ComponentType.SELECTOR_SWITCH_2POS -> {
                    if ((c.isClosedOrPressed || isWelded) && !isStuckOpen) {
                        addEdge("${c.id}:IN", "${c.id}:OUT")
                    }
                }
                ComponentType.CIRCUIT_BREAKER_MCB_3P, ComponentType.CIRCUIT_BREAKER_MCCB_3P, ComponentType.FUSE_3P -> {
                    if ((c.isClosedOrPressed || isWelded) && !isStuckOpen) {
                        addEdge("${c.id}:1", "${c.id}:2")
                        addEdge("${c.id}:3", "${c.id}:4")
                        addEdge("${c.id}:5", "${c.id}:6")
                    }
                }
                ComponentType.BIMETAL_OVERLOAD -> {
                    val isTripped = c.isBimetalTripped && !isWelded
                    if (!isTripped && !isStuckOpen) {
                        addEdge("${c.id}:95", "${c.id}:96") // NC
                    } else {
                        addEdge("${c.id}:97", "${c.id}:98") // NO هنگام تریپ
                    }
                    addEdge("${c.id}:1", "${c.id}:2")
                    addEdge("${c.id}:3", "${c.id}:4")
                    addEdge("${c.id}:5", "${c.id}:6")
                }
                ComponentType.CONTACTOR_COIL, ComponentType.TIMER_ON_DELAY_COIL, ComponentType.TIMER_OFF_DELAY_COIL,
                ComponentType.CONTROL_RELAY_1CO, ComponentType.CONTROL_RELAY_2CO -> {
                    // سیم‌پیچ سالم مقاومت دارد (پیوسته است)، سیم‌پیچ سوخته مدار باز است
                    if (!isBurnedCoil) {
                        addEdge("${c.id}:A1", "${c.id}:A2")
                    }
                    if (c.type == ComponentType.CONTROL_RELAY_1CO) {
                        if (c.isEnergized || isWelded) addEdge("${c.id}:11", "${c.id}:14") else addEdge("${c.id}:11", "${c.id}:12")
                    } else if (c.type == ComponentType.CONTROL_RELAY_2CO) {
                        if (c.isEnergized || isWelded) {
                            addEdge("${c.id}:11", "${c.id}:14")
                            addEdge("${c.id}:21", "${c.id}:24")
                        } else {
                            addEdge("${c.id}:11", "${c.id}:12")
                            addEdge("${c.id}:21", "${c.id}:22")
                        }
                    }
                }
                ComponentType.PHASE_SEQUENCE_RELAY -> {
                    if (c.isEnergized || isWelded) addEdge("${c.id}:15", "${c.id}:18") else addEdge("${c.id}:15", "${c.id}:16")
                }
                else -> {}
            }
        }

        // جستجوی مسیر (BFS) از pinA به pinB
        val visited = mutableSetOf<String>()
        val queue: Queue<String> = LinkedList()
        visited.add(pinA)
        queue.add(pinA)

        var hasPath = false
        while (queue.isNotEmpty()) {
            val curr = queue.poll() ?: continue
            if (curr == pinB) {
                hasPath = true
                break
            }
            for (nb in adj[curr] ?: emptyList()) {
                if (nb !in visited) {
                    visited.add(nb)
                    queue.add(nb)
                }
            }
        }

        return if (hasPath) {
            MultimeterReading(
                displayValue = "0.2",
                unit = "Ω",
                isBeeping = true,
                statusText = "اتصال پیوسته است (بوق ممتد)",
                educationalNote = DISCLAIMER
            )
        } else {
            MultimeterReading(
                displayValue = "O.L",
                unit = "Ω",
                isBeeping = false,
                statusText = "مدار باز (فاقد پیوستگی الکتریکی)",
                educationalNote = DISCLAIMER
            )
        }
    }

    /**
     * اندازه‌گیری ولتاژ AC با شبیه‌سازی گره‌های برق‌دار
     */
    private fun measureVoltageAc(
        components: List<CircuitComponent>,
        wires: List<WireConnection>,
        pinA: String,
        pinB: String
    ): MultimeterReading {
        val simEngine = CircuitSimulationEngine()
        val result = simEngine.stepSimulation(components, wires)

        // بررسی پتانسیل الکتریکی هر گره
        fun getVoltageState(pin: String): String {
            return when {
                pin in result.activePhaseL1Pins -> "L1"
                pin in result.activePhaseL2Pins -> "L2"
                pin in result.activePhaseL3Pins -> "L3"
                pin in result.activeNeutralPins -> "N"
                else -> "FLOAT"
            }
        }

        val stateA = getVoltageState(pinA)
        val stateB = getVoltageState(pinB)

        val isPhaseToNeutral = (stateA.startsWith("L") && stateB == "N") || (stateB.startsWith("L") && stateA == "N")
        val isPhaseToPhase = stateA.startsWith("L") && stateB.startsWith("L") && stateA != stateB

        return when {
            isPhaseToPhase -> MultimeterReading(
                displayValue = "398.5",
                unit = "V~",
                isBeeping = false,
                statusText = "ولتاژ خط سه‌فاز (فاز به فاز ~$stateA-$stateB)",
                educationalNote = DISCLAIMER
            )
            isPhaseToNeutral -> MultimeterReading(
                displayValue = "229.4",
                unit = "V~",
                isBeeping = false,
                statusText = "ولتاژ فاز به نول تک‌فاز (~230V AC)",
                educationalNote = DISCLAIMER
            )
            stateA == stateB && stateA != "FLOAT" -> MultimeterReading(
                displayValue = "0.0",
                unit = "V~",
                isBeeping = false,
                statusText = "هم‌پتانسیل (هردو متصل به گره $stateA)",
                educationalNote = DISCLAIMER
            )
            else -> MultimeterReading(
                displayValue = "0.0",
                unit = "V~",
                isBeeping = false,
                statusText = "ولتاژ صفر / یکی از پایانه‌ها فاقد تغذیه است",
                educationalNote = DISCLAIMER
            )
        }
    }

    /**
     * اندازه‌گیری ولتاژ DC
     */
    private fun measureVoltageDc(
        components: List<CircuitComponent>,
        wires: List<WireConnection>,
        pinA: String,
        pinB: String
    ): MultimeterReading {
        val simEngine = CircuitSimulationEngine()
        val result = simEngine.stepSimulation(components, wires)

        val isPosA = pinA in result.activeDcPosPins
        val isPosB = pinB in result.activeDcPosPins
        val isNeutralA = pinA in result.activeNeutralPins
        val isNeutralB = pinB in result.activeNeutralPins

        return when {
            isPosA && isNeutralB -> MultimeterReading(
                displayValue = "+24.0",
                unit = "V=",
                isBeeping = false,
                statusText = "ولتاژ تغذیه مستقیم +24V DC",
                educationalNote = DISCLAIMER
            )
            isPosB && isNeutralA -> MultimeterReading(
                displayValue = "-24.0",
                unit = "V=",
                isBeeping = false,
                statusText = "ولتاژ مستقیم معکوس (پلاریته پروب‌ها جابجاست)",
                educationalNote = DISCLAIMER
            )
            else -> MultimeterReading(
                displayValue = "0.0",
                unit = "V=",
                isBeeping = false,
                statusText = "ولتاژ مستقیم صفر",
                educationalNote = DISCLAIMER
            )
        }
    }

    /**
     * راهنمای پلکانی آموزشی ۴ مرحله‌ای بر اساس قطعه معیوب
     */
    fun getSteppedHint(step: Int, faultyComponent: CircuitComponent?): String {
        if (faultyComponent == null || faultyComponent.injectedFault == ComponentFault.NONE) {
            return "مدار بدون خطای فعال است؛ تمام قطعات طبق مشخصات استاندارد عمل می‌کنند."
        }

        return when (step) {
            1 -> "گام ۱ (بررسی تغذیه): ولتاژ خط و نول را با مولتی‌متر در ورودی کلیدهای مینیاتوری و فیوزها اندازه بگیرید."
            2 -> "گام ۲ (تست پیوستگی مدار فرمان): در حالت خاموش، با تست بوق مسیر عبور جریان را از استپ و بی‌متال ۹۵-۹۶ تا شستی استارت دنبال کنید."
            3 -> "گام ۳ (پایش تحریک بوبین): هنگام فرمان دادن به مدار، ولتاژ دو سر بوبین A1-A2 مربوط به ${faultyComponent.label} را بسنجید."
            4 -> "گام ۴ (نتیجه تشخیصی): قطعه [${faultyComponent.label}] با عیب «${faultyComponent.injectedFault.faName}» روبرو است: ${faultyComponent.injectedFault.description}."
            else -> "عیب‌یابی تکمیل شد."
        }
    }
}

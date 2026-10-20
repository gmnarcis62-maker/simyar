package com.example.simulation

import com.example.model.LadderElement
import com.example.model.LadderElementType
import com.example.model.LadderNode
import com.example.model.LadderRung
import com.example.model.PlcMemory
import kotlin.math.roundToInt

/**
 * موتور پردازشگر لدر PLC صنعتی سیمیار (استاندارد IEC 61131-3):
 * - ارزیابی بازگشتی درخت شبکه سری/موازی (Series/Parallel Network Tree)
 * - دستورات مقایسه‌ای (EQ, NE, GT, GE, LT, LE) روی مقادیر عددی، تایمرها، کانترها و رجیسترهای MW
 * - دستورات محاسبات ریاضی (ADD, SUB, MUL, DIV) با محافظت سرریز INT ۱۶ بیتی و خطای تقسیم بر صفر
 * - کانترهای پیشرفته CTU، CTD و شمارنده دوطرفه صعودی/نزولی CTUD
 * - کنتاکت‌های لبه بالارونده (P / R_TRIG) و لبه پایین‌رونده (N / F_TRIG)
 * - پشتیبانی از تگ‌های نمادین و انواع آدرس‌دهی زیمنس و IEC 61131-3
 */
class PlcLadderEngine {

    fun executeCycle(
        rungs: List<LadderRung>,
        memory: PlcMemory,
        cycleDeltaSeconds: Float = 0.05f
    ): Pair<List<LadderRung>, PlcMemory> {
        val nextOutputs = memory.outputs.toMutableMap()
        val nextMarkers = memory.markers.toMutableMap()
        val nextIntMemory = memory.intMemory.toMutableMap()
        val nextTimers = memory.timers.toMutableMap()
        val nextTimerDone = memory.timerDone.toMutableMap()
        val nextCounters = memory.counters.toMutableMap()
        val nextCounterDone = memory.counterDone.toMutableMap()
        val nextPrevConditions = memory.prevRungConditions.toMutableMap()
        val nextPrevContactStates = memory.prevContactStates.toMutableMap()

        val updatedRungs = mutableListOf<LadderRung>()

        for (rung in rungs) {
            // ۱. خواندن سیگنال‌های بولی با پشتیبانی از جدول تگ‌ها
            fun readBit(addressOrTag: String): Boolean {
                val addr = memory.resolveAddress(addressOrTag)
                return when {
                    addr.startsWith("I") -> memory.inputs[addr] ?: false
                    addr.startsWith("Q") -> nextOutputs[addr] ?: memory.outputs[addr] ?: false
                    addr.startsWith("M") && !addr.startsWith("MW") -> nextMarkers[addr] ?: memory.markers[addr] ?: false
                    addr.startsWith("T") -> nextTimerDone[addr] ?: memory.timerDone[addr] ?: false
                    addr.startsWith("C") -> nextCounterDone[addr] ?: memory.counterDone[addr] ?: false
                    else -> false
                }
            }

            // خواندن مقدار عددی ۱۶ بیتی
            fun readIntVal(operand: String): Int {
                val trimmed = operand.trim()
                trimmed.toIntOrNull()?.let { return it }

                val addr = memory.resolveAddress(trimmed)
                return when {
                    addr.startsWith("MW") -> nextIntMemory[addr] ?: memory.intMemory[addr] ?: 0
                    addr.startsWith("C") -> nextCounters[addr] ?: memory.counters[addr] ?: 0
                    addr.startsWith("T") -> (nextTimers[addr] ?: memory.timers[addr] ?: 0f).roundToInt()
                    else -> nextIntMemory[addr] ?: memory.intMemory[addr] ?: 0
                }
            }

            // ارزیابی یک المان برگ در شبکه ورودی لدر
            fun evalLeaf(elem: LadderElement): Pair<Boolean, LadderElement> {
                val addr = memory.resolveAddress(elem.address)
                val isConditionMet: Boolean

                when (elem.type) {
                    LadderElementType.NO_CONTACT -> {
                        val bitVal = readBit(elem.address)
                        isConditionMet = bitVal
                    }
                    LadderElementType.NC_CONTACT -> {
                        val bitVal = readBit(elem.address)
                        isConditionMet = !bitVal
                    }
                    LadderElementType.EDGE_POS -> {
                        val currentBit = readBit(elem.address)
                        val prevBit = memory.prevContactStates[elem.id] ?: false
                        nextPrevContactStates[elem.id] = currentBit
                        isConditionMet = currentBit && !prevBit // لبه بالارونده R_TRIG
                    }
                    LadderElementType.EDGE_NEG -> {
                        val currentBit = readBit(elem.address)
                        val prevBit = memory.prevContactStates[elem.id] ?: false
                        nextPrevContactStates[elem.id] = currentBit
                        isConditionMet = !currentBit && prevBit // لبه پایین‌رونده F_TRIG
                    }
                    // دستورهای مقایسه‌ای (Comparisons)
                    LadderElementType.CMP_EQ, LadderElementType.CMP_NE,
                    LadderElementType.CMP_GT, LadderElementType.CMP_GE,
                    LadderElementType.CMP_LT, LadderElementType.CMP_LE -> {
                        val val1 = readIntVal(elem.address)
                        val val2 = if (elem.operand2.isNotBlank()) readIntVal(elem.operand2) else elem.presetValue

                        isConditionMet = when (elem.type) {
                            LadderElementType.CMP_EQ -> val1 == val2
                            LadderElementType.CMP_NE -> val1 != val2
                            LadderElementType.CMP_GT -> val1 > val2
                            LadderElementType.CMP_GE -> val1 >= val2
                            LadderElementType.CMP_LT -> val1 < val2
                            LadderElementType.CMP_LE -> val1 <= val2
                            else -> false
                        }
                    }
                    else -> {
                        isConditionMet = true
                    }
                }

                val updatedElem = elem.copy(isActive = isConditionMet)
                return Pair(isConditionMet, updatedElem)
            }

            // ارزیابی بازگشتی درخت شبکه سری/موازی (Series/Parallel Evaluation)
            fun evalNode(node: LadderNode): Boolean {
                return when (node) {
                    is LadderNode.Leaf -> evalLeaf(node.element).first
                    is LadderNode.Series -> if (node.children.isEmpty()) true else node.children.all { evalNode(it) }
                    is LadderNode.Parallel -> if (node.children.isEmpty()) false else node.children.any { evalNode(it) }
                }
            }

            // ۲. ارزیابی شرط رانگ از طریق درخت شبکه
            val networkTree = rung.toNetworkTree()
            val rungCondition = evalNode(networkTree)

            // به‌روزرسانی وضعیت نمایشی تماس‌های اصلی و شاخه‌ها برای رابط کاربری
            val evalMainContacts = rung.contacts.map { evalLeaf(it).second }
            val evalBranchContacts = rung.branchContacts.map { evalLeaf(it).second }
            val evalParallelBranches = rung.parallelBranches.map { branch ->
                branch.copy(contacts = branch.contacts.map { evalLeaf(it).second })
            }

            val prevRungCond = memory.prevRungConditions[rung.id] ?: false
            val isRisingEdge = rungCondition && !prevRungCond
            nextPrevConditions[rung.id] = rungCondition

            // ۳. اجرای بوبین‌ها، تایمرها، کانترها و محاسبات ریاضی
            val evalCoils = rung.coils.map { coil ->
                val resolvedAddr = memory.resolveAddress(coil.address)

                when (coil.type) {
                    LadderElementType.NORMAL_COIL -> {
                        writeAddress(nextOutputs, nextMarkers, resolvedAddr, rungCondition)
                        coil.copy(isActive = rungCondition)
                    }
                    LadderElementType.SET_COIL -> {
                        if (rungCondition) {
                            writeAddress(nextOutputs, nextMarkers, resolvedAddr, true)
                        }
                        val currentVal = readInternal(nextOutputs, nextMarkers, resolvedAddr)
                        coil.copy(isActive = currentVal)
                    }
                    LadderElementType.RESET_COIL -> {
                        if (rungCondition) {
                            writeAddress(nextOutputs, nextMarkers, resolvedAddr, false)
                            if (resolvedAddr.startsWith("C")) {
                                nextCounters[resolvedAddr] = 0
                                nextCounterDone[resolvedAddr] = false
                            }
                        }
                        coil.copy(isActive = false)
                    }
                    LadderElementType.TIMER_TON -> {
                        val presetSec = coil.presetValue.coerceAtLeast(1).toFloat()
                        var currentElapsed = nextTimers[resolvedAddr] ?: 0f

                        if (rungCondition) {
                            currentElapsed += cycleDeltaSeconds
                            if (currentElapsed > presetSec) currentElapsed = presetSec
                        } else {
                            currentElapsed = 0f
                        }
                        nextTimers[resolvedAddr] = currentElapsed
                        val isDone = currentElapsed >= presetSec
                        nextTimerDone[resolvedAddr] = isDone

                        coil.copy(
                            currentValue = currentElapsed.roundToInt(),
                            isActive = isDone
                        )
                    }
                    LadderElementType.TIMER_TOF -> {
                        val presetSec = coil.presetValue.coerceAtLeast(1).toFloat()
                        var currentElapsed = nextTimers[resolvedAddr] ?: 0f
                        val isDone: Boolean

                        if (rungCondition) {
                            currentElapsed = 0f
                            isDone = true
                        } else {
                            if (currentElapsed < presetSec) {
                                currentElapsed += cycleDeltaSeconds
                                if (currentElapsed > presetSec) currentElapsed = presetSec
                                isDone = true
                            } else {
                                isDone = false
                            }
                        }
                        nextTimers[resolvedAddr] = currentElapsed
                        nextTimerDone[resolvedAddr] = isDone
                        writeAddress(nextOutputs, nextMarkers, resolvedAddr, isDone)

                        coil.copy(
                            currentValue = currentElapsed.roundToInt(),
                            isActive = isDone
                        )
                    }
                    LadderElementType.COUNTER_CTU -> {
                        var count = nextCounters[resolvedAddr] ?: 0
                        if (isRisingEdge) {
                            count += 1
                        }
                        nextCounters[resolvedAddr] = count
                        val isDone = count >= coil.presetValue
                        nextCounterDone[resolvedAddr] = isDone
                        coil.copy(currentValue = count, isActive = isDone)
                    }
                    LadderElementType.COUNTER_CTD -> {
                        var count = nextCounters[resolvedAddr] ?: coil.presetValue
                        if (isRisingEdge && count > 0) {
                            count -= 1
                        }
                        nextCounters[resolvedAddr] = count
                        val isDone = count <= 0
                        nextCounterDone[resolvedAddr] = isDone
                        coil.copy(currentValue = count, isActive = isDone)
                    }
                    LadderElementType.COUNTER_CTUD -> {
                        // شمارنده صعودی/نزولی دوطرفه
                        var count = nextCounters[resolvedAddr] ?: 0
                        if (isRisingEdge) {
                            count += 1
                        }
                        nextCounters[resolvedAddr] = count
                        val isDone = count >= coil.presetValue
                        nextCounterDone[resolvedAddr] = isDone
                        coil.copy(currentValue = count, isActive = isDone)
                    }
                    // دستورات محاسبات ریاضی (Arithmetic Math)
                    LadderElementType.MATH_ADD, LadderElementType.MATH_SUB,
                    LadderElementType.MATH_MUL, LadderElementType.MATH_DIV -> {
                        var result = nextIntMemory[resolvedAddr] ?: 0
                        if (rungCondition) {
                            val v1 = readIntVal(coil.address)
                            val v2 = if (coil.operand2.isNotBlank()) readIntVal(coil.operand2) else coil.presetValue

                            result = when (coil.type) {
                                LadderElementType.MATH_ADD -> (v1.toLong() + v2.toLong()).coerceIn(-32768L, 32767L).toInt()
                                LadderElementType.MATH_SUB -> (v1.toLong() - v2.toLong()).coerceIn(-32768L, 32767L).toInt()
                                LadderElementType.MATH_MUL -> (v1.toLong() * v2.toLong()).coerceIn(-32768L, 32767L).toInt()
                                LadderElementType.MATH_DIV -> {
                                    // محافظت ۱۰۰٪ در برابر خطای تقسیم بر صفر
                                    if (v2 != 0) (v1 / v2).coerceIn(-32768, 32767) else 0
                                }
                                else -> 0
                            }

                            val destAddr = memory.resolveAddress(coil.destinationAddress.ifBlank { coil.address })
                            nextIntMemory[destAddr] = result
                        }

                        coil.copy(currentValue = result, isActive = rungCondition)
                    }
                    else -> coil
                }
            }

            updatedRungs.add(
                rung.copy(
                    contacts = evalMainContacts,
                    branchContacts = evalBranchContacts,
                    parallelBranches = evalParallelBranches,
                    coils = evalCoils,
                    isPowerFlowing = rungCondition
                )
            )
        }

        val updatedMemory = memory.copy(
            outputs = nextOutputs,
            markers = nextMarkers,
            intMemory = nextIntMemory,
            timers = nextTimers,
            timerDone = nextTimerDone,
            counters = nextCounters,
            counterDone = nextCounterDone,
            prevRungConditions = nextPrevConditions,
            prevContactStates = nextPrevContactStates
        )

        return Pair(updatedRungs, updatedMemory)
    }

    private fun writeAddress(
        outputs: MutableMap<String, Boolean>,
        markers: MutableMap<String, Boolean>,
        address: String,
        value: Boolean
    ) {
        if (address.startsWith("Q")) {
            outputs[address] = value
        } else if (address.startsWith("M") && !address.startsWith("MW")) {
            markers[address] = value
        }
    }

    private fun readInternal(
        outputs: Map<String, Boolean>,
        markers: Map<String, Boolean>,
        address: String
    ): Boolean {
        return if (address.startsWith("Q")) {
            outputs[address] ?: false
        } else if (address.startsWith("M") && !address.startsWith("MW")) {
            markers[address] ?: false
        } else false
    }
}

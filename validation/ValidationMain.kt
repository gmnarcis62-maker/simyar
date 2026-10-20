import com.example.calc.CableSizingCalculator
import com.example.calc.VoltageDropCalculator

fun main() {
    check(CableSizingCalculator.calculateCableSize(CableSizingCalculator.CableSizingInput(Double.NaN)).isFailure)
    check(CableSizingCalculator.calculateCableSize(CableSizingCalculator.CableSizingInput(10.0, useCustomCapacityTable = true)).isFailure)
    check(CableSizingCalculator.calculateCableSize(CableSizingCalculator.CableSizingInput(10.0, groupedCircuitsCount = 0)).isFailure)
    check(CableSizingCalculator.calculateCableSize(CableSizingCalculator.CableSizingInput(10.0)).isSuccess)
    check(VoltageDropCalculator.calculateVoltageDrop(VoltageDropCalculator.VoltageDropInput(currentAmp = Double.POSITIVE_INFINITY, lengthMeters = 30.0, crossSectionMm2 = 4.0)).isFailure)
    check(VoltageDropCalculator.calculateVoltageDrop(VoltageDropCalculator.VoltageDropInput(currentAmp = 10.0, lengthMeters = 30.0, crossSectionMm2 = 4.0, customAllowablePercent = Double.NaN)).isFailure)
    check(VoltageDropCalculator.calculateVoltageDrop(VoltageDropCalculator.VoltageDropInput(currentAmp = 10.0, lengthMeters = 30.0, crossSectionMm2 = 4.0)).isSuccess)
    println("PASS: 7 calculator validation smoke checks")
}

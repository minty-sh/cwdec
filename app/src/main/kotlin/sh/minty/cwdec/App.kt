package sh.minty.cwdec

import java.util.Scanner

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.required

import kotlin.system.exitProcess

class Decryptor(val numberSequences: Pair<Array<Int?>, Array<Int?>>, val numberStations: Map<String, Int>, val scrambledLetters: String) {
    val cities = listOf(
        "Atlanta", "Baltimore", "Boston",
        "Charlotte", "Chicago", "Cincinnati",
        "Dallas", "Denver", "Detroit",
        "Houston", "Kansas City", "Los Angeles",
        "Madison", "Memphis", "Miami",
        "Milwaukee", "New Orleans", "New York",
        "Philadelphia", "Phoenix", "Pittsburgh",
        "Richmond", "San Diego", "San Francisco",
        "Seattle"
    )

    fun unscrambleCity(): String? {
        val scrambledSorted = scrambledLetters.replace(" ", "").lowercase().toCharArray().sorted()
    
        for (city in cities) {
            val citySorted = city.replace(" ", "").lowercase().toCharArray().sorted()
            if (scrambledSorted == citySorted) {
                return city
            }
        }

        return null
    }

    fun decryptFinalCode(): Pair<String, String> {
        // unscramble the city from the Observer letters
        val city = unscrambleCity() ?: error("Cannot unscramble city from letters")
    
        // get the station code for that city
        val code = numberStations[city] ?: error("No code found for city $city")
    
        // calculate the missing numbers for red and blue sequences
        // val (redMissing, blueMissing) = completeNumberSequences()
    
        // combine both missing codes to get second passphrase
        // val combinedNumber = redMissing * 100 + blueMissing
        // val secondCity = numberStations.entries.firstOrNull { it.value == combinedNumber }
        //     ?.key ?: error("No city found for combined number $combinedNumber")
    
        // println("Clue City: $city (code: $code)")
        // println("Red Missing: $redMissing, Blue Missing: $blueMissing")
        // println("Second City for passphrase: $secondCity (code: $combinedNumber)")
    
        return Pair(city, code.toString().padStart(4, '0'))
    }

    private fun findMissingNumber(sequence: Array<Int?>): Int {
        val idx = sequence.indexOfFirst { it == null }
        if (idx == -1) error("No missing number in sequence")

        val known = sequence.mapIndexedNotNull { i, v -> if (v != null) i to v else null }.sortedBy { it.first }
        if (known.size < 2) error("Not enough known numbers to infer pattern")

        // build stepDiffs for consecutive known pairs: map[startIndex] = diff (value at i+1 - value at i)
        val stepDiffs = linkedMapOf<Int, Int>()
        for (i in 0 until (sequence.size - 1)) {
            val a = sequence.getOrNull(i)
            val b = sequence.getOrNull(i + 1)
            if (a != null && b != null) {
                stepDiffs[i] = b - a
            }
        }

        fun linearInterp(prevIdx: Int, prevVal: Int, nextIdx: Int, nextVal: Int, targetIdx: Int): Int {
            val steps = nextIdx - prevIdx
            val totalDiff = nextVal - prevVal
            val perStep = totalDiff / steps
            return prevVal + perStep * (targetIdx - prevIdx)
        }

        // helper to choose most common step if needed
        fun mostCommonStep(): Int? {
            if (stepDiffs.isEmpty()) return null
            return stepDiffs.values.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
        }

        // Missing at start
        if (idx == 0) {
            if (stepDiffs.isNotEmpty()) {
                val delta = detectStepDelta(stepDiffs)
                val (anchorPos, anchorDiff) = stepDiffs.entries.first()
                if (delta != null) {
                    val diffAt0 = anchorDiff + delta * (0 - anchorPos)
                    val nextKnown = known.first { it.first > 0 }
                    return nextKnown.second - diffAt0
                } else {
                    // fallback to most common step
                    val step = mostCommonStep() ?: run {
                        val (i1, v1) = known[0]
                        val (i2, v2) = known[1]
                        return linearInterp(i1, v1, i2, v2, 0)
                    }
                    val nextKnown = known.first { it.first > 0 }
                    return nextKnown.second - step
                }
            } else {
                val (i1, v1) = known[0]
                val (i2, v2) = known[1]
                return linearInterp(i1, v1, i2, v2, 0)
            }
        }

        // missing at end
        if (idx == sequence.lastIndex) {
            if (stepDiffs.isNotEmpty()) {
                val delta = detectStepDelta(stepDiffs)
                val (anchorPos, anchorDiff) = stepDiffs.entries.first()
                if (delta != null) {
                    val diffAtPrev = anchorDiff + delta * ((sequence.lastIndex - 1) - anchorPos)
                    val prevKnown = known.last { it.first < sequence.size - 1 }
                    return prevKnown.second + diffAtPrev
                } else {
                    val step = mostCommonStep() ?: run {
                        val (i1, v1) = known[known.size - 2]
                        val (i2, v2) = known[known.size - 1]
                        return linearInterp(i1, v1, i2, v2, sequence.lastIndex)
                    }
                    val prevKnown = known.last { it.first < sequence.size - 1 }
                    return prevKnown.second + step
                }
            } else {
                val (i1, v1) = known[known.size - 2]
                val (i2, v2) = known[known.size - 1]
                return linearInterp(i1, v1, i2, v2, sequence.lastIndex)
            }
        }

        // missing in middle
        val (prevIdx, prevVal) = known.last { it.first < idx }
        val (nextIdx, nextVal) = known.first { it.first > idx }

        if (stepDiffs.isNotEmpty()) {
            val delta = detectStepDelta(stepDiffs)
            if (delta != null) {
                val (anchorPos, anchorDiff) = stepDiffs.entries.first()
                val dPrev = anchorDiff + delta * (prevIdx - anchorPos)
                val k = idx - prevIdx
                val sum = k * dPrev + delta * k * (k - 1) / 2
                return prevVal + sum
            } else {
                val mostCommon = mostCommonStep() ?: return linearInterp(prevIdx, prevVal, nextIdx, nextVal, idx)
                return prevVal + mostCommon * (idx - prevIdx)
            }
        }

        return linearInterp(prevIdx, prevVal, nextIdx, nextVal, idx)
    }

    private fun detectStepDelta(stepDiffs: Map<Int, Int>): Int? {
        if (stepDiffs.size < 2) return null
        val entries = stepDiffs.entries.sortedBy { it.key }

        val deltas = mutableListOf<Int>()
        for (i in 0 until entries.size - 1) {
            val (posA, diffA) = entries[i]
            val (posB, diffB) = entries[i + 1]
            if (posB == posA + 1) {
                deltas.add(diffB - diffA)
            }
        }
        if (deltas.isEmpty()) return null

        val distinct = deltas.distinct()
        return if (distinct.size == 1) distinct.first() else null
    }

    fun completeNumberSequences(): Pair<Int, Int> {
        val (red, blue) = numberSequences
        val redMissing = findMissingNumber(red)
        val blueMissing = findMissingNumber(blue)
        return Pair(redMissing, blueMissing)
    }
}

fun parseStations(arg: String): Map<String, Int> {
    if (arg.isBlank()) return emptyMap()
    return arg.split(",").map { token ->
        val (k, v) = token.split("=", limit = 2).map { it.trim() }
        require(k.isNotEmpty() && v.isNotEmpty()) { "Bad station token: $token" }
        // remove non digits (just in case) then toInt
        val numeric = v.filter { it.isDigit() }
        k to (numeric.toIntOrNull() ?: error("Invalid station code: $v"))
    }.toMap()
}

fun parseSequence(arg: String): Array<Int?> {
    if (arg.isBlank()) return arrayOf()
    return arg.split(",").map { token ->
        val t = token.trim()
        when {
            t == "?" || t.equals("null", true) -> null
            t.all { it.isDigit() } -> t.toInt()
            else -> error("Bad sequence token: $t")
        }
    }.toTypedArray()
}

fun main(args: Array<String>) {
    val parser = ArgParser("cwdec")

    val stationsArg by parser.option(
        ArgType.String,
        fullName = "stations",
        shortName = "s",
        description = "Numbers stations list in format City=Code,..."
    ).required()

    val scrambleArg by parser.option(
        ArgType.String,
        fullName = "scramble",
        shortName = "c",
        description = "Scrambled letters from Observer evidence"
    ).required()

    val redArg by parser.option(
        ArgType.String,
        fullName = "red",
        shortName = "r",
        description = "Red number sequence (comma, ? for missing)"
    ).required()

    val blueArg by parser.option(
        ArgType.String,
        fullName = "blue",
        shortName = "b",
        description = "Blue number sequence (comma, ? for missing)"
    ).required()

    parser.parse(args)

    val numberStations = parseStations(stationsArg)
    val redSeq = parseSequence(redArg)
    val blueSeq = parseSequence(blueArg)

    val decryptor = Decryptor(Pair(redSeq, blueSeq), numberStations, scrambleArg)
    val (city, codeStr) = decryptor.decryptFinalCode()
    val (redMissing, blueMissing) = decryptor.completeNumberSequences()
    println("First passkey city: $city, code: $codeStr")
    println("Missing numbers: red=$redMissing, blue=$blueMissing")
    val combined = redMissing * 100 + blueMissing
    val secondCity = numberStations.entries.firstOrNull { it.value == combined }?.key
    println("Combined code: ${combined.toString().padStart(4,'0')} -> ${secondCity ?: "NOT FOUND"}")
}

package sh.minty.cwdec

import kotlin.test.Test
import kotlin.test.assertEquals

class DecryptorTest {

    @Test
    fun testDecryptFloppyDiskExample() {
        // Setup numberStations: codes as integers
        val numberStations = mapOf(
            "Madison" to 629,
            "Austin" to 7143
        )

        val scrambledLetters = "NMDIASO"

        // Red: 56, 61, 66, ?, 76 -> missing 71
        val redSequence: Array<Int?> = arrayOf(56, 61, 66, null, 76)
        // Blue: 31, 33, 37, ?, 51 -> missing 43
        val blueSequence: Array<Int?> = arrayOf(31, 33, 37, null, 51)

        val decryptor = Decryptor(Pair(redSequence, blueSequence), numberStations, scrambledLetters)

        // Test unscrambleCity
        val city = decryptor.unscrambleCity()
        assertEquals("Madison", city)

        // Test findMissingNumber for red
        val redMissing = decryptor.findMissingNumber(redSequence)
        assertEquals(71, redMissing)

        // Test findMissingNumber for blue
        val blueMissing = decryptor.findMissingNumber(blueSequence)
        assertEquals(43, blueMissing)

        // Test completeNumberSequences
        val (red, blue) = decryptor.completeNumberSequences()
        assertEquals(71, red)
        assertEquals(43, blue)

        // Test decryptFinalCode
        val (firstCity, firstCodeStr) = decryptor.decryptFinalCode()
        assertEquals("Madison", firstCity)
        assertEquals("0629", firstCodeStr)

        // Test second passphrase number (combined Red+Blue)
        val combinedNumber = red * 100 + blue
        assertEquals(7143, combinedNumber)
    }
}

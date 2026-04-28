package com.stephenbrines.trailweight.service

object WeightParser {

    fun parseToGrams(raw: String): Double? {
        val s = raw.trim()
        return parseDualFormat(s)
            ?: parseGramsOnly(s)
            ?: parsePoundsAndOunces(s)
            ?: parseOuncesOnly(s)
            ?: parsePoundsOnly(s)
    }

    // "4 oz / 113g", "4 oz (113 g)", "19.0 oz (539 g)" — prefer grams component
    private fun parseDualFormat(s: String): Double? {
        if (!s.contains(Regex("""oz|ounce""", RegexOption.IGNORE_CASE))) return null
        val pattern = Regex("""(\d+(?:\.\d+)?)\s*g(?:ram[s]?)?\b""", RegexOption.IGNORE_CASE)
        return pattern.find(s)?.groupValues?.get(1)?.toDoubleOrNull()
    }

    // "119g", "119 g", "539 grams"
    private fun parseGramsOnly(s: String): Double? {
        if (s.contains(Regex("""oz|ounce""", RegexOption.IGNORE_CASE))) return null
        val pattern = Regex("""^(\d+(?:\.\d+)?)\s*g(?:ram[s]?)?\b""", RegexOption.IGNORE_CASE)
        return pattern.find(s)?.groupValues?.get(1)?.toDoubleOrNull()
    }

    // "1 lb 4 oz", "3 lbs. 14 oz."
    private fun parsePoundsAndOunces(s: String): Double? {
        val pattern = Regex(
            """(\d+(?:\.\d+)?)\s*lbs?\.?\s+(\d+(?:\.\d+)?)\s*oz""",
            RegexOption.IGNORE_CASE
        )
        val match = pattern.find(s) ?: return null
        val lbs = match.groupValues[1].toDoubleOrNull() ?: return null
        val oz = match.groupValues[2].toDoubleOrNull() ?: return null
        return lbs * 453.592 + oz * 28.3495
    }

    // "4.2 oz", "4 ounces"
    private fun parseOuncesOnly(s: String): Double? {
        if (s.contains(Regex("""lbs?""", RegexOption.IGNORE_CASE))) return null
        val pattern = Regex("""(\d+(?:\.\d+)?)\s*(?:oz|ounce[s]?)\b""", RegexOption.IGNORE_CASE)
        return pattern.find(s)?.groupValues?.get(1)?.toDoubleOrNull()?.let { it * 28.3495 }
    }

    // "0.26 lbs", "1 lb"
    private fun parsePoundsOnly(s: String): Double? {
        if (s.contains("oz", ignoreCase = true)) return null
        val pattern = Regex("""(\d+(?:\.\d+)?)\s*lbs?\.?""", RegexOption.IGNORE_CASE)
        return pattern.find(s)?.groupValues?.get(1)?.toDoubleOrNull()?.let { it * 453.592 }
    }

    fun displayString(grams: Double): String {
        val oz = grams / 28.3495
        return when {
            grams < 28.35 -> "%.0fg".format(grams)
            grams < 453.59 -> "%.1f oz (%.0fg)".format(oz, grams)
            else -> "%.2f lbs (%.0fg)".format(grams / 453.592, grams)
        }
    }
}

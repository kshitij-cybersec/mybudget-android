import java.util.Locale

val rawText = """
08-Mar-2026 Transfer to ESTHER JEAN DSOUZA €15.80 €11.23
Reference: Brekkie
To: ESTHER JEAN DSOUZA
08-Mar-2026 Oh My Street Food €5.50 €5.73
"""

val dateRegex = Regex(
    """(\d{1,2}[-/.]\d{1,2}[-/.]\d{2,4}""" +
    """|(\d{1,2})[-/.\s]+(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)(?!\w)(?:[-/.\s]+\d{2,4})?)""",
    RegexOption.IGNORE_CASE
)

for (line in rawText.lines()) {
    val match = dateRegex.find(line)
    println("Line: $line -> match: ${match?.value}")
}

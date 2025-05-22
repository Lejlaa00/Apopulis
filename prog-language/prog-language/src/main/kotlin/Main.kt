import java.io.File

fun main() {
    val fileContent = File("prog-language/test.txt").readText()
    val testInputs2 = fileContent.trim().split(Regex("\n\\s*\n")) // separating entries by blank line

    testInputs2.forEachIndexed { index, input ->
        println("\n=== Test ${index + 1} ===")

        val inputStream = input.trimIndent().byteInputStream()
        val scanner = Scanner(inputStream)
        val parser = Parser(scanner)

        val result = parser.parseProgram()

        if (result) {
            println("accept")
        } else {
            println("reject")
        }
    }
}
import java.io.File
import Scanner
import Parser
import export.GeoJsonGenerator
import java.io.FileOutputStream

/**
 * Entry point for the city infrastructure DSL compiler/interpreter.
 * Reads input files, parses them into an AST, and generates GeoJSON output.
 * @param args Command line arguments - expects input file paths
 */
fun main(args: Array<String>) {
    val fileContent = File("../test.txt").readText()
    val testInputs2 = fileContent.trim().split(Regex("\n\\s*\n"))

    testInputs2.forEachIndexed { index, input ->
        println("\n=== Test ${index + 1} ===")

        val inputStream = input.trimIndent().byteInputStream()
        val scanner = Scanner(inputStream)
        val parser = Parser(scanner)

        val result = parser.parseProgram()

        if (result != null) {
            println("accept")

            val geojson = GeoJsonGenerator.generate(result)

            val fileName = "output$index.geojson"
            File(fileName).writeText(geojson)
            println("GeoJSON written to $fileName")

        } else {
            println("reject")
        }
    }
}

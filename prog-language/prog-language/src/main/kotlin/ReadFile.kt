import java.io.*

/**
 * Handles file reading operations for the city infrastructure DSL.
 * Provides functionality to read and process input files containing DSL code.
 */
class ReadFile(filename: String) {
    private var reader: BufferedReader? = null
    private var stream: FileReader? = null
    private var fileOpen: Boolean = false
    private var lastChar: Int = -1

    /**
     * Initializes the file reader with the specified filename.
     * Attempts to open the file and set up the reader.
     * @param filename Path to the file to read
     */
    init {
        try {
            stream = FileReader(filename)
            reader = BufferedReader(stream)
            fileOpen = true
        } catch (e: FileNotFoundException) {
            println("File not found: $filename")
            fileOpen = false
        }
    }

    /**
     * Checks if the file is currently open.
     * @return True if the file is open, false otherwise
     */
    fun isOpen(): Boolean = fileOpen

    /**
     * Checks if the end of the file has been reached.
     * @return True if the end of the file has been reached, false otherwise
     */
    fun eof(): Boolean {
        if (!fileOpen) return true
        reader?.mark(1)
        val c = reader?.read()
        if (c == -1) return true
        reader?.reset()
        return false
    }

    /**
     * Reads a line from the file.
     * @return The line read from the file, or an empty string if the file is not open
     */
    fun readLine(): String {
        return if (fileOpen) {
            reader?.readLine() ?: ""
        } else {
            ""
        }
    }

    fun readChar(): Char {
        return if (fileOpen) {
            val c = reader?.read()
            if (c == -1) '\u0000' else c!!.toChar()
        } else {
            '\u0000'
        }
    }

    fun getStream(): BufferedReader? = reader

    fun close() {
        reader?.close()
        stream?.close()
        fileOpen = false
    }
}

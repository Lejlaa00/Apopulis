import java.io.*

class ReadFile(filename: String) {
    private var reader: BufferedReader? = null
    private var stream: FileReader? = null
    private var fileOpen: Boolean = false
    private var lastChar: Int = -1

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

    fun isOpen(): Boolean = fileOpen

    fun eof(): Boolean {
        if (!fileOpen) return true
        reader?.mark(1)
        val c = reader?.read()
        if (c == -1) return true
        reader?.reset()
        return false
    }

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

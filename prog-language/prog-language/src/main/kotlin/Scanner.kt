import java.io.InputStream

/**
 * Performs lexical analysis on the city infrastructure DSL source code.
 * Breaks down the input text into a sequence of tokens that can be processed by the parser.
 */
class Scanner(private val input: InputStream) {
    var row = 1
    var column = 0
    private val automat = Automat()

    /**
     * Peeks at the next character in the input stream without consuming it.
     * @return The next character in the input stream, or -1 if the end of the stream has been reached.
     */
    fun peek(): Int {
        input.mark(1)
        val result = input.read()
        input.reset()
        return result
    }

    /**
     * Reads the next character from the input stream and advances the current position.
     * @return The next character in the input stream, or -1 if the end of the stream has been reached.
     */
    fun read(): Int {
        val temp = input.read()
        column++
        if (temp == '\n'.code) {
            row++
            column = 1
        }
        return temp
    }

    /**
     * Checks if the end of the input stream has been reached.
     * @return True if the end of the stream has been reached, false otherwise.
     */
    fun eof(): Boolean {
        return peek() == -1
    }

    /**
     * Returns the current position in the input stream.
     * @return A pair containing the current row and column.
     */
    fun getPosition(): Pair<Int, Int> = Pair(row, column)

    /**
     * Sets the current position in the input stream.
     * @param row The new row position.
     * @param column The new column position.
     */
    fun setPosition(row: Int, column: Int) {
        this.row = row
        this.column = column
    }

    /**
     * Scans the input stream and returns the next token.
     * @return The next token in the input stream.
     */
    fun nextToken(): Token {
        var currentState = Automat.START_STATE
        val lexem = StringBuilder()
        val startColumn = column
        val startRow = row

        while (true) {
            val nextChar = peek()

            // Handling EOF
            if (nextChar == -1) {
                if (lexem.isEmpty()) {
                    // The end with no lexeme -> retuning EOF
                    return Token(
                        lexem = "",
                        column = startColumn,
                        row = startRow,
                        token = TokenType.EOF.ordinal,
                        tokenType = TokenType.EOF.ordinal,
                        eof = true
                    )
                }
            }

            val tempState = automat.getNextState(currentState, nextChar)

            if (tempState != Automat.NO_EDGE) {
                currentState = tempState
                lexem.append(read().toChar())
            } else {
                if (automat.isFiniteState(currentState)) {
                    val tokenTypeValue = automat.getFiniteState(currentState)
                    val token = Token(
                        lexem.toString(),
                        startColumn,
                        startRow,
                        automat.getFiniteState(currentState),
                        tokenType = tokenTypeValue,
                        eof()
                    )
                    if (token.getToken() == TokenType.IGNORE.ordinal) {
                        return nextToken()
                    } else {
                        return token
                    }
                } else {
                    // Only return ERROR if it is not an EOF
                    if (!eof()) {
                        return Token(
                            lexem = lexem.toString(),
                            column = startColumn,
                            row = startRow,
                            token = TokenType.ERROR.ordinal,
                            tokenType = TokenType.ERROR.ordinal,
                            eof = false
                        )
                    } else {
                        // At EOF with no valid token -> return EOF
                        return Token(
                            lexem = "",
                            column = startColumn,
                            row = startRow,
                            token = TokenType.EOF.ordinal,
                            tokenType = TokenType.EOF.ordinal,
                            eof = true
                        )
                    }
                }
            }
        }
    }

}
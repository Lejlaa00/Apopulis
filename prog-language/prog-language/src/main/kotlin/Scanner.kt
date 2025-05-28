import java.io.InputStream

class Scanner(private val input: InputStream) {
    var row = 1
    var column = 0
    private val automat = Automat()

    fun peek(): Int {
        input.mark(1)
        val result = input.read()
        input.reset()
        return result
    }

    fun read(): Int {
        val temp = input.read()
        column++
        if (temp == '\n'.code) {
            row++
            column = 1
        }
        return temp
    }

    fun eof(): Boolean {
        return peek() == -1
    }

    fun getPosition(): Pair<Int, Int> = Pair(row, column)
    fun setPosition(row: Int, column: Int) {
        this.row = row
        this.column = column
    }

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
/**
 * Represents a lexical token in the city infrastructure DSL.
 * Each token contains information about its type, lexeme (actual text),
 * line number where it appears, and optional literal value.
 */
class Token(
    /** The type/category of this token */
    private val lexem: String,
    private val column: Int,
    private val row: Int,
    private val token: Int,
    val tokenType: Int,
    private val eof: Boolean
) {
    /**
     * Returns the lexeme of this token.
     */
    fun getLexem(): String = lexem

    /**
     * Returns the column number of this token.
     */
    fun getColumn(): Int = column
    fun getRow(): Int = row
    fun getToken(): Int = token
    fun isEof(): Boolean = eof


    fun getTokenSubtype(): String {
        return when (tokenType) {
            TokenType.ERROR.ordinal -> "error"
            TokenType.EOF.ordinal -> "eof"
            TokenType.NUMBER.ordinal -> "number"

            TokenType.IDENTIFIER.ordinal -> {
                when (lexem.lowercase()) {
                    "region" -> "region"
                    "city" -> "city"
                    "road" -> "road"
                    "building" -> "building"
                    "news" -> "news"
                    "park" -> "park"
                    "lake" -> "lake"
                    "junction" -> "junction"
                    "marker" -> "marker"
                    "procedure" -> "procedure"
                    "unknown" -> "unknown"
                    "let" -> "let"
                    "foreach" -> "foreach"
                    "in" -> "in"
                    "translate" -> "translate"
                    "line" -> "line"
                    "bend" -> "bend"
                    "box" -> "box"
                    "circ" -> "circ"
                    "if" -> "if"
                    "else" -> "else"
                    "for" -> "for"
                    "to" -> "to"
                    "check" -> "check"
                    "validate" -> "validate"
                    "fst" -> "fst"
                    "snd" -> "snd"
                    "nil" -> "nil"
                    "set" -> "set"
                    "highlight" -> "highlight"
                    "neigh" -> "neigh"
                    else -> "identifier"
                }
            }

            TokenType.OPERATOR.ordinal -> {
                when (lexem) {
                    "+" -> "plus"
                    "-" -> "minus"
                    "*" -> "times"
                    "/" -> "divide"
                    "<" -> "lessthan"
                    ">" -> "greaterthan"
                    else -> "operator"
                }
            }

            TokenType.LBRACE.ordinal -> "lbrace"
            TokenType.RBRACE.ordinal -> "rbrace"
            TokenType.LPAREN.ordinal -> "lparen"
            TokenType.RPAREN.ordinal -> "rparen"
            TokenType.LBRACKET.ordinal -> "lbracket"
            TokenType.RBRACKET.ordinal -> "rbracket"
            TokenType.COMMA.ordinal -> "comma"
            TokenType.SEMI.ordinal -> "semi"
            TokenType.EQUALS.ordinal -> "equals"
            TokenType.STRING.ordinal -> "string"
            else -> "unknown"
        }
    }

    override fun toString(): String {
        //return "Token(lexem='$lexem', row=$row, column=$column, token=${TokenType.values()[token]}, eof=$eof)"
        return "${getTokenSubtype()}($lexem)"
    }

}
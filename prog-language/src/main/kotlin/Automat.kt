
class Automat {
    companion object {
        const val MAX_STATE = 100
        const val NO_EDGE = -1
        const val START_STATE = 0

        // Sates for keywords
        private const val REGION_STATE = 1
        private const val CITY_STATE = 2
        private const val ROAD_STATE = 3
        private const val BUILDING_STATE = 4
        private const val NEWS_STATE = 5
        private const val PARK_STATE = 6
        private const val LAKE_STATE = 7
        private const val JUNCTION_STATE = 8
        private const val MARKER_STATE = 9
        private const val PROCEDURE_STATE = 10
        private const val UNKNOWN_STATE = 11
        private const val LET_STATE = 12
        private const val FOREACH_STATE = 13
        private const val IN_STATE = 14
        private const val TRANSLATE_STATE = 15
        private const val IF_STATE = 16
        private const val ELSE_STATE = 17
        private const val FOR_STATE = 18
        private const val TO_STATE = 19
        private const val CHECK_STATE = 20
        private const val VALIDATE_STATE = 21
        private const val FST_STATE = 22
        private const val SND_STATE = 23
        private const val NIL_STATE = 24
        private const val SET_STATE = 25
        private const val HIGHLIGHT_STATE = 26
        private const val NEIGH_STATE = 27

        // States for commands
        private const val LINE_STATE = 30
        private const val BEND_STATE = 31
        private const val BOX_STATE = 32
        private const val CIRC_STATE = 33

        // States for symbols
        private const val LBRACE_STATE = 40
        private const val RBRACE_STATE = 41
        private const val LPAREN_STATE = 42
        private const val RPAREN_STATE = 43
        private const val COMMA_STATE = 44
        private const val SEMI_STATE = 45
        private const val EQUALS_STATE = 46
        private const val LBRACKET_STATE = 47
        private const val RBRACKET_STATE = 48

        // States for operators
        private const val PLUS_STATE = 50
        private const val MINUS_STATE = 51
        private const val MULTIPLY_STATE = 52
        private const val DIVIDE_STATE = 53
        private const val LESSTHAN_STATE = 54
        private const val GREATERTHAN_STATE = 55

        // States for literals
        private const val NUMBER_STATE = 60
        private const val DECIMAL_STATE = 61
        private const val IDENTIFIER_STATE = 62
        private const val STRING_STATE = 63
        private const val STRING_CONTENT_STATE = 64

        // Special token state
        private const val IGNORE_STATE = 70
    }

    private val automata = Array(MAX_STATE + 1) { IntArray(256) { NO_EDGE } }
    private val finite = IntArray(MAX_STATE + 1) { TokenType.ERROR.ordinal }

    init {

        initializeKeywords()
        initializeCommands()
        initializeSymbols()
        initializeOperators()
        initializeNumbers()
        initializeIdentifiers()
        initializeStrings()
        initializeWhitespace()

    }

    private fun initializeKeywords() {
        addKeyword("region", TokenType.REGION, REGION_STATE)
        addKeyword("city", TokenType.CITY, CITY_STATE)
        addKeyword("road", TokenType.ROAD, ROAD_STATE)
        addKeyword("building", TokenType.BUILDING, BUILDING_STATE)
        addKeyword("news", TokenType.NEWS, NEWS_STATE)
        addKeyword("park", TokenType.PARK, PARK_STATE)
        addKeyword("lake", TokenType.LAKE, LAKE_STATE)
        addKeyword("junction", TokenType.JUNCTION, JUNCTION_STATE)
        addKeyword("marker", TokenType.MARKER, MARKER_STATE)
        addKeyword("procedure", TokenType.PROCEDURE, PROCEDURE_STATE)
        addKeyword("unknown", TokenType.UNKNOWN, UNKNOWN_STATE)
        addKeyword("let", TokenType.LET, LET_STATE)
        addKeyword("foreach", TokenType.FOREACH, FOREACH_STATE)
        addKeyword("in", TokenType.IN, IN_STATE)
        addKeyword("translate", TokenType.TRANSLATE, TRANSLATE_STATE)
        addKeyword("if", TokenType.IF, IF_STATE)
        addKeyword("else", TokenType.ELSE, ELSE_STATE)
        addKeyword("for", TokenType.FOR, FOR_STATE)
        addKeyword("to", TokenType.TO, TO_STATE)
        addKeyword("check", TokenType.CHECK, CHECK_STATE)
        addKeyword("validate", TokenType.VALIDATE, VALIDATE_STATE)
        addKeyword("fst", TokenType.FST, FST_STATE)
        addKeyword("snd", TokenType.SND, SND_STATE)
        addKeyword("nil", TokenType.NIL, NIL_STATE)
        addKeyword("set", TokenType.SET, SET_STATE)
        addKeyword("highlight", TokenType.SET, HIGHLIGHT_STATE)
        addKeyword("neigh", TokenType.SET, NEIGH_STATE)
    }

    private fun initializeCommands() {
        addKeyword("line", TokenType.LINE, LINE_STATE)
        addKeyword("bend", TokenType.BEND, BEND_STATE)
        addKeyword("box", TokenType.BOX, BOX_STATE)
        addKeyword("circ", TokenType.CIRC, CIRC_STATE)
    }

    private fun initializeSymbols() {
        automata[START_STATE]['{'.code] = LBRACE_STATE
        finite[LBRACE_STATE] = TokenType.LBRACE.ordinal

        automata[START_STATE]['}'.code] = RBRACE_STATE
        finite[RBRACE_STATE] = TokenType.RBRACE.ordinal

        automata[START_STATE]['['.code] = LBRACKET_STATE
        finite[LBRACKET_STATE] = TokenType.LBRACKET.ordinal

        automata[START_STATE][']'.code] = RBRACKET_STATE
        finite[RBRACKET_STATE] = TokenType.RBRACKET.ordinal

        automata[START_STATE]['('.code] = LPAREN_STATE
        finite[LPAREN_STATE] = TokenType.LPAREN.ordinal

        automata[START_STATE][')'.code] = RPAREN_STATE
        finite[RPAREN_STATE] = TokenType.RPAREN.ordinal

        automata[START_STATE][','.code] = COMMA_STATE
        finite[COMMA_STATE] = TokenType.COMMA.ordinal

        automata[START_STATE][';'.code] = SEMI_STATE
        finite[SEMI_STATE] = TokenType.SEMI.ordinal

        automata[START_STATE]['='.code] = EQUALS_STATE
        finite[EQUALS_STATE] = TokenType.EQUALS.ordinal
    }

    private fun initializeOperators() {
        automata[START_STATE]['+'.code] = PLUS_STATE
        finite[PLUS_STATE] = TokenType.OPERATOR.ordinal

        automata[START_STATE]['-'.code] = MINUS_STATE
        finite[MINUS_STATE] = TokenType.OPERATOR.ordinal

        automata[START_STATE]['*'.code] = MULTIPLY_STATE
        finite[MULTIPLY_STATE] = TokenType.OPERATOR.ordinal

        automata[START_STATE]['/'.code] = DIVIDE_STATE
        finite[DIVIDE_STATE] = TokenType.OPERATOR.ordinal

        automata[START_STATE]['<'.code] = LESSTHAN_STATE
        finite[LESSTHAN_STATE] = TokenType.OPERATOR.ordinal

        automata[START_STATE]['>'.code] = GREATERTHAN_STATE
        finite[GREATERTHAN_STATE] = TokenType.OPERATOR.ordinal
    }

    private fun initializeNumbers() {
        // Integer part
        for (c in '0'..'9') {
            automata[START_STATE][c.code] = NUMBER_STATE
            automata[NUMBER_STATE][c.code] = NUMBER_STATE
        }

        // Decimal part
        automata[NUMBER_STATE]['.'.code] = DECIMAL_STATE
        for (c in '0'..'9') {
            automata[DECIMAL_STATE][c.code] = DECIMAL_STATE
        }

        finite[NUMBER_STATE] = TokenType.NUMBER.ordinal
        finite[DECIMAL_STATE] = TokenType.NUMBER.ordinal

        // Prevent invalid number formats
        automata[DECIMAL_STATE]['.'.code] = NO_EDGE  // No multiple decimal points
        for (c in 'a'..'z') {
            automata[NUMBER_STATE][c.code] = NO_EDGE
            automata[DECIMAL_STATE][c.code] = NO_EDGE
        }
        for (c in 'A'..'Z') {
            automata[NUMBER_STATE][c.code] = NO_EDGE
            automata[DECIMAL_STATE][c.code] = NO_EDGE
        }
        automata[NUMBER_STATE]['_'.code] = NO_EDGE
        automata[DECIMAL_STATE]['_'.code] = NO_EDGE
    }

    private fun initializeIdentifiers() {
        // First character
        for (c in 'a'..'z') automata[START_STATE][c.code] = IDENTIFIER_STATE
        for (c in 'A'..'Z') automata[START_STATE][c.code] = IDENTIFIER_STATE
        automata[START_STATE]['_'.code] = IDENTIFIER_STATE


        // Subsequent characters
        for (c in 'a'..'z') automata[IDENTIFIER_STATE][c.code] = IDENTIFIER_STATE
        for (c in 'A'..'Z') automata[IDENTIFIER_STATE][c.code] = IDENTIFIER_STATE
        for (c in '0'..'9') automata[IDENTIFIER_STATE][c.code] = IDENTIFIER_STATE
        automata[IDENTIFIER_STATE]['_'.code] = IDENTIFIER_STATE


        finite[IDENTIFIER_STATE] = TokenType.IDENTIFIER.ordinal
    }

    private fun initializeStrings() {
        automata[START_STATE]['"'.code] = STRING_CONTENT_STATE

        // String content (all ASCII chars except ")
        for (c in 32..126) {
            if (c != '"'.code) {
                automata[STRING_CONTENT_STATE][c] = STRING_CONTENT_STATE
            }
        }

        // Closing quote
        automata[STRING_CONTENT_STATE]['"'.code] = STRING_STATE
        finite[STRING_STATE] = TokenType.STRING.ordinal
    }

    private fun initializeWhitespace() {
        automata[START_STATE][' '.code] = IGNORE_STATE
        automata[START_STATE]['\t'.code] = IGNORE_STATE
        automata[START_STATE]['\n'.code] = IGNORE_STATE
        automata[START_STATE]['\r'.code] = IGNORE_STATE

        // Continue whitespace
        automata[IGNORE_STATE][' '.code] = IGNORE_STATE
        automata[IGNORE_STATE]['\t'.code] = IGNORE_STATE
        automata[IGNORE_STATE]['\n'.code] = IGNORE_STATE
        automata[IGNORE_STATE]['\r'.code] = IGNORE_STATE

        finite[IGNORE_STATE] = TokenType.IGNORE.ordinal
    }

    private fun addKeyword(keyword: String, tokenType: TokenType, baseState: Int) {
        var currentState = START_STATE
        for (i in keyword.indices) {
            val c = keyword[i]
            val nextState = baseState + i
            if (automata[currentState][c.code] == NO_EDGE) {
                automata[currentState][c.code] = nextState
            }
            currentState = automata[currentState][c.code]
        }
        finite[currentState] = tokenType.ordinal

        for (c in 'a'..'z') automata[currentState][c.code] = NO_EDGE
        for (c in 'A'..'Z') automata[currentState][c.code] = NO_EDGE
        for (c in '0'..'9') automata[currentState][c.code] = NO_EDGE
        automata[currentState]['_'.code] = NO_EDGE
    }

    fun getNextState(currentState: Int, inputChar: Int): Int {
        if (currentState < 0 || currentState > MAX_STATE) return NO_EDGE
        if (inputChar < 0 || inputChar >= 256) return NO_EDGE
        return automata[currentState][inputChar]
    }

    fun isFiniteState(state: Int): Boolean {
        return finite[state] != TokenType.ERROR.ordinal
    }

    fun getFiniteState(state: Int): Int {
        return finite[state]
    }
}
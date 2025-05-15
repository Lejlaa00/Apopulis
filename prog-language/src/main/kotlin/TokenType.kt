enum class TokenType {
    // Keywords
    REGION, CITY, ROAD, BUILDING, NEWS, PARK, LAKE,
    JUNCTION, MARKER, PROCEDURE,
    UNKNOWN, LET, FOREACH, IN, IF, ELSE, FOR, TO ,

    // Commands
    LINE, BEND, BOX, CIRC, TRANSLATE, CHECK, VALIDATE,

    // Symbols
    LBRACE, RBRACE, LPAREN, RPAREN, COMMA, SEMI, EQUALS,

    // Operators
    OPERATOR, FST, SND, NIL, SET,

    // Literals
    NUMBER, IDENTIFIER, STRING,

    // Special
    IGNORE, ERROR, EOF
}
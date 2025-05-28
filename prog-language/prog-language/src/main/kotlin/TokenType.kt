/**
 * Defines all possible token types for the city infrastructure DSL lexical analysis.
 * This enum is used by the Scanner to classify different parts of the input code
 * into meaningful tokens that can be processed by the Parser.
 */
enum class TokenType {
    // Keywords
    REGION, CITY, ROAD, BUILDING, NEWS, PARK, LAKE,
    JUNCTION, MARKER, PROCEDURE,
    UNKNOWN, LET, FOREACH, IN, IF, ELSE, FOR, TO , HIGHLIGHT, NEIGH,

    // Commands
    LINE, BEND, BOX, CIRC, TRANSLATE, CHECK, VALIDATE,

    // Symbols
    LBRACE, RBRACE, LPAREN, RPAREN, LBRACKET, RBRACKET,COMMA, SEMI, EQUALS,

    // Operators
    OPERATOR, FST, SND, NIL, SET,

    // Literals
    NUMBER, IDENTIFIER, STRING,

    // Special
    IGNORE, ERROR, EOF
}
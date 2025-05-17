class Parser(val scanner: Scanner) {
    private var currentToken: Token? = null
    private var savedPosition: Pair<Int, Int> = Pair(1, 0)

    private fun savePosition() {
        savedPosition = Pair(scanner.row, scanner.column)
    }

    private fun restorePosition() {
        scanner.row = savedPosition.first
        scanner.column = savedPosition.second
    }

    fun nextToken() {
        currentToken = scanner.nextToken()
    }

    private fun match(expectedType: TokenType): Boolean {
        println("Matching $expectedType at ${scanner.row}:${scanner.column}")
        if (currentToken?.getTokenSubtype() == expectedType.toString().lowercase()) {
            nextToken()
            return true
        }
        println("ERROR: Expected $expectedType but got ${currentToken} at ${scanner.row}:${scanner.column}")
        return false
    }

    class PositionScope(private val parser: Parser) {
        fun <T> attempt(block: () -> T): T? {
            parser.savePosition()
            val result = block()
            return if (result != false) result else {
                parser.restorePosition()
                null
            }
        }
    }

    private inline fun <T> withPosition(block: PositionScope.() -> T): T {
        return PositionScope(this).block() ?: (false as T)
    }

    private fun handleErrorToken(): Boolean {
        if (currentToken?.getTokenSubtype() == "error") {
            println("Invalid token at ${scanner.row}:${scanner.column} - ${currentToken?.getTokenSubtype().toString()}")
            return false
        }
        return true
    }

    // <program> ::= <region_or_city>
    fun parseProgram(): Boolean {
        nextToken()
        if (!handleErrorToken()) return false

        val result = parseRegionOrCity()

        // Check for EOF
        val atEnd = when {
            currentToken == null -> true
            currentToken?.getTokenSubtype() == "eof" -> true
            currentToken?.getTokenSubtype() == "error" && scanner.eof() -> true
            else -> false
        }

        if (!handleErrorToken()) return false

        println("Parse result: $result, atEnd: $atEnd, currentToken: $currentToken")
        return result && atEnd
    }

    // <region_or_city> ::= <region> | <city>
    private fun parseRegionOrCity(): Boolean = withPosition {
        val regionAttempt = attempt { parseRegion() }
        if (regionAttempt != null) {
            println("Parsed REGION successfully")
            return true
        }

        val cityAttempt = attempt { parseCity() }
        if (cityAttempt != null) {
            println("Parsed CITY successfully")

            return true
        }

        println("Failed to parse both region and city")
        return false
    }

    // <region> ::= 'region' <string> '{' <region_body> '}'
    private fun parseRegion(): Boolean = withPosition {
        match(TokenType.REGION) &&
                match(TokenType.STRING) &&
                match(TokenType.LBRACE) &&
                parseRegionBody() &&
                match(TokenType.RBRACE)
    }

    // <region_body> ::= { <region_element> }*
    private fun parseRegionBody(): Boolean {
        println("Entering region body")
        while (true) {
            if (currentToken?.getTokenSubtype() == "rbrace") {
                //println("Found city closing brace")
                return true
            }

            savePosition()
            if (!parseRegionElement()) {
                println("No more region elements at ${scanner.row}:${scanner.column}")
                restorePosition()
                break
            }
        }
        return true
    }

    // <region_element> ::= <city> | <let_statement> | <foreach_statement> |
    //                      <translate_statement> | <procedure_def> | <call_statement> | <highlight>
    private fun parseRegionElement(): Boolean = withPosition {
        attempt { parseCity() }
            ?: attempt { parseLetStatement() }
            ?: attempt { parseForeachStatement() }
            ?: attempt { parseTranslateStatement() }
            ?: attempt { parseProcedureDef() }
            ?: attempt { parseCallStatement() }
            ?: attempt { parseHighlightStatement() }
            ?: false
    }

    // <city> ::= 'city' <string> '{' <city_body> '}'
    private fun parseCity(): Boolean = withPosition {
        match(TokenType.CITY) &&
                match(TokenType.STRING) &&
                match(TokenType.LBRACE) &&
                parseCityBody().also { println("City body parsed: $it") } &&
                match(TokenType.RBRACE).also { println("City closed successfully") }

    }

    // <city_body> ::= { <element> }*
    private fun parseCityBody(): Boolean {
        while (true) {
            if (currentToken?.getTokenSubtype() == "rbrace") {
               // println("Found city closing brace")
                return true
            }

            savePosition()
            if (!parseElement()) {
                println("No more city elements at ${scanner.row}:${scanner.column}")
                restorePosition()
                break
            }
        }
        return true
    }

    // <element> ::= <road> | <building> | <lake> | <park> | <news> | <junction> | <marker>
    //             | <let_statement> | <foreach_statement> | <translate_statement>
    //             | <validate_block> | <if_statement> | <for_statement>
    //             | <procedure_def> | <call_statement>
    private fun parseElement(): Boolean = withPosition {
        attempt { parseBuilding() }
            ?: attempt { parseRoad() }
            ?: attempt { parseLake() }
            ?: attempt { parsePark() }
            ?: attempt { parseNews() }
            ?: attempt { parseJunction() }
            ?: attempt { parseMarker() }
            ?: attempt { parseLetStatement() }
            ?: attempt { parseForeachStatement() }
            ?: attempt { parseTranslateStatement() }
            ?: attempt { parseValidateBlock() }
            ?: attempt { parseIfStatement() }
            ?: attempt { parseForStatement() }
            ?: attempt { parseProcedureDef() }
            ?: attempt { parseCallStatement() }
            ?: attempt { parseHighlightStatement() }
            ?: false
    }

    // <road> ::= 'road' <string> '{' <road_body> '}'
    private fun parseRoad(): Boolean = withPosition {
        match(TokenType.ROAD) &&
                match(TokenType.STRING) &&
                match(TokenType.LBRACE) &&
                parseRoadBody() &&
                match(TokenType.RBRACE).also { println("ROAD command parsed successfully") }
    }

    // <road_body> ::= { <road_step> }*
    private fun parseRoadBody(): Boolean {
        while (true) {
            savePosition()
            if (!parseRoadStep()) {
                restorePosition()
                break
            }
        }
        return true
    }

    // <road_step> ::= <command> | <let_statement> | <foreach_statement>
    //                | <translate_statement> | <if_statement> | <for_statement> | <call_statement>
    private fun parseRoadStep(): Boolean = withPosition {
        attempt { parseCommand() }
            ?: attempt { parseLetStatement() }
            ?: attempt { parseForeachStatement() }
            ?: attempt { parseTranslateStatement() }
            ?: attempt { parseIfStatement() }
            ?: attempt { parseForStatement() }
            ?: attempt { parseCallStatement() }
            ?: false
    }

    // <building> ::= 'building' <string> '{' <building_body> '}'
    private fun parseBuilding(): Boolean = withPosition {
        match(TokenType.BUILDING) &&
                match(TokenType.STRING) &&
                match(TokenType.LBRACE) &&
                parseBuildingBody().also { println("Building body result: $it") } &&
                match(TokenType.RBRACE).also { println("BuiUILDING command parsed successfully") }
    }

    // <building_body> ::= { <building_step> }*
    private fun parseBuildingBody(): Boolean {
        while (true) {
            if (currentToken?.getTokenSubtype() == "rbrace") {
                //println("Found closing brace at ${scanner.row}:${scanner.column}")
                return true
            }

            savePosition()
            if (!parseBuildingStep()) {
                println("No more building steps at ${scanner.row}:${scanner.column}")
                restorePosition()
                break
            }
        }
        return true
    }

    // <building_step> ::= <command> | <let_statement> | <foreach_statement>
    //                    | <translate_statement> | <if_statement> | <for_statement> | <call_statement>
    private fun parseBuildingStep(): Boolean = withPosition {
        attempt { parseCommand() }
            ?: attempt { parseLetStatement() }
            ?: attempt { parseForeachStatement() }
            ?: attempt { parseTranslateStatement() }
            ?: attempt { parseIfStatement() }
            ?: attempt { parseForStatement() }
            ?: attempt { parseCallStatement() }
            ?: false
    }

    // <lake> ::= 'lake' <string> '{' <lake_body> '}'
    private fun parseLake(): Boolean = withPosition {
        match(TokenType.LAKE) &&
                match(TokenType.STRING) &&
                match(TokenType.LBRACE) &&
                run { while (parseCommand()) { /* continue */ }; true } &&
                match(TokenType.RBRACE).also { println("LAKE command parsed successfully: $it")}
    }

    // <park> ::= 'park' <string> '{' <park_body> '}'
    private fun parsePark(): Boolean = withPosition {
        match(TokenType.PARK) &&
                match(TokenType.STRING) &&
                match(TokenType.LBRACE) &&
                run { while (parseCommand()) { /* continue */ }; true } &&
                match(TokenType.RBRACE).also { println("PARK command parsed successfully: $it")}
    }

    // <news> ::= 'news' <string> <location_or_unknown> <metadata_blocks> ';'
    private fun parseNews(): Boolean = withPosition {
        // Saving initial position
        savePosition()

        if (!match(TokenType.NEWS)) {
            restorePosition()
            return@withPosition false
        }

        if (!match(TokenType.STRING)) {
            restorePosition()
            return@withPosition false
        }

        savePosition()
        val hasLocation = parsePoint() || match(TokenType.UNKNOWN) || match(TokenType.IDENTIFIER)
        if (!hasLocation) {
            println("ERROR: Expected location point, 'unknown', or identifier at ${scanner.row}:${scanner.column}")
            restorePosition()
            return@withPosition false
        }

        savePosition()
        val hasMetadata = currentToken?.getTokenSubtype() == "lbrace" && parseMetadataBlocks()
        if (!hasMetadata) {
            restorePosition()
        }

        if (!match(TokenType.SEMI)) {
            println("ERROR: Expected semicolon after news statement at ${scanner.row}:${scanner.column}")
            restorePosition()
            return@withPosition false
        }

        true
    }

    // <location_or_unknown> ::= <point> | 'unknown'
    private fun parseLocationOrUnknown(): Boolean = withPosition {
        attempt { parsePoint() } ?: attempt { match(TokenType.UNKNOWN) } ?: false
    }


    //<metadata_blocks> ::= <metadata_block> | ε
    private fun parseMetadataBlocks(): Boolean = withPosition {
        if (!match(TokenType.LBRACE)) return@withPosition false

        if (!parseMetadataStatement()) {
            println("ERROR: Expected metadata statement at ${scanner.row}:${scanner.column}")
            return@withPosition false
        }

        while (parseMetadataStatement()) { /* continue */ }

        if (!match(TokenType.RBRACE)) {
            println("ERROR: Expected '}' to close metadata block at ${scanner.row}:${scanner.column}")
            return@withPosition false
        }

        true
    }

    // <metadata_block> ::= '{' <metadata_statement> { <metadata_statement> } '}'
    private fun parseMetadataBlock(): Boolean = withPosition {
        match(TokenType.LBRACE) &&
                parseMetadataStatement() &&
                run { while (parseMetadataStatement()) { /* continue */ }; true } &&
                match(TokenType.RBRACE)
    }

    // <metadata_statement> ::= 'set' '(' <string> ',' <expression> ')' ';'
    private fun parseMetadataStatement(): Boolean = withPosition {
        match(TokenType.SET) &&
                match(TokenType.LPAREN) &&
                match(TokenType.STRING) &&
                match(TokenType.COMMA) &&
                (match(TokenType.STRING) || parseExpression()) &&
                match(TokenType.RPAREN) &&
                match(TokenType.SEMI).also { println("METADATA STATEMENT command parsed successfully: $it")}
    }

    // <junction> ::= 'junction' <point> ';'
    private fun parseJunction(): Boolean = withPosition {
        match(TokenType.JUNCTION) &&
                parsePoint() &&
                match(TokenType.SEMI)
    }

    // <marker> ::= 'marker' <string> <point> <metadata_block> ';'
    private fun parseMarker(): Boolean = withPosition {
        savePosition()

        if (!match(TokenType.MARKER)) {
            restorePosition()
            return@withPosition false
        }

        if (!match(TokenType.STRING)) {
            restorePosition()
            return@withPosition false
        }

        if (!parsePoint()) {
            restorePosition()
            return@withPosition false
        }

        val metadataResult = withPosition {
            savePosition()
            if (currentToken?.getTokenSubtype() == "lbrace") {
                if (parseMetadataBlocks()) {
                    true
                } else {
                    restorePosition()
                    false
                }
            } else {
                restorePosition()
                false
            }
        }

        if (!match(TokenType.SEMI)) {
            println("ERROR: Expected semicolon after marker at ${scanner.row}:${scanner.column}")
            restorePosition()
            return@withPosition false
        }

        true
    }

    // <command> ::= <draw_line> | <draw_bend> | <draw_box> | <draw_circle> | <call_statement> | <highlight_statement>
    private fun parseCommand(): Boolean = withPosition {
        attempt { parseDrawBox() }
            ?: attempt { parseHighlightStatement() }
            ?: attempt { parseDrawBend() }
            ?: attempt { parseDrawLine() }
            ?: attempt { parseDrawCircle() }
            ?: attempt { parseCallStatement() }
            ?: false
    }

   //<highlight_statement> ::= 'highlight' <expression> ';'
   private fun parseHighlightStatement(): Boolean = withPosition {
       savePosition()

       if (!match(TokenType.HIGHLIGHT)) {
           restorePosition()
           return@withPosition false
       }

       if (!parseExpression()) {
           println("Failed to parse highlight expression")
           restorePosition()
           return@withPosition false
       }

       if (!match(TokenType.SEMI)) {
           println("Missing semicolon after highlight")
           restorePosition()
           return@withPosition false
       }

       true
   }

    // draw_line ::= 'line' '(' <point> ',' <point> ')' ';'
    private fun parseDrawLine(): Boolean = withPosition {
        println("Parsing LINE command at ${scanner.row}:${scanner.column}")
        match(TokenType.LINE) &&
                match(TokenType.LPAREN) &&
                parsePoint() &&
                match(TokenType.COMMA) &&
                parsePoint() &&
                match(TokenType.RPAREN) &&
                match(TokenType.SEMI).also { println("LINE command parsed successfully: $it")}
    }

    // draw_bend ::= 'bend' '(' <point> ',' <point> ',' <expression> ')' ';'
    private fun parseDrawBend(): Boolean = withPosition {
        match(TokenType.BEND) &&
                match(TokenType.LPAREN) &&
                parsePoint() &&
                match(TokenType.COMMA) &&
                parsePoint() &&
                match(TokenType.COMMA) &&
                parseExpression() &&
                match(TokenType.RPAREN) &&
                match(TokenType.SEMI).also { println("BEND command parsed successfully: $it")}
    }

    // draw_box ::= 'box' '(' <point> ',' <point> ')' ';'
    private fun parseDrawBox(): Boolean = withPosition {
        match(TokenType.BOX) &&
                match(TokenType.LPAREN) &&
                parsePoint() &&
                match(TokenType.COMMA) &&
                parsePoint() &&
                match(TokenType.RPAREN) &&
                match(TokenType.SEMI).also { println("BOX command parsed successfully: $it")}
    }

    // draw_circle ::= 'circ' '(' <point> ',' <expression> ')' ';'
    private fun parseDrawCircle(): Boolean = withPosition {
        match(TokenType.CIRC) &&
                match(TokenType.LPAREN) &&
                parsePoint() &&
                match(TokenType.COMMA) &&
                parseExpression() &&
                match(TokenType.RPAREN) &&
                match(TokenType.SEMI).also { println("CIRCLE command parsed successfully: $it")}
    }

    // <let_statement> ::= 'let' <identifier> '=' (<expression> | <point> | <list> | <string>) ';'
    private fun parseLetStatement(): Boolean = withPosition {
        // Save position for backtracking
        savePosition()

        if (!match(TokenType.LET)) {
            restorePosition()
            return@withPosition false
        }

        if (!match(TokenType.IDENTIFIER)) {
            restorePosition()
            return@withPosition false
        }

        if (!match(TokenType.EQUALS)) {
            restorePosition()
            return@withPosition false
        }

        when {
            parseList() -> {}  // First try lists since they start with [
            parsePoint() -> {} // Then try points
            parseExpression() -> {} // Then expressions
            match(TokenType.STRING) -> {} // Finally strings
            else -> {
                println("ERROR: Expected expression, point, list or string after '=' at ${scanner.row}:${scanner.column}")
                restorePosition()
                return@withPosition false
            }
        }

        // Require semicolon
        if (!match(TokenType.SEMI)) {
            println("ERROR: Expected ';' after let statement at ${scanner.row}:${scanner.column}")
            restorePosition()
            return@withPosition false
        }

        true
    }

    // <list> ::= '[' (<expression> | <point>) (',' (<expression> | <point>))* ']'
    private fun parseList(): Boolean = withPosition {
        match(TokenType.LBRACKET) && run {
            if (!parseExpression() && !parsePoint()) {
                return@run false
            }

            while (match(TokenType.COMMA)) {
                if (!parseExpression() && !parsePoint()) {
                    return@run false
                }
            }

            true
        } && match(TokenType.RBRACKET)
    }

    // <foreach_statement> ::= 'foreach' <identifier> 'in' <expression> '{' <foreach_body> '}'
    private fun parseForeachStatement(): Boolean = withPosition {
        match(TokenType.FOREACH) &&
                match(TokenType.IDENTIFIER) &&
                match(TokenType.IN) &&
                parseExpression() &&
                match(TokenType.LBRACE) &&
                parseForeachBody() &&
                match(TokenType.RBRACE).also { println("FOREACH command parsed successfully: $it")}
    }

    // <foreach_body> ::= { <statement> }*
    private fun parseForeachBody(): Boolean {
        while (parseStatement()) { /* continue */ }
        return true
    }

    //<translate_statement> ::= 'translate' (<point> | <identifier>) '{' <translate_body> '}'
    private fun parseTranslateStatement(): Boolean = withPosition {
        // Saving initial position
        savePosition()

        if (!match(TokenType.TRANSLATE)) {
            restorePosition()
            return@withPosition false
        }

        val hasTarget = when {
            parsePoint() -> true
            match(TokenType.IDENTIFIER) -> true
            else -> {
                println("ERROR: Expected point or identifier after 'translate' at ${scanner.row}:${scanner.column}")
                restorePosition()
                return@withPosition false
            }
        }

        if (!match(TokenType.LBRACE)) {
            println("ERROR: Expected '{' after translate target at ${scanner.row}:${scanner.column}")
            restorePosition()
            return@withPosition false
        }

        if (!parseTranslateBody()) {
            restorePosition()
            return@withPosition false
        }

        if (!match(TokenType.RBRACE)) {
            println("ERROR: Expected '}' to close translate block at ${scanner.row}:${scanner.column}")
            restorePosition()
            return@withPosition false
        }

        true
    }

    // <translate_body> ::= { <statement> }*
    private fun parseTranslateBody(): Boolean {
        while (parseStatement()) { /* continue */ }
        return true
    }

    // <validate_block> ::= 'validate' '{' <validate_statement> <validate_block_tail> '}'
    private fun parseValidateBlock(): Boolean = withPosition {
        match(TokenType.VALIDATE) &&
                match(TokenType.LBRACE) &&
                parseValidateStatement() &&
                run { while (parseValidateStatement()) { /* continue */ }; true } &&
                match(TokenType.RBRACE)
    }

    // <validate_statement> ::= 'check' '(' <identifier> ')' ';'
    private fun parseValidateStatement(): Boolean = withPosition {
        match(TokenType.CHECK) &&
                match(TokenType.LPAREN) &&
                match(TokenType.IDENTIFIER) &&
                match(TokenType.RPAREN) &&
                match(TokenType.SEMI).also { println("VALIDATING command parsed successfully: $it")}
    }

    // <if_statement> ::= 'if' <expression> '{' <statement_list> '}' <else_opt>
    private fun parseIfStatement(): Boolean = withPosition {
        match(TokenType.IF) &&
                parseExpression() &&
                match(TokenType.LBRACE) &&
                parseStatementList() &&
                match(TokenType.RBRACE) &&
                parseElseOpt().also { println("IF command parsed successfully: $it")}
    }

    // <else_opt> ::= 'else' '{' <statement_list> '}' | ε
    private fun parseElseOpt(): Boolean = withPosition {
        if (match(TokenType.ELSE)) {
            match(TokenType.LBRACE) &&
                    parseStatementList() &&
                    match(TokenType.RBRACE)
        } else {
            true
        }
    }

    // <for_statement> ::= 'for' <identifier> '=' <expression> 'to' <expression> '{' <statement_list> '}'
    private fun parseForStatement(): Boolean = withPosition {
        match(TokenType.FOR) &&
                match(TokenType.IDENTIFIER) &&
                match(TokenType.EQUALS) &&
                parseExpression() &&
                match(TokenType.TO) &&
                parseExpression() &&
                match(TokenType.LBRACE) &&
                parseStatementList() &&
                match(TokenType.RBRACE).also { println("FOR command parsed successfully: $it")}
    }

    // <procedure_def> ::= 'procedure' <identifier> '(' <parameter_list> ')' '{' <statement_list> '}'
    private fun parseProcedureDef(): Boolean = withPosition {
        match(TokenType.PROCEDURE) &&
                match(TokenType.IDENTIFIER) &&
                match(TokenType.LPAREN) &&
                parseParameterList() &&
                match(TokenType.RPAREN) &&
                match(TokenType.LBRACE) &&
                parseStatementList() &&
                match(TokenType.RBRACE).also { println("PROCEDURE command parsed successfully: $it")}
    }

    // <parameter_list> ::= <identifier> ( ',' <identifier> )* | ε
    private fun parseParameterList(): Boolean = withPosition {
        if (currentToken != null && currentToken?.getTokenSubtype() == TokenType.IDENTIFIER.toString().lowercase()) {
            match(TokenType.IDENTIFIER) && run {
                while (match(TokenType.COMMA)) {
                    if (!match(TokenType.IDENTIFIER)) return@run false
                }
                true
            }
        } else {
            true
        }
    }

    // <call_statement> ::= <identifier> '(' <arg_list> ')' ';'
    private fun parseCallStatement(): Boolean = withPosition {
        // Saving initial position
        savePosition()

        if (!match(TokenType.IDENTIFIER)) {
            restorePosition()
            return@withPosition false
        }

        if (!match(TokenType.LPAREN)) {
            restorePosition()
            return@withPosition false
        }

        if (!parseArgList()) {
            restorePosition()
            return@withPosition false
        }

        if (!match(TokenType.RPAREN)) {
            println("ERROR: Expected ')' after argument list at ${scanner.row}:${scanner.column}")
            restorePosition()
            return@withPosition false
        }

        if (!match(TokenType.SEMI)) {
            println("ERROR: Expected ';' after call statement at ${scanner.row}:${scanner.column}")
            restorePosition()
            return@withPosition false
        }

        true
    }

    // <arg_list> ::= <expression> ( ',' <expression> )* | ε
    private fun parseArgList(): Boolean = withPosition {
        if (currentToken?.getTokenSubtype() == "rparen") {
            return true
        }

        if (!parseExpression()) {
            return false
        }

        while (match(TokenType.COMMA)) {
            if (!parseExpression()) {
                return false
            }
        }

        true
    }

    // <statement_list> ::= { <statement> }*
    private fun parseStatementList(): Boolean {
        while (parseStatement()) { /* continue */ }
        return true
    }

    // <statement> ::= <command> | <let_statement> | <foreach_statement>
    //              | <translate_statement> | <validate_block> | <if_statement>
    //              | <for_statement> | <call_statement> | <marker> | <news> | <junction>
    private fun parseStatement(): Boolean = withPosition {
        attempt { parseCommand() }
            ?: attempt { parseLetStatement() }
            ?: attempt { parseForeachStatement() }
            ?: attempt { parseTranslateStatement() }
            ?: attempt { parseValidateBlock() }
            ?: attempt { parseIfStatement() }
            ?: attempt { parseForStatement() }
            ?: attempt { parseCallStatement() }
            ?: attempt { parseMarker() }
            ?: attempt { parseNews() }
            ?: attempt { parseJunction() }
            ?: false
    }

    // <point> ::= '(' <expression> ',' <expression> ')' | <identifier>
    private fun parsePoint(): Boolean = withPosition {
        savePosition()

        if (match(TokenType.IDENTIFIER)) {
            return@withPosition true
        }
        restorePosition()

        if (match(TokenType.LPAREN) &&
            parseExpression() &&
            match(TokenType.COMMA).also {
                if (!it) println("ERROR: Missing comma in point at ${scanner.row}:${scanner.column}")
            } &&
            parseExpression() &&
            match(TokenType.RPAREN).also {
                if (!it) println("ERROR: Missing closing parenthesis at ${scanner.row}:${scanner.column}")
            }
        ) {
            return@withPosition true
        }

        restorePosition()
        false
    }

    // <expression> ::= <term> <expression_prime> | <neigh>
    private fun parseExpression(): Boolean = withPosition {
        savePosition()
        if (parseNeigh()) {
            return@withPosition true
        }
        restorePosition()

        parseTerm() && parseExpressionPrime()
    }

    // <expression_prime> ::= <operand> <term> <expression_prime> | ε
    private fun parseExpressionPrime(): Boolean = withPosition {
        if (currentToken?.getTokenSubtype() in setOf("plus", "minus", "times", "divide", "lessthan", "greaterthan")) {
            nextToken()
            parseTerm() && parseExpressionPrime()
        } else {
            true
        }
    }

    // <term> ::= <number> | <identifier> | '(' <expression> ')'
//           | 'fst' '(' <expression> ')' | 'snd' '(' <expression> ')' | 'nil'
    private fun parseTerm(): Boolean = withPosition {
        println("Parsing TERM at ${scanner.row}:${scanner.column} - ${currentToken}")

        when (currentToken?.getTokenSubtype()) {
            "number" -> {
                nextToken()
                true
            }
            TokenType.IDENTIFIER.toString().lowercase() -> {
                nextToken()
                true
            }
            TokenType.NIL.toString().lowercase() -> {
                nextToken()
                true
            }
            TokenType.FST.toString().lowercase(),
            TokenType.SND.toString().lowercase() -> {
                val type = currentToken?.getTokenSubtype()
                nextToken() // consume fst/snd

                match(TokenType.LPAREN).also {
                    if (!it) println("ERROR: Missing '(' after $type at ${scanner.row}:${scanner.column}")
                } &&
                        parseExpression().also {
                            if (!it) println("ERROR: Expected expression after $type at ${scanner.row}:${scanner.column}")
                        } &&
                        match(TokenType.RPAREN).also {
                            if (!it) println("ERROR: Missing ')' after $type argument at ${scanner.row}:${scanner.column}")
                        }
            }
            TokenType.LPAREN.toString() -> {
                match(TokenType.LPAREN) &&
                        parseExpression() &&
                        match(TokenType.RPAREN).also {
                            if (!it) println("ERROR: Missing closing parenthesis at ${scanner.row}:${scanner.column}")
                        }
            }
            else -> false
        }
    }

    // <neigh> ::= 'neigh' '(' <expression> ',' <expression> ')'
    private fun parseNeigh(): Boolean = withPosition {
        savePosition()

        if (!match(TokenType.NEIGH)) {
            restorePosition()
            return false
        }

        if (!match(TokenType.LPAREN)) {
            println("ERROR: Missing '(' after 'neigh' at ${scanner.row}:${scanner.column}")
            restorePosition()
            return false
        }

        if (!parseExpression()) {
            println("ERROR: Expected first expression argument for 'neigh' at ${scanner.row}:${scanner.column}")
            restorePosition()
            return false
        }

        if (!match(TokenType.COMMA)) {
            println("ERROR: Missing comma between 'neigh' arguments at ${scanner.row}:${scanner.column}")
            restorePosition()
            return false
        }

        if (!parseExpression()) {
            println("ERROR: Expected second expression argument for 'neigh' at ${scanner.row}:${scanner.column}")
            restorePosition()
            return false
        }

        if (!match(TokenType.RPAREN)) {
            println("ERROR: Missing closing ')' for 'neigh' at ${scanner.row}:${scanner.column}")
            restorePosition()
            return false
        }

        true
    }
}
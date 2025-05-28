import ast.*

/**
 * Parses a sequence of tokens into an Abstract Syntax Tree (AST) for the city infrastructure DSL.
 * Implements recursive descent parsing to handle the grammar rules of the language.
 */
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

    private inline fun <T> withPosition(block: PositionScope.() -> T?): T? {
        return PositionScope(this).block()
    }


    private fun handleErrorToken(): Boolean {
        if (currentToken?.getTokenSubtype() == "error") {
            println("Invalid token at ${scanner.row}:${scanner.column} - ${currentToken?.getTokenSubtype().toString()}")
            return false
        }
        return true
    }

    private fun toPoint(expr: Expression): Point? {
        return when (expr) {
            is Point -> expr
            is PointExpr -> {
                val x = evaluateNumeric(expr.x) ?: return null
                val y = evaluateNumeric(expr.y) ?: return null
                Point(x, y)
            }

            else -> null
        }
    }

    private fun toLocation(expr: Expression): Location? {
        return when (expr) {
            is Point -> expr
            is PointExpr -> PointExpr(expr.x, expr.y)
            is Identifier -> LocationIdentifier(expr.name)
            else -> null
        }
    }


    // <program> ::= <region_or_city>
    fun parseProgram(): ProgramNode? {
        nextToken()
        if (!handleErrorToken()) return null

        val items = mutableListOf<ASTNode>()

        while (!scanner.eof() && currentToken?.getTokenSubtype() != "eof") {
            val item = parseRegionOrCity() ?: return null
            items.add(item)
        }

        return ProgramNode(items)
    }


    // <region_or_city> ::= <region> | <city>
    private fun parseRegionOrCity(): ASTNode? = withPosition {
        attempt { parseRegion() } ?: attempt { parseCity() }
    }

    // <region> ::= 'region' <string> '{' <region_body> '}'
    private fun parseRegion(): RegionNode? = withPosition {
        if (!match(TokenType.REGION)) return@withPosition null

        val nameToken = currentToken
        if (!match(TokenType.STRING)) return@withPosition null
        val regionName = nameToken?.getLexem()?.removeSurrounding("\"") ?: return@withPosition null

        if (!match(TokenType.LBRACE)) return@withPosition null

        val body = mutableListOf<ASTNode>()
        while (currentToken?.getTokenSubtype() != "rbrace" && !scanner.eof()) {
            savePosition()
            val element = parseRegionElement()
            if (element != null) {
                body.add(element)
            } else {
                restorePosition()
                break
            }
        }

        if (!match(TokenType.RBRACE)) return@withPosition null

        return@withPosition RegionNode(regionName, body)
    }

    // <region_element> ::= <city> | <let_statement> | <foreach_statement> |
    // <translate_statement> | <procedure_def> | <call_statement> | <highlight>
    private fun parseRegionElement(): ASTNode? = withPosition {
        attempt { parseCity() }
            ?: attempt { parseLetStatement() }
            ?: attempt { parseForeachStatement() }
            ?: attempt { parseTranslateStatement() }
            ?: attempt { parseProcedureDef() }
            ?: attempt { parseCallStatement() }
            ?: attempt { parseHighlightNode() }
    }


    // <city> ::= 'city' <string> '{' <city_body> '}'
    private fun parseCity(): CityNode? = withPosition {
        if (!match(TokenType.CITY)) return@withPosition null

        val nameToken = currentToken
        if (!match(TokenType.STRING)) return@withPosition null
        val cityName = nameToken?.getLexem()?.removeSurrounding("\"") ?: return@withPosition null

        if (!match(TokenType.LBRACE)) return@withPosition null

        val body = mutableListOf<ASTNode>()
        while (currentToken?.getTokenSubtype() != "rbrace" && !scanner.eof()) {
            savePosition()
            val element = parseStatement()
            if (element != null) {
                body.add(element)
            } else {
                restorePosition()
                break
            }
        }

        if (!match(TokenType.RBRACE)) return@withPosition null

        return@withPosition CityNode(cityName, body)
    }

    // <element> ::= <road> | <building> | <lake> | <park> | <news> | <junction> | <marker>
    //             | <let_statement> | <foreach_statement> | <translate_statement>
    //             | <validate_block> | <if_statement> | <for_statement>
    //             | <procedure_def> | <call_statement>
    private fun parseElement(): ASTNode? = withPosition {
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
            ?: attempt { parseHighlightNode() }
    }

    // <road> ::= 'road' <string> '{' <road_body> '}'
    private fun parseRoad(): RoadNode? = withPosition {
        if (!match(TokenType.ROAD)) return@withPosition null

        val nameToken = currentToken
        if (!match(TokenType.STRING)) return@withPosition null
        val roadName = nameToken?.getLexem()?.removeSurrounding("\"") ?: return@withPosition null

        if (!match(TokenType.LBRACE)) return@withPosition null

        val body = mutableListOf<ASTNode>()
        while (currentToken?.getTokenSubtype() != "rbrace" && !scanner.eof()) {
            savePosition()
            val step = parseRoadStep()
            if (step != null) {
                body.add(step)
            } else {
                restorePosition()
                break
            }
        }

        if (!match(TokenType.RBRACE)) return@withPosition null

        return@withPosition RoadNode(roadName, body)
    }

    // <road_step> ::= <command> | <let_statement> | <foreach_statement>
    //                | <translate_statement> | <if_statement> | <for_statement> | <call_statement>
    private fun parseRoadStep(): ASTNode? = withPosition {
        attempt { parseCommandNode() }
            ?: attempt { parseLetStatement() }
            ?: attempt { parseForeachStatement() }
            ?: attempt { parseTranslateStatement() }
            ?: attempt { parseIfStatement() }
            ?: attempt { parseForStatement() }
            ?: attempt { parseCallStatement() }
    }


    // <building> ::= 'building' <string> '{' <building_body> '}'
    private fun parseBuilding(): BuildingNode? = withPosition {
        if (!match(TokenType.BUILDING)) return@withPosition null

        val nameToken = currentToken
        if (!match(TokenType.STRING)) return@withPosition null
        val buildingName = nameToken?.getLexem()?.removeSurrounding("\"") ?: return@withPosition null

        if (!match(TokenType.LBRACE)) return@withPosition null

        val body = mutableListOf<ASTNode>()
        while (currentToken?.getTokenSubtype() != "rbrace" && !scanner.eof()) {
            savePosition()
            val step = parseBuildingStep()
            if (step != null) {
                body.add(step)
            } else {
                restorePosition()
                break
            }
        }

        if (!match(TokenType.RBRACE)) return@withPosition null

        return@withPosition BuildingNode(buildingName, body)
    }

    // <building_step> ::= <command> | <let_statement> | <foreach_statement>
    //                    | <translate_statement> | <if_statement> | <for_statement> | <call_statement>
    private fun parseBuildingStep(): ASTNode? = withPosition {
        attempt { parseCommandNode() }
            ?: attempt { parseLetStatement() }
            ?: attempt { parseForeachStatement() }
            ?: attempt { parseTranslateStatement() }
            ?: attempt { parseIfStatement() }
            ?: attempt { parseForStatement() }
            ?: attempt { parseCallStatement() }
    }


    // <lake> ::= 'lake' <string> '{' <lake_body> '}'
    // <lake> ::= 'lake' <string> [ '(' <expr> ',' <expr> ')' ] '{' <lake_body> '}'
    private fun parseLake(): LakeNode? = withPosition {
        if (!match(TokenType.LAKE)) return@withPosition null

        val nameToken = currentToken
        if (!match(TokenType.STRING)) return@withPosition null
        val lakeName = nameToken?.getLexem()?.removeSurrounding("\"") ?: return@withPosition null

        val location = if (currentToken?.getTokenSubtype() == "lparen") {
            parsePointNode() ?: return@withPosition null
        } else null

        if (!match(TokenType.LBRACE)) return@withPosition null

        val commands = mutableListOf<CommandNode>()
        while (true) {
            savePosition()
            val cmd = parseCommandNode()
            if (cmd != null) {
                commands.add(cmd)
            } else {
                restorePosition()
                break
            }
        }

        if (!match(TokenType.RBRACE)) return@withPosition null

        return@withPosition LakeNode(lakeName, location, commands)
    }

    // <park> ::= 'park' <string> '{' <park_body> '}'
    private fun parsePark(): ParkNode? = withPosition {
        if (!match(TokenType.PARK)) return@withPosition null

        val nameToken = currentToken
        if (!match(TokenType.STRING)) return@withPosition null
        val parkName = nameToken?.getLexem()?.removeSurrounding("\"") ?: return@withPosition null

        if (!match(TokenType.LBRACE)) return@withPosition null

        val commands = mutableListOf<CommandNode>()
        while (true) {
            savePosition()
            val cmd = parseCommandNode()
            if (cmd != null) {
                commands.add(cmd)
            } else {
                restorePosition()
                break
            }
        }

        if (!match(TokenType.RBRACE)) return@withPosition null

        return@withPosition ParkNode(parkName, commands)
    }


    // <news> ::= 'news' <string> <location_or_unknown> <metadata_blocks> ';'
    private fun parseNews(): NewsNode? = withPosition {
        if (!match(TokenType.NEWS)) return@withPosition null

        val titleToken = currentToken
        if (!match(TokenType.STRING)) return@withPosition null
        val title = titleToken?.getLexem()?.removeSurrounding("\"") ?: return@withPosition null

        val location: Location = when {
            currentToken?.getTokenSubtype() == "lparen" -> {
                val pointExpr = parsePointNode() ?: return@withPosition null
                toLocation(pointExpr) ?: return@withPosition null
            }

            currentToken?.getTokenSubtype() == "unknown" -> {
                nextToken()
                UnknownLocation
            }

            currentToken?.getTokenSubtype() == "identifier" -> {
                val name = currentToken!!.getLexem()
                nextToken()
                LocationIdentifier(name)
            }

            else -> return@withPosition null
        }

        val metadata = if (currentToken?.getTokenSubtype() == "lbrace") {
            parseMetadataBlockList() ?: return@withPosition null
        } else {
            emptyList()
        }

        if (!match(TokenType.SEMI)) return@withPosition null

        return@withPosition NewsNode(title, location, metadata)
    }


    // <junction> ::= 'junction' <point> ';'
    private fun parseJunction(): JunctionNode? = withPosition {
        if (!match(TokenType.JUNCTION)) return@withPosition null
        val expr = parseExpressionNode() ?: return@withPosition null
        if (!match(TokenType.SEMI)) return@withPosition null
        return@withPosition JunctionNode(expr)
    }


    // <marker> ::= 'marker' <string> <point> <metadata_block> ';'
    private fun parseMarker(): MarkerNode? = withPosition {
        if (!match(TokenType.MARKER)) return@withPosition null

        val nameToken = currentToken
        if (!match(TokenType.STRING)) return@withPosition null
        val markerName = nameToken?.getLexem()?.removeSurrounding("\"") ?: return@withPosition null

        val pointExpr = when {
            currentToken?.getTokenSubtype() == "lparen" -> parsePointNode()
            currentToken?.getTokenSubtype() == "identifier" -> {
                val name = currentToken!!.getLexem()
                nextToken()
                Identifier(name)
            }

            else -> null
        } ?: return@withPosition null

        val metadata = if (match(TokenType.LBRACE)) {
            val list = mutableListOf<Metadata>()
            while (true) {
                savePosition()
                val stmt = parseMetadataStatementNode()
                if (stmt != null) list.add(stmt)
                else {
                    restorePosition()
                    break
                }
            }
            if (!match(TokenType.RBRACE)) return@withPosition null
            list
        } else {
            emptyList()
        }

        if (!match(TokenType.SEMI)) return@withPosition null
        println("Parsed marker '$markerName' at row ${scanner.row}, column ${scanner.column}")

        return@withPosition MarkerNode(markerName, pointExpr, metadata)
    }

    private fun parseMarkerCommand(): CommandNode? = withPosition {
        if (!match(TokenType.MARKER)) return@withPosition null

        val nameToken = currentToken
        if (!match(TokenType.STRING)) return@withPosition null
        val markerName = nameToken?.getLexem()?.removeSurrounding("\"") ?: return@withPosition null

        val pointExpr = when {
            currentToken?.getTokenSubtype() == "lparen" -> parsePointNode()
            currentToken?.getTokenSubtype() == "identifier" -> {
                val name = currentToken!!.getLexem()
                nextToken()
                Identifier(name)
            }

            else -> null
        } ?: return@withPosition null

        if (currentToken?.getTokenSubtype() == "lbrace") {
            var count = 1
            match(TokenType.LBRACE)
            while (count > 0 && !scanner.eof()) {
                if (currentToken?.getTokenSubtype() == "lbrace") count++
                else if (currentToken?.getTokenSubtype() == "rbrace") count--
                nextToken()
            }
        }

        if (!match(TokenType.SEMI)) return@withPosition null

        return@withPosition MarkerNode(markerName, pointExpr)
    }


    private fun parsePointNode(): Expression? = withPosition {
        if (!match(TokenType.LPAREN)) return@withPosition null

        val xExpr = parseExpressionNode() ?: return@withPosition null
        if (!match(TokenType.COMMA)) return@withPosition null
        val yExpr = parseExpressionNode() ?: return@withPosition null

        if (!match(TokenType.RPAREN)) return@withPosition null

        return@withPosition PointExpr(xExpr, yExpr)
    }


    private fun parseMetadataBlockList(): List<Metadata>? = withPosition {
        if (!match(TokenType.LBRACE)) return@withPosition null

        val metadataList = mutableListOf<Metadata>()

        while (true) {
            savePosition()
            val metadata = parseMetadataStatementNode()
            if (metadata != null) {
                metadataList.add(metadata)
            } else {
                restorePosition()
                break
            }
        }

        if (!match(TokenType.RBRACE)) return@withPosition null

        return@withPosition metadataList
    }


    private fun parseMetadataStatementNode(): Metadata? = withPosition {
        if (!match(TokenType.SET)) return@withPosition null
        if (!match(TokenType.LPAREN)) return@withPosition null

        val keyToken = currentToken
        if (!match(TokenType.STRING)) return@withPosition null
        val key = keyToken?.getLexem()?.removeSurrounding("\"") ?: return@withPosition null

        if (!match(TokenType.COMMA)) return@withPosition null

        val valueExpr = parseExpressionNode() ?: return@withPosition null

        if (!match(TokenType.RPAREN)) return@withPosition null
        if (!match(TokenType.SEMI)) return@withPosition null

        return@withPosition Metadata(key, ExpressionValue(valueExpr))
    }

    /**
     * Parses a highlight statement.
     * @return Highlight command node if successful, null otherwise
     */
    private fun parseHighlightNode(): CommandNode? = withPosition {
        if (!match(TokenType.HIGHLIGHT)) return@withPosition null
        val expr = parseExpressionNode() ?: return@withPosition null
        if (!match(TokenType.SEMI)) return@withPosition null

        return@withPosition HighlightCommand(expr)
    }


    // draw_line ::= 'line' '(' <point> ',' <point> ')' ';'
    private fun parseDrawLineNode(): CommandNode? = withPosition {
        if (!match(TokenType.LINE)) return@withPosition null
        if (!match(TokenType.LPAREN)) return@withPosition null

        val p1 = parseExpressionNode() ?: return@withPosition null
        if (!match(TokenType.COMMA)) return@withPosition null
        val p2 = parseExpressionNode() ?: return@withPosition null

        if (!match(TokenType.RPAREN)) return@withPosition null
        if (!match(TokenType.SEMI)) return@withPosition null

        return@withPosition LineCommand(p1, p2)
    }


    // draw_bend ::= 'bend' '(' <point> ',' <point> ',' <expression> ')' ';'
    private fun parseDrawBendNode(): CommandNode? = withPosition {
        if (!match(TokenType.BEND)) return@withPosition null
        if (!match(TokenType.LPAREN)) return@withPosition null

        val fromExpr = parsePointNode() ?: return@withPosition null
        val from = toPoint(fromExpr) ?: return@withPosition null

        if (!match(TokenType.COMMA)) return@withPosition null

        val toExpr = parsePointNode() ?: return@withPosition null
        val to = toPoint(toExpr) ?: return@withPosition null

        if (!match(TokenType.COMMA)) return@withPosition null
        val angle = parseExpressionNode() ?: return@withPosition null

        if (!match(TokenType.RPAREN)) return@withPosition null
        if (!match(TokenType.SEMI)) return@withPosition null

        return@withPosition BendCommand(from, to, angle)
    }


    // draw_box ::= 'box' '(' <point> ',' <point> ')' ';'
    private fun parseDrawBoxNode(): CommandNode? = withPosition {
        if (!match(TokenType.BOX)) return@withPosition null
        if (!match(TokenType.LPAREN)) return@withPosition null

        val p1 = parseExpressionNode() ?: return@withPosition null
        if (!match(TokenType.COMMA)) return@withPosition null
        val p2 = parseExpressionNode() ?: return@withPosition null

        if (!match(TokenType.RPAREN)) return@withPosition null
        if (!match(TokenType.SEMI)) return@withPosition null

        return@withPosition BoxCommand(p1, p2)
    }

    // draw_circle ::= 'circ' '(' <point> ',' <expression> ')' ';'
    private fun parseDrawCircleNode(): CommandNode? = withPosition {
        if (!match(TokenType.CIRC)) return@withPosition null
        if (!match(TokenType.LPAREN)) return@withPosition null

        val center = parsePointNode() ?: return@withPosition null
        if (!match(TokenType.COMMA)) return@withPosition null
        val radius = parseExpressionNode() ?: return@withPosition null

        if (!match(TokenType.RPAREN)) return@withPosition null
        if (!match(TokenType.SEMI)) return@withPosition null

        return@withPosition CircleCommand(center, radius)
    }


    // <let_statement> ::= 'let' <identifier> '=' (<expression> | <point> | <list> | <string>) ';'
    private    /**
     * Parses a let statement.
     * @return Let statement node if successful, null otherwise
     */
    fun parseLetStatement(): LetStatement? = withPosition {
        if (!match(TokenType.LET)) return@withPosition null

        val idToken = currentToken
        if (!match(TokenType.IDENTIFIER)) return@withPosition null
        val name = idToken!!.getLexem()

        if (!match(TokenType.EQUALS)) return@withPosition null

        val value: Value = when {
            currentToken?.getTokenSubtype() == "lbracket" -> {
                val listItems = parseListValues() ?: return@withPosition null
                ListValue(listItems)
            }

            currentToken?.getTokenSubtype() == "lparen" -> {
                val pointExpr = parsePointNode() ?: return@withPosition null
                val point = toPoint(pointExpr) ?: return@withPosition null
                PointValue(point)
            }

            currentToken?.getTokenSubtype() == "string" -> {
                val strToken = currentToken
                if (!match(TokenType.STRING)) return@withPosition null
                StringValue(strToken!!.getLexem().removeSurrounding("\""))
            }

            else -> {
                val expr = parseExpressionNode() ?: return@withPosition null
                ExpressionValue(expr)
            }
        }

        if (!match(TokenType.SEMI)) return@withPosition null

        return@withPosition LetStatement(name, value)
    }

    private fun parseListValues(): List<Value>? = withPosition {
        if (!match(TokenType.LBRACKET)) return@withPosition null

        val items = mutableListOf<Value>()

        val first = parseExpressionOrPointValue() ?: return@withPosition null
        items.add(first)

        while (match(TokenType.COMMA)) {
            val item = parseExpressionOrPointValue() ?: return@withPosition null
            items.add(item)
        }

        if (!match(TokenType.RBRACKET)) return@withPosition null

        return@withPosition items
    }

    private fun parseExpressionOrPointValue(): Value? = withPosition {
        if (currentToken?.getTokenSubtype() == "lparen") {
            val expr = parsePointNode() ?: return@withPosition null
            val point = toPoint(expr) ?: return@withPosition null
            return@withPosition PointValue(point)
        }
        parseExpressionNode()?.let { return@withPosition ExpressionValue(it) }
    }


    // <foreach_statement> ::= 'foreach' <identifier> 'in' <expression> '{' <foreach_body> '}'
    private fun parseForeachStatement(): ForeachStatement? = withPosition {
        if (!match(TokenType.FOREACH)) return@withPosition null

        val idToken = currentToken
        if (!match(TokenType.IDENTIFIER)) return@withPosition null
        val variableName = idToken!!.getLexem()

        if (!match(TokenType.IN)) return@withPosition null

        val iterableExpr = parseExpressionNode() ?: return@withPosition null

        if (!match(TokenType.LBRACE)) return@withPosition null

        val body = parseStatementBlock() ?: return@withPosition null

        if (!match(TokenType.RBRACE)) return@withPosition null

        return@withPosition ForeachStatement(variableName, iterableExpr, body)
    }

    //<translate_statement> ::= 'translate' (<point> | <identifier>) '{' <translate_body> '}'
    private fun parseTranslateStatement(): TranslateBlock? = withPosition {
        println("Trying to parse translate at ${scanner.row}:${scanner.column} with token ${currentToken}")

        if (!match(TokenType.TRANSLATE)) return@withPosition null

        val target: Location = when {
            currentToken?.getTokenSubtype() == "lparen" -> {
                val expr = parseExpressionNode() ?: return@withPosition null
                when (expr) {
                    is Grouped -> {
                        val inner = expr.expr
                        if (inner is Point) inner
                        else return@withPosition null
                    }

                    is Point -> expr
                    else -> return@withPosition null
                }
            }

            currentToken?.getTokenSubtype() == "identifier" -> {
                val name = currentToken!!.getLexem()
                nextToken()
                LocationIdentifier(name)
            }

            else -> return@withPosition null
        }


        if (!match(TokenType.LBRACE)) return@withPosition null
        val body = parseStatementBlock() ?: return@withPosition null
        if (!match(TokenType.RBRACE)) return@withPosition null

        return@withPosition TranslateBlock(target, body)
    }


    // <validate_block> ::= 'validate' '{' <validate_statement> <validate_block_tail> '}'
    private fun parseValidateBlock(): ValidateBlock? = withPosition {
        if (!match(TokenType.VALIDATE)) return@withPosition null
        if (!match(TokenType.LBRACE)) return@withPosition null

        val checks = mutableListOf<String>()

        while (true) {
            savePosition()
            val id = parseValidateStatementId()
            if (id != null) {
                checks.add(id)
            } else {
                restorePosition()
                break
            }
        }

        if (!match(TokenType.RBRACE)) return@withPosition null

        return@withPosition ValidateBlock(checks)
    }

    private fun parseValidateStatementId(): String? = withPosition {
        if (!match(TokenType.CHECK)) return@withPosition null
        if (!match(TokenType.LPAREN)) return@withPosition null

        val idToken = currentToken
        if (!match(TokenType.IDENTIFIER)) return@withPosition null
        val id = idToken!!.getLexem()

        if (!match(TokenType.RPAREN)) return@withPosition null
        if (!match(TokenType.SEMI)) return@withPosition null

        return@withPosition id
    }

    // <if_statement> ::= 'if' <expression> '{' <statement_list> '}' <else_opt>
    private fun parseIfStatement(): IfStatement? = withPosition {
        if (!match(TokenType.IF)) return@withPosition null

        val condition = parseExpressionNode() ?: return@withPosition null

        if (!match(TokenType.LBRACE)) return@withPosition null
        val thenBranch = parseStatementBlock() ?: return@withPosition null
        if (!match(TokenType.RBRACE)) return@withPosition null

        val elseBranch = parseElseOptional()

        return@withPosition IfStatement(condition, thenBranch, elseBranch)
    }

    private fun parseStatementBlock(): List<ASTNode>? = withPosition {
        val statements = mutableListOf<ASTNode>()

        while (true) {
            val stmt = parseStatement() ?: break
            statements.add(stmt)
        }

        return@withPosition statements
    }

    private fun parseElseOptional(): List<ASTNode>? = withPosition {
        if (match(TokenType.ELSE)) {
            if (!match(TokenType.LBRACE)) return@withPosition null
            val elseBody = parseStatementBlock() ?: return@withPosition null
            if (!match(TokenType.RBRACE)) return@withPosition null
            return@withPosition elseBody
        }
        return@withPosition null
    }

    // <for_statement> ::= 'for' <identifier> '=' <expression> 'to' <expression> '{' <statement_list> '}'
    private fun parseForStatement(): ForStatement? = withPosition {
        if (!match(TokenType.FOR)) return@withPosition null

        val idToken = currentToken
        if (!match(TokenType.IDENTIFIER)) return@withPosition null
        val variableName = idToken!!.getLexem()

        if (!match(TokenType.EQUALS)) return@withPosition null

        val startExpr = parseExpressionNode() ?: return@withPosition null
        if (!match(TokenType.TO)) return@withPosition null
        val endExpr = parseExpressionNode() ?: return@withPosition null

        if (!match(TokenType.LBRACE)) return@withPosition null
        val body = parseStatementBlock() ?: return@withPosition null
        if (!match(TokenType.RBRACE)) return@withPosition null

        return@withPosition ForStatement(variableName, startExpr, endExpr, body)
    }

    private fun evaluateNumeric(expr: Expression): Double? {
        return when (expr) {
            is NumberLiteral -> expr.value
            is Identifier -> null // možeš proširiti ako želiš evaluirati varijable
            is BinaryOp -> {
                val left = evaluateNumeric(expr.left)
                val right = evaluateNumeric(expr.right)
                if (left != null && right != null) {
                    when (expr.op) {
                        "+" -> left + right
                        "-" -> left - right
                        "*" -> left * right
                        "/" -> left / right
                        else -> null
                    }
                } else null
            }

            is Grouped -> evaluateNumeric(expr.expr)
            else -> null
        }
    }

    // <procedure_def> ::= 'procedure' <identifier> '(' <parameter_list> ')' '{' <statement_list> '}'
    private fun parseProcedureDef(): ProcedureDef? = withPosition {
        if (!match(TokenType.PROCEDURE)) return@withPosition null

        val nameToken = currentToken
        if (!match(TokenType.IDENTIFIER)) return@withPosition null
        val procName = nameToken!!.getLexem()

        if (!match(TokenType.LPAREN)) return@withPosition null

        val params = parseParameterListNode() ?: return@withPosition null

        if (!match(TokenType.RPAREN)) return@withPosition null
        if (!match(TokenType.LBRACE)) return@withPosition null

        val body = parseStatementBlock() ?: return@withPosition null  // ✅ KORISTI PARSER BLOK

        if (!match(TokenType.RBRACE)) return@withPosition null

        return@withPosition ProcedureDef(procName, params, body)
    }

    private fun parseParameterListNode(): List<String>? = withPosition {
        val params = mutableListOf<String>()

        // prazna lista
        if (currentToken?.getTokenSubtype() == "rparen") {
            return@withPosition params
        }

        // prvi parametar
        if (currentToken?.getTokenSubtype() != "identifier") return@withPosition null
        params.add(currentToken!!.getLexem())
        nextToken()

        // pokušaj parsiranja ostalih, ali SAMO ako postoji zarez
        while (currentToken?.getTokenSubtype() == "comma") {
            match(TokenType.COMMA)  // sada sigurno postoji, pa matchaj
            if (currentToken?.getTokenSubtype() != "identifier") return@withPosition null
            params.add(currentToken!!.getLexem())
            nextToken()
        }

        return@withPosition params
    }

    // <call_statement> ::= <identifier> '(' <arg_list> ')' ';'
    private fun parseCallStatement(): ProcedureCall? = withPosition {
        println("Parsed ProcedureCall to")
        val nameToken = currentToken
        if (!match(TokenType.IDENTIFIER)) return@withPosition null
        val name = nameToken!!.getLexem()

        if (!match(TokenType.LPAREN)) return@withPosition null

        val args = parseArgumentList() ?: return@withPosition null

        if (!match(TokenType.RPAREN)) return@withPosition null
        if (!match(TokenType.SEMI)) return@withPosition null

        return@withPosition ProcedureCall(name, args)
    }

    private fun parseArgumentList(): List<Expression>? = withPosition {
        val args = mutableListOf<Expression>()

        if (currentToken?.getTokenSubtype() == "rparen") {
            return@withPosition args // prazna lista
        }

        val first = parseExpressionNode() ?: return@withPosition null
        args.add(first)

        while (match(TokenType.COMMA)) {
            val next = parseExpressionNode() ?: return@withPosition null
            args.add(next)
        }

        return@withPosition args
    }

    // <statement> ::= <command> | <let_statement> | <foreach_statement>
    //              | <translate_statement> | <validate_block> | <if_statement>
    //              | <for_statement> | <call_statement> | <marker> | <news> | <junction>
    private fun parseStatement(): ASTNode? = withPosition {
        attempt { parseProcedureDef() } // ⬅️ procedure mora ostati prva
            ?: attempt { parseTranslateStatement() }
            ?: attempt { parseLetStatement() }
            ?: attempt { parseForeachStatement() }
            ?: attempt { parseIfStatement() }
            ?: attempt { parseForStatement() }
            ?: attempt { parseNews() }
            ?: attempt { parseJunction() }
            ?: attempt { parseMarker() }
            ?: attempt { parseValidateBlock() }
            ?: attempt { parseCommandNode() }
            ?: attempt { parseCallStatement() }
            ?: attempt { parseBuilding() }
            ?: attempt { parseLake() }
            ?: attempt { parsePark() }
            ?: attempt { parseRoad() }
            ?: attempt { parseHighlightNode() }
    }

    // <expression> ::= <term> <expression_prime> | <neigh>
    private fun parseExpressionNode(): Expression? = withPosition {
        savePosition()

        // ⬅️ prvo probaj Neigh
        val neigh = parseNeighNode()
        if (neigh != null) return@withPosition neigh

        // ⬅️ zatim probaj PointExpr
        val maybePoint = parsePointNode()
        if (maybePoint != null) return@withPosition maybePoint

        restorePosition()

        val left = parseTermNode() ?: return@withPosition null
        return@withPosition parseExpressionPrime(left)
    }

    // <expression_prime> ::= <operand> <term> <expression_prime> | ε
    private fun parseExpressionPrime(left: Expression): Expression {
        var result = left

        while (currentToken?.getTokenSubtype() in setOf(
                "plus",
                "minus",
                "times",
                "divide",
                "lessthan",
                "greaterthan"
            )
        ) {
            val op = currentToken!!.getLexem()
            nextToken()
            val right = parseTermNode() ?: break
            result = BinaryOp(result, op, right)
        }

        return result
    }

    // <term> ::= <number> | <identifier> | '(' <expression> ')'
//           | 'fst' '(' <expression> ')' | 'snd' '(' <expression> ')' | 'nil'
    private fun parseTermNode(): Expression? = withPosition {
        when (val subtype = currentToken?.getTokenSubtype()) {
            "number" -> {
                val value = currentToken!!.getLexem().toDouble()
                nextToken()
                NumberLiteral(value)
            }

            "identifier" -> {
                val name = currentToken!!.getLexem()
                nextToken()
                Identifier(name)
            }

            "nil" -> {
                nextToken()
                NilExpr
            }

            "fst", "snd" -> {
                val isFst = subtype == "fst"
                nextToken()
                if (!match(TokenType.LPAREN)) return@withPosition null
                val inner = parseExpressionNode() ?: return@withPosition null
                if (!match(TokenType.RPAREN)) return@withPosition null
                if (isFst) Fst(inner) else Snd(inner)
            }

            "lparen" -> {
                match(TokenType.LPAREN)
                val inner = parseExpressionNode() ?: return@withPosition null
                if (!match(TokenType.RPAREN)) return@withPosition null
                Grouped(inner)
            }

            "string" -> {
                val value = currentToken!!.getLexem().removeSurrounding("\"")
                nextToken()
                StringLiteral(value)
            }

            else -> null
        }
    }

    // <neigh> ::= 'neigh' '(' <expression> ',' <expression> ')'
    private fun parseNeighNode(): Expression? = withPosition {
        if (!match(TokenType.NEIGH)) return@withPosition null
        if (!match(TokenType.LPAREN)) return@withPosition null

        val location = when {
            currentToken?.getTokenSubtype() == "identifier" -> {
                val name = currentToken!!.getLexem()
                nextToken()
                LocationIdentifier(name)
            }

            currentToken?.getTokenSubtype() == "lparen" -> {
                val pointExpr = parsePointNode() ?: return@withPosition null
                toLocation(pointExpr) ?: return@withPosition null
            }

            else -> return@withPosition null
        }

        if (!match(TokenType.COMMA)) return@withPosition null

        val radius = parseExpressionNode() ?: return@withPosition null

        if (!match(TokenType.RPAREN)) return@withPosition null

        return@withPosition Neigh(location, radius)
    }

    private fun parseCommandNode(): CommandNode? = withPosition {
        attempt { parseDrawLineNode() }
            ?: attempt { parseDrawBoxNode() }
            ?: attempt { parseDrawCircleNode() }
            ?: attempt { parseDrawBendNode() }
            ?: attempt { parseHighlightNode() }
            ?: attempt { parseCallStatement() }
            ?: attempt { parseMarkerCommand() }
    }
}




package ast

sealed class ASTNode

data class ProgramNode(val items: List<ASTNode>) : ASTNode()

data class RegionNode(val name: String, val body: List<ASTNode>) : ASTNode()
data class CityNode(val name: String, val body: List<ASTNode>) : ASTNode()

data class BuildingNode(val name: String, val body: List<ASTNode>) : ASTNode()
data class RoadNode(val name: String, val body: List<ASTNode>) : ASTNode()
data class LakeNode(val name: String, val location: Expression?, val body: List<ASTNode>) : ASTNode()
data class ParkNode(val name: String, val body: List<CommandNode>) : ASTNode()

sealed class CommandNode : ASTNode()
data class LineCommand(val from: Expression, val to: Expression) : CommandNode()
data class BoxCommand(val corner1: Expression, val corner2: Expression) : CommandNode()
data class CircleCommand(val center: Expression, val radius: Expression) : CommandNode()
data class BendCommand(val from: Expression, val to: Point, val angle: Expression) : CommandNode()
data class HighlightCommand(val expression: Expression) : CommandNode()
data class CallCommand(val name: String, val arguments: List<Expression>) : CommandNode()

/**
 * Represents a marker node, containing a name, an expression representing the point, and a list of metadata.
 */
data class MarkerNode(
    val name: String,
    val pointExpr: Expression,
    val metadata: List<Metadata> = emptyList()
) : CommandNode()
/**
 * Represents a junction node, containing an expression representing the point.
 */
data class JunctionNode(val pointExpr: Expression) : ASTNode()

/**
 * Represents a news node, containing a title, a location, and a list of metadata.
 */
data class NewsNode(
    val title: String,
    val location: Location?,
    val metadata: List<Metadata> = emptyList()
) : ASTNode()

/**
 * Represents a let statement, containing a name and a value.
 */
data class LetStatement(val name: String, val value: Value) : ASTNode()
/**
 * Represents an if statement, containing a condition, a then branch, and an optional else branch.
 */
data class IfStatement(val condition: Expression, val thenBranch: List<ASTNode>, val elseBranch: List<ASTNode>?) : ASTNode()
/**
 * Represents a for statement, containing a variable, a start expression, an end expression, and a body.
 */
data class ForStatement(val variable: String, val start: Expression, val end: Expression, val body: List<ASTNode>) : ASTNode()
/**
 * Represents a foreach statement, containing a variable, an iterable expression, and a body.
 */
data class ForeachStatement(val variable: String, val iterable: Expression, val body: List<ASTNode>) : ASTNode()
/**
 * Represents a procedure definition, containing a name, a list of parameters, and a body.
 */
data class ProcedureDef(val name: String, val parameters: List<String>, val body: List<ASTNode>) : ASTNode()
/**
 * Represents a procedure call, containing a name and a list of expressions representing the arguments.
 */
data class ProcedureCall(val name: String, val arguments: List<Expression>) : CommandNode()
/**
 * Represents a translate block, containing a target location and a body.
 */
data class TranslateBlock(val target: Location, val body: List<ASTNode>) : ASTNode()
/**
 * Represents a validate block, containing a list of checks.
 */
data class ValidateBlock(val checks: List<String>) : ASTNode()

/**
 * Base interface for all values in the AST.
 * Values can be used in expressions and statements.
 */
sealed class Value : ASTNode()
/**
 * Represents a string value, containing a string.
 */
data class StringValue(val value: String) : Value()
/**
 * Represents an expression value, containing an expression.
 */
data class ExpressionValue(val expression: Expression) : Value()
/**
 * Represents a point value, containing a point.
 */
data class PointValue(val point: Point) : Value()
/**
 * Represents a list value, containing a list of values.
 */
data class ListValue(val items: List<Value>) : Value()
/**
 * Represents the nil value.
 */
object NilValue : Value()

sealed class Location : Expression()
data class Point(val x: Double, val y: Double) : Location()
data class PointExpr(val x: Expression, val y: Expression) : Location()
data class LocationIdentifier(val name: String) : Location()
object UnknownLocation : Location()

data class Metadata(val key: String, val value: Value) : ASTNode()

sealed class Expression : ASTNode()
data class NumberLiteral(val value: Double) : Expression()
data class StringLiteral(val value: String) : Expression()
data class Identifier(val name: String) : Expression()
data class BinaryOp(val left: Expression, val op: String, val right: Expression) : Expression()
data class Grouped(val expr: Expression) : Expression()
data class Fst(val expr: Expression) : Expression()
data class Snd(val expr: Expression) : Expression()
object NilExpr : Expression()
data class Neigh(val location: Location, val radius: Expression) : Expression()

fun evaluateNumeric(expr: Expression, env: Map<String, Value>): Double? {
    return when (expr) {
        is NumberLiteral -> expr.value
        is Identifier -> {
            val value = env[expr.name]
            when (value) {
                is ExpressionValue -> evaluateNumeric(value.expression, env)
                is PointValue -> null
                is StringValue -> null
                else -> null
            }
        }
        is BinaryOp -> {
            val left = evaluateNumeric(expr.left, env)
            val right = evaluateNumeric(expr.right, env)
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
        is Grouped -> evaluateNumeric(expr.expr, env)
        is Fst -> {
            val inner = evaluateExpression(expr.expr, env)
            inner?.x
        }
        is Snd -> {
            val inner = evaluateExpression(expr.expr, env)
            inner?.y
        }
        else -> null
    }
}

fun evaluateExpression(expr: Expression, env: Map<String, Value>): Point? {
    return when (expr) {
        is Point -> expr
        is Identifier -> {
            val value = env[expr.name]
            if (value is PointValue) value.point else null
        }
        is LocationIdentifier -> {
            val value = env[expr.name]
            if (value is PointValue) value.point else null
        }
        is PointExpr -> {
            val x = evaluateNumeric(expr.x, env)
            val y = evaluateNumeric(expr.y, env)
            if (x != null && y != null) Point(x, y) else null
        }

        else -> null
    }
}


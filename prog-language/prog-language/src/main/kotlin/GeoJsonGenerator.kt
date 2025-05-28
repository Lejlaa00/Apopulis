package export

import ast.*
import kotlinx.serialization.json.*
import kotlin.math.*

object GeoJsonGenerator {
    fun generate(program: ProgramNode): String {
        val features = mutableListOf<JsonObject>()
        val context = mutableMapOf<String, Value>()
        val procedures = mutableMapOf<String, ProcedureDef>()
        var currentOffset: Point? = null

        fun isInSlovenia(point: Point): Boolean {
            return point.x in 45.4..47.1 && point.y in 13.4..16.6
        }

        fun convertMetadata(metadata: List<Metadata>, env: Map<String, Value>): Map<String, String> {
            return metadata.mapNotNull {
                val key = it.key
                val value = when (it.value) {
                    is StringValue -> (it.value as StringValue).value
                    is ExpressionValue -> evaluateNumeric((it.value as ExpressionValue).expression, env)?.toString()
                    is PointValue -> {
                        val p = (it.value as PointValue).point
                        "(${p.x}, ${p.y})"
                    }

                    else -> null
                }
                if (value != null) key to value else null
            }.toMap()
        }

        fun interpolateTemplate(template: String, context: Map<String, Value>): String {
            return Regex("\\$\\{(\\w+)}").replace(template) { matchResult ->
                val varName = matchResult.groupValues[1]
                val value = context[varName]
                when (value) {
                    is ExpressionValue -> {
                        when (val expr = value.expression) {
                            is NumberLiteral -> expr.value.toInt().toString()
                            else -> {
                                val num = evaluateNumeric(expr, context)
                                num?.toInt()?.toString() ?: ""
                            }
                        }
                    }
                    is StringValue -> value.value
                    else -> value?.toString() ?: ""
                }
            }
        }

        fun applyOffset(point: Point): Point {
            return currentOffset?.let { Point(point.x + it.x, point.y + it.y) } ?: point
        }

        fun withOffset(offset: Point, block: () -> Unit) {
            val previous = currentOffset
            currentOffset = offset
            block()
            currentOffset = previous
        }

        fun addCircleMarker(point: Point, properties: Map<String, String>, color: String = "") {
            val actual = applyOffset(point)
            features.add(
                buildJsonObject {
                    put("type", JsonPrimitive("Feature"))
                    put("geometry", buildJsonObject {
                        put("type", JsonPrimitive("Point"))
                        put("coordinates", JsonArray(listOf(JsonPrimitive(actual.y), JsonPrimitive(actual.x))))
                    })
                    put("properties", buildJsonObject {
                        for ((key, value) in properties) {
                            put(key, JsonPrimitive(value))
                        }
                        put("marker-symbol", JsonPrimitive(properties["marker-symbol"] ?: "circle"))
                        put("marker-color", JsonPrimitive(properties["marker-color"] ?: color))
                    })
                }
            )
        }

        fun addFeature(location: Point, properties: Map<String, String>) {
            val actual = applyOffset(location)
            features.add(
                buildJsonObject {
                    put("type", JsonPrimitive("Feature"))
                    put("geometry", buildJsonObject {
                        put("type", JsonPrimitive("Point"))
                        put("coordinates", JsonArray(listOf(JsonPrimitive(actual.y), JsonPrimitive(actual.x))))
                    })
                    put("properties", buildJsonObject {
                        for ((key, value) in properties) {
                            put(key, JsonPrimitive(value))
                        }
                        properties["name"]?.let { put("title", JsonPrimitive(it)) }
                    })
                }
            )
        }

        fun addBox(p1: Point, p2: Point, properties: Map<String, String>) {
            val a = applyOffset(p1)
            val b = applyOffset(p2)
            val (x1, y1) = a.x to a.y
            val (x2, y2) = b.x to b.y

            val polygon = listOf(
                listOf(JsonPrimitive(y1), JsonPrimitive(x1)),
                listOf(JsonPrimitive(y1), JsonPrimitive(x2)),
                listOf(JsonPrimitive(y2), JsonPrimitive(x2)),
                listOf(JsonPrimitive(y2), JsonPrimitive(x1)),
                listOf(JsonPrimitive(y1), JsonPrimitive(x1))
            )

            features.add(
                buildJsonObject {
                    put("type", JsonPrimitive("Feature"))
                    put("geometry", buildJsonObject {
                        put("type", JsonPrimitive("Polygon"))
                        put("coordinates", JsonArray(listOf(JsonArray(polygon.map { JsonArray(it) }))))
                    })
                    put("properties", buildJsonObject {
                        for ((key, value) in properties) {
                            put(key, JsonPrimitive(value))
                        }
                        put("fill-opacity", JsonPrimitive(0.6))
                        put("stroke-width", JsonPrimitive(1))
                    })
                }
            )
        }
        fun addLine(p1: Point, p2: Point, properties: Map<String, String>) {
            val a = applyOffset(p1)
            val b = applyOffset(p2)
            val line = listOf(
                JsonArray(listOf(JsonPrimitive(a.y), JsonPrimitive(a.x))),
                JsonArray(listOf(JsonPrimitive(b.y), JsonPrimitive(b.x)))
            )

            features.add(
                buildJsonObject {
                    put("type", JsonPrimitive("Feature"))
                    put("geometry", buildJsonObject {
                        put("type", JsonPrimitive("LineString"))
                        put("coordinates", JsonArray(line))
                    })
                    put("properties", buildJsonObject {
                        for ((key, value) in properties) {
                            put(key, JsonPrimitive(value))
                        }
                    })
                }
            )
        }

        fun addCircle(center: Point, radius: Double, properties: Map<String, String>) {
            val actualCenter = applyOffset(center)
            val segments = 32
            val coords = (0..segments).map { i ->
                val angle = 2 * PI * i / segments
                val dx = radius * cos(angle)
                val dy = radius * sin(angle)
                val lon = actualCenter.x + dx
                val lat = actualCenter.y + dy
                JsonArray(listOf(JsonPrimitive(lat), JsonPrimitive(lon)))
            }

            features.add(
                buildJsonObject {
                    put("type", JsonPrimitive("Feature"))
                    put("geometry", buildJsonObject {
                        put("type", JsonPrimitive("Polygon"))
                        put("coordinates", JsonArray(listOf(JsonArray(coords))))
                    })
                    put("properties", buildJsonObject {
                        for ((key, value) in properties) {
                            put(key, JsonPrimitive(value))
                        }
                        put("fill-opacity", JsonPrimitive(0.4))
                        put("stroke-width", JsonPrimitive(1))
                    })
                }
            )
        }

        fun addArc(p1: Point, p2: Point, angle: Double, properties: Map<String, String>) {
            val a = applyOffset(p1)
            val b = applyOffset(p2)
            val segments = 20
            val midX = (a.x + b.x) / 2
            val midY = (a.y + b.y) / 2
            val dx = b.x - a.x
            val dy = b.y - a.y
            val radius = sqrt(dx * dx + dy * dy) / 2 / sin(angle / 2)
            val centerX = midX - radius * sin(angle / 2)
            val centerY = midY + radius * cos(angle / 2)

            val coords = (0..segments).map { i ->
                val theta = angle * i / segments
                val x = centerX + radius * cos(theta)
                val y = centerY + radius * sin(theta)
                JsonArray(listOf(JsonPrimitive(y), JsonPrimitive(x)))
            }

            features.add(
                buildJsonObject {
                    put("type", JsonPrimitive("Feature"))
                    put("geometry", buildJsonObject {
                        put("type", JsonPrimitive("LineString"))
                        put("coordinates", JsonArray(coords))
                    })
                    put("properties", buildJsonObject {
                        for ((key, value) in properties) {
                            put(key, JsonPrimitive(value))
                        }
                    })
                }
            )
        }

        fun evaluateBoolean(expr: Expression): Boolean {
            return when (expr) {
                is Identifier -> (context[expr.name] as? ExpressionValue)?.expression?.let { evaluateBoolean(it) }
                    ?: false

                is NumberLiteral -> expr.value != 0.0
                is BinaryOp -> when (expr.op) {
                    "<" -> (expr.left as? NumberLiteral)?.value ?: 0.0 < (expr.right as? NumberLiteral)?.value ?: 0.0
                    ">" -> (expr.left as? NumberLiteral)?.value ?: 0.0 > (expr.right as? NumberLiteral)?.value ?: 0.0
                    else -> false
                }

                else -> false
            }
        }

        fun evaluateNumeric(expr: Expression, env: Map<String, Value>): Double? {
            return when (expr) {
                is NumberLiteral -> expr.value

                is Identifier -> {
                    val value = env[expr.name]
                    when (value) {
                        is ExpressionValue -> evaluateNumeric(value.expression, env)
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

                else -> null
            }
        }

        fun evaluateExpression(expr: Expression, env: Map<String, Value>): Point? {
            return when (expr) {
                is Point -> expr
                is Identifier -> {
                    val value = env[expr.name]
                    when (value) {
                        is PointValue -> value.point
                        is ExpressionValue -> evaluateExpression(value.expression, env)
                        else -> {
                            null
                        }
                    }
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

        fun traverse(node: ASTNode) {
            when (node) {
                is ProgramNode -> node.items.forEach(::traverse)
                is RegionNode -> node.body.forEach(::traverse)
                is CityNode -> node.body.forEach(::traverse)
                is MarkerNode -> {
                    val point = evaluateExpression(node.pointExpr, context.toMap()) ?: return

                    val metaProps = convertMetadata(node.metadata, context.toMap())
                    val interpolatedName = interpolateTemplate(node.name, context)
                    val props = mutableMapOf("type" to "marker", "name" to interpolatedName)
                    props.putAll(metaProps)

                    addCircleMarker(point, props, "#ff8000")

                    addCircle(
                        point, 0.0009, mapOf(
                            "type" to "marker-area",
                            "around" to node.name,
                            "fill" to "#ff8000",
                            "fill-opacity" to "0.1",
                            "stroke" to "#ff8000",
                            "stroke-width" to "1"
                        )
                    )
                }

                is NewsNode -> {
                    val metadataMap = convertMetadata(node.metadata, context.toMap())
                    val baseProps = mutableMapOf("type" to "news", "title" to node.title)
                    baseProps.putAll(metadataMap)

                    val location = node.location?.let { evaluateExpression(it, context.toMap()) }

                    if (location != null) {
                        addFeature(location, baseProps)
                    } else {
                        println("News without location: ${node.title}")
                    }
                }

                is JunctionNode -> {
                    val point = evaluateExpression(node.pointExpr, context.toMap()) ?: return
                    val props = mutableMapOf(
                        "type" to "junction",
                        "marker-color" to "#ff0000",
                        "marker-size" to "medium",
                        "name" to "Junction"
                    )
                    addCircleMarker(point, props, "#ff0000")

                    addCircle(
                        point, 0.0001, mapOf(
                            "type" to "junction-area",
                            "around" to "Junction",
                            "fill" to "#ff0000",
                            "fill-opacity" to "0.1",
                            "stroke" to "#ff0000",
                            "stroke-width" to "1"
                        )
                    )
                }

                is LetStatement -> {
                    context[node.name] = node.value

                    if (node.value is ListValue) {
                        val items = node.value.items
                        items.forEachIndexed { index, item ->
                            if (item is PointValue) {
                                val point = item.point
                                addFeature(
                                    point, mapOf(
                                        "type" to "point",
                                        "name" to "${node.name}[$index]"
                                    )
                                )
                            }
                        }
                    }
                }

                is TranslateBlock -> {
                    val offset = when (val loc = node.target) {
                        is Point -> loc
                        is LocationIdentifier -> {
                            val value = context[loc.name]
                            when (value) {
                                is PointValue -> value.point
                                is ExpressionValue -> evaluateExpression(value.expression, context.toMap())
                                else -> null
                            }
                        }

                        is PointExpr -> evaluateExpression(loc, context.toMap())
                        is Expression -> evaluateExpression(loc, context.toMap())
                        else -> null
                    }

                    if (offset != null) {
                        withOffset(offset) {
                            node.body.forEach {
                                println("→ Translating node: ${it::class.simpleName}")
                                traverse(it)
                            }
                        }
                    } else {
                        println("Translate offset could not be resolved: ${node.target}")
                    }
                }

                is ValidateBlock -> {
                    node.checks.forEach {
                        val location = (context[it] as? PointValue)?.point
                        if (location != null) {
                            val valid = isInSlovenia(location)
                            val color = if (valid) "#00FF00" else "#FF4500"
                            val symbol = if (valid) "check" else "cross"

                            features.add(
                                buildJsonObject {
                                    put("type", JsonPrimitive("Feature"))
                                    put("geometry", buildJsonObject {
                                        put("type", JsonPrimitive("Point"))
                                        put(
                                            "coordinates",
                                            JsonArray(listOf(JsonPrimitive(location.y), JsonPrimitive(location.x)))
                                        )
                                    })
                                    put("properties", buildJsonObject {
                                        put("type", JsonPrimitive("validate"))
                                        put("check", JsonPrimitive(it))
                                        put("valid", JsonPrimitive(valid.toString()))
                                        put("marker-color", JsonPrimitive(color))
                                        put("marker-symbol", JsonPrimitive(symbol))
                                    })
                                }
                            )
                        }
                    }
                }

                is HighlightCommand -> {
                    val point = when (val value = evaluateExpression(node.expression, context.toMap())) {
                        is Point -> value
                        else -> null
                    }
                    if (point != null) {
                        addFeature(
                            point,
                            mapOf("type" to "highlight", "marker-color" to "#ff0000", "marker-symbol" to "star", "marker-size" to "medium",)
                        )
                    }
                    val neighExpr = when (node.expression) {
                        is Neigh -> node.expression
                        is Identifier -> {
                            val value = context[node.expression.name]
                            if (value is ExpressionValue && value.expression is Neigh)
                                value.expression as Neigh
                            else null
                        }

                        else -> null
                    }

                    if (neighExpr != null) {
                        val base = when (val loc = neighExpr.location) {
                            is Point -> loc
                            is LocationIdentifier -> (context[loc.name] as? PointValue)?.point
                            else -> null
                        }
                        val radius = evaluateNumeric(neighExpr.radius, context.toMap())
                        if (base != null && radius != null) {
                            addCircle(
                                base,
                                radius,
                                mapOf("type" to "neigh", "highlighted" to "true", "marker-color" to "#ffff00", "marker-size" to "medium",)
                            )
                        }
                    }
                }

                is Neigh -> {
                    val base = when (val loc = node.location) {
                        is Point -> loc
                        is LocationIdentifier -> (context[loc.name] as? PointValue)?.point
                        else -> null
                    }
                    val radius = if (node.radius is NumberLiteral) node.radius.value else null
                    if (base != null && radius != null) {
                        addCircle(base, radius, mapOf("type" to "neigh", "marker-color" to "#0000FF", "marker-size" to "medium",))
                    }
                }

                is ForeachStatement -> {
                    val items = when (val exprValue = node.iterable) {
                        is Identifier -> (context[exprValue.name] as? ListValue)?.items
                        else -> null
                    }

                    if (items != null) {
                        items.forEach { item ->
                            val localContext = context.toMutableMap()
                            localContext[node.variable] = item

                            val previousContext = context.toMap()
                            context.clear()
                            context.putAll(localContext)

                            node.body.forEach(::traverse)

                            context.clear()
                            context.putAll(previousContext)
                        }
                    }
                }

                is ForStatement -> {
                    val start = evaluateNumeric(node.start, context) ?: return
                    val end = evaluateNumeric(node.end, context) ?: return

                    for (i in start.toInt()..end.toInt()) {
                        val previousContext = context.toMap()
                        context[node.variable] = ExpressionValue(NumberLiteral(i.toDouble()))

                        node.body.forEach(::traverse)

                        context.clear()
                        context.putAll(previousContext)
                    }
                }

                is IfStatement -> {
                    if (evaluateBoolean(node.condition)) {
                        node.thenBranch.forEach(::traverse)
                    } else {
                        node.elseBranch?.forEach(::traverse)
                    }
                }

                is ParkNode -> {
                    val base = Point(46.05, 14.5)
                    addBox(
                        base,
                        Point(base.x + 0.01, base.y + 0.01),
                        mapOf("type" to "park", "name" to node.name, "fill" to "#00FF00")
                    )
                    node.body.forEach(::traverse)
                }

                is LakeNode -> {
                    val base = node.location?.let { evaluateExpression(it, context.toMap()) } ?: Point(46.354989, 14.077677)
                    addBox(
                        base,
                        Point(base.x + 0.02, base.y + 0.03),
                        mapOf("type" to "lake", "name" to node.name, "fill" to "#0000FF")
                    )
                    node.body.forEach(::traverse)
                }

                is BuildingNode -> {
                    val base = Point(46.07, 14.7)
                    addBox(
                        base,
                        Point(base.x + 0.005, base.y + 0.005),
                        mapOf("type" to "building", "name" to node.name, "fill" to "#999999")
                    )
                    node.body.forEach(::traverse)
                }

                is RoadNode -> {
                    val start = Point(46.08, 14.8)
                    val end = Point(46.085, 14.805)
                    addLine(start, end, mapOf("type" to "road", "name" to node.name))
                    node.body.forEach(::traverse)
                }

                is ProcedureDef -> {
                    procedures[node.name] = node
                }

                is ProcedureCall -> {
                    val proc = procedures[node.name] ?: return
                    if (node.arguments.size != proc.parameters.size) return

                    val localEnv = context.toMutableMap()
                    proc.parameters.zip(node.arguments).forEach { (param, argExpr) ->
                        val evaluated: Value = when (val eval = evaluateExpression(argExpr, context.toMap())) {
                            is Point -> PointValue(eval)
                            else -> {
                                val num = evaluateNumeric(argExpr, context.toMap())
                                if (num != null) ExpressionValue(NumberLiteral(num))
                                else ExpressionValue(argExpr)
                            }
                        }
                        localEnv[param] = evaluated
                    }

                    val prevEnv = context.toMap()
                    context.clear()
                    context.putAll(localEnv)

                    proc.body.forEach(::traverse)

                    context.clear()
                    context.putAll(prevEnv)
                }

                is CommandNode -> when (node) {
                    is BoxCommand -> addBox(
                        evaluateExpression(node.corner1, context.toMap()) ?: return,
                        evaluateExpression(node.corner2, context.toMap()) ?: return,
                        mapOf("type" to "box")
                    )
                    is LineCommand -> addLine(
                        evaluateExpression(node.from, context.toMap()) ?: return,
                        evaluateExpression(node.to, context.toMap()) ?: return,
                        mapOf("type" to "line")
                    )
                    is CircleCommand -> if (node.radius is NumberLiteral) {
                        val center = evaluateExpression(node.center, context.toMap())
                        if (center != null)
                            addCircle(center, node.radius.value, mapOf("type" to "circle"))
                    }
                    is BendCommand -> if (node.angle is NumberLiteral) {
                        addArc(
                            evaluateExpression(node.from, context.toMap()) ?: return,
                            evaluateExpression(node.to, context.toMap()) ?: return,
                            node.angle.value,
                            mapOf("type" to "bend")
                        )
                    }
                    is CallCommand -> {
                        traverse(node)
                    }
                    else -> {}
                }
                else -> {}
            }
        }

        traverse(program)

        val geoJson = buildJsonObject {
            put("type", JsonPrimitive("FeatureCollection"))
            put("features", JsonArray(features))
        }

        return Json.encodeToString(JsonObject.serializer(), geoJson)
    }
}
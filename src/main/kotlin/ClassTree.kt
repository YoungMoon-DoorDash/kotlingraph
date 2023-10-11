import kotlinx.serialization.*
import kotlin.text.StringBuilder

@Serializable
data class ClassNode(
    val name: String,
    val path: String,
    val dependencies: List<String>
)

object ClassTree {
    private val packageColor = mapOf(
        "common" to "black",
        "dashpass_benefits" to "blue",
        "dashpass_management" to "green",
        "dashpass_partnerships" to "cyan",
        "mealplan" to "brown",
        "redeem_grpc" to "pink",
        "subscription_cadence" to "magenta",
        "subscription_common" to "orange",
        "subscription_grpc" to "red",
        "subscription_kafka" to "yellow",
        "taxcalculation" to "purple"
    )
    private val tree: MutableMap<String, ClassNode> = mutableMapOf<String, ClassNode>().toSortedMap()
    private val packageMap: MutableMap<String, String> = mutableMapOf<String, String>()


    fun addNode(node: ClassNode) {
        tree[node.name] = node
    }

    fun addPackage(className: String, packageName: String) {
        packageMap[className] = packageName
    }

    fun getPackage(className: String): String? = packageMap[className]

    fun buildGraph(className: String): String? =
        tree[className]?.let {
            val sb = StringBuilder()
            val seen = mutableSetOf<String>()
            seen.add(className)

            sb.append("digraph G {\n")
            buildGraph(it, sb, seen)
            addColorForEachNode(seen, sb)
            sb.append("}")
            sb.toString()
        }

    private fun buildGraph(
        node: ClassNode,
        sb: StringBuilder,
        seen: MutableSet<String>
    ) {
        val from = "${packageMap[node.name]}_${node.name}"
        seen.add(node.name)
        node.dependencies.forEach { child ->
            tree[child]?.let {
                if (!seen.contains(child)) {
                    sb.append(" $from -> ${packageMap[it.name]}_${it.name};\n")
                    buildGraph(it, sb, seen)
                }
            }
        }
    }

    private fun addColorForEachNode(seen: MutableSet<String>, sb: StringBuilder) {
        seen.forEach { node ->
            val from = "${packageMap[node]}_${node}"
            val color = packageColor[packageMap[node] ?: ""] ?: "black"
            sb.append(" $from [color=$color];\n")
        }
    }

    fun findDependency(className: String): String {
        val sb = StringBuilder()
        val from = "${packageMap[className]}_${className}"
        sb.append("digraph G {\n")

        val seen = mutableSetOf<String>()
        seen.add(className)
        tree.forEach { (_, node) ->
            if (node.dependencies.contains(className)) {
                seen.add(node.name)
                sb.append(" ${packageMap[node.name]}_${node.name} -> $from;\n")
            }
        }

        addColorForEachNode(seen, sb)
        sb.append("}")
        return sb.toString()
    }
}

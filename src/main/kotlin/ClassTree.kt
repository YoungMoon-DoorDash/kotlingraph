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
        "common" to "blue",
        "dashpass_benefits" to "blue",
        "dashpass_management" to "green",
        "dashpass_partnerships" to "cyan",
        "mealplan" to "gray",
        "redeem_grpc" to "gray",
        "subscription_cadence" to "magenta",
        "subscription_common" to "magenta",
        "subscription_grpc" to "red",
        "subscription_kafka" to "yellow",
        "taxcalculation" to "gray"
    )
    private val tree: MutableMap<String, ClassNode> = mutableMapOf<String, ClassNode>().toSortedMap()
    private val packageMap: MutableMap<String, String> = mutableMapOf<String, String>()


    fun addNode(node: ClassNode) {
        tree[node.name] = node
    }

    fun addPackage(className: String, packageName: String) {
        packageMap[className] = packageName
    }

    fun buildGraph(className: String): String? =
        tree[className]?.let {
            val sb = StringBuilder()
            val seen = mutableSetOf<String>()
            seen.add(className)

            sb.append("digraph G {\n")
            buildGraph(it, sb, seen)
            seen.forEach { node ->
                if (node != it.name) {
                    val from = "${packageMap[node]}_${node}"
                    val color = packageColor[packageMap[node] ?: ""] ?: "black"
                    sb.append(" $from [color=$color];\n")
                }
            }
            sb.append(" ${packageMap[it.name]}_${className} [color=blue,shape=rect];\n}")
            sb.toString()
        }

    private fun buildGraph(node: ClassNode, sb: StringBuilder, seen: MutableSet<String>) {
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
}

import kotlinx.serialization.*
import kotlin.text.StringBuilder

@Serializable
data class ClassNode(
    val name: String,
    val path: String,
    val dependencies: List<String>
)

object ClassTree {
    private val tree: MutableMap<String, ClassNode> = mutableMapOf<String, ClassNode>().toSortedMap()

    fun addNode(node: ClassNode) {
        tree[node.name] = node
    }

    fun buildGraph(className: String): String? =
        tree[className]?.let {
            val sb = StringBuilder()
            val seen = mutableSetOf<String>()
            seen.add(className)

            sb.append("digraph G {\n")
            buildGraph(it, sb, seen)
            sb.append(" $className [color=blue,shape=rect];\n}")
            sb.toString()
        }

    private fun buildGraph(node: ClassNode, sb: StringBuilder, seen: MutableSet<String>) {
        seen.add(node.name)
        node.dependencies.forEach { child ->
            tree[child]?.let {
                if (!seen.contains(child)) {
                    sb.append(" ${node.name} -> ${it.name};\n")
                    buildGraph(it, sb, seen)
                }
            }
        }
    }
}

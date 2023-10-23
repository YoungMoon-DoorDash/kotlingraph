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
        "dashpass_benefits" to "lavender",
        "dashpass_infra" to "lawngreen",
        "dashpass_partnerships" to "cyan",
        "subscription_cadence" to "yellow",
        "subscription_core" to "pink",
        "subscription_grpc" to "pink",
        "subscription_kafka" to "sandybrown",
        "subscription_main" to "gold",
        // removed packages
        "subscription_common" to "gray",
        "mealplan" to "gray",
        "redeem_grpc" to "gray",
        "taxcalculation" to "gray"
    )
    private val tree: MutableMap<String, ClassNode> = mutableMapOf<String, ClassNode>().toSortedMap()
    private val packageMap: MutableMap<String, String> = mutableMapOf()
    private val sameNamedClasses: MutableMap<String, String> = mutableMapOf()
    private val cycleNodes: MutableSet<String> = mutableSetOf()

    fun addNode(node: ClassNode) {
        tree[node.name] = node
    }

    fun getClassNode(className: String): ClassNode? = tree[className]

    fun addPackage(className: String, packageName: String) {
        packageMap[className] = packageName
    }

    fun addSameNamedClass(leftClass: String, rightClass: String) {
        sameNamedClasses[leftClass] = rightClass
    }

    fun showSameNamedClasses() {
        if (sameNamedClasses.isNotEmpty()) {
            println("\n\nSame named classes:")
            sameNamedClasses.forEach { (left, right) ->
                println("\t$left -> $right")
            }
        }
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

    fun findCycles(className: String): String? =
        tree[className]?.let {
            val sb = StringBuilder()
            val seen = mutableSetOf<String>()
            val allNodes = mutableSetOf<String>()
            val path = mutableListOf<String>()
            seen.add(className)
            path.add("${packageMap[className]}_${className}")

            sb.append("digraph G {\n")
            val numCycles = detectCycles(it, sb, seen, allNodes, path)
            if (numCycles > 0) {
                println("\n\nDetecting $numCycles cycles")

                addColorForEachNode(cycleNodes, sb)
                sb.append("}")
                sb.toString()
            } else {
                println("\n\nThere's no cyclic dependency")
                null
            }
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
                if (!seen.contains(it.name)) {
                    sb.append(" $from -> ${packageMap[it.name]}_${it.name};\n")
                    buildGraph(it, sb, seen)
                }
            }
        }
    }

    private fun detectCycles(
        node: ClassNode,
        sb: StringBuilder,
        seen: MutableSet<String>,
        allNodes: MutableSet<String>,
        path: MutableList<String>
    ): Int {
        var cycles = 0
        seen.add(node.name)
        allNodes.add(node.name)
        node.dependencies.forEach { child ->
            tree[child]?.let {
                if (seen.contains(it.name)) {
                    path.forEach { node ->
                        sb.append(" $node ->")
                    }
                    sb.append("${packageMap[it.name]}_${it.name};\n")
                    seen.forEach { node -> cycleNodes.add(node) }
                    cycleNodes.add(it.name)
                    cycles++
                } else {
                    path.add("${packageMap[it.name]}_${it.name}")
                    cycles += detectCycles(it, sb, seen, allNodes, path)
                    path.removeLast()
                }
            }
        }

        seen.remove(node.name)
        return cycles
    }

    private fun addColorForEachNode(seen: MutableSet<String>, sb: StringBuilder) {
        seen.forEach { node ->
            val from = "${packageMap[node]}_${node}"
            val color = packageColor[packageMap[node] ?: ""] ?: "black"
            sb.append(" $from [color=$color,style=filled];\n")
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

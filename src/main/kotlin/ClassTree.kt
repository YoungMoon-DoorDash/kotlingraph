import kotlinx.serialization.*
import kotlin.text.StringBuilder

@Serializable
data class ClassNode(
    val name: String,
    val path: String,
    val dependencies: List<String>,
    val isInterface: Boolean = false
)

object ClassTree {
    private const val DEFAULT_COLOR = "slategray"
    private val packageColor = mapOf(
        "dashpass_benefits" to "lavender",
        "dashpass_infra" to "lawngreen",
        "dashpass_partnerships" to "cyan",
        "subscription_cadence" to "sandybrown",
        "subscription_core" to "pink",
        "subscription_grpc" to "pink",
        "subscription_kafka" to "tan",
        "subscription_main" to "gold",
        // removed packages
        "subscription_common" to "lightgray",
        "mealplan" to "lightgray",
        "redeem_grpc" to "lightgray",
        "taxcalculation" to "lightgray"
    )
    private val tree: MutableMap<String, ClassNode> = mutableMapOf<String, ClassNode>().toSortedMap()
    private val packageMap: MutableMap<String, String> = mutableMapOf()
    private val sameNamedClasses: MutableMap<String, String> = mutableMapOf()
    private val cycleNodes: MutableSet<String> = mutableSetOf()
    private val interfaceToClass = mapOf(
        "CadenceSharedService" to "CadenceSharedServiceImpl",
        "ScriptSharedService" to "ScriptSharedServiceImpl",
        "SubscribeSharedService" to "SubscribeSharedServiceImpl",
        "SubscriptionSharedService" to "SubscriptionSharedServiceImpl",
        "PartnerSharedService" to "PartnerSharedServiceImpl",
        "BenefitsSharedService" to "BenefitsSharedServiceImpl"
    )
    private val classToInterface = mapOf(
        "CadenceSharedServiceImpl" to "CadenceSharedService",
        "ScriptSharedServiceImpl" to "ScriptSharedService",
        "SubscribeSharedServiceImpl" to "SubscribeSharedService",
        "SubscriptionSharedServiceImpl" to "SubscriptionSharedService",
        "PartnerSharedServiceImpl" to "PartnerSharedService",
        "BenefitsSharedServiceImpl" to "BenefitsSharedService"
    )

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
            val packages = mutableSetOf<String>()
            seen.add(className)

            sb.append("digraph G {\n")
            buildGraph(it, sb, seen, packages)
            addColorForEachNode(seen, sb)
            sb.append("}")

            println("Dependent packages:")
            packages.forEach { println("\t$it") }
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
        seen: MutableSet<String>,
        packages: MutableSet<String>,
    ) {
        val curPackage = packageMap[node.name] ?: "extern"
        val from = "${curPackage}_${node.name}"
        packages.add(curPackage)
        seen.add(node.name)
        node.dependencies.forEach { child ->
            tree[child]?.let {
                if (!seen.contains(it.name)) {
                    sb.append(" $from -> ${packageMap[it.name]}_${it.name};\n")
                    buildGraph(it, sb, seen, packages)
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
                val nodeName = interfaceToClass[it.name] ?: it.name
                if (seen.contains(nodeName)) {
                    path.forEach { node ->
                        sb.append(" $node ->")
                    }
                    sb.append("${packageMap[nodeName]}_${nodeName};\n")
                    seen.forEach { node -> cycleNodes.add(node) }
                    cycleNodes.add(nodeName)
                    cycles++
                } else {
                    path.add("${packageMap[nodeName]}_${nodeName}")
                    cycles += detectCycles(it, sb, seen, allNodes, path)
                    path.removeLast()
                }
            }
        }

        seen.remove(node.name)
        return cycles
    }

    fun findDependency(className: String): String {
        val sb = StringBuilder()
        val from = "${packageMap[className]}_${className}"
        sb.append("digraph G {\n")

        val classToFind = classToInterface[className] ?: className
        val seen = mutableSetOf<String>()
        seen.add(className)
        tree.forEach { (_, node) ->
            if (node.dependencies.contains(classToFind)) {
                seen.add(node.name)
                sb.append(" ${packageMap[node.name]}_${node.name} -> $from;\n")
            }
        }

        addColorForEachNode(seen, sb)
        sb.append("}")
        return sb.toString()
    }

    private fun addColorForEachNode(seen: MutableSet<String>, sb: StringBuilder) {
        seen.forEach { className ->
            tree[className]?.let {
                packageMap[className]?.let {
                    val from = "${it}_${className}"
                    val color = packageColor[it] ?: DEFAULT_COLOR

                    val classNode = getClassNode(className)
                    if (classNode?.isInterface == true) {
                        sb.append(" $from [color=black,fillcolor=$color,style=filled,shape=rect];\n")
                    } else {
                        sb.append(" $from [color=$color,style=filled];\n")
                    }
                }
            }
        }
    }
}

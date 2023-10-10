import java.io.File

object Parser {
    private val expClass = Regex("class (\\w+) @Inject constructor")
    private val expDep = Regex("(\\w+): (\\w+),")

    fun parseFiles(rootFolder: String) =
        File(rootFolder).walk().forEach {
            if (it.isFile && it.extension == "kt") {
                parseSourceFile(it)
            }
        }

    private fun parseSourceFile(file: File) {
        var classFound = false
        var className: String? = null
        val dependentList: MutableList<String> = mutableListOf()
        file.useLines { lines ->
            lines.forEach {
                if (classFound) {
                    if (it.endsWith("{")) {
                        ClassTree.addNode(ClassNode(className!!, file.path, dependentList.toList()))

                        classFound = false
                        className = null
                        dependentList.clear()
                    } else {
                        expDep.find(it)?.let { match ->
                            val (_, depClass) = match.destructured
                            if (depClass.endsWith("Repository") || depClass.endsWith("Client")) {
                                println("Skip repository layer component: $depClass")
                            } else {
                                dependentList.add(depClass)
                            }
                        }
                    }
                } else {
                    expClass.find(it)?.let { match ->
                        classFound = true
                        className = match.destructured.component1()
                    }
                }
            }
        }
    }
}
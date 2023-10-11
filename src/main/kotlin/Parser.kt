import java.io.File
import java.lang.StringBuilder

object Parser {
    private const val PACKAGE_HEADER = "package com.doordash.subscription."
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
        println("Parsing ${file.path}")
        file.useLines { lines ->
            lines.forEach {
                if (it.startsWith(PACKAGE_HEADER)) {
                    val sb = StringBuilder()
                    var i = PACKAGE_HEADER.length;
                    while (i < it.length) {
                        if (it[i] == '.') {
                            break
                        }

                        sb.append(it[i])
                        i++
                    }
                    ClassTree.addPackage(file.nameWithoutExtension, sb.toString())
                } else if (classFound) {
                    if (it.endsWith("{")) {
                        requireNotNull(className) { "Class name is null"}

                        ClassTree.addNode(ClassNode(className!!, file.path, dependentList.toList()))

                        classFound = false
                        className = null
                        dependentList.clear()
                    } else {
                        expDep.find(it)?.let { match ->
                            val (_, depClass) = match.destructured
                            if (depClass.endsWith("Repository") ||
                                depClass.endsWith("Client") ||
                                depClass == "Location" ||
                                depClass.endsWith("DynamicValueConfig")
                            ) {
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
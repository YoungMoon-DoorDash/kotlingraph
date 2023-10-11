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
                    parsePackageName(it, file)
                } else if (classFound) {
                    if (it.endsWith("{")) {
                        addClassName(className ?: "", file, dependentList)

                        classFound = false
                        className = null
                        dependentList.clear()
                    } else {
                        expDep.find(it)?.let { match ->
                            val (_, depClass) = match.destructured
                            if (!isRepositoryClass(depClass)) {
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

    private fun addClassName(className: String, file: File, dependentList: MutableList<String>) {
        if (className.isEmpty() || className.isBlank()) {
            return
        }

        ClassTree.addNode(ClassNode(className, file.path, dependentList.toList()))
        if (ClassTree.getPackage(className) == null) {
            val packageName = ClassTree.getPackage(file.nameWithoutExtension)
            if (packageName != null) {
                ClassTree.addPackage(className, packageName)
            }
        }
    }

    private fun parsePackageName(it: String, file: File) {
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
    }

    private fun isRepositoryClass(depClass: String) = depClass.endsWith("Repository") ||
        depClass.endsWith("Client") ||
        depClass == "Location" ||
        depClass.endsWith("DynamicValueConfig")
}
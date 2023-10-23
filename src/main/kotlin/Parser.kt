import java.io.File
import java.lang.StringBuilder

object Parser {
    private const val PACKAGE_HEADER = "package com.doordash.subscription."
    private const val INTERFACE_PREFIX = "interface "
    private val classRegEx = Regex("class (\\w+)")
    private val interfaceRegEx =  Regex("interface (\\w+)")
    private val classTypeRegEx = Regex("(\\w+): (\\w+),")

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
                val tline = it.trim()
                if (tline.startsWith(PACKAGE_HEADER)) {
                    parsePackageName(it, file)
                } else if (tline.startsWith(INTERFACE_PREFIX)) {
                    interfaceRegEx.find(tline)?.let { match ->
                        val interfaceName = match.destructured.component1()
                        addClassName(interfaceName, file, mutableListOf(), true)
                    }
                } else if (classFound) {
                    if (tline.endsWith("{")) {
                        addClassName(className ?: "", file, dependentList)

                        classFound = false
                        className = null
                        dependentList.clear()
                    } else {
                        classTypeRegEx.find(tline)?.let { match ->
                            val (_, depClass) = match.destructured
                            dependentList.add(depClass)
                        }
                    }
                } else {
                    classRegEx.find(tline)?.let { match ->
                        classFound = true
                        className = match.destructured.component1()
                    }
                }
            }
        }
    }

    private fun addClassName(
        className: String,
        file: File,
        dependentList: MutableList<String>,
        isInterface: Boolean = false
    ) {
        if (className.isEmpty() || className.isBlank()) {
            return
        }

        ClassTree.getClassNode(className)?.let {
            val leftClass = "${ClassTree.getPackage(className)}_${className}"
            val rightClass = "${ClassTree.getPackage(file.nameWithoutExtension)}_${className}"
            if (leftClass != rightClass) {
                ClassTree.addSameNamedClass(leftClass, rightClass)
            }
        }

        ClassTree.addNode(ClassNode(className, file.path, dependentList.toList(), isInterface))
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
}
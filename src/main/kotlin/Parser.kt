import java.io.File

object Parser {
    private const val PACKAGE_HEADER = "package com.doordash.subscription."
    private const val INTERFACE_PREFIX = "interface "
    private val injectClassReg = Regex("class (\\w+) @Inject")
    private val openClassReg = Regex("open class (\\w+)")
    private val interfaceRegEx =  Regex("interface (\\w+)")
    private val externClass = setOf(
        "AsgardRedisClient"
    )

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
                        parseClassName(tline)?.let { depClass ->
                            dependentList.add(depClass)
                        }
                    }
                } else {
                    injectClassReg.find(tline)?.let { match ->
                        classFound = true
                        className = match.destructured.component1()
                    } ?: let {
                        openClassReg.find(tline)?.let { match ->
                            classFound = true
                            className = match.destructured.component1()
                        }
                    }
                }
            }
        }

        if (classFound) {
            addClassName(className ?: "", file, dependentList)
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

    private fun parseClassName(it: String): String? {
        if (it.startsWith("//") || it.startsWith(")"))
            return null

        var separtorIndex = it.indexOf(":")
        if (separtorIndex == -1) {
            return null
        }

        val sb = StringBuilder()
        val templateIndex = it.indexOf("<", separtorIndex + 1)
        return if (templateIndex > 0) {
            val containerClassName = it.substring(separtorIndex + 1, templateIndex).trim()
            if (externClass.contains(containerClassName)) {
                return null
            }

            var i = templateIndex + 1
            while (i < it.length) {
                if (it[i] == '>') {
                    break
                }

                sb.append(it[i])
                i++
            }
            sb.toString().trim()
        } else {
            var i = separtorIndex + 1
            while (i < it.length) {
                if (it[i] == ',' || it[i] == '?' || it[i] == '=') {
                    break
                }

                sb.append(it[i])
                i++
            }
            sb.toString().trim().replace(".", "_")
        }
    }
}
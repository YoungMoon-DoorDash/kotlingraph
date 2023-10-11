import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.required
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

fun String.runCommand(workingDir: File) {
    ProcessBuilder(*split(" ").toTypedArray())
        .directory(workingDir)
        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
        .redirectError(ProcessBuilder.Redirect.INHERIT)
        .start()
        .waitFor(60, TimeUnit.MINUTES)
}

fun main(args: Array<String>) {
    val parser = ArgParser("kotlin-dependency-graph")
    val rootFolder by parser.option(
        ArgType.String,
        shortName = "r",
        description = "Root folder to search for Kotlin files"
    ).required()
    val className by parser.option(
        ArgType.String,
        shortName = "c",
        description = "Class name to generate graph"
    ).required()

    parser.parse(args)
    println("rootFolder: $rootFolder")
    println("className: $className")

    Parser.parseFiles(rootFolder)
    ClassTree.buildGraph(className)?.let { graph ->
        val cwd = Paths.get("").toAbsolutePath().toString()
        val workingDir = File(cwd)
        File(cwd, "$className.vis").printWriter().use { out ->
            out.println(graph)
        }

        "dot -Kfdp -Tsvg $cwd/$className.vis -o$cwd/$className.svg".runCommand(workingDir)
        "rm -rf $cwd/$className.vis".runCommand(workingDir)
        "open $cwd/$className.svg".runCommand(workingDir)
    } ?: println("Class not found")
}

import edu.utsa.fileflow.analysis.Analyzer
import edu.utsa.fileflow.cfg.FlowPoint
import edu.utsa.fileflow.utilities.FileFlowHelper
import edu.utsa.fileflow.utilities.GraphvizGenerator
import fileflow.FFA
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.required
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess

private const val DEBUG = true

fun main(args: Array<String>) {
    Analyzer.CONTINUE_ON_ERROR = true
    Analyzer.VERBOSE = false

    val parser = ArgParser("ffa")
    val scanPath by parser.option(
        ArgType.String,
        fullName = "file",
        shortName = "f",
        description = "Input file or directory"
    ).required()
    val preconditionDir by parser.option(
        ArgType.String,
        fullName = "precondition",
        description = "Use this directory as the precondition"
    )
    val resultsDir by parser.option(
        ArgType.String,
        fullName = "results",
        description = "Use this directory to store results (Not yet implemented)",
    ).default("dot")
    parser.parse(args)

    // Locate files to analyze
    val files: List<File> = try {
        scanFiles(scanPath)
    } catch (e: FileNotFoundException) {
        System.err.println(e.message!!)
        exitProcess(1)
    }

    // Make sure files were found
    if (files.isEmpty()) {
        System.err.println("no '.ffa' files were found")
        exitProcess(1)
    }

    // Display the file list
    println("Files:")
    files.forEach { println("  - $it") }

    // Analyzer each file in the list
    for (f in files) {
        println()
        println("=".repeat(80))
        println("Analyzing file: $f")
        val saveDir = f.toPath().fileName.toString().replace("\\.ffa$".toRegex(), "")
        try {
            val cfg = FileFlowHelper.generateControlFlowGraphFromFile(f)
            writeDOT(cfg, saveDir)
            val ffa = FFA(cfg)
            GraphvizGenerator.PATH_PREFIX = saveDir
            if (preconditionDir != null) {
                ffa.runUsingSystemPath(preconditionDir!!)
            } else {
                ffa.run()
            }
            GraphvizGenerator.PATH_PREFIX = ""
            val timeResults = "Variable analysis elapsed time: ${ffa.variableElapsedTime}ms\n" +
                    "Grammar analysis elapsed time: ${ffa.grammarElapsedTime}ms\n" +
                    "FFA first run elapsed time: ${ffa.ffaElapsedTime1}ms\n" +
                    "FFA second run elapsed time: ${ffa.ffaElapsedTime2}ms\n"
            Files.write(Paths.get("dot", saveDir, "time.txt"), timeResults.toByteArray())
            if (DEBUG) println(timeResults)
        } catch (e: Exception) {
            e.printStackTrace()
            System.err.println("error: failed to analyze $f")
        }
    }
}

fun scanFiles(filepath: String): List<File> {
    val file = File(filepath)
    if (!file.exists())
        throw FileNotFoundException("No such file or directory: '$file'")
    val files: MutableList<File> = mutableListOf()
    if (file.isDirectory) {
        file.walk().forEach {
            if (!it.isDirectory) {
                files.add(it)
            }
        }
    } else {
        files.add(file)
    }
    return files.filter { it.toString().endsWith(".ffa") }
}

/**
 * Generate DOT file before analysis
 *
 * @param cfg FlowPoint to represent entry point of the CFG
 */
private fun writeDOT(cfg: FlowPoint, filepath: String) {
    val dot = GraphvizGenerator.generateDOT(cfg)
    val path = Paths.get("dot", filepath)
    path.toFile().mkdirs()
    GraphvizGenerator.saveDOTToFile(dot, Paths.get(filepath, "cfg.dot").toString())
    if (DEBUG) {
        println("DOT file written to: " + Paths.get(path.toString(), "cfg.dot"))
        println()
    }
}
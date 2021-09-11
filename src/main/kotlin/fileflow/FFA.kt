package fileflow

import edu.utsa.fileflow.analysis.AnalysisException
import edu.utsa.fileflow.analysis.Analyzer
import edu.utsa.fileflow.cfg.FlowPoint
import grammar.GrammarAnalysis
import grammar.GrammarAnalysisDomain
import variable.VariableAnalysis
import variable.VariableAnalysisDomain
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.system.exitProcess

class FFA internal constructor(var cfg: FlowPoint) {
    /* Variable Analyzer */
    var variableAnalysisDomain: VariableAnalysisDomain? = null
    var variableAnalysis: VariableAnalysis? = null
    var variableAnalyzer: Analyzer<VariableAnalysisDomain, VariableAnalysis>? = null
    var variableElapsedTime = 0L

    /* Grammar Analysis */
    var grammarDomain: GrammarAnalysisDomain? = null
    var grammarAnalysis: GrammarAnalysis? = null
    var grammarAnalyzer: Analyzer<GrammarAnalysisDomain, GrammarAnalysis>? = null
    var grammarElapsedTime = 0L

    /* File Flow Analysis */
    var ffaDomain: FileFlowAnalysisDomain? = null
    var ffaAnalysis: FileFlowAnalysis? = null
    var ffaAnalyzer: Analyzer<FileFlowAnalysisDomain, FileFlowAnalysis>? = null
    var ffaElapsedTime1 = 0L
    var ffaElapsedTime2 = 0L

    @Throws(IOException::class)
    fun runUsingSystemPath(filepath: String) {
        val init = FileStructure()
        val prefix = "/home/user/"
        val systemPath = Paths.get(filepath)
        val rootPath = systemPath.toString().replace(systemPath.parent.toString(), prefix) + "/"

        // Iterate through each file under the provided system path
        Files.walk(systemPath).forEach { x: Path ->
            var newpath = x.toString().replace(systemPath.parent.toString(), prefix)
            val levels = newpath.replace(prefix, "").split("[/\\\\]").toTypedArray()
            if (levels.size > 3) {
                if (levels[2] == ".git") {
                    return@forEach
                }
            }
            if (Files.isDirectory(x)) newpath += "/"
            val va = VariableAutomaton(newpath)
            init.forceCreate(va)
        }

        // Set the root path for the ffa script
        init.changeWorkingDirectory(VariableAutomaton(rootPath))
        runWithPrecondition(init)
    }

    fun run() {
        runWithPrecondition(null)
    }

    fun runWithPrecondition(precondition: FileStructure?) {
        /* Variable Analysis */
        variableAnalysisDomain = VariableAnalysisDomain()
        variableAnalysis = VariableAnalysis()
        variableAnalyzer = Analyzer(variableAnalysisDomain, variableAnalysis)

        /* Grammar Analysis */grammarDomain = GrammarAnalysisDomain()
        grammarAnalysis = GrammarAnalysis()
        grammarAnalyzer = Analyzer(grammarDomain, grammarAnalysis)

        /* File Flow Analysis */if (precondition == null) {
            ffaDomain = FileFlowAnalysisDomain()
            ffaAnalysis = FileFlowAnalysis()
        } else {
            ffaDomain = FileFlowAnalysisDomain(precondition)
            ffaAnalysis = FileFlowAnalysis(precondition)
        }
        ffaAnalyzer = Analyzer(ffaDomain, ffaAnalysis)
        try {
            var start = System.currentTimeMillis()
            variableAnalyzer!!.analyze(cfg)
            variableElapsedTime = System.currentTimeMillis() - start
            start = System.currentTimeMillis()
            grammarAnalyzer!!.analyze(cfg)
            grammarElapsedTime = System.currentTimeMillis() - start
            if (precondition == null) {
                // The post-condition of the first run will be used as the pre-condition of the second run
                start = System.currentTimeMillis()
                ffaAnalyzer!!.analyze(cfg)
                ffaElapsedTime1 = System.currentTimeMillis() - start
            }

            // Run again
            start = System.currentTimeMillis()
            ffaAnalyzer!!.analyze(cfg)
            ffaElapsedTime2 = System.currentTimeMillis() - start
        } catch (e: AnalysisException) {
            e.printStackTrace()
            exitProcess(1)
        }
    }
}
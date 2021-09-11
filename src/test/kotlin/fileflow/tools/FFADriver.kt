package fileflow.tools

import edu.utsa.fileflow.analysis.AnalysisException
import edu.utsa.fileflow.analysis.Analyzer
import edu.utsa.fileflow.cfg.FlowPoint
import edu.utsa.fileflow.utilities.FileFlowHelper
import fileflow.FileFlowAnalysis
import fileflow.FileFlowAnalysisDomain
import grammar.GrammarAnalysis
import grammar.GrammarAnalysisDomain
import variable.LiveVariableMap
import variable.VariableAnalysis
import variable.VariableAnalysisDomain

/**
 * This is a helper class to make it simple to run a script and store the results
 * during testing.
 *
 *
 * Created by Rodney on 4/26/2017.
 */
class FFADriver private constructor(script: String) {
    /* Variable Analysis */
    var variableAnalysisDomain: VariableAnalysisDomain? = null
    var variableAnalysis: VariableAnalysis? = null
    var variableAnalyzer: Analyzer<VariableAnalysisDomain, VariableAnalysis>? = null

    /* Grammar Analysis */
    var grammarDomain: GrammarAnalysisDomain? = null
    var grammarAnalysis: GrammarAnalysis? = null
    var grammarAnalyzer: Analyzer<GrammarAnalysisDomain, GrammarAnalysis>? = null

    /* File Flow Analysis */
    var ffaDomain: FileFlowAnalysisDomain? = null
    var ffaAnalysis: FileFlowAnalysis? = null
    var ffaAnalyzer: Analyzer<FileFlowAnalysisDomain, FileFlowAnalysis>? = null

    /* Results */
    var variableResult: VariableAnalysisDomain? = null
    var grammarResult: GrammarAnalysisDomain? = null
    var ffaResult: FileFlowAnalysisDomain? = null
    var liveVariables: LiveVariableMap? = null
    private val cfg: FlowPoint
    private fun setUp() {
        /* Variable Analysis */
        variableAnalysisDomain = VariableAnalysisDomain()
        variableAnalysis = VariableAnalysis()
        variableAnalyzer =
            Analyzer<VariableAnalysisDomain, VariableAnalysis>(variableAnalysisDomain, variableAnalysis)

        /* Grammar Analysis */grammarDomain = GrammarAnalysisDomain()
        grammarAnalysis = GrammarAnalysis()
        grammarAnalyzer = Analyzer<GrammarAnalysisDomain, GrammarAnalysis>(grammarDomain, grammarAnalysis)

        /* File Flow Analysis */ffaDomain = FileFlowAnalysisDomain()
        ffaAnalysis = FileFlowAnalysis()
        ffaAnalyzer = Analyzer<FileFlowAnalysisDomain, FileFlowAnalysis>(ffaDomain, ffaAnalysis)
    }

    @Throws(AnalysisException::class)
    private fun createAnalyzers() {
        variableResult = variableAnalyzer!!.analyze(cfg)
        grammarResult = grammarAnalyzer!!.analyze(cfg)
        ffaResult = ffaAnalyzer!!.analyze(cfg)
        liveVariables = variableResult?.liveVariables
    }

    companion object {
        @Throws(Exception::class)
        fun run(script: String): FFADriver {
            val ffaDriver = FFADriver(script)
            ffaDriver.setUp()
            ffaDriver.createAnalyzers()
            return ffaDriver
        }
    }

    init {
        cfg = FileFlowHelper.generateControlFlowGraphFromScript(script)
    }
}
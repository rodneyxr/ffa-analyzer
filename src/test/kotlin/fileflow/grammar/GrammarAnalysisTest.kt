package fileflow.grammar

import dk.brics.automaton.Automaton
import edu.utsa.fileflow.analysis.Analyzer
import edu.utsa.fileflow.cfg.FlowPoint
import edu.utsa.fileflow.utilities.FileFlowHelper
import grammar.GrammarAnalysis
import grammar.GrammarAnalysisDomain
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import variable.LiveVariableMap
import variable.VariableAnalysis
import variable.VariableAnalysisDomain

/**
 * This class tests functionality of the variable analysis.
 */
class GrammarAnalysisTest {
    /* Variable Analysis */
    private lateinit var variableAnalyzer: Analyzer<VariableAnalysisDomain, VariableAnalysis>

    /* Grammar Analysis */
    private lateinit var grammarAnalyzer: Analyzer<GrammarAnalysisDomain, GrammarAnalysis>

    /* Results */
    private lateinit var variableAnalysisResult: VariableAnalysisDomain
    private lateinit var grammarAnalysisResult: GrammarAnalysisDomain
    private lateinit var liveVariables: LiveVariableMap

    @BeforeEach
    @Throws(Exception::class)
    fun setUp() {
        /* Variable Analysis */
        variableAnalyzer = Analyzer(VariableAnalysisDomain(), VariableAnalysis())

        /* Grammar Analysis */
        grammarAnalyzer = Analyzer(GrammarAnalysisDomain(), GrammarAnalysis())
    }

    @Test
    @Throws(Exception::class)
    fun testAnalysis() {
        val cfg: FlowPoint = FileFlowHelper.generateControlFlowGraphFromScript(
            "" +
                    "\$x0 = 'a';" +
                    "\$x1 = 'b';" +
                    "\$x2 = 'c';" +
                    "\$x3 = \$x0;" +
                    "\$x3 = \$x1;" +
                    "if (other) {" +
                    "   \$x3 = \$x2;" +
                    "}" +
                    "\$x4 = \$x3;"
        )
        createAnalyzers(cfg)
        var a: Automaton = grammarAnalysisResult.getVariable("\$x0", liveVariables)!!
        Assertions.assertTrue(a.run("a"))
        a = grammarAnalysisResult.getVariable("\$x1", liveVariables)!!
        Assertions.assertTrue(a.run("b"))
        a = grammarAnalysisResult.getVariable("\$x2", liveVariables)!!
        Assertions.assertTrue(a.run("c"))
        a = grammarAnalysisResult.getVariable("\$x3", liveVariables)!!
        Assertions.assertTrue(a.run("b"))
        Assertions.assertTrue(a.run("c"))
        a = grammarAnalysisResult.getVariable("\$x4", liveVariables)!!
        Assertions.assertTrue(a.run("b"))
        Assertions.assertTrue(a.run("c"))
    }

    @Test
    fun testAnalysisSimple() {
        val cfg: FlowPoint = FileFlowHelper.generateControlFlowGraphFromScript(
            "" +
                    "\$x0 = 'a';" +
                    "\$x1 = 'b';" +
                    "if (other) {" +
                    "    \$x0 = \$x1;" + // x0=b
                    "}" +
                    "\$x1 = \$x0;" // x1=(a|b)
        )
        createAnalyzers(cfg)
        var a: Automaton = grammarAnalysisResult.getVariable("\$x0", liveVariables)!!
        Assertions.assertTrue(a.run("a"))
        Assertions.assertTrue(a.run("b"))
        a = grammarAnalysisResult.getVariable("\$x1", liveVariables)!!
        Assertions.assertTrue(a.run("a"))
        Assertions.assertTrue(a.run("b"))
    }

    @Test
    @Throws(Exception::class)
    fun testAnalysisWithSimpleLoop() {
        val cfg: FlowPoint = FileFlowHelper.generateControlFlowGraphFromScript(
            "" +
                    "\$x0 = 'a';" +
                    "while (other) {" +
                    "    \$x0 = 'b';" +
                    "}"
        )
        createAnalyzers(cfg)
        val result: GrammarAnalysisDomain = grammarAnalyzer.analyze(cfg)
        val a = result.getVariable("\$x0", liveVariables)!!
        Assertions.assertTrue(a.run("a"))
        Assertions.assertTrue(a.run("b"))
    }

    @Test
    @Throws(Exception::class)
    fun testAnalysisWithConcatLoop1Var() {
        val cfg: FlowPoint = FileFlowHelper.generateControlFlowGraphFromScript(
            "" +
                    "\$x0 = 'a';" +
                    "while (other) {" +
                    "    \$x0 = \$x0.\$x0;" +
                    "}"
        )
        createAnalyzers(cfg)
        val a: Automaton = grammarAnalysisResult.getVariable("\$x0", variableAnalysisResult.liveVariables)!!
        Assertions.assertTrue(a.run("a"))
        Assertions.assertTrue(a.run("aa"))
    }

    @Test
    @Throws(Exception::class)
    fun testAnalysisWithConcatLoop2Vars() {
        val cfg: FlowPoint = FileFlowHelper.generateControlFlowGraphFromScript(
            "" +
                    "\$x0 = 'a';" +
                    "\$x1 = 'b';" +
                    "while (other) {" +
                    "    \$x0 = \$x0.\$x1;" +
                    "}"
        )
        createAnalyzers(cfg)
        var a: Automaton = grammarAnalysisResult.getVariable("\$x0", liveVariables)!!
        Assertions.assertTrue(a.run("a"))
        Assertions.assertTrue(a.run("ab"))
        a = grammarAnalysisResult.getVariable("\$x1", liveVariables)!!
        Assertions.assertTrue(a.run("b"))
        Assertions.assertFalse(a.run("a"))
    }

    @Throws(Exception::class)
    private fun createAnalyzers(cfg: FlowPoint) {
        variableAnalysisResult = variableAnalyzer.analyze(cfg)
        grammarAnalysisResult = grammarAnalyzer.analyze(cfg)
        liveVariables = variableAnalysisResult.liveVariables
    }
}
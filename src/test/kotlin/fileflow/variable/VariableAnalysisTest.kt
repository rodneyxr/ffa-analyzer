package fileflow.variable

import edu.utsa.fileflow.analysis.Analyzer
import edu.utsa.fileflow.utilities.FileFlowHelper
import org.junit.jupiter.api.Test
import variable.VariableAnalysis
import variable.VariableAnalysisDomain

/**
 * This class tests the functionality of the [VariableAnalysis] class.
 */
class VariableAnalysisTest {
    @Test
    @Throws(Exception::class)
    fun testSimpleScript() {
        val cfg = FileFlowHelper.generateControlFlowGraphFromScript(
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
        val variableAnalysisDomain = VariableAnalysisDomain()
        val variableAnalysis = VariableAnalysis()
        val variableAnalyzer: Analyzer<VariableAnalysisDomain, VariableAnalysis> =
            Analyzer<VariableAnalysisDomain, VariableAnalysis>(variableAnalysisDomain, variableAnalysis)
        variableAnalyzer.analyze(cfg)
    }
}
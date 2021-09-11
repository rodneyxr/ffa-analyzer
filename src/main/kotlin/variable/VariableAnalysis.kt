package variable

import edu.utsa.fileflow.analysis.Analysis
import edu.utsa.fileflow.analysis.AnalysisException
import edu.utsa.fileflow.cfg.FlowPointContext
import edu.utsa.fileflow.utilities.AssignContext


/**
 * This class overrides some methods that the analysis framework will call when
 * traversing the control flow graph of a script.
 */
class VariableAnalysis : Analysis<VariableAnalysisDomain>() {
    @Throws(AnalysisException::class)
    override fun enterAssignment(domain: VariableAnalysisDomain, context: FlowPointContext): VariableAnalysisDomain {
        val ctx = AssignContext(context)
        val flowpointID = context.flowPoint.id
        val v0 = Variable(ctx.var0, flowpointID)
        domain.liveVariables.addVariable(v0)

        // FIXME: this should be handled in the grammar
        if (ctx.literal != null && (ctx.var1 != null || ctx.var2 != null)) {
            throw AnalysisException("literal and var cannot be both defined.")
        }
        return domain
    }
}
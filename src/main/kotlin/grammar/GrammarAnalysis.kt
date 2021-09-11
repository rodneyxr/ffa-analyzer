package grammar

import dk.brics.automaton.Automaton
import edu.utsa.fileflow.analysis.Analysis
import edu.utsa.fileflow.analysis.AnalysisException
import edu.utsa.fileflow.cfg.FlowPointContext
import edu.utsa.fileflow.utilities.AssignContext
import variable.Variable
import variable.VariableAnalysisDomain

/**
 * This class uses a live variable set to create a grammar for file flow analysis.
 */
class GrammarAnalysis : Analysis<GrammarAnalysisDomain>() {
    private lateinit var vDomain: VariableAnalysisDomain

    @Throws(AnalysisException::class)
    override fun onBefore(domain: GrammarAnalysisDomain, context: FlowPointContext): GrammarAnalysisDomain {
        vDomain = context.flowPoint.getOriginalDomain(VariableAnalysisDomain::class.java) as VariableAnalysisDomain
        return super.onBefore(domain, context)
    }

    /**
     * Supported Operations:
     * var = var
     * var = var.var
     * var = '$literal'
     */
    @Throws(AnalysisException::class)
    override fun enterAssignment(domain: GrammarAnalysisDomain, context: FlowPointContext): GrammarAnalysisDomain {
        val ctx = AssignContext(context)
        val flowpointID: Int = context.flowPoint.id
        val v0 = Variable(ctx.var0, flowpointID)

        // check if this node has been visited already
        if (domain.grammar.getVisited().contains(flowpointID)) {
            return domain
        }
        domain.grammar.addNonterminal(v0)

        // automaton production: $x0 = 'a';
        if (ctx.literal != null) {
            domain.grammar.addAutomatonProduction(v0, Automaton.makeString(ctx.literal))
        } else if (ctx.var1 != null) {
            // get or create v1
            val v1Set: Set<Variable> = vDomain.liveVariables.getVariable(ctx.var1)
                ?: throw AnalysisException(String.format("%s is not defined", ctx.var1))
            for (v1 in v1Set) {
                if (ctx.var2 != null) {
                    // pair production: $x0 = $x1.$x2;
                    val v2Set: Set<Variable> = vDomain.liveVariables.getVariable(ctx.var2)
                        ?: throw AnalysisException(String.format("%s is not defined", ctx.var2))
                    for (v2 in v2Set) {
                        if (v1.name != v2.name || v1.id == v2.id) {
                            domain.grammar.addPairProduction(v0, v1, v2)
                        }
                    }
                } else {
                    // unit production: $x0 = $x1;
                    domain.grammar.addUnitProduction(v0, v1)
                }
            }
        }
        return domain
    }
}
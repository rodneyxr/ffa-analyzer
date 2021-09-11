package variable

import dk.brics.automaton.Automaton
import edu.utsa.fileflow.analysis.AnalysisDomain


/**
 * This class is the analysis domain for variable analysis.
 * It will track all live variables in the entire program.
 */
class VariableAnalysisDomain : AnalysisDomain<VariableAnalysisDomain>() {
    companion object {
        init {
            Automaton.setMinimization(Automaton.MINIMIZE_BRZOZOWSKI)
            Automaton.setMinimizeAlways(true)
        }
    }

    var liveVariables = LiveVariableMap()

    override fun merge(other: VariableAnalysisDomain): VariableAnalysisDomain {
        liveVariables.merge(other.liveVariables)
        return this
    }

    override fun top(): VariableAnalysisDomain {
        // TODO: implement a top rather than just a new domain
        return VariableAnalysisDomain()
    }

    override fun bottom(): VariableAnalysisDomain {
        val bottom = VariableAnalysisDomain()
        bottom.liveVariables = LiveVariableMap()
        return bottom
    }

    override fun compareTo(other: VariableAnalysisDomain): Int {
        return if (liveVariables != other.liveVariables) 1 else 0
    }

    override fun clone(): VariableAnalysisDomain {
        val clone = bottom()
        clone.liveVariables = liveVariables.clone()
        return clone
    }
}

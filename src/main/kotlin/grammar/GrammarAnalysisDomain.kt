package grammar

import dk.brics.automaton.Automaton
import edu.utsa.fileflow.analysis.AnalysisDomain
import variable.LiveVariableMap
import variable.Variable

/**
 * This class is analysis domain for grammar section of the file flow analysis.
 * It holds a grammar which will track all productions in the entire program.
 */
class GrammarAnalysisDomain : AnalysisDomain<GrammarAnalysisDomain>() {
    companion object {
        init {
            Automaton.setMinimization(Automaton.MINIMIZE_BRZOZOWSKI)
            Automaton.setMinimizeAlways(true)
        }
    }

    var grammar = VariableGrammar()

    /**
     * Gets the variable value as an automaton from the grammar.
     *
     * @param variable The variable name to get the value of.
     * @return an [Automaton] of the variable value or `null` if undefined.
     */
    fun getVariable(variable: String, liveVariables: LiveVariableMap): Automaton? {
        val v: Set<Variable> = liveVariables.getVariable(variable) ?: return null
        return grammar.getVariableValue(v)
    }

    override fun merge(other: GrammarAnalysisDomain): GrammarAnalysisDomain {
        grammar.merge(other.grammar)
        return this
    }

    override fun top(): GrammarAnalysisDomain {
        // TODO: implement a top rather than just a new domain
        return GrammarAnalysisDomain()
    }

    override fun bottom(): GrammarAnalysisDomain {
        val bottom = GrammarAnalysisDomain()
        bottom.grammar = VariableGrammar()
        return bottom
    }

    override fun compareTo(other: GrammarAnalysisDomain): Int {
        return if (grammar != other.grammar) 1 else 0
    }

    override fun clone(): GrammarAnalysisDomain {
        val clone = bottom()
        clone.grammar = grammar.clone()
        return clone
    }
}
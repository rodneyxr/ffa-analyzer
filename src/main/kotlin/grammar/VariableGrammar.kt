package grammar

import dk.brics.automaton.Automaton
import dk.brics.string.grammar.Grammar
import dk.brics.string.grammar.Nonterminal
import dk.brics.string.grammar.operations.Grammar2MLFA
import dk.brics.string.grammar.operations.RegularApproximation
import dk.brics.string.mlfa.MLFA
import dk.brics.string.mlfa.operations.MLFA2Automaton
import edu.utsa.fileflow.analysis.Mergeable
import variable.Variable
import java.util.*


/**
 * This class is a wrapper for [dk.brics.string.grammar.Grammar].
 */
class VariableGrammar : Cloneable, Mergeable<VariableGrammar> {
    /**
     * Contains a set of FlowPoint ID's. A new ID is usually added when a new
     * nonterminal is inserted. Checking if visited.contains(FlowPoint.id) will
     * tell if a nonterminal for this node has been inserted in the Grammar.
     */
    private var visited: MutableSet<Int> = HashSet()

    /* The JSA Grammar that this class wraps */
    private var grammar: Grammar = Grammar()

    /* Maps variables to nonterminals in the grammar */
    private var variables: MutableMap<Variable, Nonterminal> = HashMap<Variable, Nonterminal>()

    /**
     * @return the set of visited FlowPoint ID's
     */
    fun getVisited(): Set<Int> {
        return Collections.unmodifiableSet(visited)
    }

    /**
     * Gets the variable's value from the grammar as an [Automaton].
     *
     * @param variableSet The set of live variables that represent the target variable.
     * @return an [Automaton] representing the variable's literal value.
     */
    fun getVariableValue(variableSet: Set<Variable>): Automaton {
        var a = Automaton()
        val r = RegularApproximation(grammar)
        r.approximate(variables.values)
        val g2m = Grammar2MLFA(grammar)
        val mlfa: MLFA = g2m.convert()
        val m2a = MLFA2Automaton(mlfa)
        for (v in variableSet) {
            a = a.union(m2a.extract(g2m.getMLFAStatePair(variables[v])))
        }
        return a
    }

    fun addNonterminal(v: Variable): Nonterminal {
        visited.add(v.id)
        val nonterminal: Nonterminal = grammar.addNonterminal(v.toString())
        variables[v] = nonterminal
        return nonterminal
    }

    // $x0 = 'a';
    fun addAutomatonProduction(v: Variable, a: Automaton?) {
        grammar.addAutomatonProduction(variables[v], a)
    }

    // $x0 = $x1;
    fun addUnitProduction(v0: Variable, v1: Variable) {
        val nt0: Nonterminal? = variables[v0]
        val nt1: Nonterminal? = variables[v1]
        grammar.addUnitProduction(nt0, nt1)
    }

    // $x0 = $x1.$x2;
    fun addPairProduction(v0: Variable, v1: Variable, v2: Variable) {
        val nt0: Nonterminal? = variables[v0]
        val nt1: Nonterminal? = variables[v1]
        val nt2: Nonterminal? = variables[v2]
        grammar.addPairProduction(nt0, nt1, nt2)
    }

    /**
     * No merge implementation is necessary since there is only one instance
     * of Grammar throughout the analysis.
     */
    override fun merge(other: VariableGrammar): VariableGrammar {
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (other !is VariableGrammar) return false
        return variables == other.variables && visited == other.visited
    }

    override fun toString(): String {
        return grammar.toString()
    }

    public override fun clone(): VariableGrammar {
        val clone = VariableGrammar()
        clone.grammar = grammar
        clone.visited = visited
        clone.variables = variables
        return clone
    }

    override fun hashCode(): Int {
        var result = visited.hashCode()
        result = 31 * result + grammar.hashCode()
        result = 31 * result + variables.hashCode()
        return result
    }
}

/**
 * This class simply wraps an Automaton object formatted as a variable.
 */
package fileflow

import dk.brics.automaton.Automaton
import dk.brics.automaton.State
import dk.brics.automaton.Transition
import edu.utsa.fileflow.analysis.Mergeable
import java.util.function.Consumer

// TODO: implement isAbsolute()
open class VariableAutomaton(variable: Automaton) : Mergeable<VariableAutomaton> {
    private var variable: Automaton

    constructor(fp: String) : this(Automaton.makeString(FileStructure.clean(fp)))

    /**
     * Joins two automatons to ensure that there is only one slash between the
     * join For example: 'dir1/' + '/file1' should be 'dir1/file1' instead of
     * 'dir1//file1'
     *
     * @param v The [VariableAutomaton] to append to this object.
     * @return A new [VariableAutomaton] object with `v`
     * concatenated.
     */
    fun concatenate(v: VariableAutomaton): VariableAutomaton {
        val a = variable.concatenate(v.variable)
        return VariableAutomaton(a)
    }

    fun union(v: VariableAutomaton): VariableAutomaton {
        val a = variable.union(v.variable)
        return VariableAutomaton(a)
    }

    fun intersection(v: VariableAutomaton): VariableAutomaton {
        val a = variable.intersection(v.variable)
        return VariableAutomaton(a)
    }

    fun join(v: VariableAutomaton): VariableAutomaton {
        return concatenate(SEPARATOR_VA).concatenate(v)
    }

    fun endsWith(a: Automaton?): Boolean {
        val result = variable.intersection(ANY_STRING_AUT.concatenate(a))
        return !result.isEmpty
    }

    fun startsWith(a: Automaton): Boolean {
        val result = variable.intersection(a.concatenate(ANY_STRING_AUT))
        return !result.isEmpty
    }

    fun subsetOf(a: Automaton?): Boolean {
        return variable.subsetOf(a)
    }

    fun isSamePathAs(other: VariableAutomaton): Boolean {
        val a1 = removeLastSeparator()
        val a2 = other.removeLastSeparator()
        return a1.variable == a2.variable
    }

    /**
     * @return true if the automaton ends with a separator; false otherwise
     */
    val isDirectory: Boolean
        get() = endsWith(SEPARATOR_AUT)

    /**
     * @return the parent directory of this automaton.
     */
    val parentDirectory: VariableAutomaton
        get() {
            val a = SEPARATOR_VA.concatenate(this)
            return VariableAutomaton(Transducers.parentDir(a.automaton))
        }

    /**
     * @return a clone of this automaton without the last separator if one
     * exists.
     */
    fun removeLastSeparator(): VariableAutomaton {
        return VariableAutomaton(Transducers.removeLastSeparator(variable))
    }

    fun toDot(): String {
        return variable.toDot()
    }

    /**
     * @return the automaton without separators as accept states.
     */
    val automaton: Automaton
        get() = variable.clone()

    /**
     * Sets all separators to accept states.
     *
     * @return the automaton with all separators as accept states.
     */
    val separatedAutomaton: Automaton
        get() {
            val a = variable.clone()

            // Make all separators accept states
            a.states.forEach(Consumer { s: State ->
                s.transitions.forEach(
                    Consumer { t: Transition ->
                        // if transition is a separator
                        if (t.min <= SEPARATOR_CHAR && t.max >= SEPARATOR_CHAR) {
                            // make destination state an accept state
                            t.dest.isAccept = true
                        }
                    })
            })
            return a
        }

    override fun merge(other: VariableAutomaton): VariableAutomaton {
        return union(other)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is VariableAutomaton) return false
        return variable == other.variable
    }

    override fun toString(): String {
        val singleton = variable.singleton
        return singleton ?: variable.commonPrefix
    }

    fun clone(): VariableAutomaton {
        return VariableAutomaton(variable.clone())
    }

    override fun hashCode(): Int {
        return variable.hashCode()
    }

    companion object {
        const val SEPARATOR_CHAR = '/'
        val SEPARATOR_AUT = Automaton.makeChar(SEPARATOR_CHAR)
        val SEPARATOR_VA = VariableAutomaton(SEPARATOR_AUT)
        val ANY = VariableAutomaton(Automaton.makeAnyString())
        val ANY_PATH = SEPARATOR_VA.join(ANY)
        private val ANY_STRING_AUT = Automaton.makeAnyString()
        fun bottom(): VariableAutomaton {
            return VariableAutomaton("")
        }

        fun top(): VariableAutomaton {
            return VariableAutomaton(SEPARATOR_AUT.concatenate(ANY_STRING_AUT))
        }
    }

    init {
        // Remove double separators if not bottom
        if (variable.isEmpty) this.variable = variable else this.variable = Transducers.removeDoubleSeparators(variable)
    }
}
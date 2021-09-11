package variable

import edu.utsa.fileflow.analysis.Mergeable
import java.util.*


class LiveVariableMap internal constructor() : Cloneable, Mergeable<LiveVariableMap> {
    private val m: HashMap<String, MutableSet<Variable>> = HashMap()

    /**
     * Gets a read-only set of current live variables matching the variable name provided.
     *
     * @param variable The name of the variable to retrieve.
     * @return an unmodifiable Set of the live variables or null if variable is undefined.
     */
    fun getVariable(variable: String): Set<Variable>? {
        val vars = m[variable] ?: return null
        return Collections.unmodifiableSet(vars)
    }

    fun addVariable(variable: Variable) {
        val s = m.getOrPut(variable.name) { HashSet() }
        s.clear()
        s.add(variable)
    }

    override fun merge(other: LiveVariableMap): LiveVariableMap {
        // if is bottom just return
        if (other.m.isEmpty()) return this

        // add (merge) everything in other map to this map
        other.m.forEach { (k: String, set2: Set<Variable>) ->
            // if item is only in other map: just add it to this map
            // if item exists in both: merge the two sets
            m[k]?.addAll(set2) ?: run { m[k] = set2 }
        }
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (other !is LiveVariableMap) return false
        return m == other.m
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("{\n")
        m.forEach {
            sb.append("\t${it.key}: ${it.value}\n")
        }
        sb.append("}")
        return sb.toString()
    }

    public override fun clone(): LiveVariableMap {
        val clone = LiveVariableMap()
        m.forEach { (k: String, v: Set<Variable>?) ->
            val s: MutableSet<Variable> = HashSet()
            s.addAll(v)
            clone.m[k] = s
        }
        return clone
    }

    override fun hashCode(): Int {
        return m.hashCode()
    }

}
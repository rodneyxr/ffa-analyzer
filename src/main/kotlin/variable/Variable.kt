package variable

import java.util.*

/**
 * This class contains information about a variable such as an alias, name
 * and ID that is obtained from a {@link edu.utsa.fileflow.cfg.FlowPoint FlowPoint's} unique ID.
 */
class Variable(val name: String, val id: Int) : Comparable<Variable> {

    // String representation of this variable object
    private val alias = "$name{$id}"

    override operator fun compareTo(other: Variable): Int {
        return alias.compareTo(other.alias)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Variable) return false
        return alias == other.alias
    }

    override fun toString(): String {
        return alias
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + id
        result = 31 * result + alias.hashCode()
        return result
    }
}
package fileflow

import dk.brics.automaton.*
import java.util.function.Consumer

object Transducers {
    private val FST_BASENAME = basename()
    private val FST_PARENT_DIR = parentDir()
    private val FST_REMOVE_DOUBLE_SEP = removeDoubleSeparator()
    private val FST_REMOVE_LAST_SEP = removeLastSeparator()
    fun basename(a: Automaton?): Automaton {
        val result = FST_BASENAME.intersection(a)
        result.acceptStates.forEach(Consumer { s: State -> s.isAccept = s.transitions.isEmpty() })
        return result
    }

    fun parentDir(a: Automaton?): Automaton {
        return FST_PARENT_DIR.intersection(a)
    }

    fun removeDoubleSeparators(a: Automaton?): Automaton {
        return FST_REMOVE_DOUBLE_SEP.intersection(a)
    }

    fun removeLastSeparator(a: Automaton?): Automaton {
        return FST_REMOVE_LAST_SEP.intersection(a)
    }

    fun basename(): FiniteStateTransducer {
        val fst = FiniteStateTransducer()
        val s0 = TransducerState()
        val s1 = TransducerState()
        val s2 = TransducerState()

        // s0 -> s0: accept anything => epsilon
        s0.addEpsilonAcceptAllTransition(s0)

        // s0 -> s1: accept '/' => epsilon
        s0.addTransition(TransducerTransition.createEpsilonTransition('/', '/', s1))

        // s1 -> s1: accept anything minus '/' => identical
        s1.addIdenticalExcludeTransition('/', s1)

        // s1 -> s2: accept '/' => epsilon
        s1.addTransition(TransducerTransition.createEpsilonTransition('/', '/', s2))

        // s2 -> s2: accept '/' => epsilon
        s2.addTransition(TransducerTransition.createEpsilonTransition('/', '/', s2))
        s1.isAccept = true
        s2.isAccept = true
        fst.initialState = s0
        return fst
    }

    fun parentDir(): FiniteStateTransducer {
        // Forward Slash - '/' => '\u002f'
        // Back Slash '\' => '\u005c'
        val fst = FiniteStateTransducer()
        val s0 = TransducerState()
        val s1 = TransducerState()
        val s2 = TransducerState()
        val s3 = TransducerState()

        // s0 -> s0: accept anything
        s0.addIdenticalAcceptAllTransition(s0)

        // s0 -> s1: '/' => epsilon
        s0.addTransition(TransducerTransition('/', '/', s1))

        // s1 -> s2: accept anything minus '/' => epsilon
        s1.addEpsilonExcludeTransition('/', s2)

        // s2 -> s2: accept anything minus '/' => epsilon
        s2.addEpsilonExcludeTransition('/', s2)

        // s2 -> s3: '/' => epsilon
        s2.addTransition(TransducerTransition.createEpsilonTransition('/', '/', s3))
        s2.isAccept = true
        s3.isAccept = true
        fst.initialState = s0
        return fst
    }

    // S0 -> S0: input=All-'/', output=identical
    // S0 -> S1: input='/', output = identical
    // S1 -> S2: input='/', output = empty
    // S1 -> S0: input = All-'/', output = identical
    // S2 -> S0: input = All-'/', output = identical
    fun removeDoubleSeparator(): FiniteStateTransducer {
        val fst = FiniteStateTransducer()
        val s0 = TransducerState()
        val s1 = TransducerState()
        val s2 = TransducerState()

        // s0 -> s0: accept anything minus '/' => identical output
        s0.addIdenticalExcludeTransition('/', s0)

        // s0 -> s1: only accept '/' => identical output
        s0.addTransition(TransducerTransition('/', s1))

        // s1 -> s2: only accept '/' => epsilon
        s1.addTransition(TransducerTransition.createEpsilonTransition('/', '/', s2))

        // s1 -> s0: accept anything minus '/' => identical output
        s1.addIdenticalExcludeTransition('/', s0)

        // s2 -> s0: accept anything minus '/' => identical output
        s2.addIdenticalExcludeTransition('/', s0)
        s0.isAccept = true
        s1.isAccept = true
        s2.isAccept = true
        fst.initialState = s0
        return fst
    }

    fun removeLastSeparator(): FiniteStateTransducer {
        val fst = FiniteStateTransducer()
        val s0 = TransducerState()
        val s1 = TransducerState()
        val s2 = TransducerState()

        // s0 -> s0: accept anything
        s0.addIdenticalAcceptAllTransition(s0)

        // s0 -> s1: accept anything minus '/' => identical
        s0.addIdenticalExcludeTransition('/', s1)

        // s1 -> s1: accept anything minus '/' => identical
        s1.addIdenticalExcludeTransition('/', s1)

        // s1 -> s2: '/' => epsilon
        s1.addTransition(TransducerTransition.createEpsilonTransition('/', '/', s2))
        s1.isAccept = true
        s2.isAccept = true
        fst.initialState = s0
        return fst
    }
}
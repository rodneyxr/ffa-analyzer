package fileflow

import dk.brics.automaton.Automaton
import dk.brics.automaton.RegExp
import edu.utsa.fileflow.utilities.GraphvizGenerator
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class VariableAutomatonTest {
    @BeforeEach
    fun setUp() {
        Automaton.setMinimization(Automaton.MINIMIZE_BRZOZOWSKI)
        Automaton.setMinimizeAlways(true)
    }

    @Test
    fun testCreateVariableAutomaton() {
        val a = VariableAutomaton("/file1")
        GraphvizGenerator.saveDOTToFile(a.automaton.toDot(), "test/create_var_auto.dot")
    }

    @Test
    fun testConcatVariableAutomaton() {
        val v1 = VariableAutomaton("/dir1/")
        val v2 = VariableAutomaton("/file1")
        val v = v1.concatenate(v2)
        GraphvizGenerator.saveDOTToFile(v.automaton.toDot(), "test/concat_var_auto.dot")
    }

    @Test
    fun testEndsWith() {
        val v = VariableAutomaton("dir1")
        // check if v ends with separator
        Assertions.assertFalse(v.endsWith(Automaton.makeChar('/')))
        Assertions.assertTrue(v.endsWith(Automaton.makeChar('1')))
    }

    @Test
    fun testStartsWith() {
        val v = VariableAutomaton("/dir1")
        // check if v starts with separator
        Assertions.assertFalse(v.startsWith(Automaton.makeChar('x')))
        Assertions.assertTrue(v.startsWith(Automaton.makeChar('/')))
    }

    @Test
    fun testGetPathToFileComplex() {
        val reg = RegExp("(/dir[1-9]*/q)|(/df)")
        val va = VariableAutomaton(reg.toAutomaton())
        var a = va.separatedAutomaton
        GraphvizGenerator.saveDOTToFile(a.toDot(), "test/complex.orig.dot")
        Assertions.assertTrue(a.run("/df"))
        a = va.parentDirectory.separatedAutomaton
        GraphvizGenerator.saveDOTToFile(a.toDot(), "test/complex.dot")
        Assertions.assertTrue(a.run("/dir1/"))
        Assertions.assertTrue(a.run("/dir4/"))
        Assertions.assertTrue(a.run("/dir9/"))
        Assertions.assertFalse(a.run("/df"))
    }

    @Test
    fun testGetPathToFileSingleton() {
        val va = VariableAutomaton("/dir1/file1")
        var a = va.separatedAutomaton
        GraphvizGenerator.saveDOTToFile(a.toDot(), "test/singleton.va.orig.dot")
        Assertions.assertTrue(a.run("/dir1/file1"))
        Assertions.assertFalse(a.run("/dir1/file1/"))
        a = va.parentDirectory.separatedAutomaton
        GraphvizGenerator.saveDOTToFile(a.toDot(), "test/singleton.dot")
        Assertions.assertTrue(a.run("/dir1/"))
        Assertions.assertFalse(a.run("/dir1/file1"))
    }

    @Test
    @Throws(Exception::class)
    fun testGetPathToFileInRoot() {
        val va = VariableAutomaton("/test")
        var a = va.separatedAutomaton
        GraphvizGenerator.saveDOTToFile(a.toDot(), "test/file_in_root.orig.dot")
        Assertions.assertTrue(a.run("/test"))
        a = va.parentDirectory.separatedAutomaton
        GraphvizGenerator.saveDOTToFile(a.toDot(), "test/file_in_root.dot")
        Assertions.assertTrue(a.run("/"))
        Assertions.assertFalse(a.run("/test"))
    }

    @Test
    @Throws(FileStructureException::class)
    fun testGetPathToDirectoryInRoot() {
        val va = VariableAutomaton("/home/")
        var a = va.separatedAutomaton
        GraphvizGenerator.saveDOTToFile(a.toDot(), "test/dir_in_root.orig.dot")
        Assertions.assertTrue(a.run("/home/"))
        a = va.parentDirectory.separatedAutomaton
        GraphvizGenerator.saveDOTToFile(a.toDot(), "test/dir_in_root.dot")
        Assertions.assertTrue(a.run("/"))
        Assertions.assertFalse(a.run("/home/"))
    }

    @Test
    fun testTransitionToNull() {
        val a = Automaton.makeChar('/')
        val s0 = a.initialState.transitions.toTypedArray()[0].dest
        Assertions.assertTrue(s0.transitions.isEmpty())
    }

    @Test
    fun testJoinPaths() {
        var v1 = VariableAutomaton("a")
        var v2 = VariableAutomaton("b")
        var ab = v1.join(v2)
        Assertions.assertTrue(ab.isSamePathAs(VariableAutomaton("a/b")))
        v1 = VariableAutomaton("/a/")
        v2 = VariableAutomaton("/b/")
        ab = v1.join(v2)
        Assertions.assertTrue(ab.isSamePathAs(VariableAutomaton("/a/b/")))
        Assertions.assertTrue(ab.isDirectory)
    }
}
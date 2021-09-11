package fileflow

import dk.brics.automaton.Automaton
import edu.utsa.fileflow.utilities.GraphvizGenerator
import fileflow.Transducers.basename
import fileflow.Transducers.parentDir
import fileflow.Transducers.removeDoubleSeparator
import fileflow.Transducers.removeDoubleSeparators
import fileflow.Transducers.removeLastSeparator
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TransducersTest {
    @BeforeEach
    @Throws(Exception::class)
    fun setUp() {
        Automaton.setMinimization(Automaton.MINIMIZE_BRZOZOWSKI)
        Automaton.setMinimizeAlways(true)
    }

    @Test
    fun testCreateFSTParent() {
        val fst = parentDir()
        save(fst, "fst/fst_parent.dot")
    }

    @Test
    fun testCreateFSTRemoveDoubleSeparator() {
        val fst = removeDoubleSeparator()
        save(fst, "fst/fst_rm_double_sep.dot")
    }

    @Test
    fun testCreateFSTRemoveLastSeparator() {
        val fst = removeLastSeparator()
        save(fst, "fst/fst_rm_last_sep.dot")
    }

    @Test
    fun testCreateFSTBasename() {
        val fst = basename()
        save(fst, "fst/fst_basename.dot")
    }

    @Test
    fun testBasename() {
        // test the basename of a file
        var a = Automaton.makeString("/home/user/bashrc")
        save(a, "fst_test/basename_file.orig.dot")
        a = basename(a)
        save(a, "fst_test/basename_file.dot")
        Assertions.assertTrue(a.run("bashrc"))
        Assertions.assertFalse(a.run("/home/user/bashrc"))

        // test the basename of a directory
        a = Automaton.makeString("/home/user/")
        save(a, "fst_test/basename_dir.orig.dot")
        a = basename(a)
        save(a, "fst_test/basename_dir.dot")
        Assertions.assertTrue(a.run("user"))
        Assertions.assertFalse(a.run("user/"))
        Assertions.assertFalse(a.run("/home/user/"))
    }

    @Test
    fun testRemoveDoubleSepEnd() {
        var a = Automaton.makeString("a//")
        save(a, "fst_test/rm_double_sep_end.orig.dot")
        a = removeDoubleSeparators(a)
        save(a, "fst_test/rm_double_sep_end.dot")
    }

    @Test
    fun testRemoveDoubleSep() {
        // Automaton expected = Automaton.makeString("dir1/file1");
        val a1 = Automaton.makeString("dir1/")
        val a2 = Automaton.makeString("/file1")
        var a = a1.concatenate(a2)
        save(a, "fst_test/rm_double_sep.orig.dot")
        a = removeDoubleSeparators(a)
        save(a, "fst_test/rm_double_sep.dot")
    }

    @Test
    fun testRemoveLastSeparatorFST() {
        var a = Automaton.makeString("/a/b/c/")
        Assertions.assertTrue(a.run("/a/b/c/"))
        save(a, "fst_test/rm_last_sep.orig.dot")
        a = removeLastSeparator(a)
        Assertions.assertFalse(a.run("/a/b/c/"))
        Assertions.assertTrue(a.run("/a/b/c"))
        save(a, "fst_test/rm_last_sep.dot")
        a = Automaton.makeString("/a/b/c")
        Assertions.assertTrue(a.run("/a/b/c"))
        a = removeLastSeparator(a)
        Assertions.assertTrue(a.run("/a/b/c"))
        a = Automaton.makeString("/a")
        Assertions.assertTrue(a.run("/a"))
        a = removeLastSeparator(a)
        Assertions.assertTrue(a.run("/a"))
    }

    private fun save(a: Automaton, filepath: String) {
        GraphvizGenerator.saveDOTToFile(a.toDot(), DOT_DIR + filepath)
    }

    companion object {
        private const val DOT_DIR = "test/transducers/"
    }
}
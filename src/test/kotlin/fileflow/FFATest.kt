package fileflow

import edu.utsa.fileflow.analysis.AnalysisException
import edu.utsa.fileflow.utilities.GraphvizGenerator
import fileflow.tools.FFADriver
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable

/**
 * Created by Rodney on 4/26/2017.
 *
 *
 * This class tests various File Flow Analysis scripts.\
 *
 *
 * TODO: Test undefined variables
 */
class FFATest {
    @Test
    @Throws(Exception::class)
    fun testSimpleAnalysis() {
        val driver: FFADriver = FFADriver.run(
            "" +
                    "\$x0 = 'a';" +
                    "touch \$x0;"
        )
        val post: FileStructure = driver.ffaResult!!.post
        GraphvizGenerator.saveDOTToFile(post.files.toDot(), "test/ffa/testSimpleAnalysis.dot")
        Assertions.assertTrue(post.fileExists(VariableAutomaton("a")), "'/a' should exist")
    }

    @Test
    @Throws(Exception::class)
    fun testAnalysisTemp() {
        val driver: FFADriver = FFADriver.run(
            "" +
                    "\$x0 = 'a';" +  // x0 = a
                    "touch \$x0;" // touch a
        )
        val post: FileStructure = driver.ffaResult!!.post
        GraphvizGenerator.saveDOTToFile(post.files.toDot(), "test/ffa/test_temp.dot")
        Assertions.assertTrue(post.fileExists(VariableAutomaton("/a")))
    }

    @Test
    @Throws(Exception::class)
    fun testAnalysis00() {
        val driver: FFADriver = FFADriver.run(
            "" +
                    "\$x0 = 'a';" +  // x0 = a
                    "\$x1 = 'b';" +  // x1 = b
                    "\$x2 = 'c';" +  // x2 = c
                    "\$x3 = \$x0;" +  // x3 = a
                    "\$x3 = \$x1;" +  // x3 = b
                    "if (other) {" +  // BRANCH
                    "   \$x3 = \$x2;" +  // x3 = c
                    "}" +  // MERGE
                    "\$x4 = \$x3;" +  // x4 = c
                    "touch \$x3;" // touch (b | a)
        )
        val post: FileStructure = driver.ffaResult!!.post
        GraphvizGenerator.saveDOTToFile(post.files.toDot(), "test/ffa/test_00.dot")
        Assertions.assertTrue(post.fileExists(VariableAutomaton("/b")))
        Assertions.assertTrue(post.fileExists(VariableAutomaton("/c")))
    }

    @Test
    @Throws(Exception::class)
    fun testAnalysis01() {
        val driver: FFADriver = FFADriver.run(
            "" +
                    "\$x0 = '/';" +  // x0 = /
                    "\$x1 = 'a';" +  // x1 = a
                    "mkdir \$x1;" +  // mkdir a
                    "\$x1 = \$x1.\$x0;" +  // x1 = a/
                    "\$x2 = 'b';" +  // x2 = b
                    "\$x1 = \$x1.\$x2;" +  // x1 = a/b
                    "touch \$x1;" // touch a/b
        )
        val post: FileStructure = driver.ffaResult!!.post
        GraphvizGenerator.saveDOTToFile(post.files.toDot(), "test/ffa/test_01.dot")
        Assertions.assertTrue(post.isDirectory(VariableAutomaton("/a")))
        Assertions.assertTrue(post.isRegularFile(VariableAutomaton("/a/b")))
    }

    /**
     * Cannot touch the same file twice
     */
    @Test
    @Throws(Exception::class)
    fun testAnalysis02() {
        Assertions.assertThrows(AnalysisException::class.java) {
            FFADriver.run(
                "" +
                        "\$x0 = 'a';" +
                        "\$x1 = 'b';" +
                        "while (other) {" +
                        "    \$x0 = \$x1;" +
                        "    touch \$x0;" +
                        "}" +
                        "touch \$x1;"
            )
        }
    }

    @Test
    @Throws(Exception::class)
    fun testAnalysis03() {
        Assertions.assertThrows(AnalysisException::class.java) {
            FFADriver.run(
                "" +
                        "\$x0 = 'a';" +
                        "while (other) {" +
                        "    \$x0 = \$x0.\$x0;" +
                        "    touch \$x0;" +
                        "}"
            )
        }
    }

    @Test
    @Throws(Exception::class)
    fun testAnalysis04() {
        val driver: FFADriver = FFADriver.run(
            "" +
                    "\$x0 = 'a';" +
                    "\$x0 = \$x0.\$x0;" +
                    "touch \$x0;"
        )
        val post: FileStructure = driver.ffaResult!!.post
        GraphvizGenerator.saveDOTToFile(post.files.toDot(), "test/ffa/test_04.dot")
        Assertions.assertTrue(post.isRegularFile(VariableAutomaton("/aa")))
        Assertions.assertFalse(post.fileExists(VariableAutomaton("/a")))
    }

    @Test
    @Throws(Exception::class)
    fun testAnalysis05() {
        val driver: FFADriver = FFADriver.run(
            "" +
                    "touch 'a';" +
                    "rm 'a';"
        )
        val post: FileStructure = driver.ffaResult!!.post
        GraphvizGenerator.saveDOTToFile(post.files.toDot(), "test/ffa/test_05.dot")
        Assertions.assertFalse(post.fileExists(VariableAutomaton("/a")))
    }

    @Test
    @Throws(Exception::class)
    fun testAnalysis06() {
        val driver: FFADriver = FFADriver.run(
            "" +
                    "mkdir 'a';" +
                    "cd 'a';" +
                    "touch 'b';"
        )
        val post: FileStructure = driver.ffaResult!!.post
        GraphvizGenerator.saveDOTToFile(post.files.toDot(), "test/ffa/test_06.dot")
        Assertions.assertTrue(post.isDirectory(VariableAutomaton("/a")))
        Assertions.assertTrue(post.isRegularFile(VariableAutomaton("/a/b")))
    }
}
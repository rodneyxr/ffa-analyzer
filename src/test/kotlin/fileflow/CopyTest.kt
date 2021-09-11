package fileflow

import dk.brics.automaton.Automaton
import dk.brics.automaton.RegExp
import edu.utsa.fileflow.utilities.GraphvizGenerator
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CopyTest {
    var fs: FileStructure? = null

    @BeforeEach
    fun setUp() {
        Automaton.setMinimization(Automaton.MINIMIZE_BRZOZOWSKI)
        Automaton.setMinimizeAlways(true)
        fs = FileStructure()
    }

    @Test
    fun testCopySourceDoesNotExist() {
        Assertions.assertThrows(FileStructureException::class.java) { copy("/fake", "/") }
    }

    @Test
    fun testCopyDestDoesNotExist() {
        Assertions.assertThrows(FileStructureException::class.java) {
            touch("/a")
            copy("/a", "/fakedir/fakefile")
        }
    }

    @Test
    fun testCopySamePath() {
        Assertions.assertThrows(FileStructureException::class.java) {
            touch("/a")
            copy("/a", "/a")
        }
    }

    @Test
    @Throws(FileStructureException::class)
    fun testCopyFileToFile() {
        // test when dest file does NOT exist
        // 'home/user/a' should be created
        mkdir("/tmp/")
        mkdir("/home/user/")
        touch("/tmp/a")
        touch("/tmp/b")
        copy("/tmp/a", "/home/user/a")
        Assertions.assertTrue(exists("/tmp/a"))
        Assertions.assertTrue(fs!!.isRegularFile(VariableAutomaton("/home/user/a")))
        Assertions.assertFalse(fs!!.isDirectory(VariableAutomaton("/home/user/a")))
        Assertions.assertFalse(exists("/home/user/b"))

        // test when dest file DOES exist
        // 'tmp/a' should overwrite 'home/user/a'
        fs = FileStructure()
        mkdir("/tmp/")
        mkdir("/home/user/")
        touch("/tmp/a")
        touch("/tmp/b")
        touch("/home/user/a")
        Assertions.assertTrue(exists("/home/user/a"))
        Assertions.assertTrue(fs!!.isRegularFile(VariableAutomaton("/home/user/a")))
        copy("/tmp/a", "/home/user/a")
        Assertions.assertTrue(exists("/tmp/a"))
        Assertions.assertTrue(fs!!.isRegularFile(VariableAutomaton("/home/user/a")))
        Assertions.assertFalse(fs!!.isDirectory(VariableAutomaton("/home/user/a")))
        Assertions.assertFalse(exists("/home/user/b"))

        // test when both files are in root directory
        fs = FileStructure()
        touch("/a")
        copy("/a", "/b")
        Assertions.assertTrue(exists("/a"))
        Assertions.assertTrue(exists("/b"))

        // test when both files are in root directory without slashes
        fs = FileStructure()
        touch("a")
        copy("a", "b")
        Assertions.assertTrue(exists("/a"))
        Assertions.assertTrue(exists("/b"))
    }

    @Test
    @Throws(FileStructureException::class)
    fun testCopyFileToDir() {
        touch("/a")
        mkdir("/b/")
        copy("/a", "/b")
        Assertions.assertTrue(exists("/a"))
        Assertions.assertTrue(exists("/b/"))
        Assertions.assertTrue(exists("/b/a"))
        Assertions.assertTrue(fs!!.isDirectory(regex("/b")))
        Assertions.assertFalse(fs!!.isRegularFile(regex("/b")))
        Assertions.assertTrue(fs!!.isRegularFile(regex("/b/a")))
        Assertions.assertFalse(fs!!.isDirectory(regex("/b/a")))
    }

    @Test
    @Throws(FileStructureException::class)
    fun testCopyDirToDir() {
        // cp /home/user/ /dir1/dir2/
        mkdir("/dir1/dir2/")
        mkdir("/root/")
        mkdir("/home/user/")
        touch("/home/user/bashrc")
        save(fs!!.files, "/test/fs/copy_files.orig.dot")
        Assertions.assertTrue(exists("/dir1/dir2"))
        Assertions.assertTrue(exists("/root/"))
        Assertions.assertTrue(exists("/home/user/bashrc"))
        copy("/home/user/", "/dir1/dir2")
        save(fs!!.files, "/test/fs/copy_files.dot")
        Assertions.assertTrue(exists("/dir1/dir2/user/"))
        Assertions.assertTrue(fs!!.isDirectory(VariableAutomaton("/dir1/dir2/user/")))
        Assertions.assertFalse(fs!!.isRegularFile(VariableAutomaton("/dir1/dir2/user/")))
        Assertions.assertTrue(exists("/dir1/dir2/user/bashrc"))
        Assertions.assertTrue(fs!!.isRegularFile(VariableAutomaton("/dir1/dir2/user/bashrc")))
        Assertions.assertFalse(fs!!.isDirectory(VariableAutomaton("/dir1/dir2/user/bashrc")))
    }

    @Test
    fun testCopyDirToFile() {
        Assertions.assertThrows(FileStructureException::class.java) {
            mkdir("a")
            touch("b")
            copy("a", "b")
        }
    }

    @Test
    fun testCopyFileToNonExisting() {
        Assertions.assertThrows(FileStructureException::class.java) {
            touch("a")
            copy("a", "fake/")
        }
    }

    @Test
    fun testCopyDirToNonExisting() {
        Assertions.assertThrows(FileStructureException::class.java) {
            mkdir("/a")
            copy("a", "fake/fake")
        }
    }

    @Throws(FileStructureException::class)
    fun touch(fp: String?) {
        fs!!.createFile(regex(fp))
    }

    @Throws(FileStructureException::class)
    fun mkdir(fp: String?) {
        fs!!.createDirectory(regex(fp))
    }

    @Throws(FileStructureException::class)
    fun copy(src: String?, dest: String?) {
        fs!!.copy(VariableAutomaton(src!!), VariableAutomaton(dest!!))
    }

    fun exists(fp: String?): Boolean {
        return fs!!.fileExists(VariableAutomaton(fp!!))
    }

    // returns a variable automaton given a regex
    fun regex(regex: String?): VariableAutomaton {
        return VariableAutomaton(RegExp(regex).toAutomaton())
    }

    fun save(a: Automaton, filepath: String?) {
        GraphvizGenerator.saveDOTToFile(a.toDot(), filepath)
    }
}
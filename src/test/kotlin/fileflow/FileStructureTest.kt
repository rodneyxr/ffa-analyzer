package fileflow

import dk.brics.automaton.Automaton
import dk.brics.automaton.RegExp
import edu.utsa.fileflow.utilities.GraphvizGenerator
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FileStructureTest {
    private var fs: FileStructure? = null

    @BeforeEach
    @Throws(Exception::class)
    fun setUp() {
        Automaton.setMinimization(Automaton.MINIMIZE_BRZOZOWSKI)
        Automaton.setMinimizeAlways(true)
        fs = FileStructure()
    }

    @Test
    @Throws(FileStructureException::class)
    fun testDirectoryExists() {
        fs!!.createDirectory(VariableAutomaton("/home"))
        // '/' exists by default since it is root
        Assertions.assertTrue(fs!!.fileExists(VariableAutomaton("/")), "'/' should exist")
        Assertions.assertTrue(fs!!.fileExists(VariableAutomaton("/home")), "'/home' should exist")
        Assertions.assertFalse(fs!!.fileExists(VariableAutomaton("/fake")), "'/fake' should not exist")
    }

    @Test
    @Throws(FileStructureException::class)
    fun testFileExists() {
        fs!!.createDirectory(VariableAutomaton("/home"))
        fs!!.createFile(VariableAutomaton("/home/file"))

        // '/' exists by default since it is root
        Assertions.assertTrue(fs!!.fileExists(VariableAutomaton("/")), "'/' should exist")
        Assertions.assertTrue(fs!!.fileExists(VariableAutomaton("/home")), "'/home' should exist")
        Assertions.assertTrue(fs!!.fileExists(VariableAutomaton("/home/file")), "'/home/file' should exist")
        Assertions.assertFalse(fs!!.fileExists(VariableAutomaton("/fake")), "'/fake' should not exist")
    }

    @Test
    @Throws(FileStructureException::class)
    fun testCreateFileWhenDirectoryAlreadyExists() {
        // create and assert '/a/'
        fs!!.createDirectory(VariableAutomaton("/a/"))
        Assertions.assertTrue(fs!!.fileExists(VariableAutomaton("/a/")))
        Assertions.assertThrows(FileStructureException::class.java) {
            // attempt to create file at existing directory '/a' (should fail)
            fs!!.createFile(VariableAutomaton("/a"))
        }
    }

    @Test
    @Throws(FileStructureException::class)
    fun testCreateDirectoryWhenFileAlreadyExists() {
        // create and assert '/a'
        fs!!.createFile(VariableAutomaton("/a"))
        Assertions.assertTrue(fs!!.fileExists(VariableAutomaton("/a")))
        Assertions.assertThrows(FileStructureException::class.java) {
            // attempt to create directory at existing file '/a' (should fail)
            fs!!.createDirectory(VariableAutomaton("/a/"))
        }
    }

    @Test
    @Throws(FileStructureException::class)
    fun testIsDirectoryAndIsRegularFile() {
        mkdir("/dir1")
        Assertions.assertTrue(fs!!.isDirectory(VariableAutomaton("/dir1")))
        Assertions.assertTrue(fs!!.isDirectory(VariableAutomaton("/dir1/")))
        Assertions.assertFalse(fs!!.isRegularFile(VariableAutomaton("/dir1")))
        Assertions.assertFalse(fs!!.isRegularFile(VariableAutomaton("/dir1/")))
        Assertions.assertFalse(fs!!.isDirectory(VariableAutomaton("/dir1/blah")))
        Assertions.assertFalse(fs!!.isDirectory(VariableAutomaton("/dir1blah")))
        fs = FileStructure()
        touch("/file1")
        Assertions.assertFalse(fs!!.isDirectory(VariableAutomaton("/file1")))
        Assertions.assertFalse(fs!!.isDirectory(VariableAutomaton("/file1/")))
        Assertions.assertTrue(fs!!.isRegularFile(VariableAutomaton("/file1")))
        Assertions.assertTrue(fs!!.isRegularFile(VariableAutomaton("/file1/")))
        Assertions.assertFalse(fs!!.isRegularFile(VariableAutomaton("/file1blah")))
    }

    @Test
    @Throws(FileStructureException::class)
    fun testRemoveFile() {
        touch("/file1")
        Assertions.assertTrue(exists("/file1"))
        remove("/file1")
        save(fs!!.files, "tmp/result1.dot")
        Assertions.assertFalse(exists("/file1"))
        Assertions.assertTrue(exists("/"))
        fs = FileStructure()
        mkdir("/a")
        touch("/a/b")
        Assertions.assertTrue(exists("/a"))
        remove("/a/b")
        save(fs!!.files, "tmp/result2.dot")
        Assertions.assertFalse(exists("/a/b"))
        Assertions.assertTrue(exists("/a"))
    }

    @Test
    @Throws(FileStructureException::class)
    fun testRemoveDirectory() {
        mkdir("/a/b/c")
        touch("/a/b/c/d")
        touch("/a/b/c/e")
        touch("/a/b/c/f")
        removeRecursive("/a/b/c/")
        save(fs!!.files, "tmp/remove_directory.dot")
        Assertions.assertTrue(exists("/a/b"))
        Assertions.assertFalse(exists("/a/b/c/d"))
        Assertions.assertFalse(exists("/a/b/c"))
    }

    @Test
    @Throws(FileStructureException::class)
    fun testChangeDirectory() {
        mkdir("/a")
        cd("/a")
        touch("b")
        save(fs!!.files, "tmp/change_directory.dot")
        Assertions.assertTrue(fs!!.isDirectory(VariableAutomaton("/")))
        Assertions.assertTrue(fs!!.isDirectory(VariableAutomaton("/a")))
        Assertions.assertTrue(fs!!.isRegularFile(VariableAutomaton("/a/b")))
    }

    @Throws(FileStructureException::class)
    fun cd(fp: String?) {
        fs!!.changeWorkingDirectory(regex(fp))
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

    @Throws(FileStructureException::class)
    fun remove(fp: String?) {
        fs!!.removeFile(regex(fp))
    }

    @Throws(FileStructureException::class)
    fun removeRecursive(fp: String?) {
        fs!!.removeFileRecursive(regex(fp))
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
/**
 * This class represents a file structure using an automaton.
 *
 * @author Rodney Rodriguez
 */
package fileflow

import dk.brics.automaton.*
import edu.utsa.fileflow.analysis.Mergeable
import java.util.function.Consumer

class FileStructure : Cloneable, Mergeable<FileStructure> {
    // automaton representing files in a file structure
    var files: Automaton

    companion object {
        private val SEPARATOR = Automaton.makeChar(VariableAutomaton.SEPARATOR_CHAR)
        private val ANY = Automaton.makeAnyString()
        fun top(): FileStructure {
            val files = SEPARATOR.clone()
            files.concatenate(ANY)
            return FileStructure(files)
        }

        /**
         * @return a clone of the separator automaton.
         */
        fun separator(): Automaton {
            return SEPARATOR.clone()
        }

        /**
         * Cleans a string representing a file path.
         *
         * @param fp The file path to clean.
         * @return a new String with the cleaned file path.
         */
        fun clean(fp: String): String {
            var cleanFp = fp
            cleanFp = cleanFp.trim()
            cleanFp = cleanFp.replace("[/\\\\]+".toRegex(), "/")
            return cleanFp
        }

        init {
            Automaton.setMinimization(Automaton.MINIMIZE_BRZOZOWSKI)
            Automaton.setMinimizeAlways(true)
        }
    }

    private var cwd: VariableAutomaton = VariableAutomaton(VariableAutomaton.SEPARATOR_AUT)

    constructor() {
        files = SEPARATOR.clone()
    }

    private constructor(files: Automaton) {
        this.files = files
    }

    /**
     * Changes the current working directory.
     */
    fun changeWorkingDirectory(fp: VariableAutomaton) {
        if (fp.startsWith(separator())) {
            cwd = fp.clone()
        } else {
            cwd = VariableAutomaton(absolute(fp))
        }
        // Append a slash if fp did not have one
        if (!cwd.endsWith(VariableAutomaton.SEPARATOR_AUT)) {
            cwd = cwd.concatenate(VariableAutomaton.SEPARATOR_VA)
        }
    }

    /**
     * Creates a file or directory at the path provided. This method will not throw
     * an exception. If a file does not exist it will forcefully be created by
     * creating every non-existing file in its path. If the file already exists,
     * then no changes will be made.
     *
     * @param fp The file path to create the file or directory at.
     */
    fun forceCreate(fp: VariableAutomaton) {
        union(fp)
    }

    /**
     * Creates a file at the file path provided. Parent directory must exist for
     * this operation to be successful. `fp` must not represent a
     * directory (include a trailing slash).
     *
     * @param fp The file path to create the file.
     * @throws FileStructureException if the parent directory does not exist
     */
    @Throws(FileStructureException::class)
    fun createFile(fp: VariableAutomaton) {
        if (fp.isDirectory) throw FileStructureException(
            java.lang.String.format(
                "touch: cannot touch '%s**': Cannot touch a directory",
                fp
            )
        )

        // if the parent directory does not exist throw an exception
        if (!fileExists(VariableAutomaton(absolute(fp)).parentDirectory)) throw FileStructureException(
            java.lang.String.format(
                "touch: cannot touch '%s**': No such file or directory",
                fp
            )
        )

        // if the file already exists, throw an exception
        if (fileExists(fp)) throw FileStructureException(
            java.lang.String.format(
                "touch: cannot touch '%s**': File already exists",
                fp
            )
        )
        union(fp)
    }

    /**
     * Creates a directory in the file structure at the path provided. If the
     * path to that directory does not exist, it will be created.
     *
     * @param fp The file path to the directory to be created.
     * @throws FileStructureException if a file already exists at `fp`.
     */
    @Throws(FileStructureException::class)
    fun createDirectory(fp: VariableAutomaton) {
        // if fp does not have a trailing separator, then add one
        var fp1: VariableAutomaton = fp
        if (!fp1.endsWith(SEPARATOR)) fp1 = fp1.concatenate(VariableAutomaton.SEPARATOR_VA)
        val a = VariableAutomaton(absolute(fp1))
        if (fileExists(a))
            throw FileStructureException("mkdir: cannot create directory '${a}**': File exists")
        union(fp1)
    }

    /**
     * Removes a file from the file structure at the path provided. If the path to
     * the file does not exist, an exception will be thrown.
     *
     * @param fp The file path to the file to be removed.
     * @throws FileStructureException if the file path does not exist in the file structure.
     */
    @Throws(FileStructureException::class)
    fun removeFile(fp: VariableAutomaton) {
        if (!fileExists(fp)) {
            throw FileStructureException("rm: cannot remove '${fp}**': No such file or directory")
        }
        if (!isDirectory(fp)) {
            minus(fp)
        } else {
            throw FileStructureException(
                "rm: cannot remove '${fp}**': attempting to remove directory without recursive option"
            )
        }
    }

    /**
     * Removes a file or directory from the file structure at the path provided.
     * If the file is a directory, the directory and all paths under it will
     * be removed. If the path to the file does not exist, an exception will be thrown.
     *
     * @param fp The file path to the file or directory to be removed.
     * @throws FileStructureException if the file path does not exist in the file structure.
     */
    @Throws(FileStructureException::class)
    fun removeFileRecursive(fp: VariableAutomaton) {
        if (!fileExists(fp)) {
            throw FileStructureException("rm: cannot remove '${fp}**': No such file or directory")
        }
        if (isDirectory(fp)) {
            // if dir, minus(fp) & minus(fp/*)
            minus(fp.join(VariableAutomaton.ANY_PATH))
        } else {
            minus(fp.union(fp.join(VariableAutomaton.ANY_PATH)))
        }
    }

    /**
     * - Copies a file to another location in the file structure. Consider the
     * following cases:
     * - file1 to file2: If file2 exists it will be overwritten; else it will be
     * created
     * - file1 to dir2: file1 will be copied into dir2 overwriting file1 in dir2
     * if it exists
     * - dir1 to dir2: a copy of dir1 will be created in dir2. if dir2/dir1
     * happens to exist, contents will be merged overwriting the existing
     * - dir1 to file2: cp: cannot overwrite non-directory 'file2' with directory
     * 'dir1'
     * - file1 to non-existing: cp: cannot create regular file
     * 'non-existing/file1': No such file or directory
     * - dir1 to non-existing: cp: cannot create directory 'non-existing/dir1': No
     * such file or directory
     *
     * @param source      The path pointing to the source file to copy
     * @param destination The path pointing to the destination location
     * @throws FileStructureException
     */
    @Throws(FileStructureException::class)
    fun copy(source: VariableAutomaton, destination: VariableAutomaton) {
        val destExists = fileExists(destination)
        var destination1: VariableAutomaton = destination

        // check if source exists
        if (!fileExists(source)) throw FileStructureException(
            "cp: cannot stat '${source}**': No such file or directory"
        )

        // check if the paths point to the same file
        if (source.isSamePathAs(destination)) throw FileStructureException(
            "cp: '${source}**' and '${source}**' are the same file",
        )

        // cache some booleans
        val destIsDir = isDirectory(destination)
        val destIsReg = isRegularFile(destination)
        val srcIsDir = isDirectory(source)
        val srcIsReg = isRegularFile(source)

        // make sure either the destination or parent to destination exists
        if (!destExists) {
            if (srcIsReg && destination.isDirectory) throw FileStructureException(
                "cp: cannot create regular file '${destination}**': No such file or directory"
            )
            val destParent: VariableAutomaton = destination.parentDirectory
            if (!fileExists(destParent)) {
                if (srcIsReg) {
                    throw FileStructureException(
                        "cp: cannot create file '${destination}**': No such file or directory"
                    )
                } else {
                    throw FileStructureException(
                        "cp: cannot create directory '${destination}**': No such file or directory"
                    )
                }
            }
            // if destination does not exist then change it to the parent
            destination1 = destParent
        }
        if (destIsDir) {
            // if dest is a directory, dest must end with a slash
            destination1 = destination.concatenate(VariableAutomaton.SEPARATOR_VA)
        } else if (srcIsDir && destIsReg) {
            throw FileStructureException(
                "cp: cannot overwrite non-directory '${destination}**' with directory '${source}**'"
            )
        }
        var src = absolute(source)
        val dst = absolute(destination1)
        var a: Automaton
        if (!destExists) {
            // if dest does not exist then dest base name should be created
            a = Transducers.basename(absolute(destination))
        } else {
            // get all files to be copied (absolute paths)
            a = files.intersection(src.concatenate(ANY))
            if (srcIsReg) src = absolute(source.parentDirectory)
            var src1: Automaton = Transducers.removeLastSeparator(src).concatenate(SEPARATOR)
            if (src1.isEmpty) src1 = src
            src1 = src1.concatenate(ANY)

            // we need a FST to replace src prefix with empty
            val replace = FiniteStateTransducer.AutomatonToTransducer(src1)
            replace.acceptStates.forEach(Consumer { s: State ->
                s.transitions.forEach(
                    Consumer { t: Transition -> (t as TransducerTransition).isIdentical = true })
            })
            a = replace.intersection(a)
            // if source was a directory then initial state will be true
            // but we need it to be false
            a.initialState.isAccept = false

            // after this we are left with everything after source in a
            // we need to prepend the base name to the result
            // only prepend the base name if source is a directory
            if (srcIsDir) {
                val basename: Automaton = Transducers.basename(src)
                a = basename.concatenate(SEPARATOR).concatenate(a)
            }
        }

        // insert the source files to the file structure
        val insert = VariableAutomaton(dst.concatenate(a))
        files = files.union(insert.separatedAutomaton)
    }

    /**
     * Determines whether a file exists. It does not matter if it is a directory
     * or regular file or possibly both.
     *
     * @param fp The file path of the file to check if it exists.
     * @return true if the file exists; false otherwise.
     */
    fun fileExists(fp: VariableAutomaton): Boolean {
        var fp1 = fp
        fp1 = VariableAutomaton(absolute(fp1))
        // try as a regular file
        // should not return true if fp is empty
        if (fp1.automaton.isEmpty) return false
        fp1 = fp1.removeLastSeparator()
        if (fp1.subsetOf(files)) return true

        // try as a directory
        fp1 = fp1.concatenate(VariableAutomaton(SEPARATOR))
        return fp1.subsetOf(files)
    }

    /**
     * Tells if a file path is a directory in the file structure.
     *
     * @param fp The file path to check if a directory exists at.
     * @return True if a directory exists at `fp`; false otherwise.
     */
    fun isDirectory(fp: VariableAutomaton): Boolean {
        var fp1 = fp
        fp1 = VariableAutomaton(absolute(fp1))
        fp1 = fp1.concatenate(VariableAutomaton.SEPARATOR_VA)
        return fp1.subsetOf(files)
    }

    /**
     * Tells if a file path is a regular file in the file structure.
     *
     * @param fp The file path to check if a regular file exists at.
     * @return True if a regular file exists at `fp`; false
     * otherwise.
     */
    fun isRegularFile(fp: VariableAutomaton): Boolean {
        var fp1 = fp
        fp1 = VariableAutomaton(absolute(fp1))
        fp1 = fp1.removeLastSeparator()
        return fp1.subsetOf(files)
    }

    /**
     * Graphviz DOT representation of this file structure.
     *
     * @return a Graphviz DOT representation of the files automaton.
     */
    fun toDot(): String {
        return files.toDot()
    }

    /**
     * Prepends the current working directory to the file path variable given.
     *
     * @param fp The file path to be appended to the current working directory.
     * @return the absolute file path as an automaton.
     */
    private fun absolute(fp: VariableAutomaton): Automaton {
        return if (fp.startsWith(VariableAutomaton.SEPARATOR_AUT)) {
            fp.automaton
        } else cwd.concatenate(fp).automaton
    }

    /**
     * Performs a union operation on `files`. `fp` is
     * converted to an absolute path before the union.
     *
     * @param fp The variable automaton to union with `files`.
     */
    private fun union(fp: VariableAutomaton) {
        files = files.union(VariableAutomaton(absolute(fp)).separatedAutomaton)
    }

    /**
     * Performs an intersection operation on `files`. `fp` is
     * converted to an absolute path before the intersection.
     *
     * @param fp The variable automaton to intersect with `files`.
     */
    private fun intersect(fp: VariableAutomaton) {
        files = files.intersection(VariableAutomaton(absolute(fp)).separatedAutomaton)
    }

    /**
     * Performs a minus operation on `files`. `fp` is
     * converted to an absolute path before the minus operation.
     *
     * @param fp The variable automaton to minus from `files`.
     */
    private operator fun minus(fp: VariableAutomaton) {
        files = files.minus(VariableAutomaton(absolute(fp)).automaton)
    }

    override fun merge(other: FileStructure): FileStructure {
        files = files.union(other.files)
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (other !is FileStructure) return false
        return files == other.files
    }

    public override fun clone(): FileStructure {
        val clone = FileStructure()
        clone.files = files.clone()
        clone.cwd = cwd.clone()
        return clone
    }

    override fun hashCode(): Int {
        var result = files.hashCode()
        result = 31 * result + cwd.hashCode()
        return result
    }

}
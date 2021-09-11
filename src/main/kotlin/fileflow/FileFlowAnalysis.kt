package fileflow

import edu.utsa.fileflow.analysis.Analysis
import edu.utsa.fileflow.analysis.AnalysisException
import edu.utsa.fileflow.analysis.Analyzer
import edu.utsa.fileflow.antlr.FileFlowParser.FunctionCallContext
import edu.utsa.fileflow.cfg.FlowPoint
import edu.utsa.fileflow.cfg.FlowPointContext
import edu.utsa.fileflow.utilities.AssignContext
import edu.utsa.fileflow.utilities.GraphvizGenerator
import grammar.GrammarAnalysisDomain
import variable.VariableAnalysisDomain

class FileFlowAnalysis : Analysis<FileFlowAnalysisDomain> {
    // Previous analysis domains
    private var gDomain: GrammarAnalysisDomain? = null
    private var vDomain: VariableAnalysisDomain? = null

    // Initial file structure
    private var lastInit: FileStructure? = null
    private var runCounter = 0

    constructor() : super()
    constructor(precondition: FileStructure?) : super() {
        lastInit = precondition
        runCounter = 1
    }

    @Throws(AnalysisException::class)
    override fun onBegin(domain: FileFlowAnalysisDomain, flowPoint: FlowPoint): FileFlowAnalysisDomain {
        println("\n***** Run: $runCounter")
        if (runCounter > 0) {
            domain.post = lastInit!!
            flowPoint.setDomain(domain)
        }
        return super.onBegin(domain, flowPoint)
    }

    @Throws(AnalysisException::class)
    override fun onFinish(domain: FileFlowAnalysisDomain): FileFlowAnalysisDomain {
        // Write post and init to DOT file
        GraphvizGenerator.saveDOTToFile(domain.init.toDot(), "init$runCounter.dot")
        GraphvizGenerator.saveDOTToFile(domain.post.toDot(), "post$runCounter.dot")
        lastInit = domain.init.clone()
        runCounter++
        return super.onFinish(domain)
    }

    @Throws(AnalysisException::class)
    override fun onBefore(domain: FileFlowAnalysisDomain, context: FlowPointContext): FileFlowAnalysisDomain {
        gDomain = context.flowPoint.getDomain(GrammarAnalysisDomain::class.java) as GrammarAnalysisDomain
        vDomain = context.flowPoint.getDomain(VariableAnalysisDomain::class.java) as VariableAnalysisDomain
        return super.onBefore(domain, context)
    }

    @Throws(AnalysisException::class)
    override fun touch(domain: FileFlowAnalysisDomain, context: FlowPointContext): FileFlowAnalysisDomain {
        val va = getValue(domain, context, 0)

        // add the automaton to the file structure
        try {
            domain.post.createFile(va!!)
        } catch (e: FileStructureException) {
            if (Analyzer.CONTINUE_ON_ERROR && runCounter == 0) {
                if (e.message!!.contains("No such file or directory")) {
                    domain.init.forceCreate(va!!.parentDirectory)
                }
            }
            throw AnalysisException(e.message)
        }
        return domain
    }

    @Throws(AnalysisException::class)
    override fun mkdir(domain: FileFlowAnalysisDomain, context: FlowPointContext): FileFlowAnalysisDomain {
        val va = getValue(domain, context, 0)

        // add the automaton to the file structure
        try {
            domain.post.createDirectory(va!!)
        } catch (e: FileStructureException) {
            if (Analyzer.CONTINUE_ON_ERROR && runCounter == 0) {
                domain.init.forceCreate(va!!)
                println(e.message)
            }
            throw AnalysisException(e.message)
        }
        return domain
    }

    @Throws(AnalysisException::class)
    override fun copy(domain: FileFlowAnalysisDomain, context: FlowPointContext): FileFlowAnalysisDomain {
        val v1 = getValue(domain, context, 0)
        val v2 = getValue(domain, context, 1)
        try {
            domain.post.copy(v1!!, v2!!)
        } catch (e: FileStructureException) {
            if (Analyzer.CONTINUE_ON_ERROR && runCounter == 0) {
                // Create v1 or v2 or both?
                if (!domain.post.fileExists(v1!!)) domain.init.forceCreate(v1)
                if (!domain.post.fileExists(v2!!)) domain.init.forceCreate(v2)
            }
            throw AnalysisException(e.message)
        }
        return domain
    }

    @Throws(AnalysisException::class)
    override fun remove(domain: FileFlowAnalysisDomain, context: FlowPointContext): FileFlowAnalysisDomain {
        val va = getValue(domain, context, 0)

        // remove the automaton from the file structure
        try {
            domain.post.removeFile(va!!)
        } catch (e: FileStructureException) {
            if (Analyzer.CONTINUE_ON_ERROR && runCounter == 0) {
                if (!e.message!!.endsWith("recursive option")) {
                    domain.init.forceCreate(va!!)
                }
            }
            throw AnalysisException(e.message)
        }
        return domain
    }

    @Throws(AnalysisException::class)
    override fun removeRecursive(domain: FileFlowAnalysisDomain, context: FlowPointContext): FileFlowAnalysisDomain {
        val va = getValue(domain, context, 0)

        // remove the automaton from the file structure
        try {
            domain.post.removeFileRecursive(va!!)
        } catch (e: FileStructureException) {
            if (Analyzer.CONTINUE_ON_ERROR && runCounter == 0) {
                domain.init.forceCreate(va!!)
            }
            throw AnalysisException(e.message)
        }
        return domain
    }

    @Throws(AnalysisException::class)
    override fun changeDirectory(domain: FileFlowAnalysisDomain, context: FlowPointContext): FileFlowAnalysisDomain {
        val va = getValue(domain, context, 0)
        domain.init.changeWorkingDirectory(va!!)
        domain.post.changeWorkingDirectory(va)
        return domain
    }

    @Throws(AnalysisException::class)
    override fun assertFunc(domain: FileFlowAnalysisDomain, context: FlowPointContext): FileFlowAnalysisDomain {
        val ctx = context.context as FunctionCallContext
        var s1: String? = null
        var s2: String? = null
        if (ctx.condition().children.size == 2) {
            s1 = ctx.condition().getChild(0).text
            s2 = ctx.condition().getChild(1).text
        }
        if (s1 == null || s2 == null) {
            System.err.println("Invalid command")
            return domain
        }
        if (s1 == "exists") {
            var va = domain.table[s2]
            if (va == null) {
                va = VariableAutomaton(s2)
            }
            val exists = domain.post.fileExists(va)
            if (!exists) {
                System.out.printf("WARNING: '%s**' does not exist\n", va)
            }
        } else if (s1 == "!" && s2.startsWith("exists")) {
            val b = ctx.condition().getChild(1).getChild(1).text
            val va = domain.table[b]
            val exists = domain.post.fileExists(va!!)
            if (exists) {
                System.out.printf("WARNING: '%s**' exists\n", va)
            }
        } else {
            System.err.println("Invalid assertion.")
            return domain
        }
        return domain
    }

    @Throws(AnalysisException::class)
    override fun enterAssignment(domain: FileFlowAnalysisDomain, context: FlowPointContext): FileFlowAnalysisDomain {
        val ctx = AssignContext(context)
        domain.table[ctx.var0] = VariableAutomaton(gDomain!!.getVariable(ctx.var0, vDomain!!.liveVariables)!!)
        // TODO: handle arrays and user INPUT
        return super.enterAssignment(domain, context)
    }

    /**
     * Gets the nth parameter of a function call.
     */
    private fun getValue(domain: FileFlowAnalysisDomain, context: FlowPointContext, n: Int): VariableAutomaton? {
        val ctx = context.context as FunctionCallContext
        val v = ctx.value(n)

        // get the variable from the symbol table or create a new one
        return if (v.Variable() != null) domain.table[v.Variable().text] else VariableAutomaton(v.String().text)

        // v is a string literal
    }
}
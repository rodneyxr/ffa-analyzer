package fileflow

import edu.utsa.fileflow.analysis.AnalysisDomain

class FileFlowAnalysisDomain : AnalysisDomain<FileFlowAnalysisDomain> {
    var table = SymbolTable()
    var init = FileStructure()
    var post = FileStructure()

    constructor() : super()
    constructor(init: FileStructure) : super() {
        this.init = init
    }

    override fun merge(domain: FileFlowAnalysisDomain): FileFlowAnalysisDomain {
        table = table.merge(domain.table) as SymbolTable
        post = post.merge(domain.post)
        init = init.merge(domain.init)
        return this
    }

    override fun top(): FileFlowAnalysisDomain {
        val top = FileFlowAnalysisDomain()
        top.post = FileStructure.top()
        return top
    }

    override fun bottom(): FileFlowAnalysisDomain {
        val bottom = FileFlowAnalysisDomain()
        bottom.post = FileStructure()
        return bottom
    }

    override fun compareTo(other: FileFlowAnalysisDomain): Int {
        if (table != other.table) return 1
        return if (post != other.post) 1 else 0
    }

    override fun clone(): FileFlowAnalysisDomain {
        val clone = FileFlowAnalysisDomain()
        clone.post = post.clone()
        clone.init = init.clone()
        table.forEach { k: String, v: VariableAutomaton -> clone.table[k] = v.clone() }
        return clone
    }
}
package kalkulierbar.logic

import kalkulierbar.clause.Atom
import kalkulierbar.clause.Clause
import kalkulierbar.clause.ClauseSet

class Var(var spelling: String) : PropositionalLogicNode() {

    /**
     * Translates arbitrary formulae into equivalent representations
     * using only basic operations (var, not, and, or)
     * @return representation of this LogicNode using only basic logic operations
     */
    override fun toBasicOps() = this

    /**
     * Recursively applies Tseytin transformation on a logic node
     * Adds generated CNF snippets to a given ClauseSet
     * CNF snippets based on http://gauss.ececs.uc.edu/Courses/c626/lectures/BDD/st.pdf
     * @param cs ClauseSet to add generated clauses to
     * @param index ID of the current node in the logic tree (pre-order numbering)
     * @return ID of the next logic tree node (pre-order numbering)
     */
    override fun tseytin(cs: ClauseSet, index: Int) = index + 1

    override fun getTseytinName(index: Int) = "var$spelling"

    /**
     * Translates an arbitrary fomula into an equivalent ClauseSet using naive conversion to CNF
     * Algorithm adapted from https://www.cs.jhu.edu/~jason/tutorials/convert-to-CNF.html
     * @return ClauseSet equivalent to this logic node
     */
    override fun naiveCNF(): ClauseSet {
        val atom = Atom(spelling, false)
        val clause = Clause(mutableListOf(atom))
        return ClauseSet(mutableListOf(clause))
    }

    override fun toString() = spelling
}

class Not(child: PropositionalLogicNode) : UnaryOp(child) {

    /**
     * Recursively applies Tseytin transformation on a logic node
     * Adds generated CNF snippets to a given ClauseSet
     * CNF snippets based on http://gauss.ececs.uc.edu/Courses/c626/lectures/BDD/st.pdf
     * @param cs ClauseSet to add generated clauses to
     * @param index ID of the current node in the logic tree (pre-order numbering)
     * @return ID of the next logic tree node (pre-order numbering)
     */
    override fun tseytin(cs: ClauseSet, index: Int): Int {
        var i = index
        val selfVar = getTseytinName(i)
        i += 1
        val childVar = child.getTseytinName(i)
        i = child.tseytin(cs, i)
        val clauseA = Clause(mutableListOf(Atom(childVar, true), Atom(selfVar, true)))
        val clauseB = Clause(mutableListOf(Atom(childVar, false), Atom(selfVar, false)))

        cs.addAll(listOf(clauseA, clauseB))
        return i
    }

    override fun getTseytinName(index: Int) = "not$index"

    /**
     * Translates an arbitrary fomula into an equivalent ClauseSet using naive conversion to CNF
     * Algorithm adapted from https://www.cs.jhu.edu/~jason/tutorials/convert-to-CNF.html
     * @return ClauseSet equivalent to this logic node
     */
    override fun naiveCNF(): ClauseSet {
        val res: ClauseSet

        // Perform Negation-Pushdown
        when (child) {
            is Not -> {
                val childNot = child as Not
                // Eliminate double negation
                res = childNot.child.naiveCNF()
            }
            is Or -> {
                val childOr = child as Or
                // De-Morgan Or
                res = And(Not(childOr.leftChild), Not(childOr.rightChild)).naiveCNF()
            }
            is And -> {
                val childAnd = child as And
                // De-Morgan And
                res = Or(Not(childAnd.leftChild), Not(childAnd.rightChild)).naiveCNF()
            }
            is Impl -> {
                val childImpl = child as Impl
                // !(a->b) = !(!a v b) = a^!b
                res = And(childImpl.leftChild, Not(childImpl.rightChild)).naiveCNF()
            }
            is Equiv -> {
                val childEquiv = child as Equiv
                val implA = Impl(childEquiv.leftChild, childEquiv.rightChild)
                val implB = Impl(childEquiv.rightChild, childEquiv.leftChild)
                // Translate equivalence into implications
                res = Not(And(implA, implB)).naiveCNF()
            }
            is Var -> {
                val childVar = child as Var
                val atom = Atom(childVar.spelling, true)
                val clause = Clause(mutableListOf(atom))
                return ClauseSet(mutableListOf(clause))
            }
            else -> throw Exception("Unknown PropositionalLogicNode encountered during naive CNF transformation")
        }

        return res
    }

    override fun toString() = "!$child"
}

class And(leftChild: PropositionalLogicNode, rightChild: PropositionalLogicNode) : BinaryOp(leftChild, rightChild) {

    /**
     * Recursively applies Tseytin transformation on a logic node
     * Adds generated CNF snippets to a given ClauseSet
     * CNF snippets based on http://gauss.ececs.uc.edu/Courses/c626/lectures/BDD/st.pdf
     * @param cs ClauseSet to add generated clauses to
     * @param index ID of the current node in the logic tree (pre-order numbering)
     * @return ID of the next logic tree node (pre-order numbering)
     */
    override fun tseytin(cs: ClauseSet, index: Int): Int {
        var i = index
        val selfVar = getTseytinName(i)
        i += 1
        val leftChildVar = leftChild.getTseytinName(i)
        i = leftChild.tseytin(cs, i)
        val rightChildVar = rightChild.getTseytinName(i)
        i = rightChild.tseytin(cs, i)

        val clauseA = Clause(mutableListOf(Atom(leftChildVar, false), Atom(selfVar, true)))
        val clauseB = Clause(mutableListOf(Atom(rightChildVar, false), Atom(selfVar, true)))
        val clauseC = Clause(mutableListOf(Atom(leftChildVar, true), Atom(rightChildVar, true), Atom(selfVar, false)))
        cs.addAll(listOf(clauseA, clauseB, clauseC))

        return i
    }

    override fun getTseytinName(index: Int) = "and$index"

    /**
     * Translates an arbitrary fomula into an equivalent ClauseSet using naive conversion to CNF
     * Algorithm adapted from https://www.cs.jhu.edu/~jason/tutorials/convert-to-CNF.html
     * @return ClauseSet equivalent to this logic node
     */
    override fun naiveCNF(): ClauseSet {
        val leftCS = leftChild.naiveCNF()
        val rightCS = rightChild.naiveCNF()
        leftCS.unite(rightCS)
        return leftCS
    }

    override fun toString() = "($leftChild ∧ $rightChild)"
}

class Or(leftChild: PropositionalLogicNode, rightChild: PropositionalLogicNode) : BinaryOp(leftChild, rightChild) {

    /**
     * Recursively applies Tseytin transformation on a logic node
     * Adds generated CNF snippets to a given ClauseSet
     * CNF snippets based on http://gauss.ececs.uc.edu/Courses/c626/lectures/BDD/st.pdf
     * @param cs ClauseSet to add generated clauses to
     * @param index ID of the current node in the logic tree (pre-order numbering)
     * @return ID of the next logic tree node (pre-order numbering)
     */
    override fun tseytin(cs: ClauseSet, index: Int): Int {
        var i = index
        val selfVar = getTseytinName(i)
        i += 1
        val leftChildVar = leftChild.getTseytinName(i)
        i = leftChild.tseytin(cs, i)
        val rightChildVar = rightChild.getTseytinName(i)
        i = rightChild.tseytin(cs, i)

        val clauseA = Clause(mutableListOf(Atom(leftChildVar, true), Atom(selfVar, false)))
        val clauseB = Clause(mutableListOf(Atom(rightChildVar, true), Atom(selfVar, false)))
        val clauseC = Clause(mutableListOf(Atom(leftChildVar, false), Atom(rightChildVar, false), Atom(selfVar, true)))
        cs.addAll(listOf(clauseA, clauseB, clauseC))

        return i
    }

    override fun getTseytinName(index: Int) = "or$index"

    /**
     * Translates an arbitrary fomula into an equivalent ClauseSet using naive conversion to CNF
     * Algorithm adapted from https://www.cs.jhu.edu/~jason/tutorials/convert-to-CNF.html
     * @return ClauseSet equivalent to this logic node
     */
    override fun naiveCNF(): ClauseSet {
        val leftClauses = leftChild.naiveCNF().clauses
        val rightClauses = rightChild.naiveCNF().clauses
        val cs = ClauseSet()

        for (lc in leftClauses) {
            for (rc in rightClauses) {
                val atoms = mutableListOf<Atom>()
                atoms.addAll(lc.atoms)
                atoms.addAll(rc.atoms)
                val clause = Clause(atoms)
                cs.add(clause)
            }
        }

        return cs
    }

    override fun toString() = "($leftChild ∨ $rightChild)"
}

class Impl(leftChild: PropositionalLogicNode, rightChild: PropositionalLogicNode) : BinaryOp(leftChild, rightChild) {

    /**
     * Translates arbitrary formulae into equivalent representations
     * using only basic operations (var, not, and, or)
     * @return representation of this LogicNode using only basic logic operations
     */
    override fun toBasicOps(): PropositionalLogicNode {
        leftChild = leftChild.toBasicOps()
        rightChild = rightChild.toBasicOps()
        return Or(Not(leftChild), rightChild)
    }

    /**
     * Recursively applies Tseytin transformation on a logic node
     * Adds generated CNF snippets to a given ClauseSet
     * CNF snippets based on http://gauss.ececs.uc.edu/Courses/c626/lectures/BDD/st.pdf
     * @param cs ClauseSet to add generated clauses to
     * @param index ID of the current node in the logic tree (pre-order numbering)
     * @return ID of the next logic tree node (pre-order numbering)
     */
    override fun tseytin(cs: ClauseSet, index: Int): Int {
        var i = index
        val selfVar = getTseytinName(i)
        i += 1
        val leftChildVar = leftChild.getTseytinName(i)
        i = leftChild.tseytin(cs, i)
        val rightChildVar = rightChild.getTseytinName(i)
        i = rightChild.tseytin(cs, i)

        val clauseA = Clause(mutableListOf(Atom(leftChildVar, false), Atom(selfVar, false)))
        val clauseB = Clause(mutableListOf(Atom(rightChildVar, true), Atom(selfVar, false)))
        val clauseC = Clause(mutableListOf(Atom(leftChildVar, true), Atom(rightChildVar, false), Atom(selfVar, true)))
        cs.addAll(listOf(clauseA, clauseB, clauseC))

        return i
    }

    override fun getTseytinName(index: Int) = "impl$index"

    /**
     * Translates an arbitrary fomula into an equivalent ClauseSet using naive conversion to CNF
     * Algorithm adapted from https://www.cs.jhu.edu/~jason/tutorials/convert-to-CNF.html
     * @return ClauseSet equivalent to this logic node
     */
    override fun naiveCNF() = this.toBasicOps().naiveCNF()

    override fun toString() = "($leftChild --> $rightChild)"
}

class Equiv(leftChild: PropositionalLogicNode, rightChild: PropositionalLogicNode) : BinaryOp(leftChild, rightChild) {

    /**
     * Translates arbitrary formulae into equivalent representations
     * using only basic operations (var, not, and, or)
     * @return representation of this LogicNode using only basic logic operations
     */
    override fun toBasicOps(): PropositionalLogicNode {
        leftChild = leftChild.toBasicOps()
        rightChild = rightChild.toBasicOps()
        return Or(And(leftChild, rightChild), And(Not(leftChild), Not(rightChild)))
    }

    /**
     * Recursively applies Tseytin transformation on a logic node
     * Adds generated CNF snippets to a given ClauseSet
     * CNF snippets based on http://gauss.ececs.uc.edu/Courses/c626/lectures/BDD/st.pdf
     * @param cs ClauseSet to add generated clauses to
     * @param index ID of the current node in the logic tree (pre-order numbering)
     * @return ID of the next logic tree node (pre-order numbering)
     */
    override fun tseytin(cs: ClauseSet, index: Int): Int {
        var i = index
        val selfVar = getTseytinName(i)
        i += 1
        val leftChildVar = leftChild.getTseytinName(i)
        i = leftChild.tseytin(cs, i)
        val rightChildVar = rightChild.getTseytinName(i)
        i = rightChild.tseytin(cs, i)

        val clauseA = Clause(mutableListOf(Atom(leftChildVar, false), Atom(rightChildVar, true), Atom(selfVar, true)))
        val clauseB = Clause(mutableListOf(Atom(leftChildVar, true), Atom(rightChildVar, false), Atom(selfVar, true)))
        val clauseC = Clause(mutableListOf(Atom(leftChildVar, true), Atom(rightChildVar, true), Atom(selfVar, false)))
        val clauseD = Clause(mutableListOf(Atom(leftChildVar, false), Atom(rightChildVar, false), Atom(selfVar, false)))
        cs.addAll(listOf(clauseA, clauseB, clauseC, clauseD))

        return i
    }

    override fun getTseytinName(index: Int) = "equiv$index"

    /**
     * Translates an arbitrary fomula into an equivalent ClauseSet using naive conversion to CNF
     * Algorithm adapted from https://www.cs.jhu.edu/~jason/tutorials/convert-to-CNF.html
     * @return ClauseSet equivalent to this logic node
     */
    override fun naiveCNF() = this.toBasicOps().naiveCNF()

    override fun toString() = "($leftChild <=> $rightChild)"
}

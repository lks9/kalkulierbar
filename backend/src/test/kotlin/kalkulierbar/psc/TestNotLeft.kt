package kalkulierbar.psc

import kalkulierbar.IllegalMove
import kalkulierbar.logic.Var
import kalkulierbar.parsers.PropositionalParser
import kalkulierbar.sequent.NotLeft
import kalkulierbar.sequent.NotRight
import kalkulierbar.sequent.psc.PSC
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TestNotLeft {
    val instance = PSC()
    val parser = PropositionalParser()

    @Test
    fun testBasic() {
        var state = instance.parseFormulaToState("!(!a)", null)
        println(state.getHash())
        state = instance.applyMoveOnState(state, NotRight(0, 0))
        println(state.getHash())
        state = instance.applyMoveOnState(state, NotLeft(1, 0))
        println(state.getHash())
        val formula1 = parser.parse("a ")
        val node1 = state.tree[state.tree.size - 1]

        assertTrue(node1.rightFormulas[0].synEq(formula1))

        assertTrue(node1.rightFormulas[0] is Var)
    }

    @Test
    fun testParent() {
        var state = instance.parseFormulaToState("!(!a) ", null)
        assertTrue(state.tree[0].parent == null)

        state = instance.applyMoveOnState(state, NotRight(0, 0))
        state = instance.applyMoveOnState(state, NotLeft(1, 0))

        assertTrue(state.tree[0].children.size == 1)
        assertEquals(state.tree[1].parent, 0)
    }

    @Test
    fun testWrongNode() {
        val state = instance.parseFormulaToState("a & !a", null)

        assertFailsWith<IllegalMove> {
            instance.applyMoveOnState(state, NotLeft(0, 0))
        }
    }
}

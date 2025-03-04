package kalkulierbar.sequent.fosc

import kalkulierbar.IllegalMove
import kalkulierbar.logic.Constant
import kalkulierbar.logic.ExistentialQuantifier
import kalkulierbar.logic.LogicNode
import kalkulierbar.logic.UniversalQuantifier
import kalkulierbar.logic.transform.IdentifierCollector
import kalkulierbar.logic.transform.LogicNodeVariableInstantiator
import kalkulierbar.parsers.Tokenizer
import kalkulierbar.sequent.*

/**
 * Rule AllLeft is applied, if the LogicNode is the leftChild of node and is of type All(UniversalQuantifier).
 * It replaces the UniversalQuantifier with the swapvariable
 * @param state: FOSCState state to apply move on
 * @param nodeID: ID of node to apply move on
 * @param listIndex: Index of the formula(logicNode) to which move should be applied.
 * @param varAssign: Map of swapvariable used.
 * @return new state after applying move
 */
fun applyAllLeft(state: FOSCState, nodeID: Int, listIndex: Int, varAssign: Map<String, String>): FOSCState {
    checkLeft(state, nodeID, listIndex)

    val node = state.tree[nodeID]
    val formula: LogicNode = node.leftFormulas[listIndex]

    if (formula !is UniversalQuantifier)
        throw IllegalMove("Rule allLeft can only be applied on a universal quantifier")

    // No need to check if swapVariable is already in use for rule allLeft

    var replaceWithString = varAssign[formula.varName]

    // When swapVariable is not defined try to automatically find a fitting variableName
    if (replaceWithString == null)
        replaceWithString = findFittingVariableName(node)

    // Check if varAssign is a valid string for a constant
    isAllowedVarAssign(replaceWithString)

    // The newFormula which will be added to the left side of the sequence. This is the child of the quantifier
    var newFormula = formula.child.clone()

    // Replace all occurrences of the quantifiedVariable with swapVariable
    val replaceWith = Constant(replaceWithString)
    val map = mapOf(formula.varName to replaceWith)
    newFormula = LogicNodeVariableInstantiator.transform(newFormula, map)

    // Add newFormula to the left hand side of the sequence
    var newLeftFormulas = node.leftFormulas.toMutableList()
    newLeftFormulas.add(newFormula)
    newLeftFormulas = newLeftFormulas.distinct().toMutableList()

    val newLeaf = TreeNode(
        nodeID,
        newLeftFormulas,
        node.rightFormulas.distinct().toMutableList(),
        AllLeft(nodeID, listIndex, varAssign)
    )

    state.addChildren(nodeID, newLeaf)

    return state
}

/**
 * Rule AllRight is applied, if the LogicNode is the rightChild of node and is of type All(UniversalQuantifier).
 * It replaces the UniversalQuantifier with the swapvariable.Here, Swap Variable should not be Identifier that already exist.
 * @param state: FOSCState state to apply move on
 * @param nodeID: ID of node to apply move on
 * @param listIndex: Index of the formula(logicNode) to which move should be applied.
 * @param varAssign: Map of swapvariable used.
 * @return new state after applying move
 */
fun applyAllRight(state: FOSCState, nodeID: Int, listIndex: Int, varAssign: Map<String, String>): FOSCState {
    checkRight(state, nodeID, listIndex)

    val node = state.tree[nodeID]
    val formula: LogicNode = node.rightFormulas[listIndex]

    if (formula !is UniversalQuantifier)
        throw IllegalMove("Rule allRight can only be applied on a universal quantifier")

    var replaceWithString = varAssign[formula.varName]

    // When swapVariable is not defined try to automatically find a fitting variableName
    if (replaceWithString == null)
        replaceWithString = findFittingVariableName(node)

    // Check if varAssign is a valid string for a constant
    isAllowedVarAssign(replaceWithString)

    // Check if swapVariable is not already in use in the current sequence
    if (checkIfVariableNameIsAlreadyInUse(node, replaceWithString))
        throw IllegalMove("Identifier '$replaceWithString' is already in use")

    // The newFormula which will be added to the right side of the sequence. This is the child of the quantifier
    var newFormula = formula.child.clone()

    // Replace all occurrences of the quantifiedVariable with swapVariable
    val replaceWith = Constant(replaceWithString)
    val map = mapOf(formula.varName to replaceWith)
    newFormula = LogicNodeVariableInstantiator.transform(newFormula, map)

    // Add newFormula to the right hand side of the sequence
    var newRightFormulas = node.rightFormulas.toMutableList()
    newRightFormulas.add(newFormula)
    newRightFormulas.remove(formula)
    newRightFormulas = newRightFormulas.distinct().toMutableList()

    val newLeaf = TreeNode(
        nodeID,
        node.leftFormulas.distinct().toMutableList(),
        newRightFormulas,
        AllRight(nodeID, listIndex, varAssign)
    )

    state.addChildren(nodeID, newLeaf)

    return state
}

/**
 * Rule ExLeft is applied, if the LogicNode is the leftChild of node and is of type Ex(ExistentialQuantifier).
 * It replaces the ExistentialQuantifier with the swapvariable.Here, Swap Variable should not be Identifier that already exist.
 * @param state: FOSCState state to apply move on
 * @param nodeID: ID of node to apply move on
 * @param listIndex: Index of the formula(logicNode) to which move should be applied.
 * @param varAssign: Map of swapvariable used.
 * @return new state after applying move
 */
fun applyExLeft(state: FOSCState, nodeID: Int, listIndex: Int, varAssign: Map<String, String>): FOSCState {
    checkLeft(state, nodeID, listIndex)

    val node = state.tree[nodeID]
    val formula: LogicNode = node.leftFormulas[listIndex]

    if (formula !is ExistentialQuantifier)
        throw IllegalMove("Rule exLeft can only be applied on an existential quantifier")

    var replaceWithString = varAssign[formula.varName]

    // When swapVariable is not defined try to automatically find a fitting variableName
    if (replaceWithString == null)
        replaceWithString = findFittingVariableName(node)

    // Check if varAssign is a valid string for a constant
    isAllowedVarAssign(replaceWithString)

    // Check if swapVariable is not already in use in the current sequence
    if (checkIfVariableNameIsAlreadyInUse(node, replaceWithString))
        throw IllegalMove("Identifier '$replaceWithString' is already in use")

    // The newFormula which will be added to the left side of the sequence. This is the child of the quantifier
    var newFormula = formula.child.clone()

    // Replace all occurrences of the quantifiedVariable with swapVariable
    val replaceWith = Constant(replaceWithString)
    val map = mapOf(formula.varName to replaceWith)
    newFormula = LogicNodeVariableInstantiator.transform(newFormula, map)

    // Add newFormula to the left hand side of the sequence
    var newLeftFormulas = node.leftFormulas.toMutableList()
    newLeftFormulas.add(newFormula)
    newLeftFormulas.remove(formula)
    newLeftFormulas = newLeftFormulas.distinct().toMutableList()

    val newLeaf = TreeNode(
        nodeID,
        newLeftFormulas,
        node.rightFormulas.distinct().toMutableList(),
        ExLeft(nodeID, listIndex, varAssign)
    )
    state.addChildren(nodeID, newLeaf)

    return state
}

/**
 * Rule ExRight is applied, if the LogicNode is the rightChild of node and is of type Ex(ExistentialQuantifier).
 * It replaces the ExistentialQuantifier with the swapvariable
 * @param state: FOSCState state to apply move on
 * @param nodeID: ID of node to apply move on
 * @param listIndex: Index of the formula(logicNode) to which move should be applied.
 * @param varAssign: Map of swapvariable used.
 * @return new state after applying move
 */
fun applyExRight(state: FOSCState, nodeID: Int, listIndex: Int, varAssign: Map<String, String>): FOSCState {
    checkRight(state, nodeID, listIndex)

    val node = state.tree[nodeID]
    val formula: LogicNode = node.rightFormulas[listIndex]

    if (formula !is ExistentialQuantifier)
        throw IllegalMove("Rule exRight can only be applied on an existential quantifier")

    var replaceWithString = varAssign[formula.varName]

    // When swapVariable is not defined try to automatically find a fitting variableName
    if (replaceWithString == null)
        replaceWithString = findFittingVariableName(node)

    // Check if varAssign is a valid string for a constant
    isAllowedVarAssign(replaceWithString)

    // No need to check if swapVariable is already in use for rule allLeft

    // The newFormula which will be added to the right side of the sequence. This is the child of the quantifier
    var newFormula = formula.child.clone()

    // Replace all occurrences of the quantifiedVariable with swapVariable
    val replaceWith = Constant(replaceWithString)
    val map = mapOf(formula.varName to replaceWith)
    newFormula = LogicNodeVariableInstantiator.transform(newFormula, map)

    // Add newFormula to the right hand side of the sequence
    var newRightFormulas = node.rightFormulas.toMutableList()
    newRightFormulas.add(newFormula)
    newRightFormulas = newRightFormulas.distinct().toMutableList()

    val newLeaf = TreeNode(
        nodeID,
        node.leftFormulas.distinct().toMutableList(),
        newRightFormulas,
        ExRight(nodeID, listIndex, varAssign)
    )
    state.addChildren(nodeID, newLeaf)

    return state
}

/**
 * Checks if a given variableName is used in a sequence.
 * Note: This method will check all identifiers: Relations, Functions, Constants and QuantifiedVariables
 * @param node The sequence in which to look for the variableName
 * @param varName The variable name to be compared with
 */
private fun checkIfVariableNameIsAlreadyInUse(node: TreeNode, varName: String): Boolean {
    val usedNames = (node.leftFormulas + node.rightFormulas).flatMap { IdentifierCollector.collect(it) }.toSet()
    return usedNames.contains(varName)
}

/**
 * Tries to find a variable Name which leads to solving the proof
 */
private fun findFittingVariableName(node: TreeNode): String {
    throw IllegalMove("Not yet implemented")
}

/**
 * Checks if a string is syntactically allowed to be assigned as a constant for quantifier instantiation
 */
@Suppress("ThrowsCount")
private fun isAllowedVarAssign(str: String) {
    if (str.isEmpty())
        throw IllegalMove("Can't instantiate with empty identifier")
    else if (str[0].isUpperCase())
        throw IllegalMove("Constant '$str' does not start with a lowercase letter")

    for (i in str.indices) {
        if (!Tokenizer.isAllowedChar(str[i]))
            throw IllegalMove("Character at position $i in '$str' is not allowed in constants")
    }
}

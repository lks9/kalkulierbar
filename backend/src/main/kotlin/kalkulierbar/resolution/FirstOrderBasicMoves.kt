package kalkulierbar.resolution

import kalkulierbar.IllegalMove
import kalkulierbar.UnificationImpossible
import kalkulierbar.clause.Atom
import kalkulierbar.clause.Clause
import kalkulierbar.logic.FirstOrderTerm
import kalkulierbar.logic.Relation
import kalkulierbar.logic.transform.VariableInstantiator
import kalkulierbar.logic.util.Unification
import kalkulierbar.logic.util.UnifierEquivalence

@Suppress("ThrowsCount", "LongParameterList")
/**
 * Resolve two clauses by unifying two literals by a given variable assignment or automatically
 * @param state Current proof state
 * @param c1 Id of first clause
 * @param c2 Id of second clause
 * @param c1lit The literal to unify of the first clause
 * @param c2lit The literal to unify of the second clause
 * @param varAssign Variable assignment to be used
 */
fun resolveMove(
    state: FoResolutionState,
    c1: Int,
    c2: Int,
    c1lit: Int,
    c2lit: Int,
    varAssign: Map<String, FirstOrderTerm>?
) {
    resolveCheckID(state, c1, c2, c1lit, c2lit)

    val clause1 = state.clauseSet.clauses[c1]
    val clause2 = state.clauseSet.clauses[c2]
    val literal1 = clause1.atoms[c1lit].lit
    val literal2 = clause2.atoms[c2lit].lit
    var unifier = varAssign

    // Calculate mgu if no varAssign was given
    if (unifier == null) {
        try {
            unifier = Unification.unify(literal1, literal2)
        } catch (e: UnificationImpossible) {
            throw IllegalMove("Could not unify '$literal1' and '$literal2': ${e.message}")
        }
    } // Else check varAssign == mgu
    else if (!UnifierEquivalence.isMGUorNotUnifiable(unifier, literal1, literal2)) {
        state.statusMessage = "The unifier you specified is not an MGU"
    }

    instantiate(state, c1, unifier)
    val instance1 = state.clauseSet.clauses.size - 1
    instantiate(state, c2, unifier)
    val instance2 = state.clauseSet.clauses.size - 1
    val literal = state.clauseSet.clauses[instance1].atoms[c1lit].lit

    state.resolve(instance1, instance2, literal, true)

    // We'll remove the clause instances used for resolution here
    // Technically, we could leave them in an hide them, but this causes unnecessary
    // amounts of clutter in the hidden clause set
    state.clauseSet.clauses.removeAt(instance2)
    state.clauseSet.clauses.removeAt(instance1)

    state.setSuffix(state.clauseSet.clauses.size - 1) // Re-name variables
    state.newestNode = state.clauseSet.clauses.size - 1
}

/**
 * Create a new clause by applying a variable instantiation on an existing clause
 * @param state Current proof state
 * @param clauseID ID of the clause to use for instantiation
 * @param varAssign Map of Variables and terms they are instantiated with
 */
private fun instantiate(
    state: FoResolutionState,
    clauseID: Int,
    varAssign: Map<String, FirstOrderTerm>
) {
    if (clauseID < 0 || clauseID >= state.clauseSet.clauses.size)
        throw IllegalMove("There is no clause with id $clauseID")

    val baseClause = state.clauseSet.clauses[clauseID]
    val newClause = instantiateReturn(baseClause, varAssign)

    // Add new clause to state and update newestNode pointer
    state.clauseSet.add(newClause)
    state.newestNode = state.clauseSet.clauses.size - 1
}

/**
 * Create a new clause by applying a variable instantiation on an existing clause
 * @param baseClause the clause to use for instantiation
 * @param varAssign Map of Variables and terms they are instantiated with
 * @return Instantiated clause
 */
private fun instantiateReturn(
    baseClause: Clause<Relation>,
    varAssign: Map<String, FirstOrderTerm>
): Clause<Relation> {
    val newClause = Clause<Relation>()
    val instantiator = VariableInstantiator(varAssign)

    // Build the new clause by cloning atoms from the base clause and applying instantiation
    baseClause.atoms.forEach {
        val relationArgs = it.lit.arguments.map { it.clone().accept(instantiator) }
        val newRelation = Relation(it.lit.spelling, relationArgs)
        val newAtom = Atom(newRelation, it.negated)
        newClause.add(newAtom)
    }
    return newClause
}

/**
 * Create a new atom by applying a variable instantiation on an existing atom
 * @param baseAtom the atom to use for instantiation
 * @param varAssign Map of Variables and terms they are instantiated with
 * @return Instantiated atom
 */
private fun instantiateReturn(
    baseAtom: Atom<Relation>,
    varAssign: Map<String, FirstOrderTerm>
): Atom<Relation> {
    val instantiator = VariableInstantiator(varAssign)
    val relationArgs = baseAtom.lit.arguments.map { it.clone().accept(instantiator) }
    val newRelation = Relation(baseAtom.lit.spelling, relationArgs)
    return Atom<Relation>(newRelation, baseAtom.negated)
}

/**
 * Applies the factorize move
 * @param state The state to apply the move on
 * @param clauseID Id of clause to apply the move on
 * @param atomIDs List of IDs of literals for unification (The literals should be equal)
 */
@Suppress("ThrowsCount")
fun factorize(state: FoResolutionState, clauseID: Int, atomIDs: List<Int>) {
    val clauses = state.clauseSet.clauses

    // Verify that clause id is valid
    if (clauseID < 0 || clauseID >= clauses.size)
        throw IllegalMove("There is no clause with id $clauseID")
    if (atomIDs.size < 2)
        throw IllegalMove("Please select more than 1 atom to factorize")
    // Verification of correct ID in atoms -> unifySingleClause

    var newClause = clauses[clauseID].clone()
    // Unify doubled atoms and remove all except one
    for (i in atomIDs.indices) {
        if (i < atomIDs.size - 1) {
            val firstID = atomIDs[i]
            val secondID = atomIDs[i + 1]
            // Unify the selected atoms and instantiate clause
            val mgu = unifySingleClause(clauses[clauseID], firstID, secondID)
            newClause = instantiateReturn(newClause, mgu)

            // Check equality of both atoms
            if (newClause.atoms[firstID] != newClause.atoms[secondID])
                throw IllegalMove(
                    "Atom '${newClause.atoms[firstID]}' and '${newClause.atoms[secondID]}'" +
                        " are not equal after instantiation"
                )

            // Change every unified atom to placeholder (except last) -> later remove all placeholder
            // -> One Atom remains
            newClause.atoms[atomIDs[i]] = Atom(Relation("%placeholder%", mutableListOf()), false)
        }
    }
    // Remove placeholder atoms
    newClause.atoms.removeIf { it.lit.spelling == "%placeholder%" }

    // Add new clause to clauseSet with adjusted variable-name
    clauses.add(newClause)
    state.newestNode = clauses.size - 1
    state.setSuffix(clauses.size - 1) // Re-name variables
}

/**
 * Creates a new clause in which all side premisses are resolved with the main premiss
 * while paying attention to resolving one atom in each side premiss with the main premiss.
 * Adds the result to the clause set
 * @param state Current proof state
 * @param mainID ID of main premiss clause
 * @param atomMap Maps an atom of the main premiss to an atom of a side premiss
 */
@Suppress("ThrowsCount")
fun hyper(
    state: FoResolutionState,
    mainID: Int,
    atomMap: Map<Int, Pair<Int, Int>>
) {
    // Checks for correct clauseID and IDs in Map
    checkHyperID(state, mainID, atomMap)

    if (atomMap.isEmpty())
        throw IllegalMove("Please select side premisses for hyper resolution")

    val clauses = state.clauseSet.clauses
    val oldMainPremiss = clauses[mainID]

    // produce the mgu
    val relations = mutableListOf<Pair<Relation, Relation>>()
    for ((mAtomID, pair) in atomMap) {
        val (sClauseID, sAtomID) = pair

        val rel1 = clauses[sClauseID].atoms[sAtomID].lit
        val rel2 = oldMainPremiss.atoms[mAtomID].lit

        relations.add(Pair(rel1, rel2))
    }

    val mgu: Map<String, FirstOrderTerm>
    // Get mgu from unification
    try {
        mgu = Unification.unify(relations)
    } catch (e: UnificationImpossible) {
        throw IllegalMove("Could not unify main premiss with side premiss")
    }

    // Build the new clause by cloning atoms from the base clause and applying instantiation
    var mainPremiss = instantiateReturn(oldMainPremiss, mgu)

    // Resolves each side premiss with main premiss
    for ((mAtomID, pair) in atomMap) {
        val (sClauseID, sAtomID) = pair
        val sidePremiss = clauses[sClauseID]

        // Check side premiss for positiveness
        if (!sidePremiss.isPositive())
            throw IllegalMove("Side premiss $sidePremiss is not positive")

        val mAtom = oldMainPremiss.atoms[mAtomID]
        val sAtom = sidePremiss.atoms[sAtomID]

        // Resolve side premiss into main premiss every iteration
        mainPremiss = resolveSidePremiss(
            mainPremiss,
            instantiateReturn(mAtom,mgu),
            instantiateReturn(sidePremiss, mgu),
            instantiateReturn(sAtom,mgu)
        )
    }

    // Check there are no negative atoms anymore
    if (!mainPremiss.isPositive())
        throw IllegalMove("Resulting clause $mainPremiss is not positive")

    // Add resolved clause to clause set
    clauses.add(mainPremiss)
    state.newestNode = clauses.size - 1
}


/**
 * Resolves a main premiss with a side premiss with respect to a literal
 * @param mainPremiss The main premiss to resolve
 * @param mAtom atom in main Premiss
 * @param sidePremiss The side premiss to resolve
 * @param sAtom atom in side premiss
 * @return A instantiated clause which contains all atoms from main and side premiss
 *         except the one matching the literals of mAtomID and sAtomID.
 */
private fun resolveSidePremiss(
    mainPremiss: Clause<Relation>,
    mAtom: Atom<Relation>,
    sidePremiss: Clause<Relation>,
    sAtom: Atom<Relation>
): Clause<Relation> {
    val literal1 = mAtom.lit
    val literal2 = sAtom.lit
    val mgu: Map<String, FirstOrderTerm>

    // Check that atom in main premiss is negative
    if (!mAtom.negated)
        throw IllegalMove("Literal '$mAtom' in main premiss has to be negative")

    // Get mgu from unification
    try {
        mgu = Unification.unify(literal1, literal2)
    } catch (e: UnificationImpossible) {
        throw IllegalMove(
            "Could not unify '$mAtom' of main premiss with " +
                "'$sAtom' of side premiss $sidePremiss: ${e.message}"
        )
    }
    // Resolve mainPremiss with side premiss by given atom
    val mainResolveSide = buildClause(mainPremiss, mAtom, sidePremiss, sAtom)

    val newClause = instantiateReturn(mainResolveSide, mgu)
    return newClause
}

/**
 * Unifies two literals of a clause so that unification on whole clause can be used
 * @param clause clause to unify
 * @param a1 first literal to apply unification
 * @param a2 second literal to apply unification
 * @return Mapping to unify whole clause
 */
@Suppress("ThrowsCount")
private fun unifySingleClause(clause: Clause<Relation>, a1: Int, a2: Int): Map<String, FirstOrderTerm> {
    val atoms = clause.atoms
    // Verify that atom ids are valid
    if (a1 == a2)
        throw IllegalMove("Cannot unify an atom with itself")
    if (a1 < 0 || a1 >= atoms.size)
        throw IllegalMove("There is no atom with id $a1")
    if (a2 < 0 || a2 >= atoms.size)
        throw IllegalMove("There is no atom with id $a2")

    val literal1 = atoms[a1].lit
    val literal2 = atoms[a2].lit
    val mgu: Map<String, FirstOrderTerm>

    // Get unifier for chosen Atoms
    try {
        mgu = Unification.unify(literal1, literal2)
    } catch (e: UnificationImpossible) {
        throw IllegalMove("Could not unify '$literal1' and '$literal2': ${e.message}")
    }
    return mgu
}

package kalkulierbar.resolution

import kalkulierbar.IllegalMove
import kalkulierbar.InvalidFormulaFormat
import kalkulierbar.JSONCalculus
import kalkulierbar.JsonParseException
import kalkulierbar.clause.Atom
import kalkulierbar.clause.Clause
import kalkulierbar.clause.ClauseSet
import kalkulierbar.logic.FoTermModule
import kalkulierbar.logic.Relation
import kalkulierbar.logic.transform.FirstOrderCNF
import kalkulierbar.logic.transform.VariableInstantiator
import kalkulierbar.parsers.FirstOrderParser
import kalkulierbar.tamperprotect.ProtectedState
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.plus

class FirstOrderResolution : GenericResolution<Relation>, JSONCalculus<FoResolutionState, ResolutionMove, FoResolutionParam>() {
    override val identifier = "fo-resolution"

    private val serializer = Json(context = resolutionMoveModule + FoTermModule)

    override fun parseFormulaToState(formula: String, params: FoResolutionParam?): FoResolutionState {
        val parsed = FirstOrderParser.parse(formula)
        val clauses = FirstOrderCNF.transform(parsed)

        return FoResolutionState(clauses, params?.highlightSelectable ?: false)
    }

    override fun applyMoveOnState(state: FoResolutionState, move: ResolutionMove): FoResolutionState {
        when (move) {
            is MoveResolve -> resolveFO(state, move.c1, move.c2, move.literal)
            is MoveInstantiate -> instantiate(state, move.c1, move.varAssign)
            is MoveHide -> hide(state, move.c1)
            is MoveShow -> show(state)
            else -> throw IllegalMove("Unknown move type")
        }

        return state
    }

    override fun checkCloseOnState(state: FoResolutionState) = getCloseMessage(state)

    /**
     * Create a new clause by resolving two existing clauses
     * If the given literal is null, a suitable literal will be determined automatically
     * @param state Current proof state
     * @param c1 First clause to use for resolution
     * @param c2 Second clause to use for resolution
     * @param litString String representation of the Relation to use for resolution
     */
    private fun resolveFO(state: FoResolutionState, c1: Int, c2: Int, litString: String?) {
        val literal: Relation?

        if (litString == null)
            literal = null
        else {
            try {
                literal = FirstOrderParser.parseRelation(litString)
            } catch (e: InvalidFormulaFormat) {
                throw InvalidFormulaFormat("Could not parse literal '$litString': ${e.message}")
            }
        }

        resolve(state, c1, c2, literal)
    }

    /**
     * Create a new clause by applying a variable instantiation on an existing clause
     * @param state Current proof state
     * @param clauseID ID of the clause to use for instantiation
     * @param varAssign Map of Variables and terms they are instantiated with
     * @return New state with the clause instance added
     */
    private fun instantiate(
        state: FoResolutionState,
        clauseID: Int,
        varAssign: Map<String, String>
    ) {
        if (clauseID < 0 || clauseID >= state.clauseSet.clauses.size)
            throw IllegalMove("There is no clause with id $clauseID")

        val baseClause = state.clauseSet.clauses[clauseID]
        val newClause = Clause<Relation>()

        // Parse the replacement terms and create an instantiation visitor
        val varAssignParsed = varAssign.mapValues {
            try {
                FirstOrderParser.parseTerm(it.value)
            } catch (e: InvalidFormulaFormat) {
                throw InvalidFormulaFormat("Could not parse term '${it.value}': ${e.message}")
            }
        }
        val instantiator = VariableInstantiator(varAssignParsed)

        // Build the new clause by cloning atoms from the base clause and applying instantiation
        baseClause.atoms.forEach {
            val relationArgs = it.lit.arguments.map { it.clone().accept(instantiator) }
            val newRelation = Relation(it.lit.spelling, relationArgs)
            val newAtom = Atom<Relation>(newRelation, it.negated)
            newClause.add(newAtom)
        }

        // Add new clause to state and update newestNode pointer
        state.clauseSet.add(newClause)
        state.newestNode = state.clauseSet.clauses.size - 1
    }

    @Suppress("TooGenericExceptionCaught")
    override fun jsonToState(json: String): FoResolutionState {
        try {
            val parsed = serializer.parse(FoResolutionState.serializer(), json)

            // Ensure valid, unmodified state object
            if (!parsed.verifySeal())
                throw JsonParseException("Invalid tamper protection seal, state object appears to have been modified")

            return parsed
        } catch (e: Exception) {
            val msg = "Could not parse JSON state: "
            throw JsonParseException(msg + (e.message ?: "Unknown error"))
        }
    }

    override fun stateToJson(state: FoResolutionState): String {
        state.computeSeal()
        return serializer.stringify(FoResolutionState.serializer(), state)
    }

    @Suppress("TooGenericExceptionCaught")
    override fun jsonToMove(json: String): ResolutionMove {
        try {
            return serializer.parse(ResolutionMove.serializer(), json)
        } catch (e: Exception) {
            val msg = "Could not parse JSON move: "
            throw JsonParseException(msg + (e.message ?: "Unknown error"))
        }
    }

    /*
     * Parses a JSON parameter representation into a ResolutionParam object
     * @param json JSON parameter representation
     * @return parsed param object
     */
    @Suppress("TooGenericExceptionCaught")
    override fun jsonToParam(json: String): FoResolutionParam {
        try {
            return serializer.parse(FoResolutionParam.serializer(), json)
        } catch (e: Exception) {
            val msg = "Could not parse JSON params: "
            throw JsonParseException(msg + (e.message ?: "Unknown error"))
        }
    }
}

@Serializable
class FoResolutionState(
    override val clauseSet: ClauseSet<Relation>,
    override val highlightSelectable: Boolean
) : GenericResolutionState<Relation>, ProtectedState() {
    override var newestNode = -1
    override val hiddenClauses = ClauseSet<Relation>()

    override var seal = ""

    override fun getHash(): String {
        return "resolutionstate|$clauseSet|$hiddenClauses|$highlightSelectable|$newestNode"
    }
}

@Serializable
data class FoResolutionParam(val highlightSelectable: Boolean)

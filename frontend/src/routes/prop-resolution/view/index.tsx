import { Fragment, h } from "preact";
import { useState } from "preact/hooks";
import * as style from "./style.css";

import CheckCloseBtn from "../../../components/check-close";
import Dialog from "../../../components/dialog";
import ResolutionCircle from "../../../components/resolution/circle";
import { sendMove } from "../../../helpers/api";
import { useAppState } from "../../../helpers/app-state";
import { CandidateClause } from "../../../types/clause";
import exampleState from "./example";

interface Props {}

type SelectedClauses = undefined | [number] | [number, number];

// Component displaying the content of the prop-resolution route
const ResolutionView: preact.FunctionalComponent<Props> = () => {
    const {
        server,
        ["prop-resolution"]: cState,
        onError,
        onChange
    } = useAppState();
    let state = cState;

    if (!state) {
        // return <p>Keine Daten vorhanden</p>;
        // Default state for easy testing
        onChange("prop-resolution", exampleState);
        state = exampleState;
    }

    const [selectedClauses, setSelectedClauses] = useState<SelectedClauses>(
        undefined
    );

    const selectedClauseId =
        selectedClauses === undefined ? undefined : selectedClauses[0];

    const showDialog = selectedClauses && selectedClauses.length === 2;

    /**
     * Creates an array of candidate clauses based on if a clause is selected
     * @returns {CandidateClause[]} - The new candidate clauses
     */
    const getCandidateClauses = () => {
        const newCandidateClauses: CandidateClause[] = [];

        if (selectedClauseId === undefined) {
            // Create default candidates
            state!.clauseSet.clauses.forEach((clause, index) => {
                newCandidateClauses[index] = {
                    atoms: clause.atoms,
                    candidateLiterals: []
                };
            });
        } else {
            // Get selected clause
            const selectedClause = state!.clauseSet.clauses[selectedClauseId];

            // Filter for possible resolve candidates
            state!.clauseSet.clauses.forEach((clause, index) => {
                const literals: string[] = [];
                selectedClause.atoms.forEach(atom1 => {
                    clause.atoms.forEach(atom2 => {
                        if (
                            atom1.lit === atom2.lit &&
                            atom1.negated !== atom2.negated
                        ) {
                            literals.push(atom1.lit);
                        }
                    });
                });
                newCandidateClauses[index] = {
                    atoms: clause.atoms,
                    candidateLiterals: literals
                };
            });
        }
        return newCandidateClauses;
    };

    const candidateClauses = getCandidateClauses();

    /**
     * The function to call, when the user selects a clause
     * @param {number} newClauseId - The id of the clause, which was clicked on
     * @returns {void}
     */
    const selectClauseCallback = (newClauseId: number) => {
        if (selectedClauseId === undefined) {
            setSelectedClauses([newClauseId]);
        } else if (newClauseId === selectedClauseId) {
            // The same clause was selected again => reset selection
            setSelectedClauses(undefined);
        } else {
            const candidateClause = candidateClauses[newClauseId];
            let resolventLiteral: string;
            if (candidateClause.candidateLiterals.length > 1) {
                // Show dialog for literal selection
                setSelectedClauses([selectedClauses![0], newClauseId]);
                return;
            }

            resolventLiteral = candidateClause.candidateLiterals[0];

            // Send resolve move to backend
            sendMove(
                server,
                "prop-resolution",
                state!,
                {
                    c1: selectedClauseId,
                    c2: newClauseId,
                    spelling: resolventLiteral
                },
                onChange,
                onError
            );
            // Reset selection
            setSelectedClauses(undefined);
        }
    };

    return (
        <Fragment>
            <h2>Resolution View</h2>
            <div class={style.view}>
                <div>
                    <CheckCloseBtn calculus="prop-resolution" />
                </div>
                <ResolutionCircle
                    clauses={candidateClauses}
                    selectClauseCallback={selectClauseCallback}
                    selectedClauseId={selectedClauseId}
                />
            </div>
            <Dialog
                open={showDialog}
                label="Choose Literal"
                onClose={() => setSelectedClauses([selectedClauses![0]])}
            >
                {selectedClauses &&
                    selectedClauses.length === 2 &&
                    candidateClauses[selectedClauses[1]].candidateLiterals.map(
                        l => (
                            <p
                                onClick={() => {
                                    sendMove(
                                        server,
                                        "prop-resolution",
                                        state!,
                                        {
                                            c1: selectedClauseId,
                                            c2: selectedClauses[1],
                                            spelling: l
                                        },
                                        onChange,
                                        onError
                                    );
                                    setSelectedClauses(undefined);
                                }}
                            >
                                {l}
                            </p>
                        )
                    )}
            </Dialog>
        </Fragment>
    );
};

export default ResolutionView;

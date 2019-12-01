import { Fragment, h } from "preact";
import { useState } from "preact/hooks";
import { AppStateUpdater } from "../../../types/app";
import { TableauxMove, TableauxState } from "../../../types/tableaux";
import * as style from "./style.css";

import CheckCloseBtn from "../../../components/check-close";
import ClauseList from "../../../components/clause-list";
import { D3Data } from "../../../components/tableaux/tree";
import TableauxTreeView from "../../../components/tableaux/tree";
import exampleState from "./example";

interface Props {
    server: string;
    state?: TableauxState;
    onChange: AppStateUpdater<"prop-tableaux">;
    onError: (msg: string) => void;
    onSuccess: (msg: string) => void;
}

const sendMove = async (
    url: string,
    state: TableauxState,
    move: TableauxMove,
    stateChanger: AppStateUpdater<"prop-tableaux">,
    onError: (msg: string) => void
) => {
    try {
        const res = await fetch(url, {
            headers: {
                "Content-Type": "text/plain"
            },
            method: "POST",
            body: `move=${JSON.stringify(move)}&state=${JSON.stringify(state)}`
        });
        if (res.status !== 200) {
            onError(await res.text());
        }
        const parsed = await res.json();
        stateChanger("prop-tableaux", parsed);
    } catch (e) {
        onError((e as Error).message);
    }
};

const sendClose = (
    url: string,
    state: TableauxState,
    stateChanger: AppStateUpdater<"prop-tableaux">,
    onError: (msg: string) => void,
    leaf: number,
    pred: number
) =>
    sendMove(
        url,
        state,
        { type: "c", id1: leaf, id2: pred },
        stateChanger,
        onError
    );

const sendExtend = (
    url: string,
    state: TableauxState,
    stateChanger: AppStateUpdater<"prop-tableaux">,
    onError: (msg: string) => void,
    leaf: number,
    clause: number
) =>
    sendMove(
        url,
        state,
        { type: "e", id1: leaf, id2: clause },
        stateChanger,
        onError
    );

// Availble edit modes to modify the tree
const EDIT_MODE_EXTEND = 0;
const EDIT_MODE_CLOSE = 1;

const TableauxView: preact.FunctionalComponent<Props> = ({
    state,
    server,
    onChange,
    onError,
    onSuccess
}) => {
    const [selectedEditMode, setSelectedEditMode] = useState<
        number | undefined
    >(undefined);
    const [selectedClauseId, setSelectedClauseId] = useState<
        number | undefined
    >(undefined);
    const [selectedNodeId, setSelectedNodeId] = useState<number | undefined>(
        undefined
    );

    const url = `${server}/prop-tableaux/`;
    const moveUrl = url + "move";

    // Callback function which is called when a clause is selected by the user
    const selectClauseCallback = (newClauseId: number) => {
        if (newClauseId === selectedClauseId) {
            setSelectedClauseId(undefined);
            setSelectedNodeId(undefined);
            setSelectedEditMode(undefined);
        } else {
            setSelectedClauseId(newClauseId);
            setSelectedEditMode(EDIT_MODE_EXTEND);

            if (selectedNodeId !== undefined) {
                sendExtend(
                    moveUrl,
                    state!,
                    onChange,
                    onError,
                    selectedNodeId,
                    newClauseId
                );
                setSelectedEditMode(undefined);
                setSelectedNodeId(undefined);
                setSelectedClauseId(undefined);
            }
        }
    };

    // Callback function which is called when a node is selected by the user
    const selectNodeCallback = (newNode: D3Data) => {
        if (newNode.id === selectedNodeId) {
            setSelectedNodeId(undefined);
            setSelectedEditMode(undefined);
        } else if (newNode.isLeaf) {
            // Select new leaf node
            setSelectedNodeId(newNode.id);
        } else if (selectedNodeId !== undefined) {
            // We already have a node selected. Try close
            // If we can't do it, let server handle it
            sendClose(
                moveUrl,
                state!,
                onChange,
                onError,
                selectedNodeId,
                newNode.id
            );
            setSelectedNodeId(undefined);
        }

        if (selectedEditMode === EDIT_MODE_EXTEND) {
            if (selectedClauseId !== undefined) {
                sendExtend(
                    moveUrl,
                    state!,
                    onChange,
                    onError,
                    newNode.id,
                    selectedClauseId
                );
                setSelectedEditMode(undefined);
                setSelectedNodeId(undefined);
                setSelectedClauseId(undefined);
            }
        } else if (selectedEditMode === EDIT_MODE_CLOSE) {
            // Do stuff specific to EDIT_MODE_CLOSE
        } else {
            // Show dialog to user to decide upon the desired move
        }
    };

    if (!state) {
        // return <p>Keine Daten vorhanden</p>;
        // Default state for easy testing
        state = exampleState;
    }

    return (
        <Fragment>
            <h2>Tableaux View</h2>
            <div class={style.view}>
                <div>
                    <ClauseList
                        clauseSet={state.clauseSet}
                        selectedClauseId={selectedClauseId}
                        selectClauseCallback={selectClauseCallback}
                    />
                    <CheckCloseBtn
                        server={server}
                        calculus="prop-tableaux"
                        state={state}
                        onError={onError}
                        onSuccess={onSuccess}
                    />
                </div>
                <TableauxTreeView
                    nodes={state.nodes}
                    selectedNodeId={selectedNodeId}
                    selectNodeCallback={selectNodeCallback}
                />
            </div>
        </Fragment>
    );
};

export default TableauxView;

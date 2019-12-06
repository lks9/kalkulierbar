import { event, hierarchy, HierarchyNode, select, tree, zoom } from "d3";
import { Fragment, h } from "preact";
import { useContext, useEffect, useRef, useState } from "preact/hooks";

import { TableauxNode, TableauxTreeGoToEvent } from "../../../types/tableaux";
import { SmallScreen } from "../../app";
import TableauxTreeNode from "../node";

import * as style from "./style.css";

// Properties Interface for the TableauxTreeView component
interface Props {
    /**
     * The nodes of the tree
     */
    nodes: TableauxNode[];
    /**
     * The id of a node if one is selected
     */
    selectedNodeId: number | undefined;
    /**
     * The function to call, when the user selects a node
     */
    selectNodeCallback: (node: D3Data) => void;
}

// Interface for a node
export interface D3Data {
    id: number;
    name: string;
    isLeaf: boolean;
    negated: boolean;
    isClosed: boolean;
    closeRef: number | null;
    children?: D3Data[];
}

// Creates a tree layout function
const layout = tree();

/**
 * Transforms the node data received by the server to data
 * accepted by d3
 * @param {number} id  - the node to transform
 * @param {TableauxNode[]} nodes  - list of all nodes
 * @returns {D3Data} - data as d3 parsable
 */
const transformNodeToD3Data = (id: number, nodes: TableauxNode[]): D3Data => {
    const node = nodes[id];
    const isLeaf = !node.children.length;
    const children = isLeaf
        ? undefined
        : node.children.map(c => transformNodeToD3Data(c, nodes));

    return {
        id,
        name: node.spelling,
        isLeaf,
        children,
        negated: node.negated,
        isClosed: node.isClosed,
        closeRef: node.closeRef
    };
};

/**
 *
 * @param {Array<HierarchyNode<D3Data>>} nodes - The nodes we iterate over
 * @param {number} id - Id of the ancestor
 * @returns {HierarchyNode<D3Data>} - The ancestor
 */
const getNodeById = (nodes: Array<HierarchyNode<D3Data>>, id: number) =>
    nodes.find(n => n.data.id === id)!;

interface ClosingEdgeProps {
    leaf: HierarchyNode<D3Data>;
    pred: HierarchyNode<D3Data>;
}

// Component to display an edge in a graph
const ClosingEdge: preact.FunctionalComponent<ClosingEdgeProps> = ({
    leaf,
    pred
}) => {
    // Calculate coordinates
    const x1 = (leaf as any).x - 3;
    const y1 = (leaf as any).y - 16;
    const x2 = (pred as any).x - 3;
    const y2 = (pred as any).y + 4;

    // Calculate edge
    // M -> move to point x1,y1
    // Q -> draw quadratic curve (type of Bezier Curve https://developer.mozilla.org/de/docs/Web/SVG/Tutorial/Pfade)
    //      xC,yC of the control point
    //      x2,y2 of the target
    // should look like d="M x1 x2 Q xC yC x2 y2"
    const d =
        "M " +
        x1 +
        " " +
        y1 +
        " Q " +
        (x1 - (y1 - y2) / 2) +
        " " +
        (y1 + y2) / 2 +
        " " +
        x2 +
        " " +
        y2;

    return <path d={d} class={style.link} />;
};

interface Transform {
    x: number;
    y: number;
    k: number;
}

const INIT_TRANSFORM: Transform = { x: 0, y: 0, k: 1 };

// Component displaying nodes as a TableauxTree
const TableauxTreeView: preact.FunctionalComponent<Props> = ({
    nodes,
    selectedNodeId,
    selectNodeCallback
}) => {
    // This is the reference to our SVG element
    const svgRef = useRef<any>();

    // Transform nodes to d3 hierarchy
    const root = hierarchy(transformNodeToD3Data(0, nodes));

    const smallScreen = useContext(SmallScreen);

    // Size of the nodes. [width, height]
    const nodeSize: [number, number] = smallScreen ? [70, 70] : [140, 140];

    const [transform, setTransform] = useState<Transform>(INIT_TRANSFORM);

    // If we have a SVG, set its zoom to our transform
    // Unfortunately, none of the methods that should work, do
    // so this is pretty dirty
    if (svgRef.current) {
        const e = svgRef.current;
        const t = e.__zoom;
        t.x = transform.x;
        t.y = transform.y;
        t.k = transform.k;
    }

    // Calculate tree size
    const treeHeight = root.height * nodeSize[1];
    const leaves = root.copy().count().value || 1;
    const treeWidth = leaves * nodeSize[0];

    // Let d3 calculate our layout
    layout.size([treeWidth, treeHeight]);
    layout(root);

    useEffect(() => {
        // Get the elements to manipulate
        const svg = select(`.${style.svg}`);
        // Add zoom and drag behavior
        svg.call(
            zoom().on("zoom", () => {
                const { x, y, k } = event.transform as Transform;
                setTransform({ x, y, k });
            }) as any
        );

        // Experimental
        (window as any).goToNode = (n: number) => {
            const { x, y } = getNodeById(root.descendants(), n) as any;
            setTransform({ x: treeWidth / 2 - x, y: treeHeight / 2 - y, k: 1 });
        };
    });

    useEffect(() => {
        window.addEventListener("kbar-center-tree", () => {
            setTransform(INIT_TRANSFORM);
        });

        window.addEventListener("kbar-go-to-node", e => {
            const { node: id } = (e as TableauxTreeGoToEvent).detail;
            const { x, y } = getNodeById(root.descendants(), id) as any;
            setTransform({ x: treeWidth / 2 - x, y: treeHeight / 2 - y, k: 1 });
        });
    }, []);

    return (
        <div class="card">
            <svg
                ref={svgRef}
                class={style.svg}
                width="100%"
                height={`${treeHeight + 16}px`}
                style="min-height: 60vh"
                viewBox={`0 0 ${treeWidth} ${treeHeight + 32}`}
                preserveAspectRatio="xMidyMid meet"
            >
                <g
                    transform={`translate(${transform.x} ${transform.y +
                        16}) scale(${transform.k})`}
                >
                    <g class="links">
                        {root.links().map(l => (
                            <line
                                class={style.link}
                                x1={(l.source as any).x}
                                y1={(l.source as any).y + 6}
                                x2={(l.target as any).x}
                                y2={(l.target as any).y - 18}
                            />
                        ))}
                    </g>
                    <g class="nodes">
                        {root.descendants().map(n => (
                            <Fragment>
                                <TableauxTreeNode
                                    selectNodeCallback={selectNodeCallback}
                                    node={n}
                                    selected={n.data.id === selectedNodeId}
                                />
                                {n.data.isClosed ? (
                                    <ClosingEdge
                                        leaf={n}
                                        pred={getNodeById(
                                            n.ancestors(),
                                            n.data.closeRef!
                                        )}
                                    />
                                ) : null}
                            </Fragment>
                        ))}
                    </g>
                </g>
            </svg>
        </div>
    );
};

export default TableauxTreeView;

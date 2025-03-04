import { CalculusType, Formulas } from "../calculus";
import { DPLLState } from "../calculus/dpll";
import { ModalTableauxState } from "../calculus/modal-tableaux";
import { NCTableauxState } from "../calculus/nc-tableaux";
import { FOResolutionState, PropResolutionState } from "../calculus/resolution";
import { FOSCState, PSCState } from "../calculus/sequent";
import { FOTableauxState, PropTableauxState } from "../calculus/tableaux";

import { AppStateAction } from "./action";
import { Config } from "./config";
import { Notification, NotificationHandler } from "./notification";
import { Theme } from "./theme";
import { TutorialMode } from "./tutorial";

/**
 * The state of the application
 */
export interface AppState {
    /**
     * The server we are connected to
     */
    server: string;
    /**
     * The current notification
     */
    notification?: Notification;
    /**
     * Whether the screen is currently small (< 750px width)
     */
    smallScreen: boolean;
    /**
     * The current theme
     */
    theme: Theme;
    /**
     * The currently saved formulas for each calculus
     */
    savedFormulas: Formulas;
    /**
     * The current prop-tableaux state
     */
    "prop-tableaux"?: PropTableauxState;
    /**
     * The current prop-resolution state
     */
    "prop-resolution"?: PropResolutionState;
    /**
     * The current fo-tableaux state
     */
    "fo-tableaux"?: FOTableauxState;
    /**
     * The current fo-resolution state
     */
    "fo-resolution"?: FOResolutionState;
    /**
     * The current nc-tableaux state
     */
    "nc-tableaux"?: NCTableauxState;
    /**
     * The current dpll state
     */
    dpll?: DPLLState;
    /**
     * The current prop-sequent state
     */
    "prop-sequent"?: PSCState;
    /**
     * The current fo-sequent state
     */
    "fo-sequent"?: FOSCState;
    /**
     * The current signed-modal-tableaux state
     */
    "signed-modal-tableaux"?: ModalTableauxState;
    /**
     * The current tutorial mode
     */
    tutorialMode: TutorialMode;
    /**
     * Whether the current user successfully logged in as admin
     */
    isAdmin: boolean;
    /**
     * The login that the user provided
     */
    adminKey: string;
    /**
     * The app configuration from backend (disabled calculi, examples, ..)
     */
    config: Config;
}

/**
 * The app state expanded by functions to change it
 */
export interface DerivedAppState extends AppState {
    notificationHandler: NotificationHandler;
    onChange: <C extends CalculusType = CalculusType>(
        calculus: C,
        state: AppState[C],
    ) => void;
    setConfig: (cfg: Config) => void;
    dispatch: (a: AppStateAction) => void;
}

export type AppStateUpdater = <C extends CalculusType = CalculusType>(
    id: C,
    newState: AppState[C],
) => void;

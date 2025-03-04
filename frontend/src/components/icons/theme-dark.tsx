import { h } from "preact";

interface Props {
    /**
     * Width and height of the icon. Defaults to `24`.
     */
    size?: number;
    /**
     * The fill color to use. Defaults to `#fff`.
     */
    fill?: string;
}

const ThemeDark: preact.FunctionalComponent<Props> = ({
    size = 24,
    fill = "#fff",
}) => (
    <svg
        width={size}
        height={size}
        xmlns="http://www.w3.org/2000/svg"
        viewBox="0 0 128 128"
    >
        <g fill={fill} fill-rule="evenodd">
            <path d="M43.626 19.013C39.414 26.2 37 34.568 37 43.5 37 70.286 58.714 92 85.5 92c4.821 0 9.478-.703 13.874-2.013C90.954 104.353 75.354 114 57.5 114 30.714 114 9 92.286 9 65.5c0-21.965 14.6-40.519 34.626-46.487zM98.79 32.79L95.5 54l-3.29-21.21L71 29.5l21.21-3.29L95.5 5l3.29 21.21L120 29.5zM109.447 70.447L107.5 83l-1.947-12.553L93 68.5l12.553-1.947L107.5 54l1.947 12.553L122 68.5z" />
            <path d="M64.85 52.85L62.5 68l-2.35-15.15L45 50.5l15.15-2.35L62.5 33l2.35 15.15L80 50.5z" />
        </g>
    </svg>
);

export default ThemeDark;

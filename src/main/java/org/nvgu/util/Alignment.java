package org.nvgu.util;

import static org.lwjgl.nanovg.NanoVG.*;
import static org.lwjgl.nanovg.NanoVG.NVG_ALIGN_BOTTOM;

public enum Alignment {
    LEFT_TOP(NVG_ALIGN_LEFT | NVG_ALIGN_TOP),
    CENTER_TOP(NVG_ALIGN_CENTER | NVG_ALIGN_TOP),
    RIGHT_TOP(NVG_ALIGN_RIGHT | NVG_ALIGN_TOP),

    LEFT_MIDDLE(NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE),
    CENTER_MIDDLE(NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE),
    RIGHT_MIDDLE(NVG_ALIGN_RIGHT | NVG_ALIGN_MIDDLE),

    LEFT_BOTTOM(NVG_ALIGN_LEFT | NVG_ALIGN_BOTTOM),
    CENTER_BOTTOM(NVG_ALIGN_CENTER | NVG_ALIGN_BOTTOM),
    RIGHT_BOTTOM(NVG_ALIGN_RIGHT | NVG_ALIGN_BOTTOM);

    private final int textAlignment;

    Alignment(int textAlignment) {
        this.textAlignment = textAlignment;
    }

    public int getTextAlignment() {
        return textAlignment;
    }
}

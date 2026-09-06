package com.termux.app.editor;

/**
 * Per-tab state held in memory while EditorActivity is alive, and
 * (subset of it) persisted to disk via EditorSessionManager.
 */
public class TabState {
    public final String sourcePathOrUri;
    public transient EditableSource source; // rebuilt on load, not persisted directly
    public String content = "";
    public int cursorLine = 0;
    public boolean isDirty = false;

    public TabState(String sourcePathOrUri) {
        this.sourcePathOrUri = sourcePathOrUri;
    }

    public String getDisplayName() {
        return source != null ? source.getDisplayName() : sourcePathOrUri;
    }
}

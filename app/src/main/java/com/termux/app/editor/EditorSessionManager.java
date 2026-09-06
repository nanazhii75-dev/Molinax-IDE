package com.termux.app.editor;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Persist EditorActivity's open-tab session (path/uri list + active index + cursor line)
 * to SharedPreferences, so it survives EditorActivity being finish()'d (Back press),
 * app-kill from Recents, or device reboot.
 *
 * Deliberately does NOT persist tab.content — content lives on disk via EditableSource,
 * re-read fresh on restore. Only metadata needed to reconstruct the tab list.
 */
public class EditorSessionManager {
    private static final String PREFS = "editor_session";
    private static final String KEY_SESSION = "session_json";

    public static class SessionState {
        public int activeTabIndex;
        public List<TabEntry> tabs = new ArrayList<>();
    }

    public static class TabEntry {
        public final String pathOrUri;
        public final int cursorLine;

        public TabEntry(String pathOrUri, int cursorLine) {
            this.pathOrUri = pathOrUri;
            this.cursorLine = cursorLine;
        }
    }

    public static void save(Context ctx, int activeIndex, List<TabState> tabs) {
        try {
            JSONObject root = new JSONObject();
            root.put("activeTabIndex", activeIndex);

            JSONArray arr = new JSONArray();
            for (TabState tab : tabs) {
                JSONObject o = new JSONObject();
                o.put("source", tab.sourcePathOrUri);
                o.put("cursorLine", tab.cursorLine);
                arr.put(o);
            }
            root.put("tabs", arr);

            SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            // commit() sinkron, bukan apply() — jaminan tertulis sebelum proses di-kill sistem
            // tepat setelah onPause()/onStop()
            prefs.edit().putString(KEY_SESSION, root.toString()).commit();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static SessionState load(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_SESSION, null);
        if (json == null) return null;

        try {
            JSONObject root = new JSONObject(json);
            SessionState state = new SessionState();
            state.activeTabIndex = root.optInt("activeTabIndex", 0);

            JSONArray arr = root.optJSONArray("tabs");
            if (arr == null) return null;

            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                state.tabs.add(new TabEntry(o.getString("source"), o.optInt("cursorLine", 0)));
            }
            return state;
        } catch (JSONException e) {
            // JSON korup — jangan crash, anggap tidak ada sesi tersimpan
            return null;
        }
    }
}

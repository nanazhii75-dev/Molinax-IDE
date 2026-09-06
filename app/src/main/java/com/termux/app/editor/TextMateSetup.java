package com.termux.app.editor;

import android.content.Context;

import org.eclipse.tm4e.core.registry.IThemeSource;

import java.util.concurrent.atomic.AtomicBoolean;

import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry;
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry;
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry;
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel;
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver;

public class TextMateSetup {

    private static final AtomicBoolean isReady = new AtomicBoolean(false);
    private static volatile String lastError = null;

    private static final String[] THEME_NAMES = {"ayu-dark", "dracula", "light-plus", "solarized-dark"};
    private static final String DEFAULT_THEME = "ayu-dark";

    public static void init(Context context) {
        Context appContext = context.getApplicationContext();
        new Thread(() -> {
            try {
                FileProviderRegistry.getInstance().addFileProvider(
                    new AssetsFileResolver(appContext.getAssets())
                );

                for (String theme : THEME_NAMES) {
                    String path = "textmate/themes/" + theme + ".json";
                    ThemeModel model = new ThemeModel(
                        IThemeSource.fromInputStream(
                            FileProviderRegistry.getInstance().tryGetInputStream(path),
                            path,
                            null
                        ),
                        theme
                    );
                    ThemeRegistry.getInstance().loadTheme(model);
                }
                ThemeRegistry.getInstance().setTheme(DEFAULT_THEME);

                GrammarRegistry.getInstance().loadGrammars("textmate/languages.json");

                isReady.set(true);
            } catch (Throwable e) {
                lastError = e.toString();
                e.printStackTrace();
            }
        }).start();
    }

    public static boolean isReady() {
        return isReady.get();
    }

    public static String getLastError() {
        return lastError;
    }
}

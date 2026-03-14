package com.etema.ragnarmmo.common.init.modules;

import com.etema.ragnarmmo.system.bar.RagnarIntegrationHandler;

public final class BarModule {
    private BarModule() {}

    public static void init() {
        RagnarIntegrationHandler.init();
    }
}







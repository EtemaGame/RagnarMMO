package com.etema.ragnarmmo.client.ui;

import com.etema.ragnarmmo.common.api.mobs.runtime.manual.InternalManualMobEntry;
import com.etema.ragnarmmo.common.api.mobs.runtime.manual.ManualMobCatalogEntry;

import java.util.List;

public final class ManualMobUiState {
    private static volatile List<ManualMobCatalogEntry> catalog = List.of();
    private static volatile InternalManualMobEntry detail;
    private static volatile String lastError;

    private ManualMobUiState() {
    }

    public static void setCatalog(List<ManualMobCatalogEntry> value) { catalog = value == null ? List.of() : List.copyOf(value); }
    public static List<ManualMobCatalogEntry> getCatalog() { return catalog; }

    public static void setDetail(InternalManualMobEntry value) { detail = value; }
    public static InternalManualMobEntry getDetail() { return detail; }

    public static void setLastError(String value) { lastError = value; }
    public static String getLastError() { return lastError; }
}

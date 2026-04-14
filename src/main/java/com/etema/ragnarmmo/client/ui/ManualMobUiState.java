package com.etema.ragnarmmo.client.ui;

import com.etema.ragnarmmo.common.api.mobs.runtime.manual.ManualMobDetail;
import com.etema.ragnarmmo.common.api.mobs.runtime.manual.ManualMobCatalogEntry;

import java.util.List;

public final class ManualMobUiState {
    private static volatile List<ManualMobCatalogEntry> catalog = List.of();
    private static volatile ManualMobDetail detail;
    private static volatile String lastError;
    private static volatile String lastSuccess;

    private static volatile long catalogVersion = 0;
    private static volatile long detailVersion = 0;
    private static volatile boolean catalogDirty = false;
    private static volatile boolean detailDirty = false;

    private ManualMobUiState() {
    }

    public static void setCatalog(List<ManualMobCatalogEntry> value) {
        catalog = value == null ? List.of() : List.copyOf(value);
        catalogVersion++;
        catalogDirty = false;
    }
    public static List<ManualMobCatalogEntry> getCatalog() { return catalog; }
    public static long getCatalogVersion() { return catalogVersion; }
    public static void markCatalogDirty() { catalogDirty = true; }
    public static boolean isCatalogDirty() { return catalogDirty; }

    public static void setDetail(ManualMobDetail value) {
        detail = value;
        detailVersion++;
        detailDirty = false;
    }
    public static ManualMobDetail getDetail() { return detail; }
    public static long getDetailVersion() { return detailVersion; }
    public static void markDetailDirty() { detailDirty = true; }
    public static boolean isDetailDirty() { return detailDirty; }

    public static void setLastError(String value) { lastError = value; }
    public static String getLastError() { return lastError; }

    public static void setLastSuccess(String value) { lastSuccess = value; }
    public static String getLastSuccess() { return lastSuccess; }
}

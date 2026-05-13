package com.etema.ragnarmmo.system;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class LegacyCompatibilityAuditSourceTest {
    private static final Path ROOT = Path.of("src/main/java/com/etema/ragnarmmo");
    private static final Path DOC = Path.of("docs/LEGACY_COMPATIBILITY.md");

    @Test
    void documentedCompatibilityAliasesMatchCodeAnchors() throws IOException {
        String doc = Files.readString(DOC);
        String commands = Files.readString(ROOT.resolve("common/init/RagnarCommand.java"));
        String merchantMixin = Files.readString(ROOT.resolve("mixin/MerchantOfferMixin.java"));
        String skillDefinition = Files.readString(ROOT.resolve("skills/data/SkillDefinition.java"));

        for (String alias : new String[] {
                "ragnar", "stats", "skills", "cart", "party", "memo", "lv", "job",
                "exp", "set", "unlock", "reset", "debug", "admin", "pc" }) {
            assertTrue(commands.contains("\"" + alias + "\""), () -> alias + " command alias missing from code");
            assertTrue(doc.contains("`" + alias + "`"), () -> alias + " command alias missing from audit doc");
        }

        for (String alias : new String[] {
                "f_45310_", "baseCostA", "f_45311_", "costB", "f_45312_", "result" }) {
            assertTrue(merchantMixin.contains("\"" + alias + "\""), () -> alias + " mixin alias missing from code");
            assertTrue(doc.contains("`" + alias + "`"), () -> alias + " mixin alias missing from audit doc");
        }

        for (String alias : new String[] { "resource_cost", "sp_cost", "mana_cost" }) {
            assertTrue(skillDefinition.contains("\"" + alias + "\""),
                    () -> alias + " skill data alias missing from code");
            assertTrue(doc.contains("`" + alias + "`"),
                    () -> alias + " skill data alias missing from audit doc");
        }
    }
}

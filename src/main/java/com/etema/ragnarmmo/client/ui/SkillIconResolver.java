package com.etema.ragnarmmo.client.ui;

import com.etema.ragnarmmo.common.api.lifeskills.LifeSkillType;
import com.etema.ragnarmmo.skills.api.ISkillDefinition;
import com.etema.ragnarmmo.skills.data.SkillRegistry;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Resolves skill icon textures with backwards-compatible fallbacks.
 *
 * <p>This avoids hard failures when an old skill icon name is still referenced,
 * and it gives the UI a stable way to render a fallback label if the texture is
 * genuinely missing from resources.</p>
 */
public final class SkillIconResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(SkillIconResolver.class);

    private static final Map<String, String> LEGACY_TEXTURE_ALIASES = Map.ofEntries(
            Map.entry("basic_skill", "common/common_basic_conditioning"),
            Map.entry("first_aid", "common/common_first_aid"),
            Map.entry("play_dead", "common/common_survival_instinct"),
            Map.entry("survival_instinct", "common/common_survival_instinct"),
            Map.entry("increase_hp_recovery", "swordman/swordman_increase_recuperative_power"),
            Map.entry("increase_sp_recovery", "mage/mage_increase_spiritual_power"),
            Map.entry("increase_sp_recovery_priest", "priest/priest_increase_spiritual_power"),
            Map.entry("improve_dodge", "thief/thief_increase_dodge"),
            Map.entry("improve_concentration", "archer/archer_attention_concentrate"),
            Map.entry("owls_eye", "archer/archer_owl_s_eye"),
            Map.entry("vultures_eye", "archer/archer_vulture_s_eye"),
            Map.entry("decrease_agi", "acolyte/acolyte_decrease_agility"),
            Map.entry("increase_agi", "acolyte/acolyte_increase_agility"),
            Map.entry("cloak", "assassin/assassin_cloaking"),
            Map.entry("lefthand_mastery", "assassin/assassin_left_hand_mastery"),
            Map.entry("righthand_mastery", "assassin/assassin_right_hand_mastery"),
            Map.entry("over_thrust", "blacksmith/blacksmith_power_thrust"),
            Map.entry("status_recovery", "priest/priest_recovery"),
            Map.entry("heavens_drive", "wizard/wizard_heaven_s_drive"),
            Map.entry("thunder_storm", "mage/mage_thunder_storm"));

    private static final Map<String, Optional<ResourceLocation>> CACHE = new HashMap<>();
    private static final Set<String> WARNED_UNRESOLVED = new HashSet<>();

    private SkillIconResolver() {
    }

    public static ResourceLocation resolveSkillTexture(ISkillDefinition definition) {
        if (definition == null) {
            return null;
        }

        List<String> candidates = new ArrayList<>();
        addCandidate(candidates, definition.getTextureName());
        addAliasCandidate(candidates, definition.getTextureName());
        addCandidate(candidates, definition.getId().getPath());
        addAliasCandidate(candidates, definition.getId().getPath());
        return resolve("skill:" + definition.getId(), candidates);
    }

    public static ResourceLocation resolveLifeSkillTexture(LifeSkillType type) {
        if (type == null) {
            return null;
        }

        List<String> candidates = new ArrayList<>();
        ResourceLocation skillId = ResourceLocation.fromNamespaceAndPath("ragnarmmo", type.getId());
        SkillRegistry.get(skillId).ifPresent(def -> {
            addCandidate(candidates, def.getTextureName());
            addAliasCandidate(candidates, def.getTextureName());
        });
        addCandidate(candidates, "lifeskill/" + type.getTextureName());
        addCandidate(candidates, type.getTextureName());
        addAliasCandidate(candidates, type.getTextureName());
        return resolve("life:" + type.getId(), candidates);
    }

    public static String getFallbackLabel(ISkillDefinition definition) {
        if (definition == null) {
            return "?";
        }
        return abbreviate(definition.getId().getPath());
    }

    public static String getFallbackLabel(LifeSkillType type) {
        if (type == null) {
            return "?";
        }
        return abbreviate(type.getId());
    }

    private static ResourceLocation resolve(String cacheKey, List<String> candidates) {
        Optional<ResourceLocation> cached = CACHE.get(cacheKey);
        if (cached != null) {
            return cached.orElse(null);
        }

        ResourceLocation resolved = null;
        for (String candidate : candidates) {
            ResourceLocation texture = toTextureLocation(candidate);
            if (texture != null && exists(texture)) {
                resolved = texture;
                break;
            }
        }

        CACHE.put(cacheKey, Optional.ofNullable(resolved));

        if (resolved == null && WARNED_UNRESOLVED.add(cacheKey)) {
            LOGGER.warn("Skill icon not found for {}. Candidates={}", cacheKey, candidates);
        }

        return resolved;
    }

    private static void addCandidate(List<String> candidates, String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return;
        }
        if (!candidates.contains(candidate)) {
            candidates.add(candidate);
        }
    }

    private static void addAliasCandidate(List<String> candidates, String source) {
        if (source == null || source.isBlank()) {
            return;
        }
        String alias = LEGACY_TEXTURE_ALIASES.get(source);
        if (alias != null && !candidates.contains(alias)) {
            candidates.add(alias);
        }
    }

    private static ResourceLocation toTextureLocation(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        return ResourceLocation.fromNamespaceAndPath("ragnarmmo", "textures/gui/skills/" + path + ".png");
    }

    private static boolean exists(ResourceLocation texture) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getResourceManager() == null) {
            return false;
        }
        return minecraft.getResourceManager().getResource(texture).isPresent();
    }

    private static String abbreviate(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            return "?";
        }

        String normalized = rawId.toUpperCase(Locale.ROOT).replace('-', '_');
        String[] parts = normalized.split("_+");
        StringBuilder result = new StringBuilder(2);
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            result.append(part.charAt(0));
            if (result.length() >= 2) {
                break;
            }
        }

        if (result.length() == 0) {
            return normalized.substring(0, Math.min(2, normalized.length()));
        }

        if (result.length() == 1 && normalized.length() > 1) {
            result.append(normalized.charAt(1));
        }

        return result.toString();
    }
}

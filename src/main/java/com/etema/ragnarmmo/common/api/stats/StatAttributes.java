package com.etema.ragnarmmo.common.api.stats;

import com.etema.ragnarmmo.common.init.RagnarCore;
import com.etema.ragnarmmo.common.api.attributes.RagnarAttributes;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public final class StatAttributes {
    private static final Map<StatKeys, VanillaBinding> VANILLA_BINDINGS = new EnumMap<>(StatKeys.class);

    static {
        registerVanillaBinding(StatKeys.STR,
                new VanillaBinding(() -> Attributes.ATTACK_DAMAGE, AttributeModifier.Operation.ADDITION,
                        0.5D, 0.0D));
        registerVanillaBinding(StatKeys.AGI,
                new VanillaBinding(() -> Attributes.MOVEMENT_SPEED, AttributeModifier.Operation.MULTIPLY_TOTAL,
                        0.0025D, 0.0D));
        registerVanillaBinding(StatKeys.INT,
                new VanillaBinding(() -> Attributes.KNOCKBACK_RESISTANCE, AttributeModifier.Operation.ADDITION,
                        0.005D, 0.0D));
        registerVanillaBinding(StatKeys.DEX,
                new VanillaBinding(() -> Attributes.ATTACK_SPEED, AttributeModifier.Operation.ADDITION,
                        0.015D, 0.0D));
        registerVanillaBinding(StatKeys.LUK,
                new VanillaBinding(() -> Attributes.LUCK, AttributeModifier.Operation.ADDITION,
                        0.1D, 0.0D));
    }

    private StatAttributes() {
    }

    public static Attribute getAttribute(StatKeys key) {
        return switch (key) {
            case STR -> RagnarAttributes.STR.get();
            case AGI -> RagnarAttributes.AGI.get();
            case VIT -> RagnarAttributes.VIT.get();
            case INT -> RagnarAttributes.INT.get();
            case DEX -> RagnarAttributes.DEX.get();
            case LUK -> RagnarAttributes.LUK.get();
        };
    }

    public static AttributeModifier additiveModifier(StatKeys key, String source, double amount) {
        UUID id = UUID.nameUUIDFromBytes((RagnarCore.MODID + ":" + source + ":" + key.name())
                .getBytes(StandardCharsets.UTF_8));
        return new AttributeModifier(id, source + "_" + key.name().toLowerCase(), amount,
                AttributeModifier.Operation.ADDITION);
    }

    public static double getTotal(Player player, StatKeys key) {
        AttributeInstance instance = resolveInstance(player, getAttribute(key));
        return instance != null ? instance.getValue() : 0.0;
    }

    public static double getBase(Player player, StatKeys key) {
        AttributeInstance instance = resolveInstance(player, getAttribute(key));
        return instance != null ? instance.getBaseValue() : 0.0;
    }

    public static void registerVanillaBinding(StatKeys key, VanillaBinding binding) {
        if (binding == null) {
            VANILLA_BINDINGS.remove(key);
        } else {
            VANILLA_BINDINGS.put(key, binding);
        }
    }

    public static void synchronizeVanillaAttributes(Player player) {
        if (player == null) {
            return;
        }

        // Always sync vanilla attributes (hardcoded enabled)
        if (false) { // Disabled - always sync
            clearVanillaAttributes(player);
            return;
        }

        for (Map.Entry<StatKeys, VanillaBinding> entry : VANILLA_BINDINGS.entrySet()) {
            applyBinding(player, entry.getKey(), entry.getValue());
        }
    }

    public static void clearVanillaAttributes(Player player) {
        if (player == null) {
            return;
        }
        for (Map.Entry<StatKeys, VanillaBinding> entry : VANILLA_BINDINGS.entrySet()) {
            removeBinding(player, entry.getKey(), entry.getValue());
        }
    }

    private static void applyBinding(Player player, StatKeys key, VanillaBinding binding) {
        Attribute attribute = binding.attribute();
        if (attribute == null) {
            return;
        }
        AttributeInstance instance = resolveInstance(player, attribute);
        if (instance == null) {
            return;
        }
        UUID id = binding.modifierId(key);
        instance.removeModifier(id);

        double statValue = getTotal(player, key);
        double amount = binding.computeAmount(statValue);
        if (amount == 0.0D) {
            return;
        }
        AttributeModifier modifier = new AttributeModifier(id, binding.modifierName(key), amount, binding.operation());
        instance.addTransientModifier(modifier);
    }

    private static void removeBinding(Player player, StatKeys key, VanillaBinding binding) {
        Attribute attribute = binding.attribute();
        if (attribute == null) {
            return;
        }
        AttributeInstance instance = resolveInstance(player, attribute);
        if (instance != null) {
            instance.removeModifier(binding.modifierId(key));
        }
    }

    private static AttributeInstance resolveInstance(Player player, Attribute attribute) {
        if (player == null || attribute == null) {
            return null;
        }
        return player.getAttribute(attribute);
    }

    public record VanillaBinding(Supplier<Attribute> attributeSupplier,
            AttributeModifier.Operation operation,
            double scale,
            double baseOffset) {

        public VanillaBinding {
            Objects.requireNonNull(attributeSupplier, "attributeSupplier");
            Objects.requireNonNull(operation, "operation");
        }

        public Attribute attribute() {
            return attributeSupplier.get();
        }

        public UUID modifierId(StatKeys key) {
            String keyStr = attribute() != null ? attribute().getDescriptionId() : "unknown";
            return UUID.nameUUIDFromBytes((RagnarCore.MODID + ":vanilla:" + keyStr + ":" + key.name())
                    .getBytes(StandardCharsets.UTF_8));
        }

        public String modifierName(StatKeys key) {
            return RagnarCore.MODID + "_" + key.name().toLowerCase() + "_bridge";
        }

        public double computeAmount(double statValue) {
            return baseOffset + (statValue * scale);
        }
    }
}

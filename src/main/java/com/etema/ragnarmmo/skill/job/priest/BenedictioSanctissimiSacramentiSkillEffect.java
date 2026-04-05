package com.etema.ragnarmmo.skill.job.priest;

import com.etema.ragnarmmo.skill.api.ISkillEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Benedictio Sanctissimi Sacramenti — Active (Priest)
 * RO: Enchants allies' weapons with Holy element temporarily.
 * MC: Grants REGENERATION II + ABSORPTION I to all allied players within 8 blocks
 *     for 30s. Requires Holy Water (from Aqua Benedicta) or standing in water.
 *     All affected players are notified.
 */
public class BenedictioSanctissimiSacramentiSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "benedictio_sanctissimi_sacramenti");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;

        // Require Holy Water or water
        boolean hasHolyWater = hasHolyWaterInInventory(player);
        boolean inWater = player.isInWater();

        if (!hasHolyWater && !inWater) {
            player.sendSystemMessage(Component.literal(
                    "§cBenedictio: §fRequiere Holy Water (Aqua Benedicta) o estar en agua."));
            return;
        }

        // Consume holy water if present
        if (hasHolyWater) consumeHolyWater(player);

        int durationTicks = 30 * 20;
        AABB box = player.getBoundingBox().inflate(8.0);
        List<Player> allies = player.level().getEntitiesOfClass(Player.class, box, p -> true);

        for (Player ally : allies) {
            ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, durationTicks, 1));
            ally.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, durationTicks, 0));
            if (ally != player) {
                ally.sendSystemMessage(Component.literal("§b✦ Benedictio §fde " + player.getName().getString() + " te bendice."));
            }
        }

        if (player.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.ENCHANT,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    40, 1.0, 0.5, 1.0, 0.1);
            sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0f, 1.2f);
        }

        player.sendSystemMessage(Component.literal(
                "§b✦ Benedictio §faplicado a " + allies.size() + " aliado(s) por 30s."));
    }

    private boolean hasHolyWaterInInventory(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            var stack = player.getInventory().getItem(i);
            if (stack.is(Items.POTION) && stack.getOrCreateTag().getBoolean("holy_water")) return true;
        }
        return false;
    }

    private void consumeHolyWater(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            var stack = player.getInventory().getItem(i);
            if (stack.is(Items.POTION) && stack.getOrCreateTag().getBoolean("holy_water")) {
                stack.shrink(1);
                return;
            }
        }
    }
}

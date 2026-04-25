package com.etema.ragnarmmo.skills.job.assassin;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.AxeItem;

/**
 * Dual Wield — Passive (Assassin)
 * RO: Allows equipping weapons in both hands.
 * MC: When a weapon is in BOTH main hand and offhand, stores the offhand
 *     weapon info in PersistentData. AssassinSkillEvents hooks LivingHurtEvent
 *     to add an extra hit from the offhand at (20 + level * 10)% base damage.
 */
public class DualWieldSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "dual_wield");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void execute(ServerPlayer player, int level) {
        ItemStack main = player.getMainHandItem();
        ItemStack offhand = player.getOffhandItem();

        boolean mainWeapon = main.getItem() instanceof SwordItem || main.getItem() instanceof AxeItem;
        boolean offWeapon  = offhand.getItem() instanceof SwordItem || offhand.getItem() instanceof AxeItem;

        player.getPersistentData().putInt("dual_wield_level", level);

        if (level > 0) {
            int dmgPct = 20 + level * 10;
            String status = (mainWeapon && offWeapon)
                    ? "§a[Activo - ambas manos armadas]"
                    : "§7[Requiere arma en ambas manos]";
            player.sendSystemMessage(Component.literal(
                    "§4✦ Dual Wield §flv." + level + " — Daño offhand +" + dmgPct + "% " + status));
        }
    }
}

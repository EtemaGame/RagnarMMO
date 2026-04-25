package com.etema.ragnarmmo.achievements.network;

import com.etema.ragnarmmo.achievements.data.AchievementCategory;
import com.etema.ragnarmmo.achievements.data.AchievementDefinition;
import com.etema.ragnarmmo.achievements.data.AchievementRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

public class SyncAchievementDefinitionsPacket {
    private final Collection<AchievementDefinition> definitions;

    public SyncAchievementDefinitionsPacket(Collection<AchievementDefinition> definitions) {
        this.definitions = definitions;
    }

    public SyncAchievementDefinitionsPacket(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        this.definitions = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            definitions.add(decodeDefinition(buf));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(definitions.size());
        for (AchievementDefinition def : definitions) {
            encodeDefinition(buf, def);
        }
    }

    private void encodeDefinition(FriendlyByteBuf buf, AchievementDefinition def) {
        buf.writeUtf(def.id());
        buf.writeEnum(def.category());
        buf.writeUtf(def.name());
        buf.writeUtf(def.description());
        buf.writeUtf(def.triggerType());
        buf.writeNullable(def.triggerId(), FriendlyByteBuf::writeUtf);
        buf.writeVarInt(def.requiredAmount());
        buf.writeVarInt(def.points());
        buf.writeNullable(def.title(), FriendlyByteBuf::writeUtf);
        buf.writeMap(def.rewards(), FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeVarInt);
    }

    private AchievementDefinition decodeDefinition(FriendlyByteBuf buf) {
        String id = buf.readUtf();
        AchievementCategory category = buf.readEnum(AchievementCategory.class);
        String name = buf.readUtf();
        String description = buf.readUtf();
        String triggerType = buf.readUtf();
        String triggerId = buf.readNullable(FriendlyByteBuf::readUtf);
        int requiredAmount = buf.readVarInt();
        int points = buf.readVarInt();
        String title = buf.readNullable(FriendlyByteBuf::readUtf);
        Map<String, Integer> rewards = buf.readMap(FriendlyByteBuf::readUtf, FriendlyByteBuf::readVarInt);

        return new AchievementDefinition(id, category, name, description, triggerType, triggerId, requiredAmount, points, title, rewards);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            AchievementRegistry.applySync(definitions);
        });
        context.get().setPacketHandled(true);
    }
}

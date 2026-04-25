package com.etema.ragnarmmo.skills.net;

import com.etema.ragnarmmo.skills.data.tree.SkillNode;
import com.etema.ragnarmmo.skills.data.tree.SkillTreeDefinition;
import com.etema.ragnarmmo.skills.data.tree.SkillTreeRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

public class SyncSkillTreesPacket {
    private final Collection<SkillTreeDefinition> trees;

    public SyncSkillTreesPacket(Collection<SkillTreeDefinition> trees) {
        this.trees = trees;
    }

    public SyncSkillTreesPacket(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        this.trees = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            trees.add(decodeTree(buf));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(trees.size());
        for (SkillTreeDefinition tree : trees) {
            encodeTree(buf, tree);
        }
    }

    private void encodeTree(FriendlyByteBuf buf, SkillTreeDefinition tree) {
        buf.writeResourceLocation(tree.getId());
        buf.writeUtf(tree.getJob());
        buf.writeVarInt(tree.getTier());
        buf.writeVarInt(tree.getGridWidth());
        buf.writeVarInt(tree.getGridHeight());

        buf.writeCollection(tree.getInheritFrom(), FriendlyByteBuf::writeResourceLocation);
        buf.writeCollection(tree.getSkills(), (b, node) -> {
            b.writeResourceLocation(node.getSkillId());
            b.writeVarInt(node.getGridX());
            b.writeVarInt(node.getGridY());
        });
    }

    private SkillTreeDefinition decodeTree(FriendlyByteBuf buf) {
        SkillTreeDefinition.Builder builder = SkillTreeDefinition.builder(buf.readResourceLocation());
        builder.job(buf.readUtf());
        builder.tier(buf.readVarInt());
        builder.gridSize(buf.readVarInt(), buf.readVarInt());

        List<ResourceLocation> inherits = buf.readCollection(ArrayList::new, FriendlyByteBuf::readResourceLocation);
        inherits.forEach(builder::inheritFrom);

        List<SkillNode> nodes = buf.readCollection(ArrayList::new, b -> 
            new SkillNode(b.readResourceLocation(), b.readVarInt(), b.readVarInt()));
        nodes.forEach(builder::addSkill);

        return builder.build();
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            SkillTreeRegistry.applySync(trees);
        });
        context.get().setPacketHandled(true);
    }
}

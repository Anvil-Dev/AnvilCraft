package dev.dubhe.anvilcraft.api.portal;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.util.Util;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Portal;

import java.util.Objects;

@Getter
public class PortalType {
    public static final PortalType NETHER_PORTAL = PortalType.assume(Blocks.NETHER_PORTAL);
    public static final PortalType END_PORTAL = PortalType.assume(Blocks.END_PORTAL);
    public static final PortalType END_GATEWAY = PortalType.assume(Blocks.END_GATEWAY);
    public static final MapCodec<PortalType> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
        ResourceLocation.CODEC
            .fieldOf("portal")
            .forGetter(PortalType::getId)
    ).apply(inst, PortalType::new));
    public static final StreamCodec<ByteBuf, PortalType> STREAM_CODEC = StreamCodec.composite(
        ResourceLocation.STREAM_CODEC,
        PortalType::getId,
        PortalType::new
    );
    private final ResourceLocation id;
    private final Portal portal;

    public PortalType(ResourceLocation id) {
        this.id = id;
        Block block = BuiltInRegistries.BLOCK.get(id);
        if (!(block instanceof Portal portal1)) {
            throw new IllegalArgumentException("Block '" + id + "' is not a portal");
        }
        this.portal = portal1;
    }

    @SuppressWarnings("deprecation")
    public <T extends Block & Portal> PortalType(T portal) {
        this.id = portal.builtInRegistryHolder().key().location();
        this.portal = portal;
    }

    public static PortalType assume(Block portal) {
        var casted = PortalType.cast(portal);
        return new PortalType(casted);
    }

    public static PortalType assume(Portal portal) {
        var casted = PortalType.cast(portal);
        return new PortalType(casted);
    }

    private static <T extends Block & Portal> T cast(Block portal) {
        return Util.cast(portal);
    }

    private static <T extends Block & Portal> T cast(Portal portal) {
        return Util.cast(portal);
    }

    public Component getPortalName() {
        return Util.<Block>cast(this.portal).getName();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PortalType type)) return false;
        return Objects.equals(this.getId(), type.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.getId());
    }
}

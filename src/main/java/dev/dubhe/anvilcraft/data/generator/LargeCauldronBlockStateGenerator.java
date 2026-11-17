package dev.dubhe.anvilcraft.data.generator;

import dev.anvilcraft.lib.v2.registrum.providers.DataGenContext;
import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumBlockModelGenerator;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.LargeCauldronBlock;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;

/**
 * 生成大炼药锅渲染器使用的双贴图火焰模型。
 */
public class LargeCauldronBlockStateGenerator {
    /**
     * 保留主方块的现有模型与方块状态，仅生成独立火焰模型。
     */
    public static void generate(DataGenContext<Block, LargeCauldronBlock> context, RegistrumBlockModelGenerator provider) {
        TextureSlot fire0 = TextureSlot.create("0");
        TextureSlot fire1 = TextureSlot.create("1");
        provider.getBuilder()
            .parent(AnvilCraft.of("block/fire_cauldron_fire4"))
            .texture(fire0, AnvilCraft.of("block/large_cauldron_fire_0"))
            .texture(fire1, AnvilCraft.of("block/large_cauldron_fire_1"))
            .texture(TextureSlot.PARTICLE, AnvilCraft.of("block/large_cauldron_fire_0"))
            .transformTemplate(template -> template
                .element(element -> element
                    .from(8, 15, 0.8F).to(8, 31, 15.2F)
                    .shade(false)
                    .rotation(rotation -> rotation.origin(8, 22, 8).singleAxis(Direction.Axis.Y, 45).rescale(true))
                    .face(Direction.EAST, face -> face.uvs(0, 0, 16, 16).texture(fire0))
                    .face(Direction.WEST, face -> face.uvs(0, 0, 16, 16).texture(fire0)))
                .element(element -> element
                    .from(0.8F, 15, 8).to(15.2F, 31, 8)
                    .shade(false)
                    .rotation(rotation -> rotation.origin(8, 22, 8).singleAxis(Direction.Axis.Y, 45).rescale(true))
                    .face(Direction.NORTH, face -> face.uvs(0, 0, 16, 16).texture(fire1))
                    .face(Direction.SOUTH, face -> face.uvs(0, 0, 16, 16).texture(fire1))))
            .build(AnvilCraft.of("block/large_cauldron_fire"));
    }
}

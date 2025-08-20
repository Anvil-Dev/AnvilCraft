package dev.dubhe.anvilcraft.recipe.anvil.outcome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.reicpe.ModRecipeOutcomeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.util.InWorldRecipeContext;
import lombok.Getter;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

@Getter
public class ProduceExplosion implements IRecipeOutcome<ProduceExplosion> {
    /**
     * 爆炸中心点的位置偏移量
     */
    private final Vec3 offset;
    /**
     * 爆炸威力
     */
    private final float power;
    /**
     * 是否产生火焰
     */
    private final boolean fire;
    /**
     * 爆炸的方块交互类型
     */
    private final Level.ExplosionInteraction explodeInteraction;
    /**
     * 爆炸发生的概率，范围为0f到1.0f
     */
    private final float chanceValue;

    /**
     * 构建一个新的产生爆炸配方结果
     *
     * @param offset             爆炸中心点的位置偏移量
     * @param power              爆炸威力
     * @param fire               是否产生火焰
     * @param explodeInteraction 爆炸的方块交互类型
     * @param chance             爆炸发生的概率
     */
    public ProduceExplosion(Vec3 offset, float power, boolean fire, Level.ExplosionInteraction explodeInteraction, float chance) {
        this.offset = offset;
        this.power = power;
        this.fire = fire;
        this.explodeInteraction = explodeInteraction;
        this.chanceValue = chance;
    }

    /**
     * 获取配方结果类型
     *
     * @return 配方结果类型
     */
    @Override
    public Type getType() {
        return ModRecipeOutcomeTypes.PRODUCE_EXPLOSION.get();
    }

    @Override
    public NumberProvider getChance() {
        return ConstantValue.exactly(this.chanceValue);
    }

    /**
     * 接受配方上下文并爆炸
     *
     * @param ctx 配方上下文
     */
    @Override
    public void accept(InWorldRecipeContext ctx) {
        ServerLevel level = ctx.getLevel();
        Vec3 ctr = ctx.getPos().add(this.offset);
        level.explode(null, ctr.x(), ctr.y(), ctr.z(), this.power, this.fire, this.explodeInteraction);
    }

    public static class Type implements IRecipeOutcome.Type<ProduceExplosion> {
        /**
         * 编解码器
         */
        public static final Codec<ProduceExplosion> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Vec3.CODEC.fieldOf("offset").forGetter(ProduceExplosion::getOffset),
            Codec.FLOAT.fieldOf("power").forGetter(ProduceExplosion::getPower),
            Codec.BOOL.fieldOf("fire").forGetter(ProduceExplosion::isFire),
            Level.ExplosionInteraction.CODEC.fieldOf("interact").forGetter(ProduceExplosion::getExplodeInteraction),
            Codec.FLOAT.fieldOf("chance").forGetter(ProduceExplosion::getChanceValue)
        ).apply(ins, ProduceExplosion::new));

        public static final MapCodec<ProduceExplosion> MAP_CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
            Vec3.CODEC.fieldOf("offset").forGetter(ProduceExplosion::getOffset),
            Codec.FLOAT.fieldOf("power").forGetter(ProduceExplosion::getPower),
            Codec.BOOL.fieldOf("fire").forGetter(ProduceExplosion::isFire),
            Level.ExplosionInteraction.CODEC.fieldOf("interact").forGetter(ProduceExplosion::getExplodeInteraction),
            Codec.FLOAT.fieldOf("chance").forGetter(ProduceExplosion::getChanceValue)
        ).apply(ins, ProduceExplosion::new));

        /**
         * 流编解码器
         */
        public static final StreamCodec<RegistryFriendlyByteBuf, ProduceExplosion> STREAM_CODEC = StreamCodec.of(
            (buf, recipe) -> buf.writeNbt(intoTag(recipe)),
            friendlyByteBuf -> fromTag(friendlyByteBuf.readNbt())
        );

        /**
         * 解码
         *
         * @param tag nbt形式的编码数据
         * @return 解码的对象
         */
        public static ProduceExplosion fromTag(Tag tag) {
            return CODEC.decode(NbtOps.INSTANCE, tag).getOrThrow().getFirst();
        }

        /**
         * 编码
         *
         * @param explosion 对象
         * @return 编码成nbt形式的数据
         */
        public static Tag intoTag(ProduceExplosion explosion) {
            return CODEC.encodeStart(NbtOps.INSTANCE, explosion).getOrThrow();
        }

        /**
         * 获取MapCodec编解码器
         *
         * @return MapCodec编解码器
         */
        @Override
        public @NotNull MapCodec<ProduceExplosion> codec() {
            return ProduceExplosion.Type.MAP_CODEC;
        }

        /**
         * 获取StreamCodec编解码器
         *
         * @return StreamCodec编解码器
         */
        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, ProduceExplosion> streamCodec() {
            return ProduceExplosion.Type.STREAM_CODEC;
        }
    }

}

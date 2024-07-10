package dev.dubhe.anvilcraft.data.recipe.anvil.predicate;

import com.google.common.base.Predicates;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilCraftingContext;
import dev.dubhe.anvilcraft.data.recipe.anvil.RecipePredicate;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AzaleaBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.MangroveLeavesBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class HasItemLeaves implements RecipePredicate {

    @Getter
    private final String type = "has_item_leaves";
    private final Vec3 inputOffset;
    private final Vec3 outputOffset;
    private final double leavesChance;
    private final double saplingChance;
    private ItemEntity inputItemEntity;
    private ItemStack leaves;
    private ItemStack sapling;

    /**
     * 拥有树叶物品
     *
     * @param inputOffset 输入偏移
     * @param outputOffset 输出偏移
     * @param leavesChance 输出树叶几率
     * @param saplingChance 输出树苗几率
     */
    public HasItemLeaves(Vec3 inputOffset, Vec3 outputOffset, double leavesChance, double saplingChance) {
        this.inputOffset = inputOffset;
        this.outputOffset = outputOffset;
        this.leavesChance = leavesChance;
        this.saplingChance = saplingChance;
    }

    /**
     * 拥有树叶物品
     *
     * @param serializedRecipe 序列化配方
     */
    public HasItemLeaves(JsonObject serializedRecipe) {
        JsonArray iarray = GsonHelper.getAsJsonArray(serializedRecipe, "input_offset");
        double[] ivec3 = {0.0d, 0.0d, 0.0d};
        for (int i = 0; i < iarray.size() && i < 3; i++) {
            JsonElement element = iarray.get(i);
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
                ivec3[i] = element.getAsDouble();
            } else throw new JsonSyntaxException("Expected offset to be a Double, was " + GsonHelper.getType(element));
        }
        this.inputOffset = new Vec3(ivec3[0], ivec3[1], ivec3[2]);

        JsonArray oarray = GsonHelper.getAsJsonArray(serializedRecipe, "output_offset");
        double[] ovec3 = {0.0d, 0.0d, 0.0d};
        for (int i = 0; i < oarray.size() && i < 3; i++) {
            JsonElement element = oarray.get(i);
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
                ovec3[i] = element.getAsDouble();
            } else throw new JsonSyntaxException("Expected offset to be a Double, was " + GsonHelper.getType(element));
        }
        this.outputOffset = new Vec3(ovec3[0], ovec3[1], ovec3[2]);
        if (serializedRecipe.has("leaves_chance")) {
            this.leavesChance = GsonHelper.getAsDouble(serializedRecipe, "leaves_chance");
        } else this.leavesChance = 1.0;
        if (serializedRecipe.has("sapling_chance")) {
            this.saplingChance = GsonHelper.getAsDouble(serializedRecipe, "sapling_chance");
        } else this.saplingChance = 1.0;
    }

    /**
     * @param buffer 缓冲区
     */
    public HasItemLeaves(@NotNull FriendlyByteBuf buffer) {
        this.inputOffset = new Vec3(buffer.readVector3f());
        this.outputOffset = new Vec3(buffer.readVector3f());
        this.leavesChance = buffer.readDouble();
        this.saplingChance = buffer.readDouble();
    }

    @Override
    public boolean matches(@NotNull AnvilCraftingContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getPos();
        AABB aabb = new AABB(pos).move(this.inputOffset);
        List<ItemEntity> entities =
            level.getEntities(EntityTypeTest.forClass(ItemEntity.class), aabb, Predicates.alwaysTrue());
        for (ItemEntity entity : entities) {
            ItemStack itemStack = entity.getItem();
            if (itemStack.getItem() instanceof BlockItem blockItem) {
                if (blockItem.getBlock() instanceof LeavesBlock leavesBlock) {
                    ItemStack sapling = getSapling(level, leavesBlock, pos);
                    if  (!sapling.isEmpty()) {
                        this.leaves = itemStack.copyWithCount(1);
                        this.sapling = sapling;
                        this.inputItemEntity = entity;
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean process(AnvilCraftingContext context) {
        ItemStack item = this.inputItemEntity.getItem();
        item.shrink(1);
        this.inputItemEntity.setItem(item.copy());
        return spawnResult(context);
    }

    private boolean spawnResult(AnvilCraftingContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getPos();
        Vec3 vec3 = pos.getCenter().add(this.outputOffset);
        RandomSource random = level.getRandom();
        boolean b = true;
        boolean b1 = true;
        if (random.nextDouble() <= this.leavesChance) {
            b = level.addFreshEntity(new ItemEntity(level, vec3.x, vec3.y, vec3.z, this.leaves, 0.0d, 0.0d, 0.0d));
        }
        if (random.nextDouble() <= this.saplingChance) {
            b1 = level.addFreshEntity(new ItemEntity(level, vec3.x, vec3.y, vec3.z, this.sapling, 0.0d, 0.0d, 0.0d));
        }
        return b && b1;
    }

    private ItemStack getSapling(Level level, LeavesBlock leavesBlock, BlockPos pos) {
        if (leavesBlock instanceof MangroveLeavesBlock) return new ItemStack(Items.MANGROVE_PROPAGULE);
        if (!(level instanceof ServerLevel serverLevel)) return ItemStack.EMPTY;
        ItemStack result = ItemStack.EMPTY;
        getResult:
        for (int i = 0; i < 100; i++) {
            List<ItemStack> list = Block.getDrops(
                    leavesBlock.defaultBlockState(),
                    serverLevel,
                    pos,
                    null,
                    null,
                    createFortuneItem()
            );
            for (ItemStack itemStack : list) {
                if (itemStack.getItem() instanceof BlockItem item) {
                    Block block = item.getBlock();
                    if (block instanceof SaplingBlock || block instanceof AzaleaBlock) {
                        result = itemStack;
                        break getResult;
                    }
                }
            }
        }
        return result;
    }

    private ItemStack createFortuneItem() {
        ItemStack tool = new ItemStack(Items.DIAMOND_HOE);
        CompoundTag tag = tool.getOrCreateTag();
        tag.put("Enchantments", new ListTag());
        ListTag listTag = tag.getList("Enchantments", 10);
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.putString("id", String.valueOf(EnchantmentHelper.getEnchantmentId(Enchantments.BLOCK_FORTUNE)));
        compoundTag.putShort("lvl", (short) 32767);
        listTag.add(compoundTag);
        return tool;
    }

    @Override
    public void toNetwork(@NotNull FriendlyByteBuf buffer) {
        buffer.writeUtf(this.getType());
        buffer.writeVector3f(this.inputOffset.toVector3f());
        buffer.writeVector3f(this.outputOffset.toVector3f());
        buffer.writeDouble(this.leavesChance);
        buffer.writeDouble(this.saplingChance);
    }

    @Override
    public @NotNull JsonElement toJson() {
        double[] ivec3 = {this.inputOffset.x(), this.inputOffset.y(), this.inputOffset.z()};
        JsonArray ioffset = new JsonArray();
        for (double v : ivec3) {
            ioffset.add(new JsonPrimitive(v));
        }

        double[] ovec3 = {this.outputOffset.x(), this.outputOffset.y(), this.outputOffset.z()};
        JsonArray ooffset = new JsonArray();
        for (double v : ovec3) {
            ooffset.add(new JsonPrimitive(v));
        }

        JsonObject object = new JsonObject();
        object.addProperty("type", this.getType());
        object.add("input_offset", ioffset);
        object.add("output_offset", ooffset);
        object.addProperty("leaves_chance", this.leavesChance);
        object.addProperty("sapling_chance", this.saplingChance);

        return object;
    }
}

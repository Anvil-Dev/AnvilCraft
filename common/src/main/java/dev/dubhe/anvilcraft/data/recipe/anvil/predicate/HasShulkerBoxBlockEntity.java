package dev.dubhe.anvilcraft.data.recipe.anvil.predicate;

import com.google.gson.*;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilCraftingContext;
import dev.dubhe.anvilcraft.data.recipe.anvil.RecipePredicate;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

@Getter
public class HasShulkerBoxBlockEntity implements RecipePredicate {

    private final String type = "has_shulker_box_block_entity";
    private final Vec3 offset;
    private final HasBlock.ModBlockPredicate matchBlock;
    /**
     * 是否进行以及进行何种物品栏检测
     * NO_INVENTORY_CHECK: 不检测
     * HAS_ITEM_COUNT: 有至少/至多一定数量的某种物品
     * IS_EMPTY: 物品栏为空
     */
    private final String inventoryCheck;
    private final HasItem.ModItemPredicate matchItem;

    public static final String NO_INVENTORY_CHECK = "NO_INVENTORY_CHECK";
    public static final String HAS_ITEM_COUNT = "HAS_ITEM_COUNT";
    public static final String IS_EMPTY = "IS_EMPTY";


    public HasShulkerBoxBlockEntity(Vec3 offset, HasBlock.ModBlockPredicate matchBlock, String inventoryCheck, HasItem.ModItemPredicate matchItem) {
        this.offset = offset;
        this.matchBlock = matchBlock;
        this.inventoryCheck = inventoryCheck;
        this.matchItem = matchItem;
    }

    /**
     * 拥有方块和潜影盒方块实体
     * 可以追加检测方块实体的物品栏
     * @param serializedRecipe 序列化配方
     */
    public HasShulkerBoxBlockEntity(JsonObject serializedRecipe) {
        JsonArray array = GsonHelper.getAsJsonArray(serializedRecipe, "offset");
        double[] vec3 = {0.0d, 0.0d, 0.0d};
        for (int i = 0; i < array.size() && i < 3; i++) {
            JsonElement element = array.get(i);
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
                vec3[i] = element.getAsDouble();
            } else throw new JsonSyntaxException("Expected offset to be a Double, was " + GsonHelper.getType(element));
        }
        this.offset = new Vec3(vec3[0], vec3[1], vec3[2]);
        if (!serializedRecipe.has("match_block")) throw new JsonSyntaxException("Missing match_block");
        this.matchBlock = HasBlock.ModBlockPredicate.fromJson(serializedRecipe.get("match_block"));
        if (!serializedRecipe.has("inventory_check")) {
            this.inventoryCheck = NO_INVENTORY_CHECK;
        }
        else this.inventoryCheck = serializedRecipe.get("inventory_check").getAsString();
        if (serializedRecipe.has("inventory_check")){
            if (!serializedRecipe.has("match_item")){
                throw new JsonSyntaxException("Missing match_item");
            }
            else this.matchItem = HasItem.ModItemPredicate.fromJson(serializedRecipe.get("match_item"));
        }
        else this.matchItem = HasItem.ModItemPredicate.of();
    }

    public HasShulkerBoxBlockEntity(@NotNull FriendlyByteBuf buffer) {
        this.offset = new Vec3(buffer.readVector3f());
        this.matchBlock = HasBlock.ModBlockPredicate.fromJson(AnvilCraft.GSON.fromJson(buffer.readUtf(), JsonElement.class));
        this.inventoryCheck = buffer.readUtf();
        this.matchItem = HasItem.ModItemPredicate.fromJson(AnvilCraft.GSON.fromJson(buffer.readUtf(), JsonElement.class));
    }

    @Override
    public boolean matches(AnvilCraftingContext context) {
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel level1)) return false;
        BlockPos pos = context.getPos();
        Vec3 vec3 = pos.getCenter().add(this.offset);
        BlockPos blockPos = BlockPos.containing(vec3.x, vec3.y, vec3.z);
        if(! this.matchBlock.matches(level1, blockPos)) return false;
        //below checks block entity
        BlockEntity blockEntity = null;
        //blockEntity = level1.getBlockEntity(blockPos);
        blockEntity = FindBlockEntity(level1, blockPos);
        if(blockEntity==null) return false;
        //this predicate only deals with shulker boxes
        if (!(blockEntity instanceof ShulkerBoxBlockEntity shulkerBlockEntity)) return false;
        switch (this.inventoryCheck) {
            case NO_INVENTORY_CHECK -> {
                return true;
            }
            //below checks inventory
            case IS_EMPTY -> {
                return shulkerBlockEntity.isEmpty();
            }
            case HAS_ITEM_COUNT -> {
                int x = 0;
                for (Item i : this.matchItem.getItems()) {
                    x += shulkerBlockEntity.countItem(i);
                }
                return this.matchItem.count.matches(x);
            }
        }
        //if check inventory mode is unknown, taken as no inventory check
        return true;
    }

    @Override
    public boolean process(AnvilCraftingContext context) {
        return true;
    }

    @Override
    public void toNetwork(FriendlyByteBuf buffer) {
        buffer.writeUtf(this.getType());
        buffer.writeVector3f(this.offset.toVector3f());
        buffer.writeUtf(this.matchBlock.serializeToJson().toString());
        buffer.writeUtf(this.inventoryCheck);
        buffer.writeUtf(this.matchItem.serializeToJson().toString());
    }

    @Override
    public @NotNull JsonElement toJson() {
        double[] ivec3 = {this.offset.x(), this.offset.y(), this.offset.z()};
        JsonArray ioffset = new JsonArray();
        for (double v : ivec3) {
            ioffset.add(new JsonPrimitive(v));
        }

        JsonObject object = new JsonObject();
        object.addProperty("type", this.getType());
        object.add("offset", ioffset);
        object.add("match_block", this.matchBlock.serializeToJson());
        object.addProperty("inventory_check", this.inventoryCheck);
        object.add("match_item", this.matchItem.serializeToJson());

        return object;
    }

    private @Nullable BlockEntity FindBlockEntity(@NotNull ServerLevel level, @NotNull BlockPos pos){
        LevelChunk chunk = level.getChunkAt(pos);
        for (Map.Entry<BlockPos, BlockEntity> entry: chunk.getBlockEntities().entrySet()){
            if(entry.getKey().equals(pos)) return entry.getValue();
        }
        return null;
    }
}

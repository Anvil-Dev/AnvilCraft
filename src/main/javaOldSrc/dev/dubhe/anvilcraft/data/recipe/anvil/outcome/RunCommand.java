package dev.dubhe.anvilcraft.data.recipe.anvil.outcome;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilCraftingContext;
import dev.dubhe.anvilcraft.data.recipe.anvil.RecipeOutcome;
import lombok.Getter;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

@Getter
public class RunCommand implements RecipeOutcome {
    private final String type = "run_command";
    private final Vec3 offset;
    private final double chance;
    private final String command;

    /**
     * 运行命令
     *
     * @param offset  偏移
     * @param chance  几率
     * @param command 命令
     */
    public RunCommand(Vec3 offset, double chance, String command) {
        this.offset = offset;
        this.chance = chance;
        this.command = command;
    }

    /**
     * @param buffer 缓冲
     */
    public RunCommand(@NotNull FriendlyByteBuf buffer) {
        this.offset = new Vec3(buffer.readVector3f());
        this.chance = buffer.readDouble();
        this.command = buffer.readUtf();
    }

    /**
     * @param serializedRecipe json
     */
    public RunCommand(@NotNull JsonObject serializedRecipe) {
        double[] vec3 = {0.0d, 0.0d, 0.0d};
        if (serializedRecipe.has("offset")) {
            JsonArray array = GsonHelper.getAsJsonArray(serializedRecipe, "offset");
            for (int i = 0; i < array.size() && i < 3; i++) {
                JsonElement element = array.get(i);
                if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
                    vec3[i] = element.getAsDouble();
                } else
                    throw new JsonSyntaxException("Expected offset to be a Double, was " + GsonHelper.getType(element));
            }
        }
        this.offset = new Vec3(vec3[0], vec3[1], vec3[2]);
        if (serializedRecipe.has("chance")) {
            this.chance = GsonHelper.getAsDouble(serializedRecipe, "chance");
        } else this.chance = 1.0;
        this.command = GsonHelper.getAsString(serializedRecipe, "command");
    }

    @Override
    public boolean process(@NotNull AnvilCraftingContext context) {
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) return true;
        FallingBlockEntity entity = context.getEntity();
        CommandSourceStack stack = new CommandSourceStack(entity, this.offset,
            new Vec2(entity.getXRot(), entity.getYRot()),
            serverLevel,
            (level.getServer() instanceof DedicatedServer server) ? server.getProperties().functionPermissionLevel : 2,
            "Anvil", Component.literal("Anvil"), serverLevel.getServer(), entity
        );
        try {
            serverLevel.getServer().getCommands().getDispatcher().execute(this.command, stack);
        } catch (CommandSyntaxException e) {
            AnvilCraft.LOGGER.error(e.getMessage(), e);
        }
        return false;
    }

    @Override
    public void toNetwork(@NotNull FriendlyByteBuf buffer) {
        buffer.writeUtf(this.getType());
        buffer.writeVector3f(this.offset.toVector3f());
        buffer.writeDouble(this.chance);
        buffer.writeUtf(this.command);
    }

    @Override
    public JsonElement toJson() {
        double[] vec3 = {this.offset.x(), this.offset.y(), this.offset.z()};
        JsonArray offset = new JsonArray();
        for (double v : vec3) offset.add(new JsonPrimitive(v));
        JsonObject object = new JsonObject();
        object.addProperty("type", this.getType());
        object.add("offset", offset);
        object.addProperty("chance", this.chance);
        object.addProperty("command", this.command);
        return object;
    }
}

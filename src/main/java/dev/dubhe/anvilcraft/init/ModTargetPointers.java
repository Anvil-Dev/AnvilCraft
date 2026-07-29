package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.item.IBlockItem;
import dev.dubhe.anvilcraft.api.pointer.BlockItemHandlerPointer;
import dev.dubhe.anvilcraft.api.pointer.BlockPointer;
import dev.dubhe.anvilcraft.api.pointer.ITargetPointer;
import dev.dubhe.anvilcraft.api.pointer.ItemEntityPointer;
import net.minecraft.world.item.BlockItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModTargetPointers {
    private static final DeferredRegister<ITargetPointer.Type<?>> REGISTER = DeferredRegister.create(
        ModRegistries.TARGET_POINTER_TYPE_KEY,
        AnvilCraft.MOD_ID
    );

    public static final DeferredHolder<ITargetPointer.Type<?>, ItemEntityPointer.Type> ITEM_ENTITY = REGISTER.register(
        "item_entity",
        () -> new ItemEntityPointer.Type()
    );
    public static final DeferredHolder<ITargetPointer.Type<?>, ItemEntityPointer.Type> ITEM_ENTITY_ONLY_BLOCK_ITEM = REGISTER.register(
        "item_entity_only_block_item",
        () -> new ItemEntityPointer.Type(stack -> stack.getItem() instanceof BlockItem || stack.getItem() instanceof IBlockItem)
    );
    public static final DeferredHolder<ITargetPointer.Type<?>, BlockItemHandlerPointer.Type> BLOCK_ITEM_HANDLER = REGISTER.register(
        "block_item_handler",
        () -> new BlockItemHandlerPointer.Type(stack -> !stack.isEmpty())
    );
    public static final DeferredHolder<ITargetPointer.Type<?>, BlockItemHandlerPointer.Type> BLOCK_ITEM_HANDLER_ONLY_BLOCK_ITEM =
        REGISTER.register(
            "block_item_handler_only_block_item",
            () -> new BlockItemHandlerPointer.Type(stack -> stack.getItem() instanceof BlockItem || stack.getItem() instanceof IBlockItem)
        );
    public static final DeferredHolder<ITargetPointer.Type<?>, BlockPointer.Type> BLOCK = REGISTER.register(
        "block",
        BlockPointer.Type::new
    );

    public static void register(IEventBus modEventBus) {
        REGISTER.register(modEventBus);
    }
}

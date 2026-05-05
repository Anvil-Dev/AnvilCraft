package dev.dubhe.anvilcraft.mixin.accessor;

import net.neoforged.neoforge.transfer.StacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.ArrayList;

@Mixin(StacksResourceHandler.class)
public interface StacksResourceHandlerAccessor {
    @Accessor
    ArrayList<SnapshotJournal<?>> getSnapshotJournals();
}

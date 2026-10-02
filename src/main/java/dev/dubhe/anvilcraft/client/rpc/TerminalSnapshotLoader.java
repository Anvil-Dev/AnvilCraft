package dev.dubhe.anvilcraft.client.rpc;

import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;

public final class TerminalSnapshotLoader {
    private final List<UUID> targets;
    private final BiFunction<UUID, Integer, CompletableFuture<StorageServerStub.TerminalContentsPage>> fetch;
    private final Executor executor;
    private final Map<UUID, Long> versions = new LinkedHashMap<>();
    private final Map<ItemResource, Long> items = new LinkedHashMap<>();
    private final Map<FluidResource, StorageServerStub.FluidEntry> fluids = new LinkedHashMap<>();

    private TerminalSnapshotLoader(List<UUID> targets,
                                   BiFunction<UUID, Integer, CompletableFuture<StorageServerStub.TerminalContentsPage>> fetch,
                                   Executor executor) {
        this.targets = List.copyOf(targets);
        this.fetch = fetch;
        this.executor = executor;
    }

    public static CompletableFuture<StorageServerStub.TerminalSnapshot> load(List<UUID> targets,
        BiFunction<UUID, Integer, CompletableFuture<StorageServerStub.TerminalContentsPage>> fetch, Executor executor) {
        return new TerminalSnapshotLoader(targets, fetch, executor).target(0);
    }

    private CompletableFuture<StorageServerStub.TerminalSnapshot> target(int index) {
        if (index == this.targets.size()) {
            List<ItemStack> contents = this.items.entrySet().stream()
                .map(entry -> entry.getKey().toStack((int) Math.min(Integer.MAX_VALUE, entry.getValue()))).toList();
            return CompletableFuture.completedFuture(
                new StorageServerStub.TerminalSnapshot(contents, List.copyOf(this.fluids.values()), true));
        }
        return this.fetchPage(this.targets.get(index), 0).thenComposeAsync(page -> {
            Long previous = this.versions.putIfAbsent(page.storageId(), page.version());
            if (previous != null) {
                return this.target(index + 1);
            }
            return this.page(index, 0, page.storageId(), page.version(), page);
        }, this.executor);
    }

    private CompletableFuture<StorageServerStub.TerminalSnapshot> page(int targetIndex, int offset, UUID storageId, long version,
                                                                       StorageServerStub.TerminalContentsPage page) {
        if (!storageId.equals(page.storageId()) || version != page.version()) return changed();
        if (!page.last() && page.nextOffset() <= offset) {
            return CompletableFuture.failedFuture(new IllegalStateException("Terminal contents cursor did not advance"));
        }
        for (ItemStack stack : page.items()) {
            if (!stack.isEmpty()) this.items.merge(ItemResource.of(stack), (long) stack.getCount(), Long::sum);
        }
        for (var fluid : page.fluids()) {
            this.fluids.merge(FluidResource.of(fluid.icon()), fluid, (first, second) -> new StorageServerStub.FluidEntry(first.icon(),
                (int) Math.min(Integer.MAX_VALUE, (long) first.amount() + second.amount())));
        }
        if (page.last()) return this.target(targetIndex + 1);
        return this.fetchPage(this.targets.get(targetIndex), page.nextOffset()).thenComposeAsync(
            next -> this.page(targetIndex, page.nextOffset(), storageId, version, next), this.executor);
    }

    private CompletableFuture<StorageServerStub.TerminalContentsPage> fetchPage(UUID target, int offset) {
        try {
            return this.fetch.apply(target, offset);
        } catch (RuntimeException error) {
            return CompletableFuture.failedFuture(error);
        }
    }

    private static CompletableFuture<StorageServerStub.TerminalSnapshot> changed() {
        return CompletableFuture.failedFuture(new IllegalStateException("Terminal storage changed while reading contents"));
    }
}

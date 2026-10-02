package dev.dubhe.anvilcraft.api.itemhandler.unlimited;

import dev.dubhe.anvilcraft.init.item.ModComponents;
import lombok.Getter;
import lombok.Setter;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.Objects;

public class OverflowDisposalItemStacksResourceHandler extends SpaceSizeItemStacksResourceHandler {
    @Getter
    @Setter
    private boolean dispose;
    private boolean distributing;

    public OverflowDisposalItemStacksResourceHandler(int spaceSize) {
        super(spaceSize);
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
        if (!this.dispose || resource.has(ModComponents.ETERNAL) || this.distributing) {
            return super.insert(index, resource, amount, transaction);
        }
        Objects.checkIndex(index, this.size());
        int inserted = super.insert(index, resource, amount, transaction);
        if (inserted == 0 && !this.getResource(index).isEmpty() && !this.getResource(index).equals(resource)) {
            return this.insert(resource, amount, transaction);
        }
        return amount;
    }

    @Override
    public int insert(ItemResource resource, int amount, TransactionContext transaction) {
        boolean previous = this.distributing;
        this.distributing = true;
        try {
            int inserted = super.insert(resource, amount, transaction);
            return this.dispose && !resource.has(ModComponents.ETERNAL) ? amount : inserted;
        } finally {
            this.distributing = previous;
        }
    }
}

package dev.dubhe.anvilcraft.api.event;

import lombok.Getter;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public class GuideBookEvent extends Event {
    @Getter
    public static class HasGuideBookEvent extends GuideBookEvent {
        private boolean hasGuideBook = false;

        public HasGuideBookEvent() {
        }

        public void hasGuideBook() {
            this.hasGuideBook = true;
        }
    }

    @Getter
    public static class OpenGuideBookEvent extends GuideBookEvent implements ICancellableEvent {
        private final Level level;
        private final ServerPlayer player;
        private final InteractionHand usedHand;

        public OpenGuideBookEvent(Level level, ServerPlayer player, InteractionHand usedHand) {
            this.level = level;
            this.player = player;
            this.usedHand = usedHand;
        }
    }
}

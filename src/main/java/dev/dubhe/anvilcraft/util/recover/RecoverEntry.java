package dev.dubhe.anvilcraft.util.recover;

import java.util.UUID;

public record RecoverEntry<T>(UUID id, T value) {
}

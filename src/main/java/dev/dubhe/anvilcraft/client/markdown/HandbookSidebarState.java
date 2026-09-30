package dev.dubhe.anvilcraft.client.markdown;

import java.util.Set;

public interface HandbookSidebarState {
    void anvilcraft$restoreSidebar(Set<Integer> collapsedGroups, int rows, double remainder);
}

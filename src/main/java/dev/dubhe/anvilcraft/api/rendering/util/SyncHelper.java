package dev.dubhe.anvilcraft.api.rendering.util;

import static org.lwjgl.opengl.GL45.*;

public class SyncHelper {
    private long syncObject = -1;

    public boolean isSyncSet() {
        return syncObject != -1;
    }

    public boolean isSyncSignaled() {
        return glGetSynci(syncObject, GL_SYNC_STATUS, null) == GL_SIGNALED;
    }

    public void waitSync() {
        glClientWaitSync(syncObject, GL_SYNC_FLUSH_COMMANDS_BIT, Long.MAX_VALUE);
    }

    public void setSync() {
        syncObject = glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
    }

    public void deleteSync() {
        glDeleteSync(syncObject);
    }

    public void reset() {
        this.syncObject = -1;
    }

}

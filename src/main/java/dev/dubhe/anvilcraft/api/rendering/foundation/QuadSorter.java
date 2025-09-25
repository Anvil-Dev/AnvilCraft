package dev.dubhe.anvilcraft.api.rendering.foundation;

import dev.dubhe.anvilcraft.api.rendering.foundation.buffer.vertex.QuadSortingState;
import it.unimi.dsi.fastutil.ints.IntArrays;
import org.joml.Vector3f;

public class QuadSorter {
    public static int[] buildSortedIndexByDistance(QuadSortingState state, Vector3f point){
        float[] distances = new float[state.quadCenters().size()];
        int[] indexes = new int[state.quadCenters().size()];

        for (int i = 0; i < state.quadCenters().size(); i++) {
            distances[i] = state.quadCenters().get(i).distanceSquared(point);
            indexes[i] = i;
        }
        IntArrays.mergeSort(indexes, (a,b) -> Float.compare(distances[a], distances[b]));
        return indexes;
    }
}

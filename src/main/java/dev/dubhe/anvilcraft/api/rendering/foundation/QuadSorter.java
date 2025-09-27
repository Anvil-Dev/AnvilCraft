package dev.dubhe.anvilcraft.api.rendering.foundation;

import dev.dubhe.anvilcraft.api.rendering.foundation.buffer.vertex.QuadSortingState;
import it.unimi.dsi.fastutil.ints.IntArrays;
import org.joml.Vector3f;

public class QuadSorter {
    public static int[] buildSortedIndexByDistance(QuadSortingState state, Vector3f point){
        float[] distances = new float[state.quadCenters().length];
        int[] indexes = new int[state.quadCenters().length];

        for (int i = 0; i < state.quadCenters().length; i++) {
            distances[i] = state.quadCenters()[i].distanceSquared(point);
            indexes[i] = i;
        }
        IntArrays.mergeSort(indexes, (a,b) -> Float.compare(distances[a], distances[b]));
        return indexes;
    }
}

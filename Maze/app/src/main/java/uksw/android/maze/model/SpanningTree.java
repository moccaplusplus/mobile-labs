package uksw.android.maze.model;

import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.toList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.ToIntFunction;

public class SpanningTree {
    private static class MergeSet {
        MergeSet ref;

        MergeSet findSet() {
            if (ref == null) return this;
            MergeSet root = ref;
            while (root.ref != null) root = root.ref;
            return ref = root;
        }
    }

    public static List<Graph.Edge> kruskal(Graph graph, ToIntFunction<Graph.Edge> costFunction) {
        Map<Integer, MergeSet> forest = new HashMap<>();
        Function<Integer, MergeSet> initializer = node -> new MergeSet();
        return graph.edges()
                .sorted(comparingInt(costFunction))
                .filter(edge -> {
                    MergeSet tree1 = forest.computeIfAbsent(edge.from, initializer).findSet();
                    MergeSet tree2 = forest.computeIfAbsent(edge.to, initializer).findSet();
                    if (tree1 == tree2) return false;
                    tree1.ref = tree2;
                    return true;
                })
                .limit(graph.nodeCount - 1)
                .collect(toList());
    }
}

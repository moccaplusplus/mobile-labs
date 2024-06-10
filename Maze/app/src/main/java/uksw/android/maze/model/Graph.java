package uksw.android.maze.model;

import static java.util.stream.IntStream.range;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Graph {
    public static class Edge {
        public final int from;
        public final int to;

        public Edge(int from, int to) {
            this.from = from;
            this.to = to;
        }
    }

    public final int nodeCount;
    private final Map<Integer, Map<Integer, Edge>> edgeMap = new HashMap<>();

    public Graph(int nodeCount) {
        this.nodeCount = nodeCount;
    }

    public void addEdge(int from, int to) {
        addEdge(new Edge(to, from));
    }

    protected void addEdge(Edge edge) {
        edgeMap.computeIfAbsent(edge.from, key -> new HashMap<>()).put(edge.to, edge);
        edgeMap.computeIfAbsent(edge.to, key -> new HashMap<>()).put(edge.from, edge);
    }

    public void removeEdge(int from, int to) {
        Map<Integer, Edge> bucket = edgeMap.get(from);
        if (bucket != null) {
            bucket.remove(to);
        }
        bucket = edgeMap.get(to);
        if (bucket != null) {
            bucket.remove(from);
        }
    }

    public void clearEdges() {
        edgeMap.clear();
    }

    public IntStream nodes() {
        return range(0, nodeCount);
    }

    public IntStream links(int node) {
        Map<Integer, Edge> bucket = edgeMap.get(node);
        return bucket == null ? IntStream.empty() : bucket.values().stream()
                .mapToInt(edge -> edge.from == node ? edge.to : edge.from);
    }

    public boolean isLinked(int from, int to) {
        Map<Integer, Edge> bucket = edgeMap.get(from);
        return bucket != null && bucket.containsKey(to);
    }

    public Stream<Edge> edges() {
        return edgeMap.values().stream().flatMap(bucket -> bucket.entrySet().stream()
                .filter(entry -> entry.getKey() == entry.getValue().to)
                .map(Map.Entry::getValue));
    }
}

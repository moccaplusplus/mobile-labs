package uksw.android.maze.model;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.IntStream.range;

import android.util.Base64;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Maze extends Graph {
    private static final Random random = new Random();

    public final int size;

    protected Maze(int size, int nodeCount, Collection<Edge> paths) {
        super(nodeCount);
        this.size = size;
        paths.forEach(this::addEdge);
    }

    public int x(int node) {
        return node % size;
    }

    public int y(int node) {
        return node / size;
    }

    public int nodeAt(int y, int x) {
        return (y < 0 || y >= size) || (x < 0 || x >= size) ? -1 : y * size + x % size;
    }


    public String serialize() {
        List<Edge> edges = edges().collect(toList());
        int bufferSize = (3 + 2 * edges.size()) * (Integer.SIZE / 8);
        byte[] bytes = new byte[bufferSize];
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.putInt(size);
        buffer.putInt(nodeCount);
        buffer.putInt(edges.size());
        for (Edge edge : edges) {
            buffer.putInt(edge.from);
            buffer.putInt(edge.to);
        }
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    public static Maze deserialize(String serialized) {
        byte[] bytes = Base64.decode(serialized, Base64.DEFAULT);
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        int size = buffer.getInt();
        int nodeCount = buffer.getInt();
        int edgeCount = buffer.getInt();
        List<Edge> edges = new ArrayList<>(edgeCount);
        for (int i = 0; i < edgeCount; i++) {
            edges.add(new Edge(buffer.getInt(), buffer.getInt()));
        }
        return new Maze(size, nodeCount, edges);
    }

    public static Maze generate(int size) {
        Graph paths = grid(size);
        Map<Edge, Integer> costMap = paths.edges().collect(
                toMap(identity(), id -> 1 + random.nextInt(99)));
        List<Edge> spanningTree = SpanningTree.kruskal(paths, costMap::get);
        return new Maze(size, paths.nodeCount, spanningTree);
    }

    public static Graph grid(int size) {
        int n = size * size;
        Graph g = new Graph(n);
        range(1, n).filter(i -> i % size != 0).forEach(i -> g.addEdge(i - 1, i));
        range(size, n).forEach(i -> g.addEdge(i - size, i));
        return g;
    }
}

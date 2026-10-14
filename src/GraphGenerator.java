import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public final class GraphGenerator {
    private GraphGenerator() {
    }

    public static Graph connectedUndirected(int vertices, int edges, long seed) {
        long maximum = (long) vertices * (vertices - 1) / 2;
        if (vertices < 1 || edges < vertices - 1 || edges > maximum) {
            throw new IllegalArgumentException("invalid undirected graph size");
        }
        Graph graph = new Graph(vertices, false);
        Random random = new Random(seed);
        Set<Long> used = new HashSet<>();
        for (int vertex = 1; vertex < vertices; vertex++) {
            int parent = random.nextInt(vertex);
            addUndirected(graph, used, parent, vertex, random);
        }
        while (graph.edgeCount() < edges) {
            int first = random.nextInt(vertices);
            int second = random.nextInt(vertices);
            if (first != second) {
                addUndirected(graph, used, first, second, random);
            }
        }
        return graph;
    }

    public static Graph dag(int vertices, int edges, long seed) {
        long maximum = (long) vertices * (vertices - 1) / 2;
        if (vertices < 1 || edges < 0 || edges > maximum) {
            throw new IllegalArgumentException("invalid DAG size");
        }
        Graph graph = new Graph(vertices, true);
        Random random = new Random(seed);
        Set<Long> used = new HashSet<>();
        while (graph.edgeCount() < edges) {
            int first = random.nextInt(vertices);
            int second = random.nextInt(vertices);
            if (first != second) {
                int from = Math.min(first, second);
                int to = Math.max(first, second);
                addDirected(graph, used, from, to, random);
            }
        }
        return graph;
    }

    public static Graph cyclicDirected(int vertices, int edges, long seed) {
        long maximum = (long) vertices * (vertices - 1);
        if (vertices < 3 || edges < 3 || edges > maximum) {
            throw new IllegalArgumentException("invalid cyclic graph size");
        }
        Graph graph = new Graph(vertices, true);
        Random random = new Random(seed);
        Set<Long> used = new HashSet<>();
        addDirected(graph, used, 0, 1, random);
        addDirected(graph, used, 1, 2, random);
        addDirected(graph, used, 2, 0, random);
        while (graph.edgeCount() < edges) {
            int from = random.nextInt(vertices);
            int to = random.nextInt(vertices);
            if (from != to) {
                addDirected(graph, used, from, to, random);
            }
        }
        return graph;
    }

    private static void addUndirected(Graph graph, Set<Long> used, int first, int second, Random random) {
        int from = Math.min(first, second);
        int to = Math.max(first, second);
        if (used.add(key(from, to))) {
            graph.addEdge(from, to, 1 + random.nextInt(1000));
        }
    }

    private static void addDirected(Graph graph, Set<Long> used, int from, int to, Random random) {
        if (used.add(key(from, to))) {
            graph.addEdge(from, to, 1 + random.nextInt(1000));
        }
    }

    private static long key(int from, int to) {
        return ((long) from << 32) | (to & 0xffffffffL);
    }
}

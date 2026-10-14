import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class Tests {
    private Tests() {
    }

    public static void main(String[] args) {
        testDsu();
        testMst();
        testTopologicalSort();
        testScc();
        testDijkstra();
        testGenerators();
        System.out.println("All tests passed");
    }

    private static void testDsu() {
        DSU dsu = new DSU(5);
        check(dsu.components() == 5, "initial DSU components");
        check(dsu.union(0, 1) && dsu.union(1, 2), "DSU union");
        check(!dsu.union(0, 2), "duplicate DSU union");
        check(dsu.find(0) == dsu.find(2) && dsu.find(0) != dsu.find(3), "DSU find");
        check(dsu.components() == 3 && dsu.findCalls() > 0 && dsu.parentSteps() > 0, "DSU metrics");
        expect(IndexOutOfBoundsException.class, () -> dsu.find(5));

        DSU compressed = new DSU(4);
        compressed.union(0, 1);
        compressed.union(2, 3);
        compressed.union(0, 2);
        long before = compressed.parentSteps();
        compressed.find(3);
        long firstSearch = compressed.parentSteps() - before;
        before = compressed.parentSteps();
        compressed.find(3);
        check(firstSearch == 2 && compressed.parentSteps() - before == 1, "path compression");
    }

    private static void testMst() {
        Graph graph = new Graph(4, false);
        graph.addEdge(0, 1, 1);
        graph.addEdge(1, 2, 2);
        graph.addEdge(2, 3, 3);
        graph.addEdge(0, 3, 10);
        graph.addEdge(0, 2, 4);
        graph.addEdge(1, 3, 5);
        checkMst(graph, true, 6, 3);

        Graph ties = new Graph(3, false);
        ties.addEdge(0, 1, 1);
        ties.addEdge(1, 2, 1);
        ties.addEdge(0, 2, 1);
        checkMst(ties, true, 2, 2);

        Graph disconnected = new Graph(4, false);
        disconnected.addEdge(0, 1, 2);
        disconnected.addEdge(2, 3, 3);
        checkMst(disconnected, false, 5, 2);
        checkMst(new Graph(1, false), true, 0, 0);
        expect(IllegalArgumentException.class, () -> Algorithms.kruskal(new Graph(2, true)));
        expect(IllegalArgumentException.class, () -> Algorithms.prim(new Graph(2, true)));
    }

    private static void checkMst(Graph graph, boolean connected, long weight, int edgeCount) {
        Algorithms.MstResult kruskal = Algorithms.kruskal(graph);
        Algorithms.MstResult prim = Algorithms.prim(graph);
        check(kruskal.connected == connected && prim.connected == connected, "MST connectivity");
        check(kruskal.weight == weight && prim.weight == weight, "MST weight");
        check(kruskal.edges.size() == edgeCount && prim.edges.size() == edgeCount, "MST edge count");
    }

    private static void testTopologicalSort() {
        Graph dag = new Graph(6, true);
        dag.addEdge(5, 2, 1);
        dag.addEdge(5, 0, 1);
        dag.addEdge(4, 0, 1);
        dag.addEdge(4, 1, 1);
        dag.addEdge(2, 3, 1);
        dag.addEdge(3, 1, 1);
        checkTopologicalOrder(dag, Algorithms.topologicalSort(dag));

        Graph cycle = new Graph(3, true);
        cycle.addEdge(0, 1, 1);
        cycle.addEdge(1, 2, 1);
        cycle.addEdge(2, 0, 1);
        Algorithms.TopoResult result = Algorithms.topologicalSort(cycle);
        check(!result.dag && result.order.isEmpty(), "cycle detected");
        Graph single = new Graph(1, true);
        checkTopologicalOrder(single, Algorithms.topologicalSort(single));
        expect(IllegalArgumentException.class, () -> Algorithms.topologicalSort(new Graph(2, false)));
    }

    private static void checkTopologicalOrder(Graph graph, Algorithms.TopoResult result) {
        check(result.dag && result.order.size() == graph.vertexCount(), "DAG order size");
        int[] position = new int[graph.vertexCount()];
        Arrays.fill(position, -1);
        for (int index = 0; index < result.order.size(); index++) {
            int vertex = result.order.get(index);
            check(position[vertex] == -1, "duplicate topological vertex");
            position[vertex] = index;
        }
        for (Graph.Edge edge : graph.edges()) {
            check(position[edge.from] < position[edge.to], "invalid topological edge");
        }
    }

    private static void testScc() {
        Graph graph = new Graph(7, true);
        graph.addEdge(0, 1, 1);
        graph.addEdge(1, 2, 1);
        graph.addEdge(2, 0, 1);
        graph.addEdge(2, 3, 1);
        graph.addEdge(3, 4, 1);
        graph.addEdge(4, 3, 1);
        graph.addEdge(4, 5, 1);
        Algorithms.SccResult result = Algorithms.stronglyConnectedComponents(graph);
        Set<Set<Integer>> expected = Set.of(Set.of(0, 1, 2), Set.of(3, 4), Set.of(5), Set.of(6));
        Set<Set<Integer>> actual = new HashSet<>();
        int count = 0;
        for (List<Integer> component : result.components) {
            actual.add(new HashSet<>(component));
            count += component.size();
        }
        check(actual.equals(expected) && count == 7, "SCC partition");
        check(Algorithms.stronglyConnectedComponents(new Graph(1, true)).components.equals(List.of(List.of(0))),
                "single SCC vertex");
        expect(IllegalArgumentException.class, () -> Algorithms.stronglyConnectedComponents(new Graph(2, false)));
    }

    private static void testDijkstra() {
        Graph graph = new Graph(5, true);
        graph.addEdge(0, 1, 4);
        graph.addEdge(0, 2, 1);
        graph.addEdge(2, 1, 2);
        graph.addEdge(1, 3, 1);
        graph.addEdge(2, 3, 7);
        Algorithms.PathResult paths = Algorithms.dijkstra(graph, 0);
        check(paths.distanceTo(0) == 0 && paths.distanceTo(1) == 3
                && paths.distanceTo(2) == 1 && paths.distanceTo(3) == 4, "shortest distances");
        check(paths.pathTo(3).equals(List.of(0, 2, 1, 3)), "shortest path");
        check(paths.distanceTo(4) == Long.MAX_VALUE && paths.pathTo(4).isEmpty(), "unreachable vertex");
        check(Algorithms.dijkstra(new Graph(1, false), 0).pathTo(0).equals(List.of(0)), "single vertex path");

        Graph zero = new Graph(2, false);
        zero.addEdge(0, 1, 0);
        check(Algorithms.dijkstra(zero, 0).distanceTo(1) == 0, "zero-weight edge");
        expect(IllegalArgumentException.class, () -> graph.addEdge(3, 4, -1));
        expect(IndexOutOfBoundsException.class, () -> Algorithms.dijkstra(graph, 5));
    }

    private static void testGenerators() {
        for (int vertices : new int[]{10, 30, 100}) {
            int edges = vertices == 10 ? 30 : vertices * 4;
            Graph undirected = GraphGenerator.connectedUndirected(vertices, edges, 42);
            check(undirected.edgeCount() == edges && Algorithms.kruskal(undirected).connected,
                    "connected generator");
            check(Algorithms.kruskal(undirected).weight == Algorithms.prim(undirected).weight,
                    "random MST agreement");
            Graph dag = GraphGenerator.dag(vertices, edges, 42);
            check(dag.edgeCount() == edges, "DAG edge count");
            checkTopologicalOrder(dag, Algorithms.topologicalSort(dag));
            Graph cyclic = GraphGenerator.cyclicDirected(vertices, edges, 42);
            check(cyclic.edgeCount() == edges && !Algorithms.topologicalSort(cyclic).dag,
                    "cyclic generator");
            checkPartition(cyclic, Algorithms.stronglyConnectedComponents(cyclic));
            checkDistances(cyclic, 0);
            Graph repeated = GraphGenerator.connectedUndirected(vertices, edges, 42);
            for (int index = 0; index < edges; index++) {
                Graph.Edge first = undirected.edges().get(index);
                Graph.Edge second = repeated.edges().get(index);
                check(first.from == second.from && first.to == second.to && first.weight == second.weight,
                        "fixed seed reproducibility");
            }
        }
        expect(IllegalArgumentException.class, () -> GraphGenerator.connectedUndirected(10, 5, 42));
        expect(IllegalArgumentException.class, () -> GraphGenerator.dag(3, 4, 42));
        expect(IllegalArgumentException.class, () -> GraphGenerator.cyclicDirected(2, 3, 42));
    }

    private static void checkPartition(Graph graph, Algorithms.SccResult result) {
        boolean[] seen = new boolean[graph.vertexCount()];
        for (List<Integer> component : result.components) {
            check(!component.isEmpty(), "empty SCC");
            for (int vertex : component) {
                check(!seen[vertex], "duplicate SCC vertex");
                seen[vertex] = true;
            }
        }
        for (boolean value : seen) {
            check(value, "missing SCC vertex");
        }
    }

    private static void checkDistances(Graph graph, int source) {
        long[] expected = new long[graph.vertexCount()];
        Arrays.fill(expected, Long.MAX_VALUE);
        expected[source] = 0;
        for (int pass = 1; pass < graph.vertexCount(); pass++) {
            boolean changed = false;
            for (Graph.Edge edge : graph.edges()) {
                if (expected[edge.from] != Long.MAX_VALUE
                        && expected[edge.from] + edge.weight < expected[edge.to]) {
                    expected[edge.to] = expected[edge.from] + edge.weight;
                    changed = true;
                }
            }
            if (!changed) {
                break;
            }
        }
        Algorithms.PathResult result = Algorithms.dijkstra(graph, source);
        for (int vertex = 0; vertex < graph.vertexCount(); vertex++) {
            check(result.distanceTo(vertex) == expected[vertex], "Dijkstra versus Bellman-Ford");
        }
    }

    private static void expect(Class<? extends Throwable> type, Runnable action) {
        try {
            action.run();
        } catch (Throwable error) {
            check(type.isInstance(error), "wrong exception: " + error);
            return;
        }
        throw new AssertionError("expected " + type.getSimpleName());
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}

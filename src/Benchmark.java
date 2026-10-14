import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.function.Supplier;

public final class Benchmark {
    private static final int[] VERTICES = {10, 100, 1_000};
    private static final int[] EDGES = {30, 500, 5_000};
    private static final int RUNS = 5;
    private static volatile long sink;

    private Benchmark() {
    }

    public static void main(String[] args) throws IOException {
        Path tables = Path.of("results", "tables");
        Path plots = Path.of("results", "plots");
        Files.createDirectories(tables);
        StringBuilder csv = new StringBuilder("vertices,edges,graph,algorithm,mean_time_ms,mean_edge_scans,"
                + "mean_comparisons,mean_dsu_finds,mean_dsu_unions,mean_dsu_parent_steps,"
                + "mean_queue_offers,mean_queue_polls\n");

        for (int index = 0; index < VERTICES.length; index++) {
            int vertices = VERTICES[index];
            int edges = EDGES[index];
            Graph city = GraphGenerator.connectedUndirected(vertices, edges, 42L + vertices);
            Graph dag = GraphGenerator.dag(vertices, edges, 1_042L + vertices);
            Graph cyclic = GraphGenerator.cyclicDirected(vertices, edges, 2_042L + vertices);
            verify(city, dag, cyclic);

            row(csv, vertices, edges, "undirected", "Kruskal", () -> Algorithms.kruskal(city));
            row(csv, vertices, edges, "undirected", "Prim", () -> Algorithms.prim(city));
            row(csv, vertices, edges, "dag", "TopoDAG", () -> Algorithms.topologicalSort(dag));
            row(csv, vertices, edges, "cyclic", "TopoCycle", () -> Algorithms.topologicalSort(cyclic));
            row(csv, vertices, edges, "cyclic", "Kosaraju", () -> Algorithms.stronglyConnectedComponents(cyclic));
            row(csv, vertices, edges, "undirected", "Dijkstra", () -> Algorithms.dijkstra(city, 0));
        }

        Path table = tables.resolve("measurements.csv");
        Files.writeString(table, csv, StandardCharsets.UTF_8);
        Plots.generate(table, plots);
        System.out.println("Results saved in results/tables and results/plots");
    }

    private static void verify(Graph city, Graph dag, Graph cyclic) {
        Algorithms.MstResult kruskal = Algorithms.kruskal(city);
        Algorithms.MstResult prim = Algorithms.prim(city);
        if (!kruskal.connected || !prim.connected || kruskal.weight != prim.weight) {
            throw new AssertionError("MST results disagree");
        }
        if (!Algorithms.topologicalSort(dag).dag || Algorithms.topologicalSort(cyclic).dag) {
            throw new AssertionError("directed graph type is incorrect");
        }
        int covered = 0;
        for (var component : Algorithms.stronglyConnectedComponents(cyclic).components) {
            covered += component.size();
        }
        if (covered != cyclic.vertexCount()) {
            throw new AssertionError("SCC partition is incomplete");
        }
        Algorithms.PathResult paths = Algorithms.dijkstra(city, 0);
        for (int vertex = 0; vertex < city.vertexCount(); vertex++) {
            if (paths.distanceTo(vertex) == Long.MAX_VALUE) {
                throw new AssertionError("connected city has an unreachable vertex");
            }
        }
    }

    private static void row(StringBuilder csv, int vertices, int edges, String graph,
                            String algorithm, Supplier<? extends Algorithms.Outcome> run) {
        run.get();
        run.get();
        long nanoseconds = 0;
        Algorithms.Metrics counts = new Algorithms.Metrics();
        for (int repeat = 0; repeat < RUNS; repeat++) {
            long start = System.nanoTime();
            Algorithms.Outcome result = run.get();
            nanoseconds += System.nanoTime() - start;
            counts.add(result.metrics());
        }
        sink = counts.edgeScans + counts.comparisons + counts.queuePolls;
        csv.append(String.format(Locale.US,
                "%d,%d,%s,%s,%.6f,%.1f,%.1f,%.1f,%.1f,%.1f,%.1f,%.1f%n",
                vertices, edges, graph, algorithm, nanoseconds / (RUNS * 1_000_000.0),
                counts.edgeScans / (double) RUNS, counts.comparisons / (double) RUNS,
                counts.dsuFinds / (double) RUNS, counts.dsuUnions / (double) RUNS,
                counts.dsuParentSteps / (double) RUNS, counts.queueOffers / (double) RUNS,
                counts.queuePolls / (double) RUNS));
    }
}

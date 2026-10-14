import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

public final class Algorithms {
    public interface Outcome {
        Metrics metrics();
    }

    public static final class Metrics {
        public long edgeScans;
        public long comparisons;
        public long dsuFinds;
        public long dsuUnions;
        public long dsuParentSteps;
        public long queueOffers;
        public long queuePolls;

        public void add(Metrics other) {
            edgeScans += other.edgeScans;
            comparisons += other.comparisons;
            dsuFinds += other.dsuFinds;
            dsuUnions += other.dsuUnions;
            dsuParentSteps += other.dsuParentSteps;
            queueOffers += other.queueOffers;
            queuePolls += other.queuePolls;
        }
    }

    public static final class MstResult implements Outcome {
        public final long weight;
        public final boolean connected;
        public final List<Graph.Edge> edges;
        private final Metrics metrics;

        private MstResult(long weight, boolean connected, List<Graph.Edge> edges, Metrics metrics) {
            this.weight = weight;
            this.connected = connected;
            this.edges = List.copyOf(edges);
            this.metrics = metrics;
        }

        public Metrics metrics() {
            return metrics;
        }
    }

    public static final class TopoResult implements Outcome {
        public final boolean dag;
        public final List<Integer> order;
        private final Metrics metrics;

        private TopoResult(boolean dag, List<Integer> order, Metrics metrics) {
            this.dag = dag;
            this.order = List.copyOf(order);
            this.metrics = metrics;
        }

        public Metrics metrics() {
            return metrics;
        }
    }

    public static final class SccResult implements Outcome {
        public final List<List<Integer>> components;
        private final Metrics metrics;

        private SccResult(List<List<Integer>> components, Metrics metrics) {
            List<List<Integer>> copy = new ArrayList<>();
            for (List<Integer> component : components) {
                copy.add(List.copyOf(component));
            }
            this.components = List.copyOf(copy);
            this.metrics = metrics;
        }

        public Metrics metrics() {
            return metrics;
        }
    }

    public static final class PathResult implements Outcome {
        private final int source;
        private final long[] distance;
        private final int[] previous;
        private final Metrics metrics;

        private PathResult(int source, long[] distance, int[] previous, Metrics metrics) {
            this.source = source;
            this.distance = distance;
            this.previous = previous;
            this.metrics = metrics;
        }

        public long distanceTo(int vertex) {
            checkVertex(vertex);
            return distance[vertex];
        }

        public List<Integer> pathTo(int vertex) {
            checkVertex(vertex);
            if (distance[vertex] == Long.MAX_VALUE) {
                return List.of();
            }
            List<Integer> path = new ArrayList<>();
            for (int current = vertex; current != -1; current = previous[current]) {
                path.add(current);
            }
            Collections.reverse(path);
            return path;
        }

        public int source() {
            return source;
        }

        public Metrics metrics() {
            return metrics;
        }

        private void checkVertex(int vertex) {
            if (vertex < 0 || vertex >= distance.length) {
                throw new IndexOutOfBoundsException("vertex: " + vertex);
            }
        }
    }

    private static final class State {
        private final int vertex;
        private final long distance;

        private State(int vertex, long distance) {
            this.vertex = vertex;
            this.distance = distance;
        }
    }

    private Algorithms() {
    }

    public static MstResult kruskal(Graph graph) {
        requireDirection(graph, false);
        Metrics metrics = new Metrics();
        List<Graph.Edge> sorted = new ArrayList<>(graph.edges());
        sorted.sort((first, second) -> {
            metrics.comparisons++;
            return Integer.compare(first.weight, second.weight);
        });
        DSU dsu = new DSU(graph.vertexCount());
        List<Graph.Edge> chosen = new ArrayList<>();
        long weight = 0;
        for (Graph.Edge edge : sorted) {
            metrics.edgeScans++;
            if (dsu.union(edge.from, edge.to)) {
                chosen.add(edge);
                weight += edge.weight;
                if (chosen.size() == graph.vertexCount() - 1) {
                    break;
                }
            }
        }
        metrics.dsuFinds = dsu.findCalls();
        metrics.dsuUnions = dsu.unionCalls();
        metrics.dsuParentSteps = dsu.parentSteps();
        return new MstResult(weight, dsu.components() == 1, chosen, metrics);
    }

    public static MstResult prim(Graph graph) {
        requireDirection(graph, false);
        Metrics metrics = new Metrics();
        boolean[] visited = new boolean[graph.vertexCount()];
        PriorityQueue<Graph.Edge> queue = new PriorityQueue<>((first, second) -> {
            metrics.comparisons++;
            return Integer.compare(first.weight, second.weight);
        });
        List<Graph.Edge> chosen = new ArrayList<>();
        long weight = 0;
        int components = 0;
        for (int start = 0; start < graph.vertexCount(); start++) {
            if (visited[start]) {
                continue;
            }
            components++;
            visited[start] = true;
            offerEdges(graph, start, visited, queue, metrics);
            while (!queue.isEmpty()) {
                Graph.Edge edge = queue.poll();
                metrics.queuePolls++;
                if (visited[edge.to]) {
                    continue;
                }
                visited[edge.to] = true;
                chosen.add(edge);
                weight += edge.weight;
                offerEdges(graph, edge.to, visited, queue, metrics);
            }
        }
        return new MstResult(weight, components == 1, chosen, metrics);
    }

    private static void offerEdges(Graph graph, int vertex, boolean[] visited,
                                   PriorityQueue<Graph.Edge> queue, Metrics metrics) {
        for (Graph.Edge edge : graph.neighbors(vertex)) {
            metrics.edgeScans++;
            if (!visited[edge.to]) {
                queue.add(edge);
                metrics.queueOffers++;
            }
        }
    }

    public static TopoResult topologicalSort(Graph graph) {
        requireDirection(graph, true);
        Metrics metrics = new Metrics();
        int[] indegree = new int[graph.vertexCount()];
        for (Graph.Edge edge : graph.edges()) {
            indegree[edge.to]++;
            metrics.edgeScans++;
        }
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        for (int vertex = 0; vertex < indegree.length; vertex++) {
            if (indegree[vertex] == 0) {
                queue.add(vertex);
                metrics.queueOffers++;
            }
        }
        List<Integer> order = new ArrayList<>();
        while (!queue.isEmpty()) {
            int vertex = queue.remove();
            metrics.queuePolls++;
            order.add(vertex);
            for (Graph.Edge edge : graph.neighbors(vertex)) {
                metrics.edgeScans++;
                if (--indegree[edge.to] == 0) {
                    queue.add(edge.to);
                    metrics.queueOffers++;
                }
            }
        }
        boolean dag = order.size() == graph.vertexCount();
        return new TopoResult(dag, dag ? order : List.of(), metrics);
    }

    public static SccResult stronglyConnectedComponents(Graph graph) {
        requireDirection(graph, true);
        Metrics metrics = new Metrics();
        int count = graph.vertexCount();
        boolean[] visited = new boolean[count];
        List<Integer> finishOrder = new ArrayList<>(count);
        for (int vertex = 0; vertex < count; vertex++) {
            if (!visited[vertex]) {
                finishOrder(graph, vertex, visited, finishOrder, metrics);
            }
        }
        List<List<Integer>> reverse = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            reverse.add(new ArrayList<>());
        }
        for (Graph.Edge edge : graph.edges()) {
            reverse.get(edge.to).add(edge.from);
            metrics.edgeScans++;
        }
        Arrays.fill(visited, false);
        List<List<Integer>> components = new ArrayList<>();
        for (int i = finishOrder.size() - 1; i >= 0; i--) {
            int start = finishOrder.get(i);
            if (visited[start]) {
                continue;
            }
            List<Integer> component = new ArrayList<>();
            ArrayDeque<Integer> stack = new ArrayDeque<>();
            stack.push(start);
            visited[start] = true;
            while (!stack.isEmpty()) {
                int vertex = stack.pop();
                component.add(vertex);
                for (int neighbor : reverse.get(vertex)) {
                    metrics.edgeScans++;
                    if (!visited[neighbor]) {
                        visited[neighbor] = true;
                        stack.push(neighbor);
                    }
                }
            }
            components.add(component);
        }
        return new SccResult(components, metrics);
    }

    private static void finishOrder(Graph graph, int start, boolean[] visited,
                                    List<Integer> order, Metrics metrics) {
        ArrayDeque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[]{start, 0});
        visited[start] = true;
        while (!stack.isEmpty()) {
            int[] frame = stack.peek();
            List<Graph.Edge> neighbors = graph.neighbors(frame[0]);
            if (frame[1] == neighbors.size()) {
                order.add(frame[0]);
                stack.pop();
                continue;
            }
            Graph.Edge edge = neighbors.get(frame[1]++);
            metrics.edgeScans++;
            if (!visited[edge.to]) {
                visited[edge.to] = true;
                stack.push(new int[]{edge.to, 0});
            }
        }
    }

    public static PathResult dijkstra(Graph graph, int source) {
        graph.checkVertex(source);
        Metrics metrics = new Metrics();
        long[] distance = new long[graph.vertexCount()];
        int[] previous = new int[graph.vertexCount()];
        Arrays.fill(distance, Long.MAX_VALUE);
        Arrays.fill(previous, -1);
        distance[source] = 0;
        PriorityQueue<State> queue = new PriorityQueue<>((first, second) -> {
            metrics.comparisons++;
            return Long.compare(first.distance, second.distance);
        });
        queue.add(new State(source, 0));
        metrics.queueOffers++;
        while (!queue.isEmpty()) {
            State state = queue.poll();
            metrics.queuePolls++;
            if (state.distance != distance[state.vertex]) {
                continue;
            }
            for (Graph.Edge edge : graph.neighbors(state.vertex)) {
                metrics.edgeScans++;
                if (state.distance > Long.MAX_VALUE - edge.weight) {
                    continue;
                }
                long candidate = state.distance + edge.weight;
                if (candidate < distance[edge.to]) {
                    distance[edge.to] = candidate;
                    previous[edge.to] = state.vertex;
                    queue.add(new State(edge.to, candidate));
                    metrics.queueOffers++;
                }
            }
        }
        return new PathResult(source, distance, previous, metrics);
    }

    private static void requireDirection(Graph graph, boolean directed) {
        if (graph.isDirected() != directed) {
            throw new IllegalArgumentException(directed ? "directed graph required" : "undirected graph required");
        }
    }
}

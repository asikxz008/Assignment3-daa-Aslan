public final class Demo {
    private Demo() {
    }

    public static void main(String[] args) {
        Graph city = new Graph(5, false);
        city.addEdge(0, 1, 4);
        city.addEdge(0, 2, 1);
        city.addEdge(2, 1, 2);
        city.addEdge(1, 3, 1);
        city.addEdge(2, 3, 7);
        city.addEdge(3, 4, 3);
        Algorithms.MstResult kruskal = Algorithms.kruskal(city);
        Algorithms.MstResult prim = Algorithms.prim(city);
        System.out.println("Kruskal MST: weight=" + kruskal.weight + ", connected=" + kruskal.connected);
        System.out.println("Prim MST: weight=" + prim.weight + ", connected=" + prim.connected);

        Graph dag = new Graph(4, true);
        dag.addEdge(0, 1, 1);
        dag.addEdge(0, 2, 1);
        dag.addEdge(1, 3, 1);
        dag.addEdge(2, 3, 1);
        System.out.println("Topological order: " + Algorithms.topologicalSort(dag).order);
        dag.addEdge(3, 0, 1);
        System.out.println("Cycle detected: " + !Algorithms.topologicalSort(dag).dag);
        System.out.println("Strongly connected components: "
                + Algorithms.stronglyConnectedComponents(dag).components);

        Algorithms.PathResult paths = Algorithms.dijkstra(city, 0);
        for (int vertex = 0; vertex < city.vertexCount(); vertex++) {
            System.out.println("Route 0 -> " + vertex + ": distance=" + paths.distanceTo(vertex)
                    + ", path=" + paths.pathTo(vertex));
        }
    }
}

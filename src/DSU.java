public final class DSU {
    private final int[] parent;
    private final int[] size;
    private int components;
    private long findCalls;
    private long parentSteps;
    private long unionCalls;

    public DSU(int count) {
        if (count < 1) {
            throw new IllegalArgumentException("count must be positive");
        }
        parent = new int[count];
        size = new int[count];
        components = count;
        for (int i = 0; i < count; i++) {
            parent[i] = i;
            size[i] = 1;
        }
    }

    public int find(int vertex) {
        if (vertex < 0 || vertex >= parent.length) {
            throw new IndexOutOfBoundsException("vertex: " + vertex);
        }
        findCalls++;
        int root = vertex;
        while (root != parent[root]) {
            root = parent[root];
            parentSteps++;
        }
        while (vertex != root) {
            int next = parent[vertex];
            parent[vertex] = root;
            vertex = next;
        }
        return root;
    }

    public boolean union(int first, int second) {
        unionCalls++;
        int rootFirst = find(first);
        int rootSecond = find(second);
        if (rootFirst == rootSecond) {
            return false;
        }
        if (size[rootFirst] < size[rootSecond]) {
            int temp = rootFirst;
            rootFirst = rootSecond;
            rootSecond = temp;
        }
        parent[rootSecond] = rootFirst;
        size[rootFirst] += size[rootSecond];
        components--;
        return true;
    }

    public int components() {
        return components;
    }

    public long findCalls() {
        return findCalls;
    }

    public long parentSteps() {
        return parentSteps;
    }

    public long unionCalls() {
        return unionCalls;
    }
}

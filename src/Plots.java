import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import javax.imageio.ImageIO;

public final class Plots {
    private static final String[] NAMES = {
            "Kruskal", "Prim", "TopoDAG", "TopoCycle", "Kosaraju", "Dijkstra"
    };
    private static final Color[] COLORS = {
            new Color(42, 85, 196), new Color(228, 111, 39), new Color(26, 139, 107),
            new Color(189, 62, 82), new Color(127, 72, 174), new Color(60, 66, 76)
    };

    private static final class Point {
        private final int vertices;
        private final int edges;
        private final double milliseconds;

        private Point(int vertices, int edges, double milliseconds) {
            this.vertices = vertices;
            this.edges = edges;
            this.milliseconds = milliseconds;
        }
    }

    private Plots() {
    }

    public static void generate(Path table, Path output) throws IOException {
        Map<String, List<Point>> series = new LinkedHashMap<>();
        for (String name : NAMES) {
            series.put(name, new ArrayList<>());
        }
        List<String> lines = Files.readAllLines(table, StandardCharsets.UTF_8);
        for (int i = 1; i < lines.size(); i++) {
            String[] cells = lines.get(i).split(",");
            series.get(cells[3]).add(new Point(Integer.parseInt(cells[0]),
                    Integer.parseInt(cells[1]), Double.parseDouble(cells[4])));
        }
        for (List<Point> points : series.values()) {
            points.sort(Comparator.comparingInt(point -> point.vertices));
        }
        Files.createDirectories(output);
        draw(series, false, "Execution time vs. vertices", "Vertices (V)", output.resolve("time_vs_vertices.png"));
        draw(series, true, "Execution time vs. edges", "Edges (E)", output.resolve("time_vs_edges.png"));
    }

    private static void draw(Map<String, List<Point>> series, boolean useEdges,
                             String title, String xLabel, Path file) throws IOException {
        final int width = 1100;
        final int height = 640;
        final int left = 95;
        final int right = 790;
        final int top = 92;
        final int bottom = 530;
        TreeSet<Integer> xValues = new TreeSet<>();
        double minimum = Double.POSITIVE_INFINITY;
        double maximum = 0;
        for (List<Point> points : series.values()) {
            for (Point point : points) {
                xValues.add(useEdges ? point.edges : point.vertices);
                minimum = Math.min(minimum, Math.max(point.milliseconds, 0.000001));
                maximum = Math.max(maximum, point.milliseconds);
            }
        }
        double xMin = Math.log10(xValues.first());
        double xMax = Math.log10(xValues.last());
        double yMin = Math.floor(Math.log10(minimum)) - 0.2;
        double yMax = Math.ceil(Math.log10(maximum)) + 0.2;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, width, height);
        graphics.setColor(new Color(29, 43, 68));
        graphics.setFont(new Font("SansSerif", Font.BOLD, 23));
        graphics.drawString(title, left, 43);
        graphics.setFont(new Font("SansSerif", Font.PLAIN, 14));
        graphics.drawString("Time (ms, logarithmic scale)", left, 69);

        graphics.setColor(new Color(222, 229, 240));
        graphics.setStroke(new BasicStroke(1));
        for (int power = (int) Math.ceil(yMin); power <= Math.floor(yMax); power++) {
            int y = mapY(power, yMin, yMax, top, bottom);
            graphics.drawLine(left, y, right, y);
            graphics.setColor(new Color(79, 91, 110));
            graphics.drawString(String.format(Locale.US, "%.3g", Math.pow(10, power)), 22, y + 5);
            graphics.setColor(new Color(222, 229, 240));
        }
        for (int value : xValues) {
            int x = mapX(value, xMin, xMax, left, right);
            graphics.drawLine(x, top, x, bottom);
            graphics.setColor(new Color(79, 91, 110));
            String label = String.format(Locale.US, "%,d", value);
            graphics.drawString(label, x - graphics.getFontMetrics().stringWidth(label) / 2, bottom + 24);
            graphics.setColor(new Color(222, 229, 240));
        }
        graphics.setColor(new Color(29, 43, 68));
        graphics.setStroke(new BasicStroke(2));
        graphics.drawLine(left, top, left, bottom);
        graphics.drawLine(left, bottom, right, bottom);
        graphics.drawString(xLabel, (left + right) / 2 - 37, bottom + 58);

        for (int index = 0; index < NAMES.length; index++) {
            List<Point> points = series.get(NAMES[index]);
            graphics.setColor(COLORS[index]);
            graphics.setStroke(new BasicStroke(3));
            int previousX = -1;
            int previousY = -1;
            for (Point point : points) {
                int x = mapX(useEdges ? point.edges : point.vertices, xMin, xMax, left, right);
                int y = mapY(Math.log10(Math.max(point.milliseconds, 0.000001)), yMin, yMax, top, bottom);
                if (previousX >= 0) {
                    graphics.drawLine(previousX, previousY, x, y);
                }
                graphics.fillOval(x - 5, y - 5, 10, 10);
                previousX = x;
                previousY = y;
            }
            int legendY = 150 + index * 44;
            graphics.drawLine(825, legendY, 860, legendY);
            graphics.fillOval(838, legendY - 5, 10, 10);
            graphics.setColor(new Color(29, 43, 68));
            graphics.setFont(new Font("SansSerif", Font.PLAIN, 16));
            graphics.drawString(NAMES[index], 875, legendY + 5);
        }
        graphics.dispose();
        ImageIO.write(image, "png", file.toFile());
    }

    private static int mapX(int value, double minimum, double maximum, int left, int right) {
        return left + (int) Math.round((Math.log10(value) - minimum) * (right - left) / (maximum - minimum));
    }

    private static int mapY(double value, double minimum, double maximum, int top, int bottom) {
        return bottom - (int) Math.round((value - minimum) * (bottom - top) / (maximum - minimum));
    }
}

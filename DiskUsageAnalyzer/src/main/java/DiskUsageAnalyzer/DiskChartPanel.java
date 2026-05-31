package DiskUsageAnalyzer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.File;
import java.util.*;
import java.util.List;

public class DiskChartPanel extends JPanel {

    private static final Color[] PALETTE = {
        new Color(55, 138, 221),
        new Color(29, 158, 117),
        new Color(216, 90, 48),
        new Color(127, 119, 221),
        new Color(186, 117, 23),
        new Color(212, 83, 126),
        new Color(136, 135, 128),
        new Color(95, 158, 160),
    };

    private List<Slice> slices = new ArrayList<>();
    private int hoveredIndex = -1;
    private String emptyMessage = "No folder selected";

    public DiskChartPanel() {
        setBackground(Color.WHITE);
        setOpaque(false);

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int found = findSliceAt(e.getX(), e.getY());
                if (found != hoveredIndex) {
                    hoveredIndex = found;
                    repaint();
                }
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                hoveredIndex = -1;
                repaint();
            }
        });
    }

    public void setData(File folder) {
        slices.clear();
        hoveredIndex = -1;

        if (folder == null) {
            emptyMessage = "No folder selected";
            repaint();
            return;
        }

        File[] files = folder.listFiles();
        if (files == null || files.length == 0) {
            emptyMessage = "Empty folder";
            repaint();
            return;
        }

        long totalSize = 0;
        List<long[]> rawSizes = new ArrayList<>();
        List<String> names = new ArrayList<>();
        List<Boolean> isDirs = new ArrayList<>();

        for (File f : files) {
            long size = f.isDirectory() ? folderSize(f) : f.length();
            if (size > 0) {
                rawSizes.add(new long[]{size});
                names.add(f.getName());
                isDirs.add(f.isDirectory());
                totalSize += size;
            }
        }

        // Sort descending by size
        Integer[] indices = new Integer[names.size()];
        for (int i = 0; i < indices.length; i++) indices[i] = i;
        final long[] sizes = rawSizes.stream().mapToLong(a -> a[0]).toArray();
        Arrays.sort(indices, (a, b) -> Long.compare(sizes[b], sizes[a]));

        // Group small slices into "Other" if more than 8 items
        int maxSlices = 8;
        long otherSize = 0;
        int otherCount = 0;

        for (int rank = 0; rank < indices.length; rank++) {
            int i = indices[rank];
            if (rank < maxSlices) {
                slices.add(new Slice(names.get(i), sizes[i], totalSize, isDirs.get(i)));
            } else {
                otherSize += sizes[i];
                otherCount++;
            }
        }
        if (otherCount > 0) {
            slices.add(new Slice("Other (" + otherCount + " items)", otherSize, totalSize, false));
        }

        emptyMessage = null;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        if (emptyMessage != null || slices.isEmpty()) {
            g2.setColor(new Color(130, 130, 130));
            g2.setFont(new Font("SansSerif", Font.PLAIN, 15));
            FontMetrics fm = g2.getFontMetrics();
            String msg = emptyMessage != null ? emptyMessage : "Nothing to display";
            g2.drawString(msg, (w - fm.stringWidth(msg)) / 2, h / 2);
            g2.dispose();
            return;
        }

        int legendWidth = 220;
        int chartAreaWidth = w - legendWidth - 20;
        int chartSize = Math.min(chartAreaWidth, h - 40);
        chartSize = Math.max(chartSize, 100);

        int cx = 20 + chartSize / 2;
        int cy = h / 2;
        int outerR = chartSize / 2;
        int innerR = (int) (outerR * 0.58);
        int hoverExpand = 10;

        // Draw slices — start at -90 (top), go clockwise (positive sweep in Java2D)
        double startAngle = -90.0;
        for (int i = 0; i < slices.size(); i++) {
            Slice s = slices.get(i);
            double sweep = s.fraction * 360.0;

            // Store exactly what we draw so findSliceAt can use the same values
            s.startAngle = startAngle;
            s.sweepAngle = sweep;

            Color base = PALETTE[i % PALETTE.length];
            boolean hovered = (i == hoveredIndex);

            int r = outerR + (hovered ? hoverExpand : 0);
            int ir = innerR + (hovered ? hoverExpand / 2 : 0);

            g2.setColor(hovered ? base.brighter() : base);
            g2.fill(makeDonutSlice(cx, cy, r, ir, startAngle, sweep));

            g2.setColor(new Color(255, 255, 255, 180));
            g2.setStroke(new BasicStroke(1.5f));
            g2.draw(new Arc2D.Double(cx - r, cy - r, r * 2, r * 2, startAngle, sweep, Arc2D.PIE));

            startAngle += sweep;
        }

        // Center text
        Slice active = hoveredIndex >= 0 && hoveredIndex < slices.size() ? slices.get(hoveredIndex) : null;
        String centerTop = active != null ? active.name : "Total";
        String centerBot = active != null
                ? readableSize(active.size)
                : readableSize(slices.stream().mapToLong(s -> s.size).sum());

        g2.setColor(new Color(60, 60, 60));
        g2.setFont(new Font("SansSerif", Font.BOLD, 13));
        FontMetrics fm = g2.getFontMetrics();

        int maxCenterWidth = (innerR * 2) - 16;
        String topLabel = centerTop;
        while (topLabel.length() > 1 && fm.stringWidth(topLabel) > maxCenterWidth)
            topLabel = topLabel.substring(0, topLabel.length() - 1);
        if (!topLabel.equals(centerTop)) topLabel += "…";

        g2.drawString(topLabel, cx - fm.stringWidth(topLabel) / 2, cy - 4);

        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g2.setColor(new Color(100, 100, 100));
        fm = g2.getFontMetrics();
        g2.drawString(centerBot, cx - fm.stringWidth(centerBot) / 2, cy + 14);

        if (active != null) {
            String pct = String.format("%.1f%%", active.fraction * 100);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g2.setColor(new Color(130, 130, 130));
            fm = g2.getFontMetrics();
            g2.drawString(pct, cx - fm.stringWidth(pct) / 2, cy + 28);
        }

        // Legend
        int lx = 20 + chartSize + 20;
        int ly = Math.max(20, cy - (slices.size() * 22) / 2);

        for (int i = 0; i < slices.size(); i++) {
            Slice s = slices.get(i);
            Color base = PALETTE[i % PALETTE.length];
            boolean hovered = (i == hoveredIndex);

            g2.setColor(hovered ? base.brighter() : base);
            g2.fillRoundRect(lx, ly + i * 22, 12, 12, 3, 3);

            g2.setColor(hovered ? new Color(20, 20, 20) : new Color(60, 60, 60));
            g2.setFont(new Font("SansSerif", hovered ? Font.BOLD : Font.PLAIN, 12));
            fm = g2.getFontMetrics();

            String label = s.name;
            int maxLW = legendWidth - 20;
            while (label.length() > 1 && fm.stringWidth(label + "  " + readableSize(s.size)) > maxLW)
                label = label.substring(0, label.length() - 1);
            if (!label.equals(s.name)) label += "…";

            g2.drawString(label, lx + 18, ly + i * 22 + 11);

            g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g2.setColor(new Color(120, 120, 120));
            fm = g2.getFontMetrics();
            g2.drawString(readableSize(s.size), lx + 18, ly + i * 22 + 22);
        }

        g2.dispose();
    }

    private Shape makeDonutSlice(int cx, int cy, int outerR, int innerR, double startAngle, double sweep) {
        Area outer = new Area(new Arc2D.Double(cx - outerR, cy - outerR, outerR * 2, outerR * 2,
                startAngle, sweep, Arc2D.PIE));
        Area inner = new Area(new Ellipse2D.Double(cx - innerR, cy - innerR, innerR * 2, innerR * 2));
        outer.subtract(inner);
        return outer;
    }

    private int findSliceAt(int mx, int my) {
        if (slices.isEmpty()) return -1;

        int w = getWidth();
        int legendWidth = 220;
        int chartAreaWidth = w - legendWidth - 20;
        int chartSize = Math.min(chartAreaWidth, getHeight() - 40);
        chartSize = Math.max(chartSize, 100);

        int cx = 20 + chartSize / 2;
        int cy = getHeight() / 2;
        int outerR = chartSize / 2 + 10;
        int innerR = (int) (chartSize / 2 * 0.58);

        double dx = mx - cx, dy = my - cy;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < innerR || dist > outerR) return -1;

        // Raw angle in Java2D convention: 0 = right, counter-clockwise positive
        // This matches exactly what Arc2D uses, so we can compare directly to s.startAngle
        double angle = Math.toDegrees(Math.atan2(-dy, dx));

        for (int i = 0; i < slices.size(); i++) {
            Slice s = slices.get(i);
            double start = s.startAngle;
            double end = start + s.sweepAngle;

            // Normalise angle into the same range as [start, end)
            double a = angle;
            while (a < start) a += 360.0;
            while (a >= start + 360.0) a -= 360.0;

            if (a >= start && a < end) return i;
        }
        return slices.size() - 1;
    }

    private long folderSize(File folder) {
        long total = 0;
        File[] files = folder.listFiles();
        if (files == null) return 0;
        for (File f : files) total += f.isFile() ? f.length() : folderSize(f);
        return total;
    }

    private String readableSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    private static class Slice {
        String name;
        long size;
        double fraction;
        boolean isDirectory;
        double startAngle, sweepAngle;

        Slice(String name, long size, long total, boolean isDirectory) {
            this.name = name;
            this.size = size;
            this.fraction = (double) size / total;
            this.isDirectory = isDirectory;
        }
    }
}
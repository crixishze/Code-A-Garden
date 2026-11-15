package codeagarden;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;

/**
 * The farm screen: grass background, the grid of soil plots, the drone and a message line.
 * It only DRAWS and reacts to clicks. The rules (what a plot is doing, the farm size,
 * where the drone is) live in Farm and Plot, reached through GameSession.
 */
public class FarmPanel extends JPanel {

    private static final int MAX_TILE_SIZE = 260;     // pixels per plot at most
    private static final int MIN_TILE_SIZE = 24;      // lets a 6x6 grid shrink with a small window
    private static final int TILE_GAP = 2;
    private static final int GRID_MARGIN = 20;
    private static final double DRONE_SCALE = 0.5;    // drone size as a fraction of a plot
    private static final int DRONE_LIFT = 6;          // pixels the drone sits above the plot's middle
    private static final double CROP_SCALE = 2.0;     // crop picture size relative to its file
    private static final double CROP_SCALE_READY = 2.2;
    private static final double CROP_MAX_WIDTH = 0.78;   // fractions of the plot a crop may fill
    private static final double CROP_MAX_HEIGHT = 0.82;
    private static final int GROWTH_TICK_MILLIS = 150;
    private static final int MESSAGE_MILLIS = 2500;
    private static final Color GRASS_COLOR = new Color(86, 171, 47);   // shown if the grass picture is missing

    private final GameSession session = GameSession.getInstance();
    private final Runnable onMoneyChanged;
    private final PlotTile[][] tiles = new PlotTile[Farm.MAX_SIZE][Farm.MAX_SIZE];
    private final Map<String, BufferedImage> cropImages = new HashMap<>();
    private final BufferedImage droneImage = Resources.loadImage("/images/drone.png");
    private final BufferedImage plotImage = Resources.loadImage("/images/Plot.png");
    private final BufferedImage backgroundImage = Resources.loadImage("/images/FarmPlot.png");

    // the grid draws its tiles first and the drone on top of them
    private final JPanel grid = new JPanel() {
        @Override
        protected void paintChildren(Graphics g) {
            super.paintChildren(g);
            drawDrone(g);
        }
    };
    private final JPanel center = new JPanel(new GridBagLayout());
    private final JLabel status = new JLabel(" ", SwingConstants.CENTER);
    private final Timer messageTimer;
    private final Timer growthTimer;

    /** onMoneyChanged runs after a harvest so the bottom bar can show the new balance. */
    public FarmPanel(Runnable onMoneyChanged) {
        this.onMoneyChanged = onMoneyChanged;
        setLayout(new BorderLayout());
        setBackground(GRASS_COLOR);

        // create every tile now, so upgrading only has to show more of them
        for (int row = 0; row < Farm.MAX_SIZE; row++) {
            for (int col = 0; col < Farm.MAX_SIZE; col++) {
                PlotTile tile = new PlotTile(row, col);
                tile.addActionListener(e -> onTileClicked(tile.row, tile.col));
                tiles[row][col] = tile;
            }
        }

        grid.setOpaque(false);
        rebuildGrid();

        // GridBagLayout centers the grid at its preferred size
        center.setOpaque(false);
        center.add(grid);
        add(center, BorderLayout.CENTER);
        center.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateGridSize();
            }
        });

        // message line under the grid, just above the bottom bar
        status.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        status.setForeground(Color.WHITE);
        status.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        add(status, BorderLayout.SOUTH);

        // invisible spacer the same height as the message line, so the grid sits in the true middle
        JLabel spacer = new JLabel(" ");
        spacer.setFont(status.getFont());
        spacer.setBorder(status.getBorder());
        add(spacer, BorderLayout.NORTH);

        messageTimer = new Timer(MESSAGE_MILLIS, e -> status.setText(" "));
        messageTimer.setRepeats(false);

        // ONE timer grows every plant (the original had one timer per plant)
        growthTimer = new Timer(GROWTH_TICK_MILLIS, e -> onGrowthTick());
        growthTimer.start();
    }

    /** Stops the timers. Call when the game ends, restarts or returns to the menu. */
    public void stop() {
        growthTimer.stop();
        messageTimer.stop();
    }

    /** Call after the farm was upgraded so the new plots appear. */
    public void refreshGrid() {
        rebuildGrid();
    }

    // grass picture fills the whole panel (scaled to cover, cropping the overflow)
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        double scale = Math.max((double) getWidth() / backgroundImage.getWidth(),
                (double) getHeight() / backgroundImage.getHeight());
        int w = (int) Math.ceil(backgroundImage.getWidth() * scale);
        int h = (int) Math.ceil(backgroundImage.getHeight() * scale);
        g2.drawImage(backgroundImage, (getWidth() - w) / 2, (getHeight() - h) / 2, w, h, null);
        g2.dispose();
    }

    // ---- grid layout ----
    private Farm farm() {
        return session.getFarm();
    }

    // puts the first size x size tiles into the grid panel
    private void rebuildGrid() {
        int size = farm().getSize();
        grid.removeAll();
        grid.setLayout(new GridLayout(size, size, TILE_GAP, TILE_GAP));
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                grid.add(tiles[row][col]);
            }
        }
        updateGridSize();
    }

    // plots grow to fill the available space, so full screen gets bigger plots
    private void updateGridSize() {
        int size = farm().getSize();
        int available = Math.min(center.getWidth(), center.getHeight()) - GRID_MARGIN;
        int tileSize = available > 0 ? (available - (size - 1) * TILE_GAP) / size : MAX_TILE_SIZE / 2;
        tileSize = Math.max(MIN_TILE_SIZE, Math.min(MAX_TILE_SIZE, tileSize));
        int gridPixels = size * tileSize + (size - 1) * TILE_GAP;
        grid.setPreferredSize(new Dimension(gridPixels, gridPixels));
        grid.revalidate();
        grid.repaint();
        center.revalidate();
        repaint();
    }

    private void showStatus(String message) {
        status.setText(message);
        messageTimer.restart();   // each new message gets the full time
    }

    // ---- reacting to the player ----
    private void onTileClicked(int row, int col) {
        Plot plot = farm().getPlot(row, col);
        if (plot.isEmpty()) {
            plant(row, col);
        } else if (plot.isReady()) {
            harvest(row, col);
        }
        // a growing plot does nothing when clicked
    }

    private void plant(int row, int col) {
        Seed seed = session.getInventory().takeSelectedSeed();   // uses up one seed
        if (seed == null) {
            Toolkit.getDefaultToolkit().beep();
            showStatus("No seeds! Buy some in the shop.");
            return;
        }
        farm().getPlot(row, col).plant(seed, System.currentTimeMillis());
        showStatus("Planted " + seed.getName() + "!");
        grid.repaint();
    }

    private void harvest(int row, int col) {
        Seed seed = farm().getPlot(row, col).harvest();
        session.addBalance(seed.getSellPrice());
        grid.repaint();
        showStatus("Harvested " + seed.getName() + "!");
        onMoneyChanged.run();
    }

    private void onGrowthTick() {
        List<Plot> ready = farm().tick(System.currentTimeMillis());
        for (Plot plot : ready) {
            showStatus(plot.getSeed().getName() + " is ready to harvest!");
        }
        grid.repaint();
    }

    // ---- drone commands (called by DroneController on the Swing thread) ----
    public void move(String direction) {
        farm().moveDrone(direction);
        grid.repaint();
    }

    public void plantHere() {
        if (farm().getDronePlot().isEmpty()) {
            plant(farm().getDroneRow(), farm().getDroneCol());
        }
    }

    public void harvestHere() {
        if (farm().getDronePlot().isReady()) {
            harvest(farm().getDroneRow(), farm().getDroneCol());
        }
    }

    public boolean canHarvest() {
        return farm().getDronePlot().isReady();
    }

    // ---- drawing ----
    private void drawDrone(Graphics g) {
        Rectangle b = tiles[farm().getDroneRow()][farm().getDroneCol()].getBounds();
        int s = (int) (Math.min(b.width, b.height) * DRONE_SCALE);
        int x = b.x + (b.width - s) / 2;
        int y = b.y + (b.height - s) / 2 - DRONE_LIFT;
        g.drawImage(droneImage, x, y, s, s, null);
    }

    // crop pictures are loaded once and remembered: images/<CropName>/Stage1.png .. Stage3.png
    private BufferedImage cropImage(String cropName, int stage) {
        String key = cropName + "#" + stage;
        BufferedImage image = cropImages.get(key);
        if (image == null) {
            image = Resources.loadImage("/images/" + cropName + "/Stage" + stage + ".png");
            cropImages.put(key, image);
        }
        return image;
    }

    /** One square of soil on screen. It draws the soil and the crop of its Plot. */
    private class PlotTile extends JButton {
        private final int row;
        private final int col;

        PlotTile(int row, int col) {
            this.row = row;
            this.col = col;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);   // keeps pixel art crisp
            g2.drawImage(plotImage, 0, 0, getWidth(), getHeight(), null);

            Plot plot = farm().getPlot(row, col);
            if (!plot.isEmpty()) {
                drawCrop(g2, plot);
            }
            g2.dispose();
        }

        private void drawCrop(Graphics2D g2, Plot plot) {
            int stage = plot.getStage(System.currentTimeMillis());
            BufferedImage crop = cropImage(plot.getSeed().getName(), stage);

            // keep the picture's proportions, and keep it inside the plot
            double scale = stage >= 3 ? CROP_SCALE_READY : CROP_SCALE;
            int width = Math.max(1, (int) Math.round(crop.getWidth() * scale));
            int height = Math.max(1, (int) Math.round(crop.getHeight() * scale));
            double fit = Math.min(1.0, Math.min(getWidth() * CROP_MAX_WIDTH / width,
                    getHeight() * CROP_MAX_HEIGHT / height));
            width = Math.max(1, (int) Math.round(width * fit));
            height = Math.max(1, (int) Math.round(height * fit));
            g2.drawImage(crop, (getWidth() - width) / 2, (getHeight() - height) / 2, width, height, null);
        }
    }
}

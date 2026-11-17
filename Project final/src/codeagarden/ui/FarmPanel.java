package codeagarden.ui;

import codeagarden.game.Farm;
import codeagarden.game.GameSession;
import codeagarden.game.Plot;
import codeagarden.game.Seed;
import codeagarden.game.SeedFactory;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
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
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;

/**
 * The farm screen: grass background, the 4 x 6 grid of soil plots, the drone and a message line.
 * The plots can NOT be clicked: the farm is only changed by the player's code, through the
 * drone commands (moveDrone, plantHere, harvestHere, canHarvest) that DroneController calls.
 * The rules (what a plot is doing, where the drone is) live in Farm and Plot.
 */
public class FarmPanel extends JPanel {

    private static final int MAX_TILE_SIZE = 200;     // pixels per plot at most
    private static final int MIN_TILE_SIZE = 24;
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
    private final PlotTile[][] tiles = new PlotTile[Farm.ROWS][Farm.COLS];
    private final Map<String, BufferedImage> cropImages = new HashMap<>();
    private final BufferedImage droneImage = Resources.loadImage("/images/drone.png");
    private final BufferedImage plotImage = Resources.loadImage("/images/Plot.png");
    private final BufferedImage backgroundImage = Resources.loadImage("/images/FarmPlot.png");

    // the grid draws its tiles first and the drone on top of them
    private final JPanel grid = new JPanel(new GridLayout(Farm.ROWS, Farm.COLS, TILE_GAP, TILE_GAP)) {
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

        // 4 rows x 6 columns of plots, added row by row
        grid.setOpaque(false);
        for (int row = 0; row < Farm.ROWS; row++) {
            for (int col = 0; col < Farm.COLS; col++) {
                tiles[row][col] = new PlotTile(row, col);
                grid.add(tiles[row][col]);
            }
        }

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

        // message line under the grid
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

        // ONE timer grows every plant
        growthTimer = new Timer(GROWTH_TICK_MILLIS, e -> onGrowthTick());
        growthTimer.start();
    }

    /** Stops the timers. Call when the game ends, restarts or returns to the menu. */
    public void stop() {
        growthTimer.stop();
        messageTimer.stop();
    }

    /** Shows a message under the farm for a few seconds (each new message gets the full time). */
    public void showMessage(String message) {
        status.setText(message);
        messageTimer.restart();
    }

    /** Draws the grass picture over the whole panel (scaled to cover, cropping the overflow). */
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

    // ---- drone commands (called by DroneController on the Swing thread) ----

    /** move(North/South/East/West): moves the drone one plot, or says why it can't. */
    public void moveDrone(String direction) {
        if (!farm().moveDrone(direction)) {
            boolean isDirection = direction.equalsIgnoreCase("North") || direction.equalsIgnoreCase("South")
                    || direction.equalsIgnoreCase("East") || direction.equalsIgnoreCase("West");
            showMessage(isDirection
                    ? "The drone can't move " + direction + ": that is the edge of the farm."
                    : "\"" + direction + "\" is not a direction. Use North, South, East or West.");
        }
        grid.repaint();
    }

    /** plant(Carrot): plants one owned seed of that kind in the plot under the drone. */
    public void plantHere(String typedSeed) {
        String seedName = SeedFactory.findSeedName(typedSeed);
        if (seedName == null) {
            showMessage("\"" + typedSeed + "\" is not a seed. Use Carrot, Corn, Tomato or Pumpkin.");
            return;
        }
        Plot plot = farm().getDronePlot();
        if (!plot.isEmpty()) {
            showMessage("This plot already has a crop.");
            return;
        }
        Seed seed = session.getInventory().takeSeed(seedName);   // uses up one seed
        if (seed == null) {
            Toolkit.getDefaultToolkit().beep();
            showMessage("No " + seedName + " seeds! Buy some in the shop.");
            return;
        }
        plot.plant(seed, System.currentTimeMillis());
        showMessage("Planted " + seedName + "!");
        grid.repaint();
    }

    /** harvest(): sells the ready crop under the drone and adds its price to the balance. */
    public void harvestHere() {
        Plot plot = farm().getDronePlot();
        if (!plot.isReady()) {
            showMessage(plot.isEmpty() ? "Nothing is planted here." : "This crop is not ready yet.");
            return;
        }
        Seed seed = plot.harvest();
        session.addBalance(seed.getSellPrice());
        grid.repaint();
        showMessage("Harvested " + seed.getName() + " for $" + seed.getSellPrice() + "!");
        onMoneyChanged.run();
    }

    /** canHarvest(): true if the plot under the drone has a ready crop. */
    public boolean canHarvest() {
        return farm().getDronePlot().isReady();
    }

    // ---- inside ----

    // The farm of the current game (always read fresh from GameSession).
    private Farm farm() {
        return session.getFarm();
    }

    // Plots grow to fill the space: the biggest square size where 6 fit across and 4 fit down.
    private void updateGridSize() {
        int width = center.getWidth() - GRID_MARGIN - (Farm.COLS - 1) * TILE_GAP;
        int height = center.getHeight() - GRID_MARGIN - (Farm.ROWS - 1) * TILE_GAP;
        int tileSize = Math.min(width / Farm.COLS, height / Farm.ROWS);
        tileSize = Math.max(MIN_TILE_SIZE, Math.min(MAX_TILE_SIZE, tileSize));
        grid.setPreferredSize(new Dimension(Farm.COLS * tileSize + (Farm.COLS - 1) * TILE_GAP,
                Farm.ROWS * tileSize + (Farm.ROWS - 1) * TILE_GAP));
        grid.revalidate();
        center.revalidate();
        repaint();
    }

    // Called every 150 ms: grows the crops and announces the ones that just became ready.
    private void onGrowthTick() {
        List<Plot> ready = farm().tick(System.currentTimeMillis());
        for (Plot plot : ready) {
            showMessage(plot.getSeed().getName() + " is ready to harvest!");
        }
        grid.repaint();
    }

    // Draws the drone picture over the plot it is above.
    private void drawDrone(Graphics g) {
        Rectangle b = tiles[farm().getDroneRow()][farm().getDroneCol()].getBounds();
        int s = (int) (Math.min(b.width, b.height) * DRONE_SCALE);
        int x = b.x + (b.width - s) / 2;
        int y = b.y + (b.height - s) / 2 - DRONE_LIFT;
        g.drawImage(droneImage, x, y, s, s, null);
    }

    // Crop pictures are loaded once and remembered: images/<CropName>/Stage1.png .. Stage3.png
    private BufferedImage cropImage(String cropName, int stage) {
        String key = cropName + "#" + stage;
        BufferedImage image = cropImages.get(key);
        if (image == null) {
            image = Resources.loadImage("/images/" + cropName + "/Stage" + stage + ".png");
            cropImages.put(key, image);
        }
        return image;
    }

    /** One square of soil on screen. It only draws (it is not a button): the soil and its crop. */
    private class PlotTile extends JComponent {
        private final int row;
        private final int col;

        PlotTile(int row, int col) {
            this.row = row;
            this.col = col;
            setOpaque(false);
        }

        /** Draws the soil picture, then the crop picture for the crop's growth stage. */
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

        // Keeps the crop picture's proportions and keeps it inside the plot.
        private void drawCrop(Graphics2D g2, Plot plot) {
            int stage = plot.getStage(System.currentTimeMillis());
            BufferedImage crop = cropImage(plot.getSeed().getName(), stage);
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

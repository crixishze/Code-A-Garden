package codeagarden;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.util.function.IntConsumer;
import java.util.Map;
import java.util.HashMap;
import javax.swing.*;

public class FarmPlot extends JPanel {

    private enum TileState {
        EMPTY,
        GROWING,
        READY
    }

    public static final int START_SIZE = 3;
    public static final int MAX_SIZE = 6;

    private static final int MAX_TILE_SIZE = 260;   // pixels per plot at most (tiles now scale with the window)
    private static final int MIN_TILE_SIZE = 24;    // lets a 6x6 grid shrink with a small window
    private static final double DRONE_SCALE = 0.5;  // drone size as a fraction of a tile (was 0.8)
    private static final int TILE_GAP = 2;
    private static final Color GRASS_COLOR = new Color(86, 171, 47); // fallback behind the grass image

    // arrays are sized for the maximum grid; only the first size x size tiles are shown
    private final PlotTile[][] tiles = new PlotTile[MAX_SIZE][MAX_SIZE];
    private final TileState[][] tileStates = new TileState[MAX_SIZE][MAX_SIZE];
    private final Seed[][] planted = new Seed[MAX_SIZE][MAX_SIZE]; // null = empty
    private final long[][] plantedAtMillis = new long[MAX_SIZE][MAX_SIZE];
    private final BufferedImage[][] cropStageImages = new BufferedImage[MAX_SIZE][MAX_SIZE];
    private final Map<String, BufferedImage> cropImageCache = new HashMap<>();
    private final Map<String, BufferedImage> tintedCropImageCache = new HashMap<>();
    private final Inventory inventory;
    private final IntConsumer harvestReward;

    private final JPanel grid = new JPanel() {
        @Override
        protected void paintChildren(Graphics g) {
            super.paintChildren(g);   // puts tiles first
            makeDrone(g);             // puts drone on top of tile

        }
    };
    private final JPanel center = new JPanel(new GridBagLayout());
    private int size = START_SIZE;

    private final JLabel status = new JLabel(" ", SwingConstants.CENTER);
    private final Timer statusTimer;
    private final DayNightCycle dayNight = new DayNightCycle(60); // one full day/night cycle per 60 seconds

    private int droneRow = 0;
    private int droneCol = 0;
    private final BufferedImage droneImage = loadImage("/images/drone.png");
    private final BufferedImage plotImage = loadImage("/images/Plot.png");           // soil tile
    private final BufferedImage backgroundImage = loadImage("/images/FarmPlot.png"); // grass background
    // Keep this as an ImageIcon (rather than ImageIO/BufferedImage) so Swing can animate GIF frames.
    private final ImageIcon harvestReadyIcon = loadAnimatedIcon("/images/Exclamation.gif");
    // Pre-tinted copies preserve each PNG's original alpha channel. This prevents
    // the tint from coloring transparent pixels or the components behind them.
    private BufferedImage tintedDroneImage;
    private BufferedImage tintedPlotImage;
    private BufferedImage tintedBackgroundImage;
    private Color cachedAssetTint;

    private static BufferedImage loadImage(String path) {
        try {
            return ImageIO.read(FarmPlot.class.getResource(path));
        } catch (Exception e) {
            throw new RuntimeException("Could not load " + path, e);
        }
    }

    private static BufferedImage loadOptionalImage(String path) {
        try (java.io.InputStream in = FarmPlot.class.getResourceAsStream(path)) {
            return in == null ? null : ImageIO.read(in);
        } catch (Exception e) {
            System.err.println("Could not load optional image " + path + ": " + e.getMessage());
            return null;
        }
    }

    private static ImageIcon loadAnimatedIcon(String path) {
        java.net.URL url = FarmPlot.class.getResource(path);
        if (url == null) {
            System.err.println("Could not find harvest-ready icon at classpath resource: " + path);
            return null;
        }
        ImageIcon icon = new ImageIcon(url);
        if (icon.getIconWidth() <= 0 || icon.getIconHeight() <= 0) {
            System.err.println("Could not load harvest-ready icon: " + url);
            return null;
        }
        return icon;
    }

    // One soil plot; crop state is shown by the crop sprite and harvest-ready icon.
    private class PlotTile extends JButton {
        private final int row;
        private final int col;

        PlotTile(String text, int row, int col) {
            super(text);
            this.row = row;
            this.col = col;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setForeground(Color.WHITE);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR); // keeps pixel art crisp
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            g2.drawImage(tintedPlotImage != null ? tintedPlotImage : plotImage,
                    0, 0, getWidth(), getHeight(), null);

            // Draw the crop artwork over the soil. Crop labels are intentionally not
            // rendered on the plot; the stage artwork communicates growth visually.
            int cropTopY = getHeight() / 2; // fallback anchor if a crop sprite failed to load
            if (cropStageImages[row][col] != null) {
                BufferedImage crop = tintedCropImageCache.get(cropImageKey(planted[row][col], currentStage(row, col)));
                if (crop == null) crop = cropStageImages[row][col];
                // Preserve the sprite's original aspect ratio instead of forcing it into a square.
                int stage = currentStage(row, col);
                double scale = stage >= 3 ? 2.2 : 2.0;
                int cropWidth = Math.max(1, (int) Math.round(crop.getWidth() * scale));
                int cropHeight = Math.max(1, (int) Math.round(crop.getHeight() * scale));

                // Keep sprites inside the plot tile without distorting them.
                int maxWidth = Math.max(1, (int) (getWidth() * 0.78));
                int maxHeight = Math.max(1, (int) (getHeight() * 0.82));
                double fitScale = Math.min(1.0, Math.min(maxWidth / (double) cropWidth,
                                                        maxHeight / (double) cropHeight));
                cropWidth = Math.max(1, (int) Math.round(cropWidth * fitScale));
                cropHeight = Math.max(1, (int) Math.round(cropHeight * fitScale));

                int cx = (getWidth() - cropWidth) / 2;
                int cy = (getHeight() - cropHeight) / 2;
                cropTopY = cy;
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g2.drawImage(crop, cx, cy, cropWidth, cropHeight, null);
            }

            // Draw the animated harvest icon independently of the crop sprite. This way,
            // a missing/unreadable crop-stage image cannot suppress the ready indicator.
            if (tileStates[row][col] == TileState.READY && harvestReadyIcon != null
                    && harvestReadyIcon.getIconWidth() > 0 && harvestReadyIcon.getIconHeight() > 0) {
                double iconScale = 1.5;
                int iconWidth = Math.max(1, (int) Math.round(harvestReadyIcon.getIconWidth() * iconScale));
                int iconHeight = Math.max(1, (int) Math.round(harvestReadyIcon.getIconHeight() * iconScale));
                int iconX = (getWidth() - iconWidth) / 2;
                int iconY = Math.max(0, cropTopY - iconHeight - 5);
                Graphics2D iconGraphics = (Graphics2D) g2.create();
                iconGraphics.translate(iconX, iconY);
                iconGraphics.scale(iconScale, iconScale);
                // The tile is the ImageObserver, so Swing can repaint as GIF frames advance.
                harvestReadyIcon.paintIcon(this, iconGraphics, 0, 0);
                iconGraphics.dispose();
            }
            g2.dispose();
        }
    }

    public FarmPlot(Inventory inventory, IntConsumer harvestReward) {
        this.inventory = inventory;
        this.harvestReward = harvestReward;

        setLayout(new BorderLayout());
        setBackground(GRASS_COLOR);

        // create every tile now, so upgrading only has to show more of them
        for (int i = 0; i < MAX_SIZE; i++) {
            for (int j = 0; j < MAX_SIZE; j++) {
                PlotTile tile = new PlotTile("", i, j);

                final int r = i;
                final int c = j;
                tileStates[i][j] = TileState.EMPTY;
                tile.addActionListener(e -> onTileClicked(r, c));

                tiles[i][j] = tile;
            }
        }

        grid.setOpaque(false);
        rebuildGrid();

        // GridBagLayout centers the grid at its preferred size
        center.setOpaque(false);
        center.add(grid);
        add(center, BorderLayout.CENTER);

        // whenever the window changes size (full screen, resize), resize the plots to match
        center.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateGridSize();
            }
        });

        // message line under the grid, just above the bottom bar
        status.setFont(new Font("SansSerif", Font.BOLD, 18));
        status.setForeground(Color.WHITE);
        status.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        add(status, BorderLayout.SOUTH);

        // invisible spacer the same height as the message line, so the grid sits in the true middle
        JLabel spacer = new JLabel(" ");
        spacer.setFont(status.getFont());
        spacer.setBorder(status.getBorder());

        add(spacer, BorderLayout.NORTH);

        statusTimer = new Timer(2500, e -> status.setText(" ")); // clears the message
        statusTimer.setRepeats(false);
        new Timer(30, e -> grid.repaint()).start();
        updateTintedAssets();
        dayNight.start(() -> {
            updateTintedAssets();
            repaint();
            grid.repaint();
        });

        setupFullScreenKey();
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
        int x = (getWidth() - w) / 2;
        int y = (getHeight() - h) / 2;
        g2.drawImage(tintedBackgroundImage != null ? tintedBackgroundImage : backgroundImage,
                x, y, w, h, null);
        g2.dispose();
    }

    // ---- full screen: press F11 to toggle, Esc to leave ----
    private boolean fullScreen = false;

    private void setupFullScreenKey() {
        InputMap im = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0), "toggleFullScreen");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "exitFullScreen");
        am.put("toggleFullScreen", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setFullScreen(!fullScreen);
            }
        });
        am.put("exitFullScreen", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (fullScreen) setFullScreen(false);
            }
        });
    }

    public void setFullScreen(boolean on) {
        Window w = SwingUtilities.getWindowAncestor(this);
        if (!(w instanceof JFrame frame) || on == fullScreen) {
            return;
        }
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        frame.dispose();                 // decoration can only change while the frame isn't showing
        frame.setUndecorated(on);
        if (on) {
            device.setFullScreenWindow(frame);
        } else {
            device.setFullScreenWindow(null);
        }
        frame.setVisible(true);
        fullScreen = on;
        revalidate();
        repaint();
    }

    public int getGridSize() {
        return size;
    }

    public boolean canExpand() {
        return size < MAX_SIZE;
    }

    // called by the Store after an upgrade is paid for
    public void expand() {
        if (!canExpand()) {
            return;
        }
        size++;
        rebuildGrid();
    }

    // puts the first size x size tiles into the grid panel
    private void rebuildGrid() {
        grid.removeAll();
        grid.setLayout(new GridLayout(size, size, TILE_GAP, TILE_GAP));
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                grid.add(tiles[i][j]);
            }
        }
        updateGridSize();
    }

    // plots grow to fill the available space, so full screen gets bigger plots
    private void updateGridSize() {
        int available = Math.min(center.getWidth(), center.getHeight()) - 20; // small margin around the grid
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
        statusTimer.restart(); // each new message gets the full 2.5 seconds
    }

    private void onTileClicked(int row, int col) {
        switch (tileStates[row][col]) {
            case EMPTY -> plant(row, col);
            case READY -> harvest(row, col);
            case GROWING -> { }
        }
    }

    private void plant(int row, int col) {
        Seed seed = inventory.takeSelectedSeed(); // uses up one seed
        if (seed == null) {
            Toolkit.getDefaultToolkit().beep();
            showStatus("No seeds! Buy some in the shop.");
            return;
        }

        planted[row][col] = seed;
        plantedAtMillis[row][col] = System.currentTimeMillis();
        tileStates[row][col] = TileState.GROWING;
        tiles[row][col].setText("");
        cropStageImages[row][col] = loadCropStage(seed.getName(), 1);
        if (cropStageImages[row][col] == null) {
            showStatus("Missing crop image: images/" + seed.getName() + "/Stage1.png");
        } else {
            showStatus("Planted " + seed.getName() + "!");
        }
        grid.repaint();

        // Growth is split into three visual stages across the seed's existing grow time.
        int growMillis = Math.max(1, seed.getGrowTime() * 1000);
        Timer growthTimer = new Timer(150, e -> {
            if (planted[row][col] != seed) {
                ((Timer) e.getSource()).stop();
                return;
            }
            long elapsed = System.currentTimeMillis() - plantedAtMillis[row][col];
            double progress = Math.min(1.0, elapsed / (double) growMillis);
            int stage = progress >= (2.0 / 3.0) ? 3 : progress >= (1.0 / 3.0) ? 2 : 1;
            BufferedImage nextStageImage = loadCropStage(seed.getName(), stage);
            if (nextStageImage != null) cropStageImages[row][col] = nextStageImage;
            if (progress >= 1.0) {
                tileStates[row][col] = TileState.READY;
                ((Timer) e.getSource()).stop();
                showStatus(seed.getName() + " is ready to harvest!");
            }
            grid.repaint();
        });
        growthTimer.start();
    }

    private void harvest(int row, int col) {
        Seed seed = planted[row][col];
        if (seed == null) return;
        planted[row][col] = null;
        plantedAtMillis[row][col] = 0L;
        cropStageImages[row][col] = null;
        tileStates[row][col] = TileState.EMPTY;
        tiles[row][col].setText("");
        grid.repaint();
        harvestReward.accept(seed.getSellPrice());
        showStatus("Harvested " + seed.getName() + "!");
    }

    private int currentStage(int row, int col) {
        Seed seed = planted[row][col];
        if (seed == null) return 1;
        if (tileStates[row][col] == TileState.READY) return 3;
        int growMillis = Math.max(1, seed.getGrowTime() * 1000);
        double progress = Math.min(1.0, (System.currentTimeMillis() - plantedAtMillis[row][col]) / (double) growMillis);
        if (progress >= 2.0 / 3.0) return 3;
        if (progress >= 1.0 / 3.0) return 2;
        return 1;
    }

    private String cropImageKey(Seed seed, int stage) {
        return seed == null ? "" : seed.getName() + "#" + stage;
    }

    /** Loads images/<CropName>/Stage1.png (and Stage2/Stage3) from the classpath. */
    private BufferedImage loadCropStage(String cropName, int stage) {
        String key = cropName + "#" + stage;
        BufferedImage cached = cropImageCache.get(key);
        if (cached != null) return cached;

        String base = "/images/" + cropName + "/Stage" + stage;
        String[] extensions = { ".png", ".PNG", ".jpg", ".jpeg", ".gif" };
        for (String extension : extensions) {
            try (java.io.InputStream in = FarmPlot.class.getResourceAsStream(base + extension)) {
                if (in != null) {
                    BufferedImage image = ImageIO.read(in);
                    if (image != null) {
                        cropImageCache.put(key, image);
                        tintedCropImageCache.put(key, tintImage(image, dayNight.getTint()));
                        return image;
                    }
                }
            } catch (Exception ignored) { }
        }
        // Try the path without an extension too, for projects that package it that way.
        try (java.io.InputStream in = FarmPlot.class.getResourceAsStream(base)) {
            if (in != null) {
                BufferedImage image = ImageIO.read(in);
                if (image != null) {
                    cropImageCache.put(key, image);
                    tintedCropImageCache.put(key, tintImage(image, dayNight.getTint()));
                    return image;
                }
            }
        } catch (Exception ignored) { }
        return null;
    }

    //drone functions
    public void move(String direction) {
        int r = droneRow, c = droneCol;
        switch (direction) {                                     //drone movement
            case "North": r--; break;
            case "South": r++; break;
            case "East":  c++; break;
            case "West":  c--; break;
        }

        if (r < 0 || r >= size || c < 0 || c >= size) return;   //so it can't leave the grid
        droneRow = r;
        droneCol = c;
        grid.repaint();
    }
    //plant and harvest functions
    public void plantHere() {
        if (tileStates[droneRow][droneCol] == TileState.EMPTY) plant(droneRow, droneCol);
    }
    
    public void harvestHere() {
        if (tileStates[droneRow][droneCol] == TileState.READY) harvest(droneRow, droneCol);
    }
    
    public boolean canHarvest() {
        return tileStates[droneRow][droneCol] == TileState.READY;
    }

    private void makeDrone(Graphics g) {
        Rectangle b = tiles[droneRow][droneCol].getBounds();
        int s = (int) (Math.min(b.width, b.height) * DRONE_SCALE);
        int bob = (int) (Math.sin(System.currentTimeMillis() / 250.0) * 4);   //to hover up and down

        int x = b.x + (b.width - s) / 2;
        int y = b.y + (b.height - s) / 2 - 6 + bob;

        g.setColor(new Color(0, 0, 0, 70));                                   //gives shadow
        g.fillOval(b.x + b.width / 4, b.y + b.height * 3 / 4, b.width / 2, b.height / 8);

        g.drawImage(tintedDroneImage != null ? tintedDroneImage : droneImage, x, y, s, s, null);
    }

    /**
     * Refreshes tinted copies of the assets. Tint is applied to each source pixel
     * using its alpha value, so transparent PNG areas stay transparent.
     */
    private void updateTintedAssets() {
        Color tint = dayNight.getTint();
        if (tint.equals(cachedAssetTint)) {
            return;
        }
        cachedAssetTint = tint;
        tintedPlotImage = tintImage(plotImage, tint);
        tintedBackgroundImage = tintImage(backgroundImage, tint);
        tintedDroneImage = tintImage(droneImage, tint);
        tintedCropImageCache.clear();
        for (Map.Entry<String, BufferedImage> entry : cropImageCache.entrySet()) {
            tintedCropImageCache.put(entry.getKey(), tintImage(entry.getValue(), tint));
        }
    }

    private static BufferedImage tintImage(BufferedImage source, Color tint) {
        BufferedImage result = new BufferedImage(
                source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        int tintAlpha = tint.getAlpha();
        int tr = tint.getRed();
        int tg = tint.getGreen();
        int tb = tint.getBlue();

        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int argb = source.getRGB(x, y);
                int alpha = (argb >>> 24) & 0xff;
                if (alpha == 0 || tintAlpha == 0) {
                    // Preserve fully transparent pixels exactly.
                    result.setRGB(x, y, argb);
                    continue;
                }

                int r = (argb >>> 16) & 0xff;
                int g = (argb >>> 8) & 0xff;
                int b = argb & 0xff;
                float amount = tintAlpha / 255.0f;
                int nr = Math.round(r + (tr - r) * amount);
                int ng = Math.round(g + (tg - g) * amount);
                int nb = Math.round(b + (tb - b) * amount);
                result.setRGB(x, y, (alpha << 24) | (nr << 16) | (ng << 8) | nb);
            }
        }
        return result;
    }
}

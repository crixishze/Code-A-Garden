package codeagarden;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.*;
import java.util.function.IntConsumer;
import javax.swing.*;

public class FarmPlot extends JPanel {

    private enum TileState {
        EMPTY,
        GROWING,
        READY
    }

    public static final int START_SIZE = 3;
    public static final int MAX_SIZE = 6;

    private static final int MAX_TILE_SIZE = 140;   // pixels per tile on small grids
    private static final int MAX_GRID_PIXELS = 700; // bigger grids shrink their tiles to stay under this
    private static final Color GRASS_COLOR = new Color(86, 171, 47);
    private static final Color SOIL_COLOR = new Color(139, 90, 43);
    private static final Color GROWING_COLOR = new Color(218, 165, 32);
    private static final Color READY_COLOR = new Color(34, 139, 34);

    // arrays are sized for the maximum grid; only the first size x size tiles are shown
    private final JButton[][] tiles = new JButton[MAX_SIZE][MAX_SIZE];
    private final TileState[][] tileStates = new TileState[MAX_SIZE][MAX_SIZE];
    private final Seed[][] planted = new Seed[MAX_SIZE][MAX_SIZE]; // null = empty
    private final Inventory inventory;
    private final IntConsumer harvestReward;

    private final JPanel grid = new JPanel() {
        @Override
        protected void paintChildren(Graphics g) {
            super.paintChildren(g);   // puts tiles first
            makeDrone(g);             // puts drone on top of tile
        }
    };
    private int size = START_SIZE;

    private final JLabel status = new JLabel(" ", SwingConstants.CENTER);
    private final Timer statusTimer;

    private int droneRow = 0;
    private int droneCol = 0;
    private final BufferedImage droneImage = loadImage("/images/drone.png");
    private static BufferedImage loadImage(String path) {
        try {
            return ImageIO.read(FarmPlot.class.getResource(path));
        } catch (Exception e) {
            throw new RuntimeException("Could not load " + path, e);
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
                JButton tile = new JButton("Empty");
                tile.setBackground(SOIL_COLOR);
                tile.setForeground(Color.WHITE);
                tile.setFocusPainted(false);

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
        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        center.add(grid);
        add(center, BorderLayout.CENTER);

        // message line under the grid, just above the bottom bar
        status.setFont(new Font("SansSerif", Font.BOLD, 18));
        status.setForeground(new Color(20, 50, 10));
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
        grid.setLayout(new GridLayout(size, size, 5, 5));
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                grid.add(tiles[i][j]);
            }
        }
        int tileSize = Math.min(MAX_TILE_SIZE, MAX_GRID_PIXELS / size);
        grid.setPreferredSize(new Dimension(size * tileSize, size * tileSize));
        grid.revalidate();
        grid.repaint();
        revalidate();
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
        tileStates[row][col] = TileState.GROWING;
        JButton tile = tiles[row][col];
        tile.setText(seed.getName() + "...");
        tile.setBackground(GROWING_COLOR);
        showStatus("Planted " + seed.getName() + "!");

        Timer timer = new Timer(seed.getGrowTime() * 1000, e -> {
            tileStates[row][col] = TileState.READY;
            tile.setText(seed.getName() + " ready!");
            tile.setBackground(READY_COLOR);
        });
        timer.setRepeats(false); // fires once, then stops by itself
        timer.start();
    }

    private void harvest(int row, int col) {
        Seed seed = planted[row][col];
        planted[row][col] = null;
        tileStates[row][col] = TileState.EMPTY;
        tiles[row][col].setText("Empty");
        tiles[row][col].setBackground(SOIL_COLOR);
        harvestReward.accept(seed.getSellPrice());
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
        int s = (int) (Math.min(b.width, b.height) * 0.8);
        int bob = (int) (Math.sin(System.currentTimeMillis() / 250.0) * 4);   //to hover up and down

        int x = b.x + (b.width - s) / 2;
        int y = b.y + (b.height - s) / 2 - 6 + bob;

        g.setColor(new Color(0, 0, 0, 70));                                   //gives shadow
        g.fillOval(b.x + b.width / 4, b.y + b.height * 3 / 4, b.width / 2, b.height / 8);

        g.drawImage(droneImage, x, y, s, s, null);
    }
}
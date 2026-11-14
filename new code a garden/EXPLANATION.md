# Code A Garden: Explanation of Everything

This file explains the whole project in plain language so anyone in the group
can present it. It is updated after every change. Newest changes are at the bottom.

## 1. What the project is
A farming game made with Java Swing (Java's built-in window toolkit). You buy
seeds, plant them, harvest them for money, and try to reach $10,000 in 30
in-game days. A small drone can be programmed with a simple script to farm for you.

## 2. Compatibility (works on other PCs)
- The code is written for **Java 17**. A program built for Java 17 also runs on
  newer Java (21...). So any PC with Java 17 or newer can run it. The script
  interpreter (made by a teammate) uses Java 17 features, so older Java will not work.
- We compile with the flag `--release 17`. It makes the compiler refuse anything
  newer than Java 17, so we cannot accidentally use features the other PC lacks.
- Before the demo, check `java -version` on each PC: it must say 17 or higher.
- No outside libraries are used except the MySQL driver (added later for saving).
- Images and fonts are loaded from inside the project (the "classpath"), not from
  fixed folders on one computer, so moving the project to another PC keeps working.

## 3. How to run
See README.md. Short version, from the project folder:

    mkdir -p out
    javac --release 17 -d out $(find src -name "*.java")
    java -cp out:src codeagarden.Main

(`javac` compiles the code, `java` runs it. `-cp` is the classpath: where Java
looks for compiled code (`out`) and pictures/fonts (`src`). On Windows use `;`
instead of `:`.)

## 4. Folder map
- `src/codeagarden/`  all Java code
- `src/images/`, `src/fonts/`  pictures and font, loaded by paths like `/images/title.png`
- `lib/`  outside jar files (MySQL driver later)
- `sql/`  database setup script (later)
- `.vscode/settings.json`  tells VS Code where code, output and jars are
- `CLAUDE.md`  the rules for how this rewrite is done
- `EXPLANATION.md`  this file

## 5. The code so far
### Main.java
The only starting point. `main()` is the method Java runs first. It asks Swing to
build the menu with `SwingUtilities.invokeLater`, because Swing windows must be
created on the "Event Dispatch Thread" (the one thread Swing uses for everything
you see). Calls: MainMenu. Called by: Java itself.

### Resources.java (shared loader)
One place that loads pictures and fonts from the classpath. `loadImage(path)` returns a
picture, or stops with an error naming the exact missing path (a missing picture is a
real bug). `loadFont(path, size)` returns the font, or falls back to SansSerif with a
warning (a missing font should not crash the game). It replaces the three copies of
`loadImage`/`loadFont` in the original MainMenu, BottomBar and FarmPlot. It has a private
constructor because it only has static methods and nobody should create one.
Called by: Theme, MainMenu (later: every screen). Calls: nothing of ours.

### Theme.java (shared colors and fonts)
All colors (wood, parchment, buttons, messages, HUD, menu text) and fonts (PIXEL, HUD =
Poppins Black, DIALOG = DD) as named constants, e.g. `Theme.WOOD_DARK`. It replaces the
palette that Store, Inventory and StoryPanel each declared separately. To get another
size of a font: `Theme.PIXEL.deriveFont(32f)`. Calls: Resources (to load the two font
files). Called by: MainMenu now; later every screen.

### Layer 1: the game's data and rules (no windows, no drawing)
These classes know the rules of the game but nothing about buttons or screens. That
separation means the rules can be tested without a window, and the screens we build
next only have to display what these classes say.

**Seed.java**: one kind of crop (name, price, grow time in seconds, sell price). It is
immutable: after it is created its numbers never change. Used by everything below.

**SeedFactory.java (DESIGN PATTERN 1: Factory Method)**
- Problem it solves: in the original, the seed numbers (for example Carrot 10/3/20) were typed
  in two places, Store and StoryModel, so they could drift apart.
- How it works: `SeedFactory.createSeed("Carrot")` is the factory method. It is the only place
  that knows each seed's numbers. Shop, story and inventory just ask it for a seed by name.
  `createAllSeeds()` gives the shop its list, cheapest first.
- Demo line: "To add a new crop I change one class, SeedFactory, and the shop, story and
  inventory all pick it up."
- Honest note: this is the common "static factory method" form of the pattern. The strict
  textbook form has a creator class with subclasses that each decide what to create. If your
  teacher expects that exact shape, tell me and I will restructure it.

**PlayerInventory.java**: which seeds you own, how many, and which is selected for planting.
Rules kept from the original: the first seed you ever get is selected automatically; when
the selected seed runs out, the selection moves to another owned seed; you can't select a
seed you don't own. Called by: GameSession, later the shop, inventory screen and farm.

**PlotState.java / Plot.java**: one square of soil. States: EMPTY, GROWING, READY. `plant`
starts growth; `getStage` says which of the 3 pictures to show (stage 2 after 1/3 of the grow
time, stage 3 after 2/3); `update` turns GROWING into READY when the time is up; `harvest`
returns the seed (its sell price is the reward) and empties the plot. The current time is
passed in as a number, so the checks don't have to wait real seconds.

**Farm.java**: the grid of plots (built 6x6, only size x size are used), the size upgrades
(3x3 to 6x6, costing $150, $400, $1000), and the drone's position. `moveDrone` takes North,
South, East or West and refuses to leave the grid. `tick(now)` grows every plot; the screen
will call it about 10 times a second with one timer (the original had one timer per plant).

**StoryModel.java**: the 3 chapters, their reply choices and what each reply gives (money
and/or a seed), plus how far the player is. Seeds in replies now come from SeedFactory. A
`record` is a small class that only holds data; Java writes the getters for us. Rules kept:
one reply per chapter; you must pick a reply before continuing.

**GameSession.java (DESIGN PATTERN 2: Singleton)**
- Problem it solves: in the original, the balance lived inside the bottom bar, seeds inside
  the inventory screen, and the farm size inside the farm screen, so screens had to be wired
  to each other in Main with callbacks.
- How it works: the constructor is private and one instance is created when the class loads.
  Everyone calls `GameSession.getInstance()` and gets that same object, so the shop and the
  bottom bar can never show different balances. It holds the balance (start $100, goal
  $10,000), the elapsed seconds (60 per day, 30 days), the Farm, the PlayerInventory and the
  StoryModel. `reset()` starts a fresh game for Play Again. Saving and loading will read and
  write this one object.
- Demo line: "Every screen asks GameSession for the state; there is only one, so there is one
  source of truth."
- Behavior kept from the original on purpose: a story reply can push the balance below $0
  (for example -$10 with $5), exactly as before. Tell me if you would like that changed.

### How to test Layer 1 (console check, no window)
    mkdir -p out
    javac --release 17 -d out $(find src test -name "*.java")
    java -cp out:src codeagarden.GameLogicCheck
Expected last line: `ALL CHECKS PASSED` (43 checks: seeds, money, inventory, growth stages,
farm and drone, days, story). `test/` is not part of the game; it only holds the check.

### Layer 2: the screens (everything you see)
Rule of this layer: **screens only display and react to clicks.** All numbers and rules come
from GameSession (layer 1). Screens never keep their own copy of the balance, seeds or farm size.

**Main.java**: the only entry point; opens MainMenu on the Swing thread.

**MainMenu.java**: title screen. Draws the garden background (scaled to cover the window), the
title picture and four picture buttons. NEW GAME asks name + password (creates the player), resets
GameSession and opens GameScreen. CONTINUE logs in and offers Load / Delete save. LEADERBOARD opens the
leaderboard window. EXIT quits. The buttons shrink so all four always fit the window height. Inner classes: `MenuPanel` (layout and
background), `ImageLabel` (scalable title), `MenuButton` (picture button; its text changes color on
hover). Removed from the original: button bounce and press/hover squash.

**GameScreen.java**: the game window. Creates the four screens, puts them on "cards" (a CardLayout
shows one at a time), builds the bottom bar buttons, and runs the **one-second day timer**
(`session.tickSecond()`, refresh the bar, check if 30 days are over). `onMoneyChanged()` is the one
method every screen calls after something changes the balance: it refreshes the bar and checks the
$10,000 win. `finishGame` shows the Play Again / Quit box, after `stopEverything()` stops both timers
and the script window, and saves the result for the leaderboard. MENU and the close button ask
SAVE AND QUIT / QUIT / CANCEL (`askToLeave`, the day clock pauses meanwhile). Play Again:
`session.reset()` (same player) then a new GameScreen.

**BottomBar.java + HudLabel.java + HudButton.java + Drawing.java**: the white bar at the bottom.
`BottomBar` lays out balance (left), day counter (middle) and buttons (right) and shrinks everything
when the window is narrow. `refresh()` reads GameSession and sets the texts; the final day turns red.
`HudLabel` = picture with text (balance, day), `HudButton` = round picture button, `Drawing` = shared
drawing helpers (smooth drawing, stretching a picture without squashing its rounded ends, centered
text). Removed: the press-dim effect. The bar starts shrinking below 1400 pixels wide because Poppins
Black is wider than the fallback font; without that the day counter was squeezed out at 1280.

**LeaveGameDialog.java** (replaced ReturnToMenuDialog): the pop-up shown by MENU and by the window's close
button. With a player: SAVE AND QUIT / QUIT / CANCEL. Without a database: YES / NO. It returns which button
was picked; GameScreen decides what to do (save, leave, or stay).

**FarmPanel.java**: grass background, the grid of plots, the drone and the message line. Each plot is
a `PlotTile` that draws the soil and, if the Plot has a crop, the picture for its stage (1 to 3).
Clicking an empty plot plants the selected seed (or says "No seeds!"); clicking a ready plot harvests
(`session.addBalance(sellPrice)` then `onMoneyChanged`). **One** timer (every 150 ms) calls
`farm.tick(now)` and shows "X is ready to harvest!". Drone commands `move`, `plantHere`, `harvestHere`,
`canHarvest` are called by DroneController. Removed: drone bobbing and shadow, the animated "!" icon,
tinting, the extra repaint timer, one timer per plant.

**ShopPanel.java**: seed rows (from `SeedFactory.createAllSeeds()`) and the farm upgrade row. Buying
calls `session.spend(...)`; a message shows for 2.5 seconds (no fade). After an upgrade it tells the
farm screen to show the new plots. **InventoryPanel.java**: lists owned seeds; Select changes the
selected seed. It re-reads GameSession each time the Inventory page is opened. **StoryPanel.java**:
shows the chapter from StoryModel; a reply gives its money/seed through `session.applyConsequence`.

**UIFactory.java (DESIGN PATTERN 1, second use: Factory Method)**: builds the screen background,
titles, headings, parchment rows, green buttons (with hover color), the gold "Selected" button and
the on/off look. Shop, inventory and story ask it for "a green button" instead of each repeating about
15 lines of styling. Demo line: "To change how every button looks I edit one method in UIFactory."

**FullscreenHelper.java**: the one fullscreen helper used by MainMenu and GameScreen. F11 toggles,
Esc leaves; it remembers the window size to return to.

**Interpreter files (Lexer, Ast, Parser, Interpreter, InterpreterWindow)**: copied unchanged from the
original (byte-for-byte identical; the teammate's code). **DroneController.java**: the original with
only the type name changed (FarmPlot to FarmPanel). It is the **Adapter** (optional bonus pattern): it
turns script commands like `move(North)` into FarmPanel calls on the Swing thread.

### How the pieces connect
    Main -> MainMenu -> (NEW GAME) GameSession.reset() -> GameScreen
    GameScreen -> BottomBar, FarmPanel, ShopPanel, InventoryPanel, StoryPanel, InterpreterWindow
    InterpreterWindow -> Interpreter -> DroneController -> FarmPanel -> Farm/Plot (in GameSession)
    every screen -> GameSession.getInstance() for all numbers; UIFactory for look; Theme for colors

### How to test Layer 2
Run the game (README), then try: NEW GAME; buy seeds in Shop (balance drops); open Inventory and Select
a seed; on Farm click a plot to plant, wait, click again to harvest (balance rises); buy the farm
upgrade; Story replies; MENU then YES returns to the menu; the Code button opens the script window
and Run moves the drone. Also press F11 / Esc. Checked here on a virtual screen: shop buying and "not
enough money", inventory selecting, drone planting and harvesting, farm upgrade, story reward, bottom
bar updates, and the pop-up.

## 5b. Saving, players and leaderboard (Layer 3, version 2)

**What the player sees.**
- NEW GAME asks for a player name and password. A new name creates the player. An existing name only works with
  its password (otherwise: "name already taken"). If that player has a game in progress, they are asked first.
- CONTINUE asks for name and password, then shows the saved day and balance with **Load / Delete save / Cancel**.
  Your save belongs to you: nobody can load it without your password.
- Delete save removes the player completely: save, farm, seeds and their leaderboard line. The name becomes free.
- LEADERBOARD (new main-menu button) lists every player and their currency, highest first (the SQL sorts it).
- MENU or the window's close button asks **SAVE AND QUIT / QUIT / CANCEL**. The day clock pauses while it is open.
  If saving fails you see why and stay in the game.
- Winning or losing saves the final currency (it counts on the leaderboard) and marks the save finished, so
  CONTINUE will not load a finished game. Play Again keeps the same player.
- No database? NEW GAME offers "Play without saving?" and the MENU pop-up becomes plain YES / NO.

**What is saved** (one save per player): currency, time played (`elapsed_seconds`, so the time remaining is exact),
farm size, every planted tile (row, column, crop, how many milliseconds it had grown), seeds owned, story chapters done.
A crop does not grow while the game is closed: it continues from where it was.

**Tables** (sql/schema.sql, adapted from the team's file, same table names):
PLAYER (name, salt, password hash, currency), FARM, CROP (the 4 seeds, same numbers as SeedFactory), PLANTED_CROP
(tiles), INVENTORY, INVENTORY_CROP (seed counts), SAVE (time played, chapters, finished). FEATURE and
INVENTORY_FEATURE were removed (the buy-code system is gone). Wheat became Corn to match the game.
The 10 sample players are kept; their password is `demo1234`.

**Classes.**
- `Player` - id + name of whoever is logged in. `GameSession` keeps it (null = no database). reset() keeps it.
- `Passwords` - stores passwords safely: random salt + SHA-256(salt + password). The real password is never stored.
- `SaveData`, `SavedTile` - plain holders of what is saved. They know nothing about the database.
- `LeaderboardEntry` - one leaderboard line (name, currency).
- `SaveException` - an error with a message for the player. `isConnectionProblem()` separates "database down"
  from normal answers like "wrong password".
- `SaveRepository` - the ONLY class with SQL: playerExists, createPlayer, logIn, save, saveFinishedGame, load,
  hasGameInProgress, deleteSave, getLeaderboard. Every write is one transaction (all or nothing). Values are always
  passed with `?` (PreparedStatement), so a name like `x'); DROP TABLE PLAYER;--` is stored as plain text.
- `LoginForm` - the name + password form. `LeaderboardDialog` - the leaderboard window (built with UIFactory).
- `LeaveGameDialog` - the SAVE AND QUIT / QUIT / CANCEL pop-up (replaces ReturnToMenuDialog).
- `GameSession.createSaveData()` / `restore()` copy the game out to / back from a SaveData. restore() fixes
  out-of-range numbers and skips unknown seeds or tiles outside the farm, so a bad save cannot break the game.

**Set it up:** see README.md "Saving, players and leaderboard".

**Test.**
1. `GameLogicCheck` (no database needed): includes the save round trip with tiles and the password checks.
2. With MySQL: LEADERBOARD shows Emi first. CONTINUE as Ijin / demo1234 -> Load: 3 crops on the farm, $500.
3. Earn some money, MENU -> SAVE AND QUIT, LEADERBOARD: Ijin moved up. CONTINUE again: same day, money and crops.
4. NEW GAME as yourself, plant something, close the window -> SAVE AND QUIT. CONTINUE with a wrong password: refused.
5. CONTINUE -> Delete save: you disappear from the leaderboard. Stop MySQL and press CONTINUE: a friendly message.

## 6. Plan (from CLAUDE.md, short)
- Two design patterns that really run in the game: **Singleton** (GameSession: one
  shared object holding balance, day, inventory, farm size, story progress) and
  **Factory Method** (SeedFactory/UIFactory: one place that creates seeds, buttons
  and panels).
- Logic is separated from screens, duplicates become shared classes, long methods
  are split, magic numbers get names.
- Saving to MySQL is a later step. Note: the PC used for the demo then needs a
  running MySQL server. If that is a problem, we should decide on an alternative
  before building it.

## 7. Glossary
- **Swing**: Java's toolkit for windows, buttons and drawing.
- **Classpath**: the list of places Java searches for code and resources.
- **Singleton**: a class that can only ever have one object, shared by everything.
- **Factory Method**: a method that creates objects for you so the rest of the
  code does not repeat how to build them.

## 8. Change history
- 2026-11-13: Skeleton created (folders, resources copied, README, Main, placeholder
  MainMenu, VS Code settings). Tested: compiles with `--release 8`; both menu
  pictures are found on the classpath.
- 2026-11-13: Added `src/fonts/Poppins-Black.ttf` (used by the bottom bar HUD). Checked that Java can load it. It is not used by any code yet.
- 2026-11-13: Target changed from Java 8 to Java 17+ (owner confirmed all demo PCs have 17 or newer). The interpreter files stay unchanged. Updated README, EXPLANATION.md and CLAUDE.md to match.
- 2026-11-13: Added `Resources` (shared image/font loader) and `Theme` (shared colors and fonts); MainMenu now uses them and shows a font-check line. Tested: compiles with `--release 17`, both fonts load as Poppins Black and Daydream, a missing image gives a clear error, a missing font falls back, and the window draws correctly on a virtual display.
- 2026-11-13: Layer 1 built: Seed, SeedFactory (Factory Method), PlayerInventory, PlotState, Plot, Farm, StoryModel, GameSession (Singleton), plus test/codeagarden/GameLogicCheck.java. Tested: compiles with `--release 17`, 43/43 console checks pass. No screens yet; the next layer is the UI.
- 2026-11-13: Layer 2 built: real MainMenu, GameScreen, BottomBar (+HudLabel, HudButton, Drawing, ReturnToMenuDialog), FarmPanel, ShopPanel, InventoryPanel, StoryPanel, UIFactory, FullscreenHelper; interpreter files copied unchanged; DroneController adapted to FarmPanel; Farm.tick now returns the plots that became ready. Tested on a virtual screen (15 UI checks + screenshots). Behavior changes: F11/Esc fullscreen in menu and game, window title "Code A Garden" (was "Grow a Garden" on the menu), CONTINUE shows a message, story buttons got the hover color, bar shrinks from 1400 px.
- 2026-11-13: Layer 3 built: SaveData, SaveException, SaveRepository, schema.sql, db.properties.example; CONTINUE, autosave and best score wired in.
- 2026-11-15: Saving v2 on the team's schema: players with passwords, per-player save incl. planted tiles and exact time, Delete save, LEADERBOARD, SAVE AND QUIT / QUIT / CANCEL pop-up.

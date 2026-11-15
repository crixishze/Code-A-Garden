-- =====================================================================
-- Code A Garden: database (adapted from the team's Code_a_Garden_Database.sql)
-- Run ONCE on each PC:   mysql -u root -p < sql/schema.sql
-- WARNING: running it again deletes every player and save and starts over.
-- Sample players all use the password: demo1234
-- =====================================================================
DROP DATABASE IF EXISTS code_a_garden;
CREATE DATABASE code_a_garden CHARACTER SET utf8mb4;
USE code_a_garden;

-- ENTITY 1: PLAYER  (one row per player name; currency is what the leaderboard ranks)
-- The password is never stored. We store a random salt and SHA-256(salt + password).
-- currency may go below 0 because some story choices cost money.
CREATE TABLE PLAYER (
    player_id     INT PRIMARY KEY AUTO_INCREMENT,
    player_name   VARCHAR(50) NOT NULL UNIQUE,
    password_salt CHAR(32)    NOT NULL,
    password_hash CHAR(64)    NOT NULL,
    currency      INT         NOT NULL DEFAULT 100
);

-- ENTITY 2: FARM  (one per player)
CREATE TABLE FARM (
    farm_id   INT PRIMARY KEY AUTO_INCREMENT,
    player_id INT NOT NULL UNIQUE,
    farm_size INT NOT NULL DEFAULT 3,
    FOREIGN KEY (player_id) REFERENCES PLAYER(player_id),
    CHECK (farm_size BETWEEN 3 AND 6)
);

-- ENTITY 3: CROP  (the four seeds; must match SeedFactory.java)
CREATE TABLE CROP (
    crop_id     VARCHAR(10) PRIMARY KEY,
    crop_name   VARCHAR(50) NOT NULL UNIQUE,
    cost        INT NOT NULL,
    growth_time INT NOT NULL COMMENT 'Growth time in seconds',
    reward      INT NOT NULL,
    CHECK (cost >= 0),
    CHECK (growth_time > 0),
    CHECK (reward >= 0)
);

-- ENTITY 4: PLANTED_CROP  (one row per planted tile; row_num/col_num start at 1)
-- grown_ms = how many milliseconds the crop has already grown, so it continues where it stopped.
CREATE TABLE PLANTED_CROP (
    planted_crop_id INT PRIMARY KEY AUTO_INCREMENT,
    farm_id  INT NOT NULL,
    crop_id  VARCHAR(10) NOT NULL,
    row_num  INT NOT NULL,
    col_num  INT NOT NULL,
    grown_ms INT NOT NULL DEFAULT 0,
    state    VARCHAR(20) NOT NULL,
    FOREIGN KEY (farm_id) REFERENCES FARM(farm_id),
    FOREIGN KEY (crop_id) REFERENCES CROP(crop_id),
    UNIQUE (farm_id, row_num, col_num),
    CHECK (row_num BETWEEN 1 AND 6),
    CHECK (col_num BETWEEN 1 AND 6),
    CHECK (grown_ms >= 0),
    CHECK (state IN ('Seed', 'Growing', 'Matured'))
);

-- ENTITY 5: INVENTORY  (one per player)
CREATE TABLE INVENTORY (
    inventory_id INT PRIMARY KEY AUTO_INCREMENT,
    player_id    INT NOT NULL UNIQUE,
    FOREIGN KEY (player_id) REFERENCES PLAYER(player_id)
);

-- ENTITY 6: INVENTORY_CROP  (how many of each seed the player owns)
CREATE TABLE INVENTORY_CROP (
    inventory_id INT NOT NULL,
    crop_id      VARCHAR(10) NOT NULL,
    quantity     INT NOT NULL DEFAULT 0,
    PRIMARY KEY (inventory_id, crop_id),
    FOREIGN KEY (inventory_id) REFERENCES INVENTORY(inventory_id),
    FOREIGN KEY (crop_id) REFERENCES CROP(crop_id),
    CHECK (quantity >= 0)
);

-- ENTITY 7: SAVE  (one per player: the game they can CONTINUE)
-- elapsed_seconds = time played this season (30 days x 60 s = 1800 s), so time remaining = 1800 - elapsed_seconds.
-- finished = 1 once the game was won or lost (CONTINUE then refuses to load it).
CREATE TABLE `SAVE` (
    save_id         INT PRIMARY KEY AUTO_INCREMENT,
    player_id       INT NOT NULL UNIQUE,
    inventory_id    INT NOT NULL,
    farm_id         INT NOT NULL,
    saved_at        DATETIME NOT NULL,
    elapsed_seconds INT NOT NULL DEFAULT 0,
    chapters_done   INT NOT NULL DEFAULT 0,
    finished        TINYINT NOT NULL DEFAULT 0,
    FOREIGN KEY (player_id) REFERENCES PLAYER(player_id),
    FOREIGN KEY (inventory_id) REFERENCES INVENTORY(inventory_id),
    FOREIGN KEY (farm_id) REFERENCES FARM(farm_id),
    CHECK (elapsed_seconds BETWEEN 0 AND 1800),
    CHECK (chapters_done BETWEEN 0 AND 3)
);

-- CROP DATA (same numbers as SeedFactory.java)
INSERT INTO CROP (crop_id, crop_name, cost, growth_time, reward) VALUES
('00A', 'Carrot', 10, 3, 20),
('00B', 'Corn', 20, 5, 45),
('00C', 'Tomato', 30, 8, 70),
('00D', 'Pumpkin', 50, 12, 120);

-- SAMPLE DATA: PLAYER (password for all: demo1234)
INSERT INTO PLAYER (player_id, player_name, password_salt, password_hash, currency) VALUES
(1, 'Ijin', 'f0d23dda43c74efaa29126abf53df394', 'dd8feaaf15972015923ac7843937476e8d0018e6b6f551d461c3a9f64fd9a404', 500),
(2, 'Mika', '1c2f31041e4f35aedc651071b6179d22', '6eb7b10f1e5ac33fbb91cf036e923d63a666c70b5de474065609fda54155eeab', 350),
(3, 'Ren', '7df5866f9ebe0c8ddd0837cfeb16b0f7', 'db47fc0428b51bf1399e84cfa09cd105319b009b2af49e011193fb414d4bba49', 800),
(4, 'Kai', '67838e0bfc791191e20277d24ea0ffbe', 'd94a06cdd22ae68efe110193cf6d875ef48fb7b2b52372894d5665a23367ceaa', 650),
(5, 'Yuna', 'e7c8ae36e566bcb8df3afb9f12a762c5', '24f79d413f92d26a9ea9aaaa48e1ef2551f622c1090fb5d43bd94a6aedc2cd38', 1200),
(6, 'Haru', '54764d9d0027c40619c58574b8aca879', '64e33cce216b18edaa971ed01064e942ab8234d8a51768c7af5fbbf0add441ed', 450),
(7, 'Aki', '3157b211cff0b2f988ecfaf31c0dc24c', 'e145a6320eeb49c5451ab778325077986535e57b97c29fa78676f0ef210d8566', 950),
(8, 'Sora', '83d6c7d3a38d29bea4302c233d99e72c', '1f43766c099c4a57d66a173229c00515b6b6465e48da86c5a1fce34d37710555', 700),
(9, 'Riku', 'f61cb4f7d20c893d7f77b2462a30f61c', 'bbcd3247fb341436e3676785e3c17509da9f0842416d2c9bf34a996dadd8d15a', 300),
(10, 'Emi', '26cc022d8cd824ea1357cce96da0f971', '19af8922cb07dbeba4259e84989d4248162565f16aa94d4f2bb976532a657198', 1500);

-- SAMPLE DATA: FARM
INSERT INTO FARM (farm_id, player_id, farm_size) VALUES
(1, 1, 3),
(2, 2, 3),
(3, 3, 5),
(4, 4, 3),
(5, 5, 5),
(6, 6, 3),
(7, 7, 5),
(8, 8, 4),
(9, 9, 3),
(10, 10, 5);

-- SAMPLE DATA: PLANTED_CROP
INSERT INTO PLANTED_CROP (farm_id, crop_id, row_num, col_num, grown_ms, state) VALUES
(1, '00A', 1, 1, 3000, 'Matured'),
(1, '00B', 1, 2, 2500, 'Growing'),
(1, '00C', 2, 1, 0, 'Seed'),
(2, '00A', 1, 1, 1500, 'Growing'),
(2, '00D', 2, 2, 0, 'Seed'),
(3, '00C', 3, 3, 8000, 'Matured'),
(4, '00B', 1, 1, 2500, 'Growing'),
(4, '00D', 2, 2, 12000, 'Matured'),
(5, '00A', 1, 1, 3000, 'Matured'),
(5, '00C', 2, 2, 4000, 'Growing'),
(5, '00D', 3, 3, 0, 'Seed'),
(6, '00A', 1, 1, 0, 'Seed'),
(7, '00B', 1, 1, 5000, 'Matured'),
(7, '00C', 2, 2, 4000, 'Growing'),
(8, '00D', 1, 1, 12000, 'Matured'),
(8, '00A', 2, 2, 0, 'Seed'),
(9, '00B', 1, 1, 2500, 'Growing'),
(10, '00C', 1, 1, 8000, 'Matured'),
(10, '00D', 2, 2, 6000, 'Growing');

-- SAMPLE DATA: INVENTORY
INSERT INTO INVENTORY (inventory_id, player_id) VALUES
(1, 1),
(2, 2),
(3, 3),
(4, 4),
(5, 5),
(6, 6),
(7, 7),
(8, 8),
(9, 9),
(10, 10);

-- SAMPLE DATA: INVENTORY_CROP
INSERT INTO INVENTORY_CROP (inventory_id, crop_id, quantity) VALUES
(1, '00A', 5),
(1, '00B', 3),
(2, '00A', 2),
(2, '00C', 4),
(3, '00B', 10),
(3, '00D', 2),
(4, '00A', 6),
(4, '00D', 1),
(5, '00A', 12),
(5, '00B', 8),
(5, '00C', 5),
(6, '00B', 3),
(7, '00A', 10),
(7, '00C', 7),
(7, '00D', 4),
(8, '00B', 6),
(8, '00C', 3),
(9, '00A', 2),
(9, '00D', 2),
(10, '00A', 15),
(10, '00B', 10),
(10, '00C', 8),
(10, '00D', 5);

-- SAMPLE DATA: SAVE (each sample player has a game on day 1 they can CONTINUE)
INSERT INTO `SAVE` (player_id, inventory_id, farm_id, saved_at, elapsed_seconds, chapters_done, finished) VALUES
(1, 1, 1, '2026-10-15 10:00:00', 0, 0, 0),
(2, 2, 2, '2026-10-15 10:05:00', 0, 0, 0),
(3, 3, 3, '2026-10-15 10:10:00', 0, 0, 0),
(4, 4, 4, '2026-10-15 10:15:00', 0, 0, 0),
(5, 5, 5, '2026-10-15 10:20:00', 0, 0, 0),
(6, 6, 6, '2026-10-15 10:25:00', 0, 0, 0),
(7, 7, 7, '2026-10-15 10:30:00', 0, 0, 0),
(8, 8, 8, '2026-10-15 10:35:00', 0, 0, 0),
(9, 9, 9, '2026-10-15 10:40:00', 0, 0, 0),
(10, 10, 10, '2026-10-15 10:45:00', 0, 0, 0);

-- LEADERBOARD (the same query the game runs): richest player first
SELECT player_name, currency FROM PLAYER ORDER BY currency DESC, player_name;

DROP DATABASE IF EXISTS chess;
CREATE DATABASE chess;
USE chess;

SET FOREIGN_KEY_CHECKS=0;

-- Users table
CREATE TABLE users (
    user_id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role ENUM('USER', 'ADMIN') DEFAULT 'USER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP NULL,
    UNIQUE KEY unique_username (username),
    UNIQUE KEY unique_email (email)
);

-- Games table
CREATE TABLE games (
    game_id INT PRIMARY KEY AUTO_INCREMENT,
    player1_id INT NOT NULL,
    player2_id INT NULL,
    start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    end_time TIMESTAMP NULL,
    winner_id INT NULL,
    game_state TEXT NULL,
    game_type ENUM('SINGLE_PLAYER', 'MULTIPLAYER') NOT NULL,
    status ENUM('IN_PROGRESS', 'COMPLETED', 'ABANDONED') DEFAULT 'IN_PROGRESS',
    FOREIGN KEY (player1_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (player2_id) REFERENCES users(user_id) ON DELETE SET NULL,
    FOREIGN KEY (winner_id) REFERENCES users(user_id) ON DELETE SET NULL
);

-- Moves table
CREATE TABLE moves (
    move_id INT PRIMARY KEY AUTO_INCREMENT,
    game_id INT NOT NULL,
    player_id INT NOT NULL,
    move_number INT NOT NULL,
    move_notation VARCHAR(10) NOT NULL,
    board_state TEXT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (game_id) REFERENCES games(game_id) ON DELETE CASCADE,
    FOREIGN KEY (player_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Statistics table
CREATE TABLE statistics (
    user_id INT PRIMARY KEY,
    total_games INT DEFAULT 0,
    wins INT DEFAULT 0,
    losses INT DEFAULT 0,
    draws INT DEFAULT 0,
    rating INT DEFAULT 1200,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Tournament tables
CREATE TABLE tournaments (
    tournament_id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    description TEXT NULL,
    format VARCHAR(20) NOT NULL DEFAULT 'SWISS',
    start_date TIMESTAMP NULL,
    end_date TIMESTAMP NULL,
    max_participants INT NOT NULL,
    tournament_type VARCHAR(20) NOT NULL,
    rounds_count INT NOT NULL,
    time_control_minutes INT NOT NULL,
    time_increment_seconds INT NOT NULL,
    round_duration INT DEFAULT 120,
    break_duration INT DEFAULT 15,
    rounds_per_day INT DEFAULT 3,
    current_round INT DEFAULT 0,
    round_start_time TIMESTAMP NULL,
    status VARCHAR(20) DEFAULT 'UPCOMING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE tournament_participants (
    tournament_id INT,
    user_id INT,
    score DOUBLE DEFAULT 0,
    player_rank INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (tournament_id, user_id)
);

CREATE TABLE tournament_rounds (
    tournament_id INT,
    round_number INT,
    start_time TIMESTAMP NULL,
    end_time TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (tournament_id, round_number)
);

CREATE TABLE tournament_pairings (
    tournament_id INT,
    round_number INT,
    player1_id INT,
    player2_id INT,
    game_id INT,
    winner_id INT,
    is_draw BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (tournament_id, round_number, player1_id, player2_id)
);

CREATE TABLE tournament_results (
    tournament_id INT,
    round_number INT,
    user_id INT,
    opponent_id INT,
    is_white BOOLEAN,
    score DOUBLE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (tournament_id, round_number, user_id)
);

CREATE TABLE tournament_games (
    game_id INT PRIMARY KEY,
    tournament_id INT NOT NULL,
    round_number INT NOT NULL,
    scheduled_start TIMESTAMP NULL,
    scheduled_end TIMESTAMP NULL,
    status VARCHAR(20) DEFAULT 'CREATED',
    is_losers_bracket BOOLEAN DEFAULT FALSE,
    match_number INT,
    FOREIGN KEY (game_id) REFERENCES games(game_id)
);

CREATE INDEX idx_tournament_games_round ON tournament_games(tournament_id, round_number);
CREATE INDEX idx_tournament_games_schedule ON tournament_games(scheduled_start, status);

-- Tournament notification tables
CREATE TABLE tournament_notifications (
    notification_id INT PRIMARY KEY AUTO_INCREMENT,
    tournament_id INT,
    user_id INT,
    type VARCHAR(50) NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tournament statistics tables
CREATE TABLE tournament_statistics (
    tournament_id INT NOT NULL,
    user_id INT NOT NULL,
    games_played INT DEFAULT 0,
    games_won INT DEFAULT 0,
    games_drawn INT DEFAULT 0,
    games_lost INT DEFAULT 0,
    points DOUBLE DEFAULT 0,
    performance_rating INT,
    PRIMARY KEY (tournament_id, user_id)
);

CREATE TABLE tournament_game_statistics (
    tournament_id INT NOT NULL,
    round_number INT NOT NULL,
    total_games INT DEFAULT 0,
    completed_games INT DEFAULT 0,
    white_wins INT DEFAULT 0,
    black_wins INT DEFAULT 0,
    draws INT DEFAULT 0,
    avg_game_length INT,
    PRIMARY KEY (tournament_id, round_number)
);

-- Tournament series tables
CREATE TABLE tournament_series (
    series_id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'UPCOMING',
    points_system VARCHAR(20) NOT NULL DEFAULT 'STANDARD',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE tournament_series_events (
    series_id INT NOT NULL,
    tournament_id INT NOT NULL,
    event_order INT NOT NULL,
    points_multiplier DECIMAL(3,2) DEFAULT 1.00,
    PRIMARY KEY (series_id, tournament_id)
);

CREATE TABLE tournament_series_standings (
    series_id INT NOT NULL,
    user_id INT NOT NULL,
    total_points DECIMAL(10,2) DEFAULT 0.00,
    tournaments_played INT DEFAULT 0,
    best_placement INT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (series_id, user_id)
);

-- Tournament analytics tables
CREATE TABLE tournament_analytics (
    analytics_id INT PRIMARY KEY AUTO_INCREMENT,
    tournament_id INT NOT NULL,
    total_games INT DEFAULT 0,
    total_moves INT DEFAULT 0,
    avg_game_length INT DEFAULT 0,
    white_wins INT DEFAULT 0,
    black_wins INT DEFAULT 0,
    draws INT DEFAULT 0,
    shortest_game INT,
    longest_game INT,
    avg_rating INT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE tournament_opening_stats (
    tournament_id INT NOT NULL,
    eco_code VARCHAR(3) NOT NULL,
    opening_name VARCHAR(100) NOT NULL,
    frequency INT DEFAULT 0,
    white_wins INT DEFAULT 0,
    black_wins INT DEFAULT 0,
    draws INT DEFAULT 0,
    avg_moves INT DEFAULT 0,
    PRIMARY KEY (tournament_id, eco_code)
);

CREATE TABLE tournament_player_analytics (
    tournament_id INT NOT NULL,
    user_id INT NOT NULL,
    avg_move_time INT DEFAULT 0,
    avg_accuracy DECIMAL(5,2) DEFAULT 0.00,
    blunders INT DEFAULT 0,
    mistakes INT DEFAULT 0,
    inaccuracies INT DEFAULT 0,
    avg_centipawn_loss INT DEFAULT 0,
    PRIMARY KEY (tournament_id, user_id)
);

-- Tournament chat tables
CREATE TABLE tournament_chat_messages (
    message_id INT PRIMARY KEY AUTO_INCREMENT,
    tournament_id INT NOT NULL,
    user_id INT NOT NULL,
    content TEXT NOT NULL,
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE tournament_moderators (
    tournament_id INT NOT NULL,
    user_id INT NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'MODERATOR',
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (tournament_id, user_id)
);

CREATE TABLE tournament_muted_users (
    tournament_id INT NOT NULL,
    user_id INT NOT NULL,
    muted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    muted_by INT NOT NULL,
    PRIMARY KEY (tournament_id, user_id)
);

-- Achievements table
CREATE TABLE achievements (
    achievement_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    type VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    icon VARCHAR(50) NOT NULL,
    points INT NOT NULL,
    earned_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_achievement (user_id, type)
);

-- Puzzles table
CREATE TABLE puzzles (
    puzzle_id INT PRIMARY KEY AUTO_INCREMENT,
    initial_position TEXT NOT NULL,
    solution VARCHAR(255) NOT NULL,
    difficulty ENUM('EASY', 'MEDIUM', 'HARD') NOT NULL,
    rating INT DEFAULT 1200
);

-- User puzzle progress
CREATE TABLE user_puzzle_progress (
    user_id INT,
    puzzle_id INT,
    completed BOOLEAN DEFAULT FALSE,
    attempts INT DEFAULT 0,
    completed_at TIMESTAMP NULL,
    PRIMARY KEY (user_id, puzzle_id)
);

-- Table for storing game variations
CREATE TABLE game_variations (
    variation_id INT PRIMARY KEY AUTO_INCREMENT,
    game_id INT NOT NULL,
    position VARCHAR(100) NOT NULL,  -- FEN position
    user_id INT NOT NULL,
    variation TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (game_id) REFERENCES games(game_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

SET FOREIGN_KEY_CHECKS=1;

-- Add indexes for better performance
ALTER TABLE games ADD INDEX idx_games_players (player1_id, player2_id);
ALTER TABLE moves ADD INDEX idx_moves_game (game_id);
ALTER TABLE statistics ADD INDEX idx_statistics_rating (rating);
ALTER TABLE tournament_participants ADD INDEX idx_tp_tournament (tournament_id);
ALTER TABLE tournament_participants ADD INDEX idx_tp_user (user_id);

-- Add foreign key constraints after data is loaded
ALTER TABLE tournament_participants 
ADD CONSTRAINT fk_tp_tournament 
FOREIGN KEY (tournament_id) REFERENCES tournaments(tournament_id) ON DELETE CASCADE;

ALTER TABLE tournament_participants 
ADD CONSTRAINT fk_tp_user 
FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE;

ALTER TABLE tournament_rounds 
ADD CONSTRAINT fk_tr_tournament 
FOREIGN KEY (tournament_id) REFERENCES tournaments(tournament_id) ON DELETE CASCADE;

ALTER TABLE tournament_pairings 
ADD CONSTRAINT fk_tp_tournament 
FOREIGN KEY (tournament_id) REFERENCES tournaments(tournament_id) ON DELETE CASCADE;

ALTER TABLE tournament_pairings 
ADD CONSTRAINT fk_tp_round 
FOREIGN KEY (tournament_id, round_number) REFERENCES tournament_rounds(tournament_id, round_number) ON DELETE CASCADE;

ALTER TABLE tournament_pairings 
ADD CONSTRAINT fk_tp_player1 
FOREIGN KEY (player1_id) REFERENCES users(user_id) ON DELETE CASCADE;

ALTER TABLE tournament_pairings 
ADD CONSTRAINT fk_tp_player2 
FOREIGN KEY (player2_id) REFERENCES users(user_id) ON DELETE CASCADE;

ALTER TABLE tournament_pairings 
ADD CONSTRAINT fk_tp_game 
FOREIGN KEY (game_id) REFERENCES games(game_id) ON DELETE CASCADE;

ALTER TABLE tournament_pairings 
ADD CONSTRAINT fk_tp_winner 
FOREIGN KEY (winner_id) REFERENCES users(user_id) ON DELETE CASCADE;

ALTER TABLE tournament_results 
ADD CONSTRAINT fk_tr_tournament 
FOREIGN KEY (tournament_id) REFERENCES tournaments(tournament_id) ON DELETE CASCADE;

ALTER TABLE tournament_results 
ADD CONSTRAINT fk_tr_round 
FOREIGN KEY (tournament_id, round_number) REFERENCES tournament_rounds(tournament_id, round_number) ON DELETE CASCADE;

ALTER TABLE tournament_results 
ADD CONSTRAINT fk_tr_user 
FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE;

ALTER TABLE tournament_results 
ADD CONSTRAINT fk_tr_opponent 
FOREIGN KEY (opponent_id) REFERENCES users(user_id) ON DELETE CASCADE;

ALTER TABLE tournament_games 
ADD CONSTRAINT fk_tg_tournament 
FOREIGN KEY (tournament_id) REFERENCES tournaments(tournament_id) ON DELETE CASCADE;

ALTER TABLE tournament_games 
ADD CONSTRAINT fk_tg_round 
FOREIGN KEY (tournament_id, round_number) REFERENCES tournament_rounds(tournament_id, round_number) ON DELETE CASCADE;

ALTER TABLE tournament_games 
ADD CONSTRAINT fk_tg_game 
FOREIGN KEY (game_id) REFERENCES games(game_id) ON DELETE CASCADE;

ALTER TABLE tournament_notifications 
ADD CONSTRAINT fk_tn_tournament 
FOREIGN KEY (tournament_id) REFERENCES tournaments(tournament_id) ON DELETE CASCADE;

ALTER TABLE tournament_notifications 
ADD CONSTRAINT fk_tn_user 
FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE;

ALTER TABLE tournament_statistics 
ADD CONSTRAINT fk_ts_tournament 
FOREIGN KEY (tournament_id) REFERENCES tournaments(tournament_id) ON DELETE CASCADE;

ALTER TABLE tournament_statistics 
ADD CONSTRAINT fk_ts_user 
FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE;

ALTER TABLE tournament_game_statistics 
ADD CONSTRAINT fk_tgs_tournament 
FOREIGN KEY (tournament_id) REFERENCES tournaments(tournament_id) ON DELETE CASCADE;

ALTER TABLE tournament_game_statistics 
ADD CONSTRAINT fk_tgs_round 
FOREIGN KEY (tournament_id, round_number) REFERENCES tournament_rounds(tournament_id, round_number) ON DELETE CASCADE;

ALTER TABLE tournament_series_events 
ADD CONSTRAINT fk_tse_series 
FOREIGN KEY (series_id) REFERENCES tournament_series(series_id) ON DELETE CASCADE;

ALTER TABLE tournament_series_events 
ADD CONSTRAINT fk_tse_tournament 
FOREIGN KEY (tournament_id) REFERENCES tournaments(tournament_id) ON DELETE CASCADE;

ALTER TABLE tournament_series_standings 
ADD CONSTRAINT fk_tss_series 
FOREIGN KEY (series_id) REFERENCES tournament_series(series_id) ON DELETE CASCADE;

ALTER TABLE tournament_series_standings 
ADD CONSTRAINT fk_tss_user 
FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE;

ALTER TABLE tournament_analytics 
ADD CONSTRAINT fk_ta_tournament 
FOREIGN KEY (tournament_id) REFERENCES tournaments(tournament_id) ON DELETE CASCADE;

ALTER TABLE tournament_opening_stats 
ADD CONSTRAINT fk_tos_tournament 
FOREIGN KEY (tournament_id) REFERENCES tournaments(tournament_id) ON DELETE CASCADE;

ALTER TABLE tournament_player_analytics 
ADD CONSTRAINT fk_tpa_tournament 
FOREIGN KEY (tournament_id) REFERENCES tournaments(tournament_id) ON DELETE CASCADE;

ALTER TABLE tournament_player_analytics 
ADD CONSTRAINT fk_tpa_user 
FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE;

ALTER TABLE tournament_chat_messages 
ADD CONSTRAINT fk_tcm_tournament 
FOREIGN KEY (tournament_id) REFERENCES tournaments(tournament_id) ON DELETE CASCADE;

ALTER TABLE tournament_chat_messages 
ADD CONSTRAINT fk_tcm_user 
FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE;

ALTER TABLE tournament_moderators 
ADD CONSTRAINT fk_tm_tournament 
FOREIGN KEY (tournament_id) REFERENCES tournaments(tournament_id) ON DELETE CASCADE;

ALTER TABLE tournament_moderators 
ADD CONSTRAINT fk_tm_user 
FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE;

ALTER TABLE tournament_muted_users 
ADD CONSTRAINT fk_tmu_tournament 
FOREIGN KEY (tournament_id) REFERENCES tournaments(tournament_id) ON DELETE CASCADE;

ALTER TABLE tournament_muted_users 
ADD CONSTRAINT fk_tmu_user 
FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE;

ALTER TABLE tournament_muted_users 
ADD CONSTRAINT fk_tmu_muted_by 
FOREIGN KEY (muted_by) REFERENCES users(user_id) ON DELETE CASCADE;

ALTER TABLE achievements 
ADD CONSTRAINT fk_a_user 
FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE;

ALTER TABLE user_puzzle_progress 
ADD CONSTRAINT fk_upp_user 
FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE;

ALTER TABLE user_puzzle_progress 
ADD CONSTRAINT fk_upp_puzzle 
FOREIGN KEY (puzzle_id) REFERENCES puzzles(puzzle_id) ON DELETE CASCADE;

ALTER TABLE game_variations 
ADD CONSTRAINT fk_gv_game 
FOREIGN KEY (game_id) REFERENCES games(game_id) ON DELETE CASCADE;

ALTER TABLE game_variations 
ADD CONSTRAINT fk_gv_user 
FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE;

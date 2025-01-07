package com.chessgame.model;

public class Puzzle {
    private int puzzleId;
    private String initialPosition;  // FEN notation
    private String solution;         // Sequence of moves
    private String difficulty;       // EASY, MEDIUM, HARD
    private int rating;
    private String title;
    private String description;
    private String theme;           // e.g., "mate in 2", "fork", "pin"
    
    // Default constructor
    public Puzzle() {}
    
    // Constructor for new puzzle
    public Puzzle(String initialPosition, String solution, String difficulty, String title, String theme) {
        this.initialPosition = initialPosition;
        this.solution = solution;
        this.difficulty = difficulty;
        this.title = title;
        this.theme = theme;
        this.rating = 1200;  // Default rating
    }
    
    // Getters and Setters
    public int getPuzzleId() {
        return puzzleId;
    }
    
    public void setPuzzleId(int puzzleId) {
        this.puzzleId = puzzleId;
    }
    
    public String getInitialPosition() {
        return initialPosition;
    }
    
    public void setInitialPosition(String initialPosition) {
        this.initialPosition = initialPosition;
    }
    
    public String getSolution() {
        return solution;
    }
    
    public void setSolution(String solution) {
        this.solution = solution;
    }
    
    public String getDifficulty() {
        return difficulty;
    }
    
    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }
    
    public int getRating() {
        return rating;
    }
    
    public void setRating(int rating) {
        this.rating = rating;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getTheme() {
        return theme;
    }
    
    public void setTheme(String theme) {
        this.theme = theme;
    }
    
    // Utility methods
    public boolean isEasy() {
        return "EASY".equals(difficulty);
    }
    
    public boolean isMedium() {
        return "MEDIUM".equals(difficulty);
    }
    
    public boolean isHard() {
        return "HARD".equals(difficulty);
    }
    
    public String[] getSolutionMoves() {
        return solution.split(" ");
    }
    
    public boolean checkSolution(String[] moves) {
        String[] correctMoves = getSolutionMoves();
        if (moves.length != correctMoves.length) {
            return false;
        }
        
        for (int i = 0; i < moves.length; i++) {
            if (!moves[i].equals(correctMoves[i])) {
                return false;
            }
        }
        
        return true;
    }
}

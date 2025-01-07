package com.chessgame.model;

import java.time.LocalDateTime;

public class Achievement {
    private int id;
    private String name;
    private String description;
    private String icon;
    private int points;
    private LocalDateTime earnedDate;

    public Achievement(int id, String name, String description, String icon, int points, LocalDateTime earnedDate) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.points = points;
        this.earnedDate = earnedDate;
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getIcon() {
        return icon;
    }

    public int getPoints() {
        return points;
    }

    public LocalDateTime getEarnedDate() {
        return earnedDate;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public void setEarnedDate(LocalDateTime earnedDate) {
        this.earnedDate = earnedDate;
    }
}

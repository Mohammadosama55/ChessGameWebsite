package com.chessgame.model;

public class TournamentData {
    private final Tournament tournament;
    private final User user;
    
    public TournamentData(Tournament tournament, User user) {
        this.tournament = tournament;
        this.user = user;
    }
    
    public Tournament getTournament() {
        return tournament;
    }
    
    public User getUser() {
        return user;
    }
    
    public String getParticipantName() {
        return user.getFirstName() + " " + user.getLastName();
    }
    
    public String getTournamentName() {
        return tournament.getName();
    }
    
    public String getTournamentDate() {
        return tournament.getStartDate().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"));
    }
}

<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Create Tournament</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.7.2/font/bootstrap-icons.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/css/styles.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/flatpickr/dist/flatpickr.min.css">
</head>
<body>
    <nav class="navbar navbar-expand-lg navbar-dark bg-dark">
        <div class="container">
            <a class="navbar-brand" href="${pageContext.request.contextPath}/dashboard">Chess Game</a>
            <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarNav">
                <span class="navbar-toggler-icon"></span>
            </button>
            <div class="collapse navbar-collapse" id="navbarNav">
                <ul class="navbar-nav me-auto">
                    <li class="nav-item">
                        <a class="nav-link" href="${pageContext.request.contextPath}/game">Play</a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link" href="${pageContext.request.contextPath}/puzzle">Puzzles</a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link active" href="${pageContext.request.contextPath}/tournament">Tournaments</a>
                    </li>
                </ul>
                <span class="navbar-text">
                    Welcome, ${sessionScope.username}!
                    <a href="${pageContext.request.contextPath}/logout" class="btn btn-outline-light ms-3">Logout</a>
                </span>
            </div>
        </div>
    </nav>

    <div class="container mt-4">
        <div class="row justify-content-center">
            <div class="col-md-8">
                <div class="card shadow">
                    <div class="card-header">
                        <h3 class="card-title mb-0">Create New Tournament</h3>
                    </div>
                    <div class="card-body">
                        <form id="tournamentForm" action="${pageContext.request.contextPath}/tournament" method="post">
                            <input type="hidden" name="action" value="create">
                            
                            <div class="mb-3">
                                <label for="name" class="form-label">Tournament Name</label>
                                <input type="text" class="form-control" id="name" name="name" required
                                       placeholder="Enter tournament name">
                            </div>
                            
                            <div class="mb-3">
                                <label for="description" class="form-label">Description</label>
                                <textarea class="form-control" id="description" name="description" rows="3"
                                          placeholder="Enter tournament description"></textarea>
                            </div>
                            
                            <div class="row mb-3">
                                <div class="col-md-6">
                                    <label for="startDate" class="form-label">Start Date & Time</label>
                                    <input type="text" class="form-control" id="startDate" name="startDate" required>
                                </div>
                                <div class="col-md-6">
                                    <label for="endDate" class="form-label">End Date & Time</label>
                                    <input type="text" class="form-control" id="endDate" name="endDate" required>
                                </div>
                            </div>
                            
                            <div class="row mb-3">
                                <div class="col-md-6">
                                    <label for="tournamentType" class="form-label">Tournament Type</label>
                                    <select class="form-select" id="tournamentType" name="tournamentType" required>
                                        <option value="">Select tournament type</option>
                                        <option value="SWISS">Swiss System</option>
                                        <option value="ROUND_ROBIN">Round Robin</option>
                                        <option value="ELIMINATION">Single Elimination</option>
                                    </select>
                                </div>
                                <div class="col-md-6">
                                    <label for="maxParticipants" class="form-label">Maximum Participants</label>
                                    <input type="number" class="form-control" id="maxParticipants" name="maxParticipants"
                                           min="4" max="128" required>
                                </div>
                            </div>
                            
                            <div class="row mb-3">
                                <div class="col-md-4">
                                    <label for="roundsCount" class="form-label">Number of Rounds</label>
                                    <input type="number" class="form-control" id="roundsCount" name="roundsCount"
                                           min="1" max="15" required>
                                </div>
                                <div class="col-md-4">
                                    <label for="timeControlMinutes" class="form-label">Time Control (minutes)</label>
                                    <input type="number" class="form-control" id="timeControlMinutes" 
                                           name="timeControlMinutes" min="1" max="180" required>
                                </div>
                                <div class="col-md-4">
                                    <label for="timeIncrementSeconds" class="form-label">Increment (seconds)</label>
                                    <input type="number" class="form-control" id="timeIncrementSeconds"
                                           name="timeIncrementSeconds" min="0" max="60" required>
                                </div>
                            </div>
                            
                            <div class="d-grid gap-2">
                                <button type="submit" class="btn btn-primary">Create Tournament</button>
                                <a href="${pageContext.request.contextPath}/tournament" 
                                   class="btn btn-outline-secondary">Cancel</a>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/flatpickr"></script>
    <script>
        // Initialize datetime pickers
        flatpickr("#startDate", {
            enableTime: true,
            dateFormat: "Y-m-d H:i",
            minDate: "today",
            time_24hr: true
        });
        
        flatpickr("#endDate", {
            enableTime: true,
            dateFormat: "Y-m-d H:i",
            minDate: "today",
            time_24hr: true
        });
        
        // Update rounds count based on tournament type and participants
        document.getElementById('tournamentType').addEventListener('change', function() {
            const maxParticipants = document.getElementById('maxParticipants').value;
            const roundsCount = document.getElementById('roundsCount');
            
            switch(this.value) {
                case 'SWISS':
                    roundsCount.value = Math.ceil(Math.log2(maxParticipants));
                    break;
                case 'ROUND_ROBIN':
                    roundsCount.value = maxParticipants - 1;
                    break;
                case 'ELIMINATION':
                    roundsCount.value = Math.ceil(Math.log2(maxParticipants));
                    break;
            }
        });
        
        // Form validation
        document.getElementById('tournamentForm').addEventListener('submit', function(e) {
            const startDate = new Date(document.getElementById('startDate').value);
            const endDate = new Date(document.getElementById('endDate').value);
            
            if (endDate <= startDate) {
                e.preventDefault();
                alert('End date must be after start date');
            }
        });
    </script>
</body>
</html>

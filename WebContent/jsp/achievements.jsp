<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Achievements - Chess Game</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.7.2/font/bootstrap-icons.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/css/styles.css" rel="stylesheet">
    <style>
        .achievement-card {
            transition: transform 0.2s;
        }
        .achievement-card:hover {
            transform: translateY(-5px);
        }
        .achievement-icon {
            font-size: 2rem;
            margin-bottom: 1rem;
        }
        .achievement-locked {
            filter: grayscale(100%);
            opacity: 0.5;
        }
        .achievement-progress {
            height: 5px;
            margin-top: 10px;
        }
        .points-badge {
            position: absolute;
            top: 10px;
            right: 10px;
            background-color: #ffc107;
            color: #000;
            padding: 5px 10px;
            border-radius: 20px;
            font-weight: bold;
        }
    </style>
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
                        <a class="nav-link" href="${pageContext.request.contextPath}/tournament">Tournaments</a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link active" href="${pageContext.request.contextPath}/achievements">Achievements</a>
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
        <!-- Achievement Summary -->
        <div class="row mb-4">
            <div class="col-md-4">
                <div class="card text-center">
                    <div class="card-body">
                        <h5 class="card-title">Total Achievements</h5>
                        <p class="display-4">${userStats.totalAchievements}</p>
                    </div>
                </div>
            </div>
            <div class="col-md-4">
                <div class="card text-center">
                    <div class="card-body">
                        <h5 class="card-title">Achievement Points</h5>
                        <p class="display-4">${userStats.totalPoints}</p>
                    </div>
                </div>
            </div>
            <div class="col-md-4">
                <div class="card text-center">
                    <div class="card-body">
                        <h5 class="card-title">Completion Rate</h5>
                        <p class="display-4">
                            <fmt:formatNumber value="${userStats.completionRate}" type="percent"/>
                        </p>
                    </div>
                </div>
            </div>
        </div>

        <!-- Achievement Categories -->
        <div class="row mb-4">
            <div class="col">
                <div class="btn-group w-100">
                    <button type="button" class="btn btn-outline-primary active" data-category="all">All</button>
                    <button type="button" class="btn btn-outline-primary" data-category="tournament">Tournament</button>
                    <button type="button" class="btn btn-outline-primary" data-category="rating">Rating</button>
                    <button type="button" class="btn btn-outline-primary" data-category="special">Special</button>
                </div>
            </div>
        </div>

        <!-- Achievements Grid -->
        <div class="row row-cols-1 row-cols-md-3 g-4">
            <c:forEach items="${achievements}" var="achievement">
                <div class="col achievement-item" data-category="${achievement.category}">
                    <div class="card h-100 achievement-card ${achievement.earned ? '' : 'achievement-locked'}">
                        <div class="card-body text-center">
                            <span class="points-badge">${achievement.points} pts</span>
                            <div class="achievement-icon">
                                <i class="bi bi-${achievement.icon}"></i>
                            </div>
                            <h5 class="card-title">${achievement.name}</h5>
                            <p class="card-text">${achievement.description}</p>
                            <c:if test="${achievement.earned}">
                                <small class="text-muted">
                                    Earned on <fmt:formatDate value="${achievement.earnedDate}" pattern="MMM dd, yyyy"/>
                                </small>
                            </c:if>
                            <c:if test="${achievement.hasProgress}">
                                <div class="progress achievement-progress">
                                    <div class="progress-bar" role="progressbar" 
                                         style="width: ${achievement.progress}%"
                                         aria-valuenow="${achievement.progress}" 
                                         aria-valuemin="0" 
                                         aria-valuemax="100">
                                    </div>
                                </div>
                                <small class="text-muted">
                                    Progress: ${achievement.currentValue}/${achievement.targetValue}
                                </small>
                            </c:if>
                        </div>
                    </div>
                </div>
            </c:forEach>
        </div>
    </div>

    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js"></script>
    <script>
        $(document).ready(function() {
            // Achievement filtering
            $('.btn-group button').click(function() {
                $('.btn-group button').removeClass('active');
                $(this).addClass('active');
                
                const category = $(this).data('category');
                if (category === 'all') {
                    $('.achievement-item').show();
                } else {
                    $('.achievement-item').hide();
                    $('.achievement-item[data-category="' + category + '"]').show();
                }
            });
            
            // Achievement card hover effect
            $('.achievement-card').hover(
                function() {
                    $(this).addClass('shadow-lg');
                },
                function() {
                    $(this).removeClass('shadow-lg');
                }
            );
        });
    </script>
</body>
</html>

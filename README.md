# Chess Game Website

A full-featured online chess gaming platform built with Java EE, offering real-time gameplay, tournaments, and advanced chess analysis.

## 🛠 Technology Stack


- **Frontend**
  - HTML5, CSS3, JavaScript
  - jQuery
  - Bootstrap 5
  - WebSocket client

 - **Backend**
   - Java EE 10
   - Jakarta Servlet/JSP/jstl/El
   - test case using junit
   - WebSocket API
   - MySQL Database
   - Maven for dependency management


- **Libraries**
  - chesslib: Chess move validation and game logic
  - iText 7: PDF generation for certificates
  - BCrypt: Password hashing
  - GSON: JSON processing
  - JUnit 5 & Mockito: Testing


## 🎮 Features

- **User Authentication & Profiles**
  - Secure registration and login system
  - ELO rating system
  - User profile management
  - Password encryption using BCrypt

- **Real-time Chess Gameplay**
  - Real-time multiplayer chess matches
  - WebSocket-based live game updates
  - Move validation and game state management
  - Voice control support for moves

- **Tournament System**
  - Create and join tournaments
  - Tournament brackets and matchmaking
  - Real-time tournament updates
  - Tournament certificates (PDF generation)

- **Expert Analysis**
  - Position evaluation
  - Move suggestions
  - Game analysis and replay
  - Material balance calculation

- **Social Features**
  - Friend system
  - Game history
  - Leaderboards
  - Chat functionality


## 📋 Prerequisites

- JDK 17 or higher
- Apache Tomcat 10.1
- MySQL 8.0 or higher
- Maven 3.8+
- Eclipse IDE (2023-09 or later recommended)

## 🚀 Installation & Setup

1. **Clone the Repository**
   ```bash
   git clone https://github.com/Mohammadosama55/ChessGameWebsite.git
   cd ChessGameWebsite
   ```

2. **Database Setup**
   - Create a MySQL database
   - Execute the SQL scripts from `Database/schema.sql`
   - Configure database connection in `src/main/resources/database.properties`

3. **Project Setup in Eclipse**
   - Import as Maven project
   - Right-click project → Properties → Project Facets
   - Enable: Dynamic Web Module 5.0, Java 17, JavaScript
   - Configure build path to use JDK 17

4. **Server Configuration**
   - Install Apache Tomcat 10.1
   - Add Tomcat server in Eclipse
   - Configure project deployment assembly

5. **Build & Run**
   ```bash
   mvn clean install
   ```
   - Deploy to Tomcat server
   - Access at `http://localhost:8080/ChessGameWebsite`

## 🧪 Testing

The project includes comprehensive test coverage:

```bash
# Run all tests
mvn test

# Run specific test category
mvn test -Dtest=UserDAOTest
mvn test -Dtest=TournamentServiceTest
```

## 📁 Project Structure

```
ChessGameWebsite/
├── src/
│   ├── main/
│   │   ├── java/com/chessgame/
│   │   │   ├── controller/    # Servlets
│   │   │   ├── dao/          # Data Access Objects
│   │   │   ├── model/        # Entity classes
│   │   │   ├── service/      # Business logic
│   │   │   └── util/         # Utilities
│   │   └── resources/        # Configuration files
│   └── test/                 # Test cases
├── WebContent/
│   ├── WEB-INF/
│   ├── css/
│   ├── js/
│   ├── images/
│   └── jsp/
├── Database/                 # SQL scripts
└── pom.xml                  # Maven configuration
```

## 🔒 Security Features

- Password hashing with BCrypt
- SQL injection prevention
- XSS protection
- CSRF protection
- Secure WebSocket communication
- Input validation and sanitization

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request



## 👥 Authors

- mohammad osama  - *first project* - [(https://github.com/Mohammadosama55/ChessGameWebsite))

## 🙏 Acknowledgments

- Chess library by [bhlangonijr](https://github.com/bhlangonijr/chesslib)
- iText PDF library
- Apache Tomcat team
- All contributors and testers

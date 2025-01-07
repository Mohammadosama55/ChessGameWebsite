# Chess Game Website

The Chess Game Website is a web application that allows users to play chess, solve puzzles, participate in tournaments, and analyze games. This project is built using Java Servlets, JSP, and MySQL, following the MVC (Model-View-Controller) architecture.

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


### Review 1: Project Setup
- **Create a new project with JDK setup**: Ensure that the Java Development Kit (JDK) is installed and configured.
- **Define the project structure**: Organize the project into appropriate directories for controllers, models, and views.
- **Design the database schema for the project**: Create a schema that includes tables for users, games, puzzles, and tournaments.
- **Create MySQL tables**: Use the provided `schema.sql` file to create the necessary tables in your MySQL database.
- **Implement JDBC for database connectivity**: Use the `DBUtil.java` class for establishing connections to the database.
- **Create DAO classes for database operations**: Implement DAO classes for managing CRUD operations for each entity.

### Review 2: User Management
- **Design HTML templates for user management**: Create user registration and login forms using HTML.
- **Style HTML templates using CSS and Bootstrap**: Use Bootstrap for responsive design and styling.
- **Implement JavaScript for form validation and interactivity**: Add client-side validation to enhance user experience.

### Review 3: Servlets and JSP Integration
- **Create and configure Servlets**: Implement servlets for handling user requests and responses.
- **Implement doGet and doPost methods**: Handle GET and POST requests in your servlets.
- **Implement user form registration and profile using Servlets**: Manage user registration and profile updates through servlets.
- **Integrate JSP with Servlets**: Use JSP pages to render dynamic content based on user interactions.
- **Implement JSP pages for displaying user data**: Create JSP pages to show user profiles, game history, and more.
- **Use JSTL and EL in JSP pages**: Utilize JavaServer Pages Standard Tag Library (JSTL) and Expression Language (EL) for easier data handling in JSP.

### Review 4: Testing and Documentation
- **Create unit tests for service and DAO layers using JUnit**: Write tests to ensure the functionality of your service and DAO classes.
- **Perform a final review of the project**: Review the code for any improvements or optimizations.
- **Prepare project documentation**: Document the project setup, usage, and any other relevant information.

   



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

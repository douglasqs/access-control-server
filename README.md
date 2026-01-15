# Access Control Server

This project is a backend server developed in **Java (Spring Boot)** with a Web interface, designed to interact with **Dahua** access controllers and LPR (License Plate Recognition) cameras.

The system acts as an intermediary that receives event notifications from controllers and allows sending remote commands (such as opening doors/gates) using Digest authentication.

## 🚀 Features

-   **Event Monitoring**: Receives and processes event notifications from controllers via HTTP multipart.
-   **Remote Command**: Sends CGI commands to controllers (e.g., open door) supporting GET and POST methods with Digest authentication.
-   **Friendly Web Interface**:
    -   Real-time dashboard with event and command counters.
    -   Activity log viewable on screen.
    -   **Dark Theme** mode.
-   **Stress Test (Continuous Trigger)**: Functionality to send multiple commands in sequence to test device stability and response.
-   **Dynamic Configuration**: Change the server port via the graphical interface.
-   **Logs**: Export activity logs to text files.

## 🛠️ Technologies Used

-   **Java 17**
-   **Spring Boot**: Main framework (Web, Thymeleaf).
-   **Apache HttpClient**: For robust HTTP communication with Digest Auth support.
-   **Thymeleaf**: Template engine for the frontend.
-   **HTML5 / CSS3 / JavaScript**: User interface (no heavy frameworks).

## 📋 Prerequisites

-   JDK 17 installed.
-   Maven installed (or use the included `mvnw` wrapper).

## ⚙️ Configuration

Main configurations are located in the `src/main/resources/application.properties` file.

```properties
server.port=3000
# Log Configuration
logging.level.root=INFO
```

You can also change the server port directly through the web interface by clicking the **⚙️ Config** button.

## 🚀 How to Run

### Via Maven

```bash
mvn spring-boot:run
```

### Via Jar (Production)

1.  Compile the project:
    ```bash
    mvn clean package
    ```
2.  Run the generated `.jar` file in the `target` folder:
    ```bash
    java -jar target/access-control-server-0.0.1-SNAPSHOT.jar
    ```

## 🖥️ Interface Usage

Access `http://localhost:3000` (or the configured port) in your browser.

### Control Panel
-   **Quick Commands**: Pre-configured buttons for common actions (e.g., Open Door 1).
-   **Custom Command**: Form to build specific CGI requests for the camera/controller.
-   **Continuous Trigger**: Configure the number of repetitions and interval for load intervals.
-   **Logs**: The black area on the right shows logs in real-time. Use the buttons below to save or clear.

## 🔒 Authentication with Controllers

The server automatically implements **Digest Authentication** when communicating with Dahua devices, ensuring commands are accepted by password-protected devices.

## 🤝 Contribution

Feel free to open issues or send pull requests for improvements.

---
Developed for integration with electronic security devices.

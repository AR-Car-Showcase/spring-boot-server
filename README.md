# 🖥️ AR Car Showcase — Server

The backend for the **AR Car Showcase** mobile application. Built with **Spring Boot** (Java 17) and a **Python** microservice for 3D model processing. Exposes a RESTful API for car data, user authentication, customization, and AI-powered recommendations.

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Core API | Spring Boot 3.x (Java 17) |
| Build Tool | Maven |
| Security | Spring Security + JWT |
| Database | PostgreSQL / MySQL (JPA/Hibernate) |
| 3D Processing | Python 3.x + Flask + Blender headless |
| Recommendation Engine | Java (Spring Service) + Python ML |
| Static Model Hosting | Spring Boot Static Resources |

---

## ✨ Features

- 🔐 **JWT Authentication** – Stateless sign-in/sign-up with token-based session management
- 🚗 **Car Catalog API** – Full CRUD for car data with filtering by brand, body type, fuel type, and budget
- 🎨 **3D Customization Engine** – Triggers a Python/Blender microservice to generate custom GLB models
- 🤖 **Recommendation Engine** – Personalized car suggestions based on user preferences and viewing history
- 💾 **Customization Persistence** – Saves and retrieves user-specific design configurations
- 🌐 **Cross-Origin Support** – Configured for React Native / Expo mobile client

---

## 🏗️ System Architecture

```mermaid
graph TB
    subgraph "Mobile Client (React Native / Expo)"
        UI["User Interface"]
        API_Client["Fetch / AsyncStorage"]
    end

    subgraph "Spring Boot Core Server (Java 17)"
        AuthController["AuthController\n/api/auth/**"]
        CarController["CarController\n/api/cars/**"]
        CustomController["CustomizationController\n/api/customizations/**"]
        RecController["RecommendationController\n/api/recommendations/**"]

        AuthService["AuthService"]
        CarService["CarService"]
        CustomService["CustomizationService"]
        RecService["RecommendationService"]

        JwtFilter["AuthTokenFilter (JWT)"]
        SecurityConfig["WebSecurityConfig"]

        CarRepo["CarRepository (JPA)"]
        UserRepo["UserRepository (JPA)"]
        CustomRepo["CustomizationRepository (JPA)"]
    end

    subgraph "Microservices"
        BlenderSvc["Blender Service\n(Python/Flask :5000)"]
        MLSvc["Recommendation ML\n(Python :8000)"]
    end

    DB[(PostgreSQL)]
    ModelStore[("Static .glb Files\n/resources/static/models")]

    API_Client <--> AuthController
    API_Client <--> CarController
    API_Client <--> CustomController
    API_Client <--> RecController

    JwtFilter --> SecurityConfig

    AuthController --> AuthService
    CarController --> CarService
    CustomController --> CustomService
    RecController --> RecService

    AuthService --> UserRepo
    CarService --> CarRepo
    CustomService --> CustomRepo
    CustomService --> BlenderSvc
    RecService --> MLSvc

    CarRepo --> DB
    UserRepo --> DB
    CustomRepo --> DB

    BlenderSvc --> ModelStore
    ModelStore --> API_Client
```

---

## 🔄 Sequence Diagram: Authentication Flow

```mermaid
sequenceDiagram
    participant U as User
    participant UI as LoginScreen
    participant AC as AuthContext
    participant CL as API Client
    participant S as Spring Boot Server
    participant AS as AsyncStorage

    U->>UI: Enter Credentials
    UI->>AC: signIn(user, pass)
    AC->>CL: post('/auth/signin')
    CL->>S: POST /api/auth/signin
    S-->>CL: Response (JWT + User Data)
    CL-->>AC: Adapted Data

    alt Success
        AC->>AS: Save Token & User Body
        AC->>UI: Update Auth State
        UI->>U: Navigate to Home
    else Failure
        AC->>UI: Return Error Message
        UI->>U: Display Alert
    end
```

---

## 🔄 Sequence Diagram: 3D Studio & AR Flow

```mermaid
sequenceDiagram
    participant U as User
    participant HS as HybridScreen (3D Studio)
    participant CC as CarContext (React)
    participant CD as CustomizationDrawer
    participant AR as ARHybridScene (Viro)
    participant BV as Blender Microservice (Python)

    U->>HS: Select Car from Catalog
    HS->>CC: updateVehicle(carData)
    U->>HS: Toggle Customizer Workspace
    HS->>CD: mount(isPickerVisible=true)

    U->>CD: Modify Material Color (e.g. #FF0000)
    CD->>CC: updateMaterialColor(part, color)
    CC-->>HS: trigger(r3f-re-render)
    HS->>HS: Apply Material to Mesh Object

    U->>HS: Click "Apply to AR"
    HS->>BV: generateCustomModel(vehicleId, materials)
    activate BV
    BV->>BV: Load Base .blend
    BV->>BV: Update Cycles/Eevee Materials
    BV->>BV: Export .glb Stream
    BV-->>HS: Return Model Metadata & Filename
    deactivate BV

    HS->>HS: setGeneratedModelUrl(newUrl)
    HS->>AR: mount(viroAppProps)
    AR->>AR: initialize(ARSession)
    AR->>AR: fetch(customModelUrl)
    AR-->>U: Project Customized Car in AR
```

---

## 🔄 Sequence Diagram: Recommendation & Search Flow

```mermaid
sequenceDiagram
    participant U as User
    participant SC as Search/Home Screen
    participant AC as AuthContext
    participant RA as Recommendation API
    participant S as Spring Boot Server
    participant RE as Java Recommendation Engine

    U->>SC: Enter Search Query / View Home
    SC->>AC: Get User Preferences
    SC->>RA: getUserRecommendations()
    RA->>S: GET /api/recommendations/personalized
    S->>RE: Query Preferred Matches
    RE-->>S: List of Recommended Cars
    S-->>RA: JSON Data
    RA-->>SC: Map to UI Components
    SC->>U: Display "Recommended for You"

    U->>SC: Click on a Car
    SC->>RA: trackInteraction(carId, 'view')
    RA->>S: POST /api/recommendations/feedback
    S->>RE: Update User Behavior Model
```

---

## 📊 Class Diagram: Core Logic

```mermaid
classDiagram
    class AuthController {
        +signin(LoginRequest) ResponseEntity
        +signup(SignupRequest) ResponseEntity
    }

    class CarController {
        +getAllCars() List~Car~
        +getCarById(id) Car
        +searchCars(query) List~Car~
        +filterCars(params) List~Car~
    }

    class CustomizationController {
        +saveCustomization(request) Customization
        +getUserCustomizations(userId) List~Customization~
        +getCustomizationById(id) Customization
    }

    class AuthService {
        +authenticateUser(username, password) JwtResponse
        +registerUser(SignupRequest) MessageResponse
    }

    class CarService {
        +findAll() List~Car~
        +findById(id) Car
        +search(query) List~Car~
    }

    class CustomizationService {
        +saveCustomization(userId, carId, materials) Customization
        -callBlenderService(vehicleId, materials) String
    }

    class RecommendationService {
        +getPersonalizedRecommendations(userId) List~Car~
        +getSimilarCars(carId) List~Car~
        +recordInteraction(userId, carId, type) void
    }

    class User {
        +Long id
        +String username
        +String email
        +String password
        +Set~Role~ roles
        +List~String~ favBrands
        +List~String~ preferredBodyTypes
        +Double maxBudget
    }

    class Car {
        +Long id
        +String brand
        +String model
        +String fuelType
        +String transmissionType
        +String bodyType
        +Double price
        +String model3D
        +CarImages images
    }

    class Customization {
        +Long id
        +Long userId
        +Long carId
        +Map~String,String~ materials
        +String generatedModelUrl
        +LocalDateTime createdAt
    }

    class JwtUtils {
        +generateJwtToken(auth) String
        +validateJwtToken(token) boolean
        +getUsernameFromToken(token) String
    }

    class AuthTokenFilter {
        +doFilterInternal(req, res, chain) void
    }

    AuthController --> AuthService
    CarController --> CarService
    CustomizationController --> CustomizationService
    AuthService --> JwtUtils
    AuthService --> User
    CarService --> Car
    CustomizationService --> Customization
    AuthTokenFilter --> JwtUtils
```

---

## 🔁 State Machine Diagram

```mermaid
stateDiagram-v2
    [*] --> Guest_State

    state Guest_State {
        [*] --> Home_Browse
        Home_Browse --> Details_View
        Details_View --> Home_Browse
        Details_View --> 3D_Studio_Limited
    }

    Guest_State --> Authenticated_State : Login Success

    state Authenticated_State {
        [*] --> Dashboard
        Dashboard --> Customization_Studio
        Customization_Studio --> AR_Mode
        AR_Mode --> Customization_Studio

        Dashboard --> Comparison_Tool
        Dashboard --> Personal_Showroom

        state Customization_Studio {
            [*] --> Exterior_View
            Exterior_View --> Interior_View
            Interior_View --> Exterior_View
        }
    }

    Authenticated_State --> Guest_State : Logout
```

---

## 📂 Project Structure

```
AR-Car-Showcase-Server/
│
├── src/
│   └── main/
│       ├── java/com/arcarshowcaseserver/
│       │   ├── controller/           # REST Controllers (Auth, Car, Customization, Recommendation)
│       │   ├── service/              # Business logic layer
│       │   ├── repository/           # Spring Data JPA repositories
│       │   ├── model/                # JPA Entities (User, Car, Customization)
│       │   ├── payload/              # Request/Response DTOs
│       │   ├── security/             # JWT utils, AuthTokenFilter, WebSecurityConfig
│       │   └── ArCarShowcaseServerApplication.java
│       └── resources/
│           ├── application.properties
│           ├── static/models/        # Hosted .glb 3D model files
│           └── data/cars_data_final.json  # Car catalog seed data
│
├── blender-service/                  # Python Flask microservice
│   ├── server.py                     # Flask API entry point
│   ├── generate.py                   # Blender headless script
│   └── requirements.txt
│
├── car-recommendation-service/       # Recommendation microservice
├── pom.xml                           # Maven build file
└── mvnw / mvnw.cmd                   # Maven wrapper scripts
```

---

## 🚀 Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+
- PostgreSQL (or MySQL) running locally
- Python 3.9+ with Blender installed (for 3D generation)

### Running the Spring Boot Server

1. **Clone the repository**

   ```bash
   git clone https://github.com/AdepuSriCharan/AR-Car-Showcase-Server.git
   cd AR-Car-Showcase-Server
   ```

2. **Configure the database** — edit `src/main/resources/application.properties`:

   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/arcarshowcase
   spring.datasource.username=your_user
   spring.datasource.password=your_password
   spring.jpa.hibernate.ddl-auto=update
   ```

3. **Run the server**

   ```bash
   ./mvnw spring-boot:run
   ```

   The API will be available at `http://localhost:8080`.

### Running the Blender Service

```bash
cd blender-service
pip install -r requirements.txt
python server.py
```

The Blender microservice will start on `http://localhost:5000`.

---

## 🔌 API Endpoints

### Auth

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/auth/signin` | Login and receive JWT |
| `POST` | `/api/auth/signup` | Register a new user |

### Cars

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/cars/allcars` | Get all cars |
| `GET` | `/api/cars/{id}` | Get car by ID |
| `GET` | `/api/cars/search?q=` | Search cars by query |
| `GET` | `/api/cars/filter` | Filter by body type, fuel, budget |

### Customizations *(requires auth)*

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/customizations` | Save a customization |
| `GET` | `/api/customizations/user/{userId}` | Get user's saved designs |
| `GET` | `/api/customizations/{id}` | Get specific customization |

### Recommendations *(requires auth)*

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/recommendations/personalized` | Personalized car feed |
| `GET` | `/api/recommendations/similar/{carId}` | Similar cars |
| `POST` | `/api/recommendations/feedback` | Record user interaction |

---

## 🔐 Security Architecture

```mermaid
sequenceDiagram
    participant C as Client
    participant F as AuthTokenFilter
    participant SC as SecurityContext
    participant CTR as Controller

    C->>F: HTTP Request + Authorization: Bearer <token>
    F->>F: validateJwtToken(token)
    alt Token Valid
        F->>SC: setAuthentication(userDetails)
        F->>CTR: Forward Request
        CTR-->>C: 200 Response
    else Token Invalid / Missing
        F-->>C: 401 Unauthorized
    end
```

---

## 🎨 Blender Service: How It Works

The Python microservice receives customization requests from the Spring Boot server and generates a new `.glb` model file with the specified colors applied.

```
Spring Boot → POST /generate → Flask Server → subprocess(blender --background --python generate.py) → New .glb → Return URL → Spring Boot → Frontend
```

**Input payload:**
```json
{
  "base_model": "car.glb",
  "materials": {
    "CAR_BODY_PRIMARY": "#FF0000",
    "CAR_RIM": "#C0C0C0"
  }
}
```

**Output:**
```json
{
  "model_url": "/models/custom_abc123.glb",
  "filename": "custom_abc123.glb"
}
```

---

## 📦 Key Dependencies (pom.xml)

| Dependency | Purpose |
|---|---|
| `spring-boot-starter-web` | REST API framework |
| `spring-boot-starter-data-jpa` | ORM / Database access |
| `spring-boot-starter-security` | Authentication & authorization |
| `jjwt` | JWT generation & validation |
| `postgresql` / `mysql-connector` | Database driver |
| `lombok` | Boilerplate reduction |
| `spring-boot-starter-validation` | Bean validation (`@Valid`) |

---

## 🌐 Learn More

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/)
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/)
- [Blender Python API](https://docs.blender.org/api/current/)
- [ViroReact Documentation](https://viro-community.readme.io/)

---

## 📄 License

MIT License — See [LICENSE](./LICENSE) for details.
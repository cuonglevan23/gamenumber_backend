# 🎯 Tổng Quan Kiến Trúc Project

## 📁 Cấu Trúc Package

```
src/main/java/com/numbergame/gamenumber/
├── config/                 # Configuration classes
│   ├── JwtTokenProvider.java
│   ├── RedisConfig.java
│   └── SecurityConfig.java
│
├── controller/            # REST API Controllers
│   ├── AuthController.java
│   ├── GameController.java
│   └── UserController.java
│
├── dto/                   # Data Transfer Objects
│   ├── request/          # Request DTOs
│   │   ├── LoginRequest.java
│   │   ├── RegisterRequest.java
│   │   ├── GuessRequest.java
│   │   └── BuyTurnsRequest.java
│   └── response/         # Response DTOs
│       ├── ApiResponse.java
│       ├── AuthResponse.java
│       ├── GuessResponse.java
│       ├── UserInfoResponse.java
│       ├── LeaderboardResponse.java
│       ├── TransactionResponse.java
│       └── GameHistoryResponse.java
│
├── entity/               # JPA Entities (với indexing)
│   ├── User.java
│   ├── GameHistory.java
│   └── Transaction.java
│
├── exception/            # Exception Handling
│   ├── ErrorResponse.java
│   ├── GlobalExceptionHandler.java
│   └── custom/          # Custom Exceptions
│       ├── ResourceNotFoundException.java
│       ├── DuplicateResourceException.java
│       ├── InsufficientTurnsException.java
│       ├── GameLockException.java
│       └── InvalidCredentialsException.java
│
├── mapper/              # Entity <-> DTO Mappers
│   ├── UserMapper.java
│   ├── GameHistoryMapper.java
│   └── TransactionMapper.java
│
├── repository/          # JPA Repositories
│   ├── UserRepository.java
│   ├── GameHistoryRepository.java
│   └── TransactionRepository.java
│
├── security/            # Security Components
│   ├── JwtAuthenticationFilter.java
│   └── CustomUserDetailsService.java
│
├── service/            # Service Interfaces
│   ├── IAuthService.java
│   ├── IGameService.java
│   ├── IUserService.java
│   └── impl/          # Service Implementations
│       ├── AuthServiceImpl.java
│       ├── GameServiceImpl.java
│       └── UserServiceImpl.java
│
└── utils/             # Utility Classes
    ├── SecurityUtils.java
    ├── GameUtils.java
    └── RedisUtils.java
```

## 🎯 Các Tính Năng Chính Đã Implement

### 1. **Authentication & Authorization**
- ✅ JWT Token-based authentication
- ✅ Refresh token support
- ✅ Password encryption (BCrypt)
- ✅ Custom UserDetailsService

### 2. **Game Logic**
- ✅ Distributed locking (Redis) để tránh race condition
- ✅ Pessimistic locking (JPA) cho concurrent requests
- ✅ Win rate control (5% configurable)
- ✅ Turn management system

### 3. **Caching Strategy**
- ✅ Redis caching cho leaderboard (TTL: 5 minutes)
- ✅ Redis Sorted Set cho fast leaderboard queries
- ✅ User info caching (TTL: 30 minutes)
- ✅ Cache eviction strategy

### 4. **Database Indexing**
- ✅ **Users table**: idx_username, idx_score_desc, idx_email
- ✅ **GameHistory table**: idx_user_id, idx_played_at, composite indexes
- ✅ **Transactions table**: idx_user_transactions, idx_transaction_type
- ✅ Optimistic locking với @Version

### 5. **Exception Handling**
- ✅ Global exception handler
- ✅ Custom exceptions với meaningful messages
- ✅ Validation error handling
- ✅ Structured error responses

### 6. **Mappers**
- ✅ UserMapper: Entity -> UserInfoResponse
- ✅ GameHistoryMapper: Entity -> GameHistoryResponse
- ✅ TransactionMapper: Entity -> TransactionResponse

### 7. **Utilities**
- ✅ **SecurityUtils**: Current user, authentication helpers
- ✅ **GameUtils**: Random number generation, win rate calculation
- ✅ **RedisUtils**: Distributed locking, cache operations

## 🚀 API Endpoints

### Authentication
```
POST /api/auth/register - Đăng ký user mới
POST /api/auth/login    - Đăng nhập
```

### Game
```
POST /api/game/guess     - Đoán số (authenticated)
GET  /api/game/history   - Lịch sử chơi game (authenticated)
```

### User
```
GET  /api/me            - Thông tin user hiện tại (authenticated)
GET  /api/leaderboard   - Bảng xếp hạng (public)
POST /api/buy-turns     - Mua lượt chơi (authenticated)
GET  /api/transactions  - Lịch sử giao dịch (authenticated)
```

## 🔧 Docker Services

- **MySQL 8.0**: Database với indexing tối ưu
- **Redis 7**: Caching & distributed locking
- **Spring Boot App**: Application server

## 📊 Performance Optimizations

1. **MySQL Indexing**: 
   - Composite indexes cho query phức tạp
   - Covering indexes cho leaderboard
   
2. **Redis Caching**:
   - Sorted Set cho leaderboard (O(log N) complexity)
   - Cache-aside pattern
   
3. **Concurrency Control**:
   - Distributed locks (Redis)
   - Pessimistic locks (JPA)
   - Optimistic locking (@Version)

4. **Connection Pooling**:
   - HikariCP với max 10 connections
   - Proper timeout configurations

## 📝 Best Practices Đã Áp Dụng

✅ **Separation of Concerns**: Controller -> Service -> Repository
✅ **DTO Pattern**: Request/Response DTOs riêng biệt
✅ **Mapper Pattern**: Entity <-> DTO transformation
✅ **Service Interface**: Abstraction layer
✅ **Custom Exceptions**: Domain-specific exceptions
✅ **Validation**: JSR-303 validation annotations
✅ **Logging**: SLF4J với meaningful log messages
✅ **Transaction Management**: @Transactional annotations
✅ **API Response Wrapper**: Consistent API response format

## 🔐 Security Features

- JWT với expiration time
- Refresh token mechanism
- Password hashing (BCrypt)
- CORS configuration
- SQL injection prevention (JPA)
- Distributed locking cho critical sections

---

**Status**: ✅ Project đã sẵn sàng để chạy với Docker!


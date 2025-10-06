# API Documentation - Game Number System

## Tổng quan

Hệ thống Game Đoán Số cung cấp các API RESTful để quản lý xác thực người dùng, chơi game đoán số, và quản lý giao dịch mua lượt chơi.

### Cơ chế bảo mật

- **JWT Authentication**: Access token (15 phút) + Refresh token (7 ngày)
- **HttpOnly Cookie**: Refresh token được lưu trong cookie HttpOnly, Secure, SameSite=Strict
- **Token Rotation**: Mỗi lần refresh, token cũ bị thu hồi và tạo token mới
- **Token Blacklist**: Access token bị đăng xuất được lưu vào Redis blacklist

### Response Format chuẩn

Tất cả API đều trả về format:

```json
{
  "success": Boolean,
  "message": String (optional),
  "data": Object/Array,
  "timestamp": ISO-8601 DateTime String
}
```

### Error Response Format

```json
{
  "type": String (URI),
  "title": String,
  "status": Integer (HTTP Status Code),
  "detail": String,
  "instance": String (Request URI),
  "properties": {
    "timestamp": ISO-8601 DateTime String
  }
}
```

---

## 1. Authentication API (AuthController)

Base path: `/api/v1/auth`

### 1.1. POST /register - Đăng ký người dùng mới

#### Mô tả luồng

1. Client gửi thông tin đăng ký (username, email, password)
2. Server validate input (username 3-50 ký tự, password tối thiểu 6 ký tự, email hợp lệ)
3. Server kiểm tra username và email đã tồn tại chưa
4. Mã hóa password bằng BCrypt
5. Tạo user mới với score = 0, turns = 5 (mặc định)
6. Sinh JWT access token (TTL 15 phút)
7. Sinh refresh token (TTL 7 ngày) và lưu vào database
8. Set refresh token vào HttpOnly cookie
9. Ghi audit log (USER_REGISTER event)
10. Publish event "user.registered" lên Kafka
11. Trả về access token và thông tin user

#### Request Body

```json
{
  "username": String (required, 3-50 chars),
  "email": String (required, valid email format),
  "password": String (required, min 6 chars)
}
```

**Validation Rules:**
- `username`: NotBlank, Size(min=3, max=50)
- `email`: Email format valid
- `password`: NotBlank, Size(min=6)

#### Response Body (201 Created)

```json
{
  "success": true,
  "message": "Registration successful",
  "data": {
    "accessToken": String (JWT),
    "tokenType": "Bearer",
    "expiresIn": Long (seconds, default 900),
    "username": String,
    "email": String,
    "score": Integer (default 0),
    "turns": Integer (default 5)
  },
  "timestamp": "2025-10-06T10:00:00.000Z"
}
```

**Response Headers:**
- `Set-Cookie`: refreshToken=<token>; HttpOnly; Secure; SameSite=Strict; Max-Age=604800; Path=/

**Error Cases:**
- 400 Bad Request: Validation failed (invalid input)
- 409 Conflict: Username hoặc email đã tồn tại

---

### 1.2. POST /sign-in - Đăng nhập

#### Mô tả luồng

1. Client gửi username và password
2. Server validate input
3. Xác thực username/password với BCrypt
4. Kiểm tra số lượng refresh token hiện tại của user
5. Nếu vượt quá MAX_ACTIVE_TOKENS (3), thu hồi các token cũ nhất
6. Sinh JWT access token mới (TTL 15 phút)
7. Sinh refresh token mới (TTL 7 ngày) và lưu vào database
8. Set refresh token vào HttpOnly cookie
9. Cập nhật lastLogin timestamp
10. Ghi audit log (USER_LOGIN event)
11. Publish event "user.login" lên Kafka
12. Cache thông tin user vào Redis (TTL 1 giờ)
13. Trả về access token và thông tin user

#### Request Body

```json
{
  "username": String (required),
  "password": String (required)
}
```

**Validation Rules:**
- `username`: NotBlank
- `password`: NotBlank

#### Response Body (200 OK)

```json
{
  "success": true,
  "message": "Sign in successful",
  "data": {
    "accessToken": String (JWT),
    "tokenType": "Bearer",
    "expiresIn": Long (seconds, default 900),
    "username": String,
    "email": String,
    "score": Integer,
    "turns": Integer
  },
  "timestamp": "2025-10-06T10:00:00.000Z"
}
```

**Response Headers:**
- `Set-Cookie`: refreshToken=<token>; HttpOnly; Secure; SameSite=Strict; Max-Age=604800; Path=/

**Error Cases:**
- 400 Bad Request: Validation failed
- 401 Unauthorized: Sai username hoặc password

---

### 1.3. POST /refresh-token - Làm mới access token

#### Mô tả luồng

1. Client gửi request với refresh token trong cookie
2. Server đọc refresh token từ cookie "refreshToken"
3. Validate refresh token (kiểm tra signature, expiry)
4. Kiểm tra token có bị thu hồi (revoked) không
5. Kiểm tra token có hết hạn không
6. Thu hồi refresh token cũ (set revoked=true, revokedAt=now)
7. Sinh JWT access token mới (TTL 15 phút)
8. Sinh refresh token mới (TTL 7 ngày) với rotation
9. Set refresh token mới vào HttpOnly cookie
10. Ghi audit log (TOKEN_REFRESH event)
11. Trả về access token mới

#### Request

**Cookie Required:**
- `refreshToken`: String (JWT refresh token)

#### Response Body (200 OK)

```json
{
  "success": true,
  "message": "Token refreshed successfully",
  "data": {
    "accessToken": String (JWT),
    "tokenType": "Bearer",
    "expiresIn": Long (seconds, default 900)
  },
  "timestamp": "2025-10-06T10:00:00.000Z"
}
```

**Response Headers:**
- `Set-Cookie`: refreshToken=<new_token>; HttpOnly; Secure; SameSite=Strict; Max-Age=604800; Path=/

**Error Cases:**
- 401 Unauthorized: Refresh token invalid, expired, hoặc revoked
- 400 Bad Request: Cookie không tồn tại

---

### 1.4. POST /logout - Đăng xuất

#### Mô tả luồng

1. Client gửi access token trong request body
2. Server validate access token
3. Thêm access token vào Redis blacklist (TTL = thời gian còn lại của token)
4. Thu hồi tất cả refresh tokens của user (set revoked=true)
5. Xóa HttpOnly cookie chứa refresh token
6. Xóa cache user info trong Redis
7. Ghi audit log (USER_LOGOUT event)
8. Publish event "user.logout" lên Kafka
9. Trả về success response

#### Request Body

```json
{
  "accessToken": String (required, JWT access token)
}
```

**Validation Rules:**
- `accessToken`: NotBlank

#### Response Body (200 OK)

```json
{
  "success": true,
  "message": "Sign out successful",
  "data": null,
  "timestamp": "2025-10-06T10:00:00.000Z"
}
```

**Response Headers:**
- `Set-Cookie`: refreshToken=; HttpOnly; Secure; SameSite=Strict; Max-Age=0; Path=/

**Error Cases:**
- 400 Bad Request: Access token invalid
- 401 Unauthorized: Token đã hết hạn hoặc không hợp lệ

---

## 2. Game API (GameController)

Base path: `/api/v1/game`

**Authentication Required:** Bearer Token trong header `Authorization: Bearer <access_token>`

### 2.1. POST /guess - Đoán số

#### Mô tả luồng

1. Client gửi số đoán (1-5) và có thể gửi winProbability tùy chỉnh
2. Server extract username từ JWT token
3. Áp dụng distributed lock (Redis) cho user để tránh race condition khi gọi đồng thời
4. Lock timeout: 5 giây
5. Kiểm tra số lượt chơi còn lại (turns > 0)
6. Nếu không đủ turns, trả về lỗi 400
7. Trừ 1 lượt chơi từ user (turns - 1)
8. Xác định tỉ lệ thắng:
   - Nếu có winProbability trong request: dùng giá trị đó (0.01-1.0)
   - Nếu không: dùng game.win-rate từ config (mặc định 0.05 = 5%)
9. Random số từ 1-5 của server
10. Tính kết quả dựa trên tỉ lệ thắng:
    - Random một số từ 0.0 đến 1.0
    - Nếu số random < winProbability: user thắng (actualNumber = guessedNumber)
    - Nếu không: user thua (actualNumber khác guessedNumber)
11. Nếu đoán đúng: cộng 1 điểm score
12. Lưu game history vào database
13. Cập nhật leaderboard Redis (sorted set theo score)
14. Cache invalidate: xóa cache user info, turns, score
15. Ghi audit log (GAME_PLAYED event)
16. Publish event "game.played" lên Kafka
17. Release distributed lock
18. Trả về kết quả game

#### Request Body

```json
{
  "number": Integer (required, 1-5),
  "winProbability": Double (optional, 0.01-1.0)
}
```

**Validation Rules:**
- `number`: NotNull, Min(1), Max(5)
- `winProbability`: DecimalMin(0.01), DecimalMax(1.0)

**Request Headers:**
- `Authorization`: Bearer <access_token>

#### Response Body (200 OK)

```json
{
  "success": true,
  "data": {
    "correct": Boolean,
    "guessedNumber": Integer (1-5),
    "actualNumber": Integer (1-5),
    "scoreEarned": Integer (0 hoặc 1),
    "totalScore": Integer,
    "remainingTurns": Integer,
    "message": String ("Correct! You win!" hoặc "Wrong! Try again."),
    "gameId": Long
  },
  "timestamp": "2025-10-06T10:00:00.000Z"
}
```

**Response Data Types:**
- `correct`: Boolean - True nếu đoán đúng
- `guessedNumber`: Integer - Số mà user đoán (1-5)
- `actualNumber`: Integer - Số thực tế của server (1-5)
- `scoreEarned`: Integer - Điểm nhận được lượt này (0 hoặc 1)
- `totalScore`: Integer - Tổng điểm hiện tại của user
- `remainingTurns`: Integer - Số lượt chơi còn lại
- `message`: String - Thông báo kết quả
- `gameId`: Long - ID của game history record

**Error Cases:**
- 400 Bad Request: Validation failed, hoặc không đủ turns
- 401 Unauthorized: Token invalid hoặc expired
- 409 Conflict: Distributed lock timeout (user đang có request khác đang xử lý)
- 500 Internal Server Error: Lỗi hệ thống

---

### 2.2. GET /history - Lấy lịch sử chơi game

#### Mô tả luồng

1. Client gửi request với access token
2. Server extract username từ JWT
3. Query database lấy danh sách game history của user
4. Sắp xếp theo thời gian chơi giảm dần (mới nhất trước)
5. Trả về danh sách lịch sử

#### Request Headers

- `Authorization`: Bearer <access_token>

#### Response Body (200 OK)

```json
{
  "success": true,
  "data": [
    {
      "id": Long,
      "guessedNumber": Integer (1-5),
      "actualNumber": Integer (1-5),
      "isCorrect": Boolean,
      "scoreEarned": Integer (0 hoặc 1),
      "playedAt": "2025-10-06T10:00:00.000Z"
    }
  ],
  "timestamp": "2025-10-06T10:00:00.000Z"
}
```

**Response Array Item Types:**
- `id`: Long - ID của game history record
- `guessedNumber`: Integer - Số user đã đoán
- `actualNumber`: Integer - Số thực tế của server
- `isCorrect`: Boolean - Kết quả đoán đúng/sai
- `scoreEarned`: Integer - Điểm nhận được (0 hoặc 1)
- `playedAt`: ISO-8601 DateTime String - Thời gian chơi

**Error Cases:**
- 401 Unauthorized: Token invalid hoặc expired

---

## 3. User API (UserController)

Base path: `/api/v1`

**Authentication Required:** Bearer Token trong header `Authorization: Bearer <access_token>`

### 3.1. GET /me - Lấy thông tin user hiện tại

#### Mô tả luồng

1. Client gửi request với access token
2. Server extract username từ JWT
3. Kiểm tra cache Redis với key `user:info:{userId}`
4. Nếu cache HIT: trả về data từ cache
5. Nếu cache MISS:
   - Query user từ database
   - Tính rank của user từ Redis sorted set leaderboard
   - Cache kết quả vào Redis (TTL 1 giờ)
6. Trả về thông tin user với rank

#### Request Headers

- `Authorization`: Bearer <access_token>

#### Response Body (200 OK)

```json
{
  "success": true,
  "data": {
    "id": Long,
    "username": String,
    "email": String,
    "score": Integer,
    "turns": Integer,
    "rank": Long (null nếu chưa có trong leaderboard),
    "lastLogin": "2025-10-06T10:00:00.000Z",
    "createdAt": "2025-10-06T10:00:00.000Z"
  },
  "timestamp": "2025-10-06T10:00:00.000Z"
}
```

**Response Data Types:**
- `id`: Long - User ID
- `username`: String - Tên đăng nhập
- `email`: String - Email
- `score`: Integer - Tổng điểm hiện tại
- `turns`: Integer - Số lượt chơi còn lại
- `rank`: Long - Thứ hạng trên bảng xếp hạng (null nếu chưa có)
- `lastLogin`: ISO-8601 DateTime String - Lần đăng nhập cuối
- `createdAt`: ISO-8601 DateTime String - Thời gian tạo tài khoản

**Error Cases:**
- 401 Unauthorized: Token invalid hoặc expired
- 404 Not Found: User không tồn tại

---

### 3.2. GET /leaderboard - Lấy bảng xếp hạng

#### Mô tả luồng

1. Client gửi request (có thể không cần authentication)
2. Kiểm tra cache Redis với key `leaderboard:top100`
3. Nếu cache HIT:
   - Deserialize data từ Redis List
   - Trả về kết quả
4. Nếu cache MISS:
   - Query top 100 users từ Redis sorted set `leaderboard` (sắp xếp theo score giảm dần)
   - Nếu Redis sorted set rỗng, rebuild từ database
   - Serialize và cache vào Redis List (TTL 5 phút)
   - Trả về kết quả
5. Kết quả bao gồm top 100 users với rank, username, score

#### Response Body (200 OK)

```json
{
  "success": true,
  "data": [
    {
      "rank": Integer (1-100),
      "userId": Long,
      "username": String,
      "score": Integer
    }
  ],
  "timestamp": "2025-10-06T10:00:00.000Z"
}
```

**Response Array Item Types:**
- `rank`: Integer - Thứ hạng (1 = cao nhất)
- `userId`: Long - User ID
- `username`: String - Tên đăng nhập
- `score`: Integer - Tổng điểm

**Optimization:**
- Cache 2 lớp: Redis List (TTL 5 phút) + Redis Sorted Set (persistent)
- Rebuild leaderboard khi Redis sorted set rỗng
- Limit 100 records để đảm bảo performance

**Error Cases:**
- 500 Internal Server Error: Lỗi khi query hoặc cache

---

### 3.3. POST /buy-turns - Mua thêm lượt chơi

#### Mô tả luồng

1. Client gửi số lượng gói lượt chơi muốn mua (quantity)
2. Server extract username từ JWT
3. Validate quantity > 0
4. Tính toán:
   - Mỗi gói = 5 turns
   - Giá mỗi gói = 10,000 VND (config: payment.turn-price)
   - Total turns = quantity × 5
   - Total amount = quantity × 10,000
5. Tạo transaction record:
   - Type: BUY_TURNS
   - Payment method: DIRECT (có thể mở rộng VNPAY, MOMO, PAYPAL)
   - Payment status: SUCCESS (giả định thanh toán thành công)
   - Transaction ref: UUID
6. Cộng turns vào user (turns += totalTurns)
7. Cache invalidate: xóa cache turns và user info
8. Ghi audit log (TURN_PURCHASED event)
9. Publish event "turns.purchased" lên Kafka
10. Trả về transaction info

#### Request Body

```json
{
  "quantity": Integer (required, min 1)
}
```

**Validation Rules:**
- `quantity`: NotNull, Min(1)

**Request Headers:**
- `Authorization`: Bearer <access_token>

#### Response Body (200 OK)

```json
{
  "success": true,
  "message": "Purchase successful",
  "data": {
    "id": Long,
    "transactionType": "BUY_TURNS",
    "turnsAdded": Integer (quantity × 5),
    "amount": Decimal (quantity × 10000),
    "paymentMethod": "DIRECT",
    "paymentStatus": "SUCCESS",
    "transactionRef": String (UUID),
    "createdAt": "2025-10-06T10:00:00.000Z"
  },
  "timestamp": "2025-10-06T10:00:00.000Z"
}
```

**Response Data Types:**
- `id`: Long - Transaction ID
- `transactionType`: String - Loại giao dịch (BUY_TURNS)
- `turnsAdded`: Integer - Số lượt đã mua (quantity × 5)
- `amount`: BigDecimal - Tổng tiền (VND)
- `paymentMethod`: String - Phương thức thanh toán (DIRECT, VNPAY, MOMO, PAYPAL)
- `paymentStatus`: String - Trạng thái (SUCCESS, PENDING, FAILED)
- `transactionRef`: String - Mã tham chiếu giao dịch (UUID)
- `createdAt`: ISO-8601 DateTime String - Thời gian tạo

**Error Cases:**
- 400 Bad Request: Validation failed (quantity < 1)
- 401 Unauthorized: Token invalid hoặc expired

---

### 3.4. GET /transactions - Lấy lịch sử giao dịch

#### Mô tả luồng

1. Client gửi request với access token
2. Server extract username từ JWT
3. Query database lấy tất cả transactions của user
4. Sắp xếp theo thời gian tạo giảm dần (mới nhất trước)
5. Trả về danh sách giao dịch

#### Request Headers

- `Authorization`: Bearer <access_token>

#### Response Body (200 OK)

```json
{
  "success": true,
  "data": [
    {
      "id": Long,
      "transactionType": String,
      "turnsAdded": Integer,
      "amount": Decimal,
      "paymentMethod": String,
      "paymentStatus": String,
      "transactionRef": String,
      "createdAt": "2025-10-06T10:00:00.000Z"
    }
  ],
  "timestamp": "2025-10-06T10:00:00.000Z"
}
```

**Response Array Item Types:**
- `id`: Long - Transaction ID
- `transactionType`: String - BUY_TURNS
- `turnsAdded`: Integer - Số lượt đã mua
- `amount`: BigDecimal - Số tiền (VND)
- `paymentMethod`: String - DIRECT, VNPAY, MOMO, PAYPAL
- `paymentStatus`: String - SUCCESS, PENDING, FAILED
- `transactionRef`: String - Mã tham chiếu (UUID)
- `createdAt`: ISO-8601 DateTime String - Thời gian giao dịch

**Error Cases:**
- 401 Unauthorized: Token invalid hoặc expired

---

## 4. Data Types Reference

### Enum Types

#### TransactionType
- `BUY_TURNS`: Mua lượt chơi

#### PaymentMethod
- `DIRECT`: Thanh toán trực tiếp (giả định)
- `VNPAY`: VNPay gateway (có thể mở rộng)
- `MOMO`: MoMo wallet (có thể mở rộng)
- `PAYPAL`: PayPal (có thể mở rộng)

#### PaymentStatus
- `SUCCESS`: Thành công
- `PENDING`: Đang chờ
- `FAILED`: Thất bại

#### GameEventType
- `USER_REGISTER`: Đăng ký user
- `USER_LOGIN`: Đăng nhập
- `USER_LOGOUT`: Đăng xuất
- `TOKEN_REFRESH`: Làm mới token
- `GAME_PLAYED`: Chơi game
- `TURN_PURCHASED`: Mua lượt chơi

### Common Field Types

- **String**: Chuỗi ký tự UTF-8
- **Integer**: Số nguyên 32-bit
- **Long**: Số nguyên 64-bit
- **Boolean**: true/false
- **BigDecimal**: Số thập phân chính xác cao (cho tiền tệ)
- **ISO-8601 DateTime**: Format `YYYY-MM-DDTHH:mm:ss.SSSZ` (UTC)

---

## 5. HTTP Status Codes

### Success Codes
- **200 OK**: Request thành công
- **201 Created**: Resource được tạo thành công (register)

### Client Error Codes
- **400 Bad Request**: Validation failed, input invalid
- **401 Unauthorized**: Authentication failed, token invalid/expired
- **403 Forbidden**: Không có quyền truy cập
- **404 Not Found**: Resource không tồn tại
- **409 Conflict**: Resource conflict (username/email đã tồn tại, distributed lock timeout)

### Server Error Codes
- **500 Internal Server Error**: Lỗi hệ thống không mong đợi

---

## 6. Security Features

### Token Management
- **Access Token**: JWT, TTL 15 phút, chứa username
- **Refresh Token**: UUID, TTL 7 ngày, lưu database
- **Max Active Tokens**: 3 tokens/user, tự động thu hồi token cũ nhất
- **Token Rotation**: Refresh token cũ bị thu hồi khi tạo token mới
- **Token Blacklist**: Access token đăng xuất được thêm vào Redis blacklist

### Cookie Security
- **HttpOnly**: Không thể truy cập từ JavaScript
- **Secure**: Chỉ gửi qua HTTPS (production)
- **SameSite=Strict**: Chống CSRF attack
- **Max-Age**: 604800 giây (7 ngày)

### Concurrency Control
- **Distributed Lock**: Redis lock cho API /guess để tránh race condition
- **Lock Timeout**: 5 giây
- **Optimistic Locking**: Version field trong User entity

### Audit & Monitoring
- **Audit Log**: Ghi lại tất cả events quan trọng
- **Event Publishing**: Kafka events cho monitoring và analytics
- **Cache Strategy**: Redis cache 2 lớp cho performance

---

## 7. Performance Optimizations

### Caching Strategy

#### User Info Cache
- **Key**: `user:info:{userId}`
- **TTL**: 1 giờ
- **Invalidate**: Khi update score/turns

#### Leaderboard Cache
- **Layer 1**: Redis List `leaderboard:top100` (TTL 5 phút)
- **Layer 2**: Redis Sorted Set `leaderboard` (persistent)
- **Rebuild**: Khi sorted set rỗng, query top 100 từ database

#### Score/Turns Cache
- **Key**: `user:score:{userId}`, `user:turns:{userId}`
- **TTL**: 1 giờ
- **Invalidate**: Sau mỗi game hoặc mua turns

### Distributed Locking
- **Purpose**: Tránh race condition khi user gọi /guess đồng thời
- **Implementation**: Redis SETNX với TTL 5 giây
- **Key Pattern**: `game:lock:{userId}`

### Async Processing
- **Audit Logging**: Async executor với thread pool
- **Event Publishing**: Kafka async producer
- **Cache Operations**: Non-blocking I/O

---

## 8. Configuration Parameters

### JWT Settings
- `jwt.expiration`: 900000ms (15 phút)
- `jwt.refresh-expiration`: 604800000ms (7 ngày)

### Game Settings
- `game.win-rate`: 0.05 (5% tỉ lệ thắng mặc định)
- `game.initial-turns`: 5 (lượt chơi ban đầu)

### Payment Settings
- `payment.turn-price`: 10000 (VND mỗi gói 5 turns)
- `payment.turns-per-package`: 5

### Redis Settings
- `spring.data.redis.host`: localhost
- `spring.data.redis.port`: 6379

### Kafka Settings
- `spring.kafka.bootstrap-servers`: localhost:9092
- Topic: `user-events`

---

## 9. CORS Configuration

### Allowed Origins (Development)
- `http://localhost:3000` - React/Vue/Angular dev server
- `http://localhost:3001` - Alternative port
- `http://127.0.0.1:3000` - IP-based access

### Allowed Methods
- GET, POST, PUT, DELETE, PATCH, OPTIONS

### Credentials Support
- **Enabled**: `Access-Control-Allow-Credentials: true`
- Frontend phải set `credentials: 'include'` khi gọi API để gửi cookies

### Frontend Fetch Example

```javascript
// Axios configuration
axios.defaults.withCredentials = true;

// Fetch API
fetch('http://localhost:8080/api/v1/auth/sign-in', {
  method: 'POST',
  credentials: 'include', // Important for cookies
  headers: {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer ' + accessToken // For authenticated requests
  },
  body: JSON.stringify({ username, password })
})
```

### Important Notes

1. **Credentials Mode**: Khi `credentials: 'include'`, browser sẽ:
   - Gửi cookies (refresh token) trong request
   - Lưu cookies từ response (Set-Cookie header)
   - Yêu cầu origin phải được chỉ định cụ thể (không dùng `*`)

2. **Preflight Requests**: Browser tự động gửi OPTIONS request trước khi gọi API
   - Server trả về CORS headers
   - Cached trong 1 giờ (`max-age: 3600`)

3. **Production**: Thay thế `https://*.yourdomain.com` bằng domain thực tế

4. **Local Testing**: Nếu dùng port khác (ví dụ 3002), thêm vào `allowedOriginPatterns` trong `SecurityConfig.java`

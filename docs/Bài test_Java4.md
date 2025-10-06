**BÀI TEST JAVA** 

**GAMEĐOÁNSỐ** 

**Ngôn ngữ & Framework: Java \+ Spring Boot** 

**YÊUCẦUCHÍNH:** 

**1.API (Có thể làm thêm giao diện hoặc triển khai qua postman)** 

POST /register: Đăng ký (username, password hoặc oauth2) 

POST /login: Đăng nhập 

POST /guess: Đoán số từ 1–5 (server random số từ 1 – 5\) 

Mỗi lần đoán trừ 1 lượt chơi (turns) 

Nếu đoán đúng: cộng điểm score \+1 

Trả về kết quả của server

POST /buy-turns: Mua thêm 5 lượt chơi cho user hiện tại (Có thể cộng trực tiếp hoặc triển khai hệ thống thanh toán VNPAY, PAYPAL, MOMO,…) GET /leaderboard: Trả top 10 người đoán đúng nhiều nhất 

GET /me: Trả về email, score, turns còn lại 

**2.NÂNG CAO** 

Nếu 1 user gọi/guess nhiều lần cùng lúc, xử lý sao để đảm bảo tính đúng đắn của api? 

Đảm bảo khi hệ thống có lượng user lớn, các api /leaderboard/me vẫn trả kết quả nhanh 

**3.BẢO MẬT** 

**Yêu cầu có cơ chế bảo mật cho API** (Có thể dùng JWT, Basic Auth, Session…, hoặc tự nghĩ ra giải pháp riêng)   
**4.LOGIC**

Số random từ 1–5 

**5% tỷ lệ thắng** cho user (tức là tỉ lệ thắng có thể bị điều chỉnh xác suất) Chỉ được đoán nếu turns \> 0 

Nếu còn lượt (turns \> 0), thực hiện đoán và trừ 1 lượt 

Nếu đoán đúng, cộng 1 điểm vào score 

**5.GỢI Ý TABLE** 

\`\`\`sql 

users ( 

id BIGINT AUTO\_INCREMENT PRIMARY KEY, 

username VARCHAR, 

password VARCHAR, 

score INT DEFAULT 0, 

turns INT DEFAULT 0 

) 

**6.LƯU Ý** 

Ưu tiên thiết kế API rõ ràng, dễ hiểu, đặt tên endpoint, method đúng chuẩn RESTful. 

Có giao diện, hệ thống thanh toán là 1 điểm cộng 

Phải đảm bảo có cơ chế bảo mật cho hệ thống 

Có file README.md mô tả cách chạy project: 

Hướng dẫn setup môi trường. 

Cách build/run project. 

Hướng dẫn test nhanh các API (nếu có). 

Nếu có authentication thì mô tả cách lấy token/đăng nhập. 

***Cảm ơn bạn đã dành thời gian làm bài test Chúc bạn hoàn thành xuất sắc\!\!\!*** 
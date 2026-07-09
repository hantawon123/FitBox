USE fitbox_db;

ALTER TABLE user_table AUTO_INCREMENT = 1;
ALTER TABLE notification_table AUTO_INCREMENT = 1;
ALTER TABLE user_preference_table AUTO_INCREMENT = 1;
ALTER TABLE address_table AUTO_INCREMENT = 1;
ALTER TABLE ingredient_table AUTO_INCREMENT = 1;
ALTER TABLE meal_table AUTO_INCREMENT = 1;
ALTER TABLE meal_ingredient_table AUTO_INCREMENT = 1;
ALTER TABLE store_table AUTO_INCREMENT = 1;
ALTER TABLE pickup_point_table AUTO_INCREMENT = 1;
ALTER TABLE store_daily_capacity_table AUTO_INCREMENT = 1;
ALTER TABLE order_table AUTO_INCREMENT = 1;
ALTER TABLE subscription_group_table AUTO_INCREMENT = 1;
ALTER TABLE subscription_template_table AUTO_INCREMENT = 1;
ALTER TABLE subscript_order_table AUTO_INCREMENT = 1;
ALTER TABLE cart_table AUTO_INCREMENT = 1;
ALTER TABLE cart_item_table AUTO_INCREMENT = 1;
ALTER TABLE payment_table AUTO_INCREMENT = 1;
ALTER TABLE user_allergy_table AUTO_INCREMENT = 1;

-- 사용자 더미 데이터 (신체 정보 및 활동량 반영)
INSERT INTO user_table
(user_id, password, name, phone, gender, age, height, weight, activity_level, purpose)
VALUES
('user01', '1234', '김민준', '010-1111-2222', 'M', 25, 175.0, 82.5, 2, '다이어트'),
('user02', '1234', '이서연', '010-3333-4444', 'F', 28, 162.3, 52.0, 4, '고단백'),
('user03', '1234', '박지훈', '010-5555-6666', 'M', 31, 181.2, 74.0, 5, '벌크업'),
('user04', '1234', '최유진', '010-7777-8888', 'F', 23, 158.5, 49.5, 1, '유지어터'),
('seller01', '1234', '핏박스 구미점 관리자', '010-9999-0000', 'M', 35, 178.0, 78.0, 0, 'SELLER');

-- 유저별 알레르기 더미데이터
INSERT INTO user_allergy_table (user_id, allergy_name)
VALUES
(1, '계란'),
(1, '우유'),
(2, '땅콩'),
(3, '대두'),
(3, '밀'),
(3, '갑각류'),
(4, '우유');

-- 주소 더미데이터
INSERT INTO address_table
(user_id, zone_code, road_address, detail_address, address)
VALUES
(1, '39200', '경상북도 구미시 대학로 61', '101동 101호', '[39200] 경상북도 구미시 대학로 61 101동 101호'),
(2, '39430', '경상북도 구미시 인동중앙로 15', '202호', '[39430] 경상북도 구미시 인동중앙로 15 202호'),
(3, '39281', '경상북도 구미시 송정대로 77', '3층', '[39281] 경상북도 구미시 송정대로 77 3층'),
(4, '39160', '경상북도 구미시 산동읍 신당리 1200', 'A동 401호', '[39160] 경상북도 구미시 산동읍 신당리 1200 A동 401호'),
(5, '39220', '경상북도 구미시 구미중앙로 100', '501호', '[39220] 경상북도 구미시 구미중앙로 100 501호');

-- 재료 더미데이터
INSERT INTO ingredient_table
(name, calories, carbohydrate, protein, fat, categories, price, image_url)
VALUES
('현미밥', 1.66, 0.350, 0.035, 0.010, 'BASE', 10, '/images/ingredients/4.jpg'),
('백미밥', 1.30, 0.287, 0.024, 0.003, 'BASE', 10, '/images/ingredients/3.jpg'),
('양상추', 0.15, 0.029, 0.014, 0.002, 'VEGETABLE', 10, '/images/ingredients/15.jpg'),
('퀴노아', 1.20, 0.213, 0.044, 0.019, 'BASE', 15, '/images/ingredients/2.jpg'),

('닭가슴살', 1.65, 0.000, 0.310, 0.036, 'PROTEIN', 42, '/images/ingredients/10.jpg'),
('연어', 2.08, 0.000, 0.200, 0.130, 'PROTEIN', 55, '/images/ingredients/9.jpg'),
('삶은 계란', 1.55, 0.011, 0.130, 0.110, 'PROTEIN', 12, '/images/ingredients/8.jpg'),
('두부', 0.76, 0.019, 0.080, 0.048, 'PROTEIN', 10, '/images/ingredients/7.jpg'),
('새우', 0.99, 0.002, 0.240, 0.003, 'PROTEIN', 30, '/images/ingredients/6.jpg'),

('브로콜리', 0.34, 0.066, 0.028, 0.004, 'VEGETABLE', 10, '/images/ingredients/14.jpg'),
('방울토마토', 0.18, 0.039, 0.009, 0.002, 'VEGETABLE', 10, '/images/ingredients/13.jpg'),
('파프리카', 0.31, 0.060, 0.010, 0.003, 'VEGETABLE', 10, '/images/ingredients/12.jpg'),

('발사믹 드레싱', 0.88, 0.170, 0.005, 0.015, 'SAUCE', 10, '/images/ingredients/19.jpg'),
('오리엔탈 드레싱', 2.50, 0.200, 0.030, 0.180, 'SAUCE', 10, '/images/ingredients/18.jpg'),
('스리라차 소스', 0.93, 0.200, 0.020, 0.010, 'SAUCE', 10, '/images/ingredients/17.jpg'),
('그릭요거트 드레싱', 1.20, 0.060, 0.060, 0.080, 'SAUCE', 12, '/images/ingredients/16.jpg'),

('귀리밥', 1.54, 0.280, 0.055, 0.028, 'BASE', 10, '/images/ingredients/1.jpg'),
('소고기 우둔살', 1.65, 0.000, 0.290, 0.050, 'PROTEIN', 45, '/images/ingredients/5.jpg'),
('오이', 0.15, 0.036, 0.007, 0.001, 'VEGETABLE', 10, '/images/ingredients/11.jpg'),

('고구마', 0.86, 0.201, 0.016, 0.001, 'TOPPING', 10, ''),
('아보카도', 1.60, 0.085, 0.020, 0.147, 'TOPPING', 20, ''),
('옥수수', 0.96, 0.210, 0.034, 0.015, 'TOPPING', 10, ''),
('견과류 믹스', 6.07, 0.210, 0.200, 0.540, 'TOPPING', 25, ''),
('단호박', 0.66, 0.150, 0.014, 0.001, 'TOPPING', 10, '');

-- 완제품 식단 더미데이터
INSERT INTO meal_table
(name, meal_type, price, calories, carbohydrate, protein, fat, image_url)
VALUES
('고단백 닭가슴살 도시락', 'PRODUCT', 8140, 569, 61.7, 51.3, 11.9, '/images/products/1.jpg'),
('연어 아보카도 샐러드', 'PRODUCT', 8500, 360, 14.0, 23.2, 23.9, '/images/products/2.jpg'),
('저탄수 두부 샐러드', 'PRODUCT', 4040, 197, 15.5, 17.3, 9.5, '/images/products/3.jpg'),
('새우 퀴노아 포케', 'PRODUCT', 6500, 421, 44.5, 32.6, 14.3, '/images/products/4.jpg'),
('벌크업 현미 닭새우 도시락', 'PRODUCT', 11240, 722, 95.5, 67.2, 7.0, '/images/products/5.jpg'),
('가벼운 점심 샐러드', 'PRODUCT', 5960, 256, 10.0, 33.5, 9.0, '/images/products/6.jpg'),
('인기 균형 도시락', 'PRODUCT', 7850, 512, 71.4, 23.9, 14.0, '/images/products/7.jpg'),
('저탄수 연어 두부볼', 'PRODUCT', 7640, 309, 9.3, 28.8, 18.0, '/images/products/8.jpg'),
('소고기 귀리 파워볼', 'PRODUCT', 8100, 522, 68.5, 41.5, 10.2, '/images/products/9.jpg'),
('비건 고구마 두부 샐러드', 'PRODUCT', 5200, 353, 38.1, 18.8, 16.6, '/images/products/10.jpg'),

-- 커스텀 식단 예시
('나만의 커스텀 식단 A', 'CUSTOM', 8900, 520, 55.0, 42.0, 12.0, '/images/products/11.jpg'),
('나만의 커스텀 식단 B', 'CUSTOM', 7600, 430, 45.0, 35.0, 10.0, '/images/products/12.jpg');

-- 식단-재료 연결 더미데이터
INSERT INTO meal_ingredient_table
(meal_id, ingredient_id, amount)
VALUES
(1, 1, 150),
(1, 5, 120),
(1, 10, 80),
(1, 7, 50),
(1, 16, 20),

(2, 3, 80),
(2, 6, 100),
(2, 12, 70),
(2, 13, 60),
(2, 16, 20),

(3, 3, 90),
(3, 8, 150),
(3, 10, 80),
(3, 14, 60),
(3, 19, 20),

(4, 4, 120),
(4, 9, 100),
(4, 12, 50),
(4, 15, 50),
(4, 17, 20),

(5, 1, 200),
(5, 5, 120),
(5, 9, 80),
(5, 11, 100),
(5, 10, 80),

(6, 3, 100),
(6, 7, 50),
(6, 5, 80),
(6, 13, 80),
(6, 16, 20),

(7, 2, 150),
(7, 6, 80),
(7, 10, 80),
(7, 11, 100),
(7, 17, 15),

(8, 3, 100),
(8, 6, 80),
(8, 8, 120),
(8, 24, 80),
(8, 19, 20),

(9, 21, 160),
(9, 22, 100),
(9, 10, 80),
(9, 23, 100),
(9, 16, 20),

(10, 3, 100),
(10, 8, 180),
(10, 11, 120),
(10, 12, 50),
(10, 16, 20),

-- 커스텀 식단 A
(11, 1, 100),
(11, 5, 150),
(11, 10, 80),
(11, 16, 20),

-- 커스텀 식단 B
(12, 21, 120),
(12, 22, 100),
(12, 24, 80),
(12, 19, 20);

-- 매장 더미데이터
INSERT INTO store_table
(name, address, longitude, latitude)
VALUES
('핏박스 구미역점', '경상북도 구미시 구미중앙로 76', 128.3356, 36.1284),
('핏박스 인동점', '경상북도 구미시 인동중앙로 42', 128.4182, 36.1079),
('핏박스 산동점', '경상북도 구미시 산동읍 신당리 1420', 128.4315, 36.1542);


INSERT INTO store_table
(name, address, longitude, latitude)
VALUES
('핏박스 구미 송정점', '경북 구미시 송정동 45', 128.3448, 36.1196),
('핏박스 구미 형곡점', '경북 구미시 형곡동 155', 128.3341, 36.1132),
('핏박스 구미 봉곡점', '경북 구미시 봉곡동 387', 128.3147, 36.1395),
('핏박스 구미 인동점', '경북 구미시 인동동 312', 128.4205, 36.1098),
('핏박스 구미 진평점', '경북 구미시 진평동 99', 128.4239, 36.0964),
('핏박스 구미 옥계점', '경북 구미시 옥계동 834', 128.4232, 36.1384),
('핏박스 구미 사곡점', '경북 구미시 사곡동 688', 128.3567, 36.1019),
('핏박스 구미 도량점', '경북 구미시 도량동 246', 128.3375, 36.1337),
('핏박스 구미 원평점', '경북 구미시 원평동 102', 128.3379, 36.1261),
('핏박스 구미 공단점', '경북 구미시 공단동 280', 128.3808, 36.1026);
INSERT INTO pickup_point_table
(store_id, name, address, longitude, latitude)
VALUES
(1, '구미역 1번 출구 픽업박스', '경상북도 구미시 구미중앙로 76 인근 구미역 1번 출구', 128.3362, 36.1289),
(1, '금오천 산책로 픽업존', '경상북도 구미시 원평동 금오천 산책로 입구', 128.3327, 36.1273),
(2, '인동광장 무인 픽업함', '경상북도 구미시 인동중앙로 50 인동광장 앞', 128.4190, 36.1085),
(2, '인의동 버스정류장 픽업존', '경상북도 구미시 인의동 인동중앙로 버스정류장', 128.4168, 36.1071),
(3, '산동 행정복지센터 픽업함', '경상북도 구미시 산동읍 신당리 행정복지센터 앞', 128.4322, 36.1548),
(3, '산동 공원 입구 픽업존', '경상북도 구미시 산동읍 신당리 근린공원 입구', 128.4298, 36.1535);

-- 매장별 관리자 연결 더미데이터
-- seller01은 위 사용자 더미데이터에서 5번 사용자로 생성됨
INSERT INTO pickup_point_table
(store_id, name, address, longitude, latitude)
VALUES
(4, '송정 시청 픽업박스', '경북 구미시 송정동 시청 정류장 앞', 128.3455, 36.1202),
(5, '형곡 도서관 픽업박스', '경북 구미시 형곡동 도서관 입구', 128.3333, 36.1140),
(6, '봉곡 공원 픽업박스', '경북 구미시 봉곡동 공원 입구', 128.3155, 36.1401),
(7, '인동 광장 픽업박스', '경북 구미시 인동동 광장 출구', 128.4213, 36.1104),
(8, '진평역길 픽업박스', '경북 구미시 진평동 역길 입구', 128.4247, 36.0971),
(9, '옥계 커뮤니티 픽업박스', '경북 구미시 옥계동 행정복지센터 앞', 128.4240, 36.1390),
(10, '사곡 시장 픽업박스', '경북 구미시 사곡동 시장 입구', 128.3574, 36.1025),
(11, '도량 초교 픽업박스', '경북 구미시 도량동 초등학교 앞', 128.3382, 36.1344),
(12, '원평 2번출구 픽업박스', '경북 구미시 원평동 2번 출구', 128.3386, 36.1268),
(13, '공단 정문 픽업박스', '경북 구미시 공단동 산업단지 정문', 128.3816, 36.1032);
INSERT INTO store_admin_table
(store_id, user_id)
VALUES
(1, 5),
(2, 5),
(3, 5);

INSERT INTO store_admin_table
(store_id, user_id)
VALUES
(4, 5),
(5, 5),
(6, 5),
(7, 5),
(8, 5),
(9, 5),
(10, 5),
(11, 5),
(12, 5),
(13, 5);
-- fcm_user_token_table에는 더미데이터를 넣지 않음
-- FCM 토큰은 앱 설치 및 로그인 시 Firebase에서 발급받아 자동 저장됨

-- notification_table에도 초기 더미데이터를 넣지 않음
-- 주문 생성 및 주문 상태 변경 시 실제 알림 내역이 자동 저장됨

-- 매장별 날짜별 픽업 가능 수량 더미데이터
INSERT INTO store_daily_capacity_table
(pickup_point_id, pickup_date, total_cnt, remain_cnt)
WITH RECURSIVE date_list AS (
    SELECT DATE('2026-05-14') AS pickup_date

    UNION ALL

    SELECT DATE_ADD(pickup_date, INTERVAL 1 DAY)
    FROM date_list
    WHERE pickup_date < DATE('2026-06-30')
)
SELECT
    pp.id AS pickup_point_id,
    d.pickup_date,
    20 + MOD(pp.id, 11) AS total_cnt,
    20 + MOD(pp.id, 11) AS remain_cnt
FROM pickup_point_table pp
CROSS JOIN date_list d;

-- 단건 주문 더미데이터
INSERT INTO order_table (
    user_id,
    order_time,
    menu_id,
    quantity,
    receive_type,
    receive_date,
    store_id,
    address
)
VALUES
(1, '2026-05-14 11:30:00', 1, 1, 'PICKUP', '2026-05-14', 1, NULL),
(2, '2026-05-14 12:10:00', 2, 1, 'PICKUP', '2026-05-14', 2, NULL),

(3, '2026-05-14 12:30:00', 5, 2, 'DELIVERY', '2026-05-14', NULL,
    (SELECT address FROM address_table WHERE user_id = 3 LIMIT 1)
),

(4, '2026-05-14 13:00:00', 6, 1, 'PICKUP', '2026-05-14', 3, NULL),

(1, '2026-05-15 11:40:00', 4, 1, 'PICKUP', '2026-05-15', 1, NULL),

(2, '2026-05-15 12:20:00', 7, 1, 'DELIVERY', '2026-05-15', NULL,
    (SELECT address FROM address_table WHERE user_id = 2 LIMIT 1)
),

(3, '2026-05-16 12:00:00', 9, 1, 'PICKUP', '2026-05-16', 1, NULL),
(4, '2026-05-16 12:30:00', 10, 1, 'PICKUP', '2026-05-16', 3, NULL),

-- 커스텀 식단 주문 예시
(1, '2026-05-17 12:00:00', 11, 1, 'DELIVERY', '2026-05-17', NULL,
    (SELECT address FROM address_table WHERE user_id = 1 LIMIT 1)
),

(2, '2026-05-17 12:30:00', 12, 1, 'PICKUP', '2026-05-17', 2, NULL);

-- 단건 픽업 주문 더미데이터만큼 날짜별 잔여 수량 차감

UPDATE order_table
SET order_status = 'PICKED_UP'
WHERE id > 0
  AND receive_date < CURRENT_DATE();

UPDATE store_daily_capacity_table sdc
JOIN (
    SELECT
        pickup_point_id,
        receive_date AS pickup_date,
        SUM(quantity) AS used_count
    FROM order_table
    WHERE receive_type = 'PICKUP_POINT'
      AND pickup_point_id IS NOT NULL
      AND receive_date IS NOT NULL
    GROUP BY pickup_point_id, receive_date
) used
ON sdc.pickup_point_id = used.pickup_point_id
AND sdc.pickup_date = used.pickup_date
SET sdc.remain_cnt = sdc.remain_cnt - used.used_count
WHERE sdc.id > 0;

-- 정기 구독 그룹 더미데이터
-- status = ACTIVE: 실제 정기 구독 상품
-- status = CANCELED: 취소된 구독 상품. 앱에서 취소됨 표시 및 재구독 테스트용
-- next_order_month: 다음 달 주문 생성 기준일
-- 현재 앱/서버 정책상 구독은 DELIVERY만 지원한다.
INSERT INTO subscription_group_table
(user_id, order_time, subscription_start_date, subscription_end_date, next_order_month, status, receive_type, store_id, address)
VALUES
-- 그룹 1: user 1 배달 정기 구독
(1, '2026-06-15 10:00:00', '2026-06-17', NULL, '2026-07-17', 'ACTIVE', 'DELIVERY', NULL,
    (SELECT address FROM address_table WHERE user_id = 1 LIMIT 1)
),

-- 그룹 2: user 2 배달 정기 구독
(2, '2026-06-15 11:00:00', '2026-06-17', NULL, '2026-07-17', 'ACTIVE', 'DELIVERY', NULL,
    (SELECT address FROM address_table WHERE user_id = 2 LIMIT 1)
),

-- 그룹 3: user 1 취소된 배달 정기 구독
(1, '2026-05-10 09:30:00', '2026-05-13', '2026-06-12', '2026-06-13', 'CANCELED', 'DELIVERY', NULL,
    (SELECT address FROM address_table WHERE user_id = 1 LIMIT 1)
);

-- 정기 구독 반복 식단표 더미데이터
-- week_of_month: 1~5주차
-- day_of_week: 1=일, 2=월, 3=화, 4=수, 5=목, 6=금, 7=토
INSERT INTO subscription_template_table
(subscription_group_id, week_of_month, day_of_week, meal_id, quantity)
VALUES
-- 그룹 1: 매월 3주차 목요일 meal 1, 4주차 목요일 meal 2
(1, 3, 5, 1, 1),
(1, 4, 5, 2, 1),

-- 그룹 2: 매월 3주차 금요일 meal 3/4, 3주차 일요일 meal 5
(2, 3, 6, 3, 1),
(2, 3, 6, 4, 2),
(2, 3, 1, 5, 1),

-- 그룹 3: 취소된 구독 재구독 테스트용 식단표
(3, 2, 3, 8, 1),
(3, 2, 5, 9, 1);

-- 구독 주문 더미데이터
-- subscription_group_id가 NULL인 데이터는 기존 반복 구독 방식 예시입니다.
-- subscription_group_id가 있는 데이터는 정기 구독 생성 시 첫 달에 실제 생성된 주문 예시입니다.
-- 현재 앱/서버 정책상 구독 주문은 모두 DELIVERY만 사용한다.
INSERT INTO subscript_order_table (
    subscription_group_id,
    user_id,
    order_time,
    date_start,
    date_end,
    mon,
    tue,
    wed,
    thu,
    fri,
    sat,
    sun,
    quantity,
    receive_type,
    store_id,
    address
)
VALUES
-- 기존 반복 구독/예약 주문 예시
(NULL, 1, '2026-05-14 10:00:00', '2026-05-18', '2026-06-14',
 1, NULL, 3, NULL, 6, NULL, NULL, 1, 'DELIVERY', NULL,
    (SELECT address FROM address_table WHERE user_id = 1 LIMIT 1)
),

(NULL, 2, '2026-05-14 10:20:00', '2026-05-18', '2026-06-14',
 2, 4, NULL, 8, 7, NULL, NULL, 1, 'DELIVERY', NULL,
    (SELECT address FROM address_table WHERE user_id = 2 LIMIT 1)
),

(NULL, 3, '2026-05-14 10:40:00', '2026-05-18', '2026-06-14',
 5, 9, 4, 5, 1, NULL, NULL, 1, 'DELIVERY', NULL,
    (SELECT address FROM address_table WHERE user_id = 3 LIMIT 1)
),

(NULL, 4, '2026-05-14 11:00:00', '2026-05-18', '2026-06-14',
 NULL, 6, NULL, 3, NULL, 10, NULL, 1, 'DELIVERY', NULL,
    (SELECT address FROM address_table WHERE user_id = 4 LIMIT 1)
),

-- 커스텀 식단이 포함된 기존 반복 구독 예시
(NULL, 1, '2026-05-17 10:00:00', '2026-05-20', '2026-06-20',
 11, NULL, 1, NULL, 12, NULL, NULL, 1, 'DELIVERY', NULL,
    (SELECT address FROM address_table WHERE user_id = 1 LIMIT 1)
),

-- 그룹 1: user 1 배달 정기 구독 첫 달 주문
-- 2026-06-18 목요일: 3주차 목요일 meal 1
(1, 1, '2026-06-15 10:00:00', '2026-06-18', '2026-06-18',
 NULL, NULL, NULL, 1, NULL, NULL, NULL, 1, 'DELIVERY', NULL,
    (SELECT address FROM address_table WHERE user_id = 1 LIMIT 1)
),

-- 2026-06-25 목요일: 4주차 목요일 meal 2
(1, 1, '2026-06-15 10:00:00', '2026-06-25', '2026-06-25',
 NULL, NULL, NULL, 2, NULL, NULL, NULL, 1, 'DELIVERY', NULL,
    (SELECT address FROM address_table WHERE user_id = 1 LIMIT 1)
),

-- 그룹 2: user 2 배달 정기 구독 첫 달 주문
-- 2026-06-19 금요일: 3주차 금요일 meal 3, 수량 1
(2, 2, '2026-06-15 11:00:00', '2026-06-19', '2026-06-19',
 NULL, NULL, NULL, NULL, 3, NULL, NULL, 1, 'DELIVERY', NULL,
    (SELECT address FROM address_table WHERE user_id = 2 LIMIT 1)
),

-- 2026-06-19 금요일: 3주차 금요일 meal 4, 수량 2
(2, 2, '2026-06-15 11:00:00', '2026-06-19', '2026-06-19',
 NULL, NULL, NULL, NULL, 4, NULL, NULL, 2, 'DELIVERY', NULL,
    (SELECT address FROM address_table WHERE user_id = 2 LIMIT 1)
),

-- 2026-06-21 일요일: 3주차 일요일 meal 5, 수량 1
(2, 2, '2026-06-15 11:00:00', '2026-06-21', '2026-06-21',
 NULL, NULL, NULL, NULL, NULL, NULL, 5, 1, 'DELIVERY', NULL,
    (SELECT address FROM address_table WHERE user_id = 2 LIMIT 1)
),

-- 그룹 3: user 1 취소된 배달 정기 구독의 과거 주문 예시
(3, 1, '2026-05-10 09:30:00', '2026-05-19', '2026-05-19',
 NULL, 8, NULL, NULL, NULL, NULL, NULL, 1, 'DELIVERY', NULL,
    (SELECT address FROM address_table WHERE user_id = 1 LIMIT 1)
),

(3, 1, '2026-05-10 09:30:00', '2026-05-21', '2026-05-21',
 NULL, NULL, NULL, 9, NULL, NULL, NULL, 1, 'DELIVERY', NULL,
    (SELECT address FROM address_table WHERE user_id = 1 LIMIT 1)
);

-- 장바구니 더미데이터
INSERT INTO cart_table
(user_id)
VALUES
(1),
(2),
(3),
(4);

INSERT INTO cart_item_table
(cart_id, meal_id, quantity)
VALUES
-- user 1 장바구니
(1, 1, 1),
(1, 11, 1),

-- user 2 장바구니
(2, 2, 1),
(2, 7, 2),

-- user 3 장바구니
(3, 5, 1),
(3, 9, 1),

-- user 4 장바구니
(4, 6, 1),
(4, 12, 1);

-- 가상 결제 더미데이터
-- order_id는 위에서 생성된 order_table id 기준
INSERT INTO payment_table
(order_id, payment_method, payment_status, amount)
VALUES
(1, 'MOCK', 'SUCCESS', 8140),
(2, 'MOCK', 'SUCCESS', 8500),
(3, 'MOCK', 'SUCCESS', 22480),
(4, 'MOCK', 'SUCCESS', 5960),
(5, 'MOCK', 'SUCCESS', 6500),
(6, 'MOCK', 'SUCCESS', 7850),
(7, 'MOCK', 'SUCCESS', 8100),
(8, 'MOCK', 'SUCCESS', 5200),
(9, 'MOCK', 'SUCCESS', 8900),
(10, 'MOCK', 'SUCCESS', 7600);

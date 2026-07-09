CREATE DATABASE IF NOT EXISTS fitbox_db
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_general_ci;

USE fitbox_db;

-- FK 관계 때문에 자식 테이블부터 삭제
DROP TABLE IF EXISTS notification_table;
DROP TABLE IF EXISTS fcm_user_token_table;
DROP TABLE IF EXISTS store_admin_table;
DROP TABLE IF EXISTS store_daily_capacity_table;
DROP TABLE IF EXISTS payment_table;
DROP TABLE IF EXISTS cart_item_table;
DROP TABLE IF EXISTS cart_table;
DROP TABLE IF EXISTS subscript_order_table;
DROP TABLE IF EXISTS subscription_template_table;
DROP TABLE IF EXISTS subscription_group_table;
DROP TABLE IF EXISTS order_table;
DROP TABLE IF EXISTS pickup_point_table;
DROP TABLE IF EXISTS meal_ingredient_table;
DROP TABLE IF EXISTS address_table;
DROP TABLE IF EXISTS store_table;
DROP TABLE IF EXISTS meal_table;
DROP TABLE IF EXISTS ingredient_table;
DROP TABLE IF EXISTS user_allergy_table;
DROP TABLE IF EXISTS user_preference_table;
DROP TABLE IF EXISTS user_table;

-- 사용자 테이블 (신체 정보 및 활동량 추가 버전)
CREATE TABLE user_table (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(50) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    gender VARCHAR(20),
    age INT,
    height DOUBLE,
    weight DOUBLE,
    activity_level INT,
    purpose VARCHAR(100)
);

-- 사용자별 FCM 토큰 테이블
-- 하나의 기기에서 여러 계정으로 로그인할 수 있으므로
-- 사용자와 토큰을 복합 기본키로 관리
CREATE TABLE fcm_user_token_table (
    user_id INT NOT NULL,
    token VARCHAR(512) NOT NULL,
    updated_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (user_id, token),

    CONSTRAINT fk_fcm_user_token_user
        FOREIGN KEY (user_id)
        REFERENCES user_table(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_fcm_user_token_updated_at
    ON fcm_user_token_table(updated_at);

-- 사용자 및 관리자 앱 내 알림함 테이블
CREATE TABLE notification_table (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    title VARCHAR(100) NOT NULL,
    message VARCHAR(500) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_notification_user
        FOREIGN KEY (user_id)
        REFERENCES user_table(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_notification_user_id_created_at
    ON notification_table(user_id, created_at);

CREATE INDEX idx_notification_user_id_is_read
    ON notification_table(user_id, is_read);

CREATE TABLE user_preference_table (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL UNIQUE,
    preference_prompt TEXT NOT NULL,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_preference_user
        FOREIGN KEY (user_id)
        REFERENCES user_table(id)
        ON DELETE CASCADE
);

CREATE TABLE user_allergy_table (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    allergy_name VARCHAR(100) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_user_allergy (user_id, allergy_name),
    CONSTRAINT fk_allergy_user
        FOREIGN KEY (user_id)
        REFERENCES user_table(id)
        ON DELETE CASCADE
);

-- 주소 테이블
CREATE TABLE address_table (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    zone_code VARCHAR(10),
    road_address VARCHAR(255) NOT NULL,
    detail_address VARCHAR(255),
    address VARCHAR(512) NOT NULL,

    CONSTRAINT fk_address_user
        FOREIGN KEY (user_id)
        REFERENCES user_table(id)
        ON DELETE CASCADE
);

-- 재료 테이블
CREATE TABLE ingredient_table (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    image_url VARCHAR(500),
    calories DOUBLE NOT NULL DEFAULT 0,
    carbohydrate DOUBLE NOT NULL DEFAULT 0,
    protein DOUBLE NOT NULL DEFAULT 0,
    fat DOUBLE NOT NULL DEFAULT 0,
    categories VARCHAR(50) NOT NULL,
    price INT NOT NULL DEFAULT 0
);

-- 식단 테이블
-- meal_type: PRODUCT(완제품), CUSTOM(커스텀 식단)
CREATE TABLE meal_table (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    meal_type VARCHAR(20) NOT NULL,
    image_url VARCHAR(500),
    price INT NOT NULL DEFAULT 0,
    calories INT NOT NULL DEFAULT 0,
    carbohydrate DOUBLE NOT NULL DEFAULT 0,
    protein DOUBLE NOT NULL DEFAULT 0,
    fat DOUBLE NOT NULL DEFAULT 0
);

-- 식단-재료 연결 테이블
CREATE TABLE meal_ingredient_table (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    meal_id BIGINT NOT NULL,
    ingredient_id BIGINT NOT NULL,
    amount DOUBLE NOT NULL DEFAULT 1,

    CONSTRAINT fk_meal_ingredient_meal
        FOREIGN KEY (meal_id)
        REFERENCES meal_table(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_meal_ingredient_ingredient
        FOREIGN KEY (ingredient_id)
        REFERENCES ingredient_table(id)
        ON DELETE CASCADE
);

-- 매장 테이블
CREATE TABLE store_table (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    address VARCHAR(255) NOT NULL,
    longitude DOUBLE NOT NULL,
    latitude DOUBLE NOT NULL
);

CREATE TABLE pickup_point_table (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    address VARCHAR(255) NOT NULL,
    longitude DOUBLE NOT NULL,
    latitude DOUBLE NOT NULL,

    CONSTRAINT fk_pickup_point_store
        FOREIGN KEY (store_id)
        REFERENCES store_table(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_pickup_point_store
    ON pickup_point_table(store_id);

-- 매장별 관리자 연결 테이블
-- 한 관리자가 여러 매장을 관리하거나
-- 한 매장에 여러 관리자를 연결할 수 있음
CREATE TABLE store_admin_table (
    store_id BIGINT NOT NULL,
    user_id INT NOT NULL,

    PRIMARY KEY (store_id, user_id),

    CONSTRAINT fk_store_admin_store
        FOREIGN KEY (store_id)
        REFERENCES store_table(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_store_admin_user
        FOREIGN KEY (user_id)
        REFERENCES user_table(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_store_admin_user_id
    ON store_admin_table(user_id);

-- 단건 주문 테이블
CREATE TABLE order_table (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    order_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    menu_id BIGINT NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    receive_type VARCHAR(20) NOT NULL,
    receive_date DATE,
    store_id BIGINT,
    pickup_point_id BIGINT,
    locker_number VARCHAR(30),
    address VARCHAR(255),
    order_status VARCHAR(30) NOT NULL DEFAULT 'ORDER_REVIEW',

    CONSTRAINT fk_order_user
        FOREIGN KEY (user_id)
        REFERENCES user_table(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_order_meal
        FOREIGN KEY (menu_id)
        REFERENCES meal_table(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_order_store
        FOREIGN KEY (store_id)
        REFERENCES store_table(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_order_pickup_point
        FOREIGN KEY (pickup_point_id)
        REFERENCES pickup_point_table(id)
        ON DELETE SET NULL
);

CREATE INDEX idx_order_status
    ON order_table(order_status);

-- 구독 그룹 테이블
-- 역할:
-- 1) status = 'ACTIVE' / 'PAUSED' / 'CANCELED'
--    → 매월 반복되는 정기 구독 상품 자체
-- 2) status = 'ONE_TIME'
--    → 여러 날짜 선택 주문을 하나로 묶는 1회성 구독/예약 주문 그룹
CREATE TABLE subscription_group_table (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    order_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    subscription_start_date DATE NOT NULL,
    subscription_end_date DATE,
    next_order_month DATE NOT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    -- 현재 앱 정책상 구독은 배송만 지원한다.
    receive_type VARCHAR(20) NOT NULL DEFAULT 'DELIVERY',
    store_id BIGINT,
    address VARCHAR(255),

    CONSTRAINT fk_subscription_group_user
        FOREIGN KEY (user_id)
        REFERENCES user_table(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_subscription_group_store
        FOREIGN KEY (store_id)
        REFERENCES store_table(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_subscription_group_receive_type
        CHECK (receive_type = 'DELIVERY')
);

CREATE INDEX idx_subscription_group_user_order_time
    ON subscription_group_table(user_id, order_time);

CREATE INDEX idx_subscription_group_status
    ON subscription_group_table(status);

CREATE INDEX idx_subscription_group_next_order_month
    ON subscription_group_table(next_order_month);

-- 정기 구독 반복 식단표 테이블
-- week_of_month: 1~5주차
-- day_of_week: 1=일요일, 2=월요일, 3=화요일, 4=수요일, 5=목요일, 6=금요일, 7=토요일
-- meal_id: 완제품(PRODUCT) 또는 커스텀(CUSTOM) 식단 모두 가능
CREATE TABLE subscription_template_table (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    subscription_group_id BIGINT NOT NULL,

    week_of_month INT NOT NULL,
    day_of_week INT NOT NULL,

    meal_id BIGINT NOT NULL,
    quantity INT NOT NULL DEFAULT 1,

    CONSTRAINT fk_subscription_template_group
        FOREIGN KEY (subscription_group_id)
        REFERENCES subscription_group_table(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_subscription_template_meal
        FOREIGN KEY (meal_id)
        REFERENCES meal_table(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_subscription_template_week
        CHECK (week_of_month BETWEEN 1 AND 5),

    CONSTRAINT chk_subscription_template_day
        CHECK (day_of_week BETWEEN 1 AND 7),

    CONSTRAINT chk_subscription_template_quantity
        CHECK (quantity > 0)
);

CREATE INDEX idx_subscription_template_group
    ON subscription_template_table(subscription_group_id);

CREATE INDEX idx_subscription_template_meal
    ON subscription_template_table(meal_id);

-- 구독 실제 주문 테이블
-- 정기 구독 생성 시 첫 달 실제 주문이 저장됨
-- 이후 매월 자동 생성될 실제 주문도 이 테이블에 저장하는 구조
CREATE TABLE subscript_order_table (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    subscription_group_id BIGINT,
    user_id INT NOT NULL,
    order_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_start DATE NOT NULL,
    date_end DATE NOT NULL,

    mon BIGINT,
    tue BIGINT,
    wed BIGINT,
    thu BIGINT,
    fri BIGINT,
    sat BIGINT,
    sun BIGINT,

    quantity INT NOT NULL DEFAULT 1,

    -- 현재 앱 정책상 구독 실제 주문도 배송만 지원한다.
    receive_type VARCHAR(20) NOT NULL DEFAULT 'DELIVERY',
    store_id BIGINT,
    address VARCHAR(255),

    CONSTRAINT fk_subscript_subscription_group
        FOREIGN KEY (subscription_group_id)
        REFERENCES subscription_group_table(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_subscript_user
        FOREIGN KEY (user_id)
        REFERENCES user_table(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_subscript_mon_meal
        FOREIGN KEY (mon)
        REFERENCES meal_table(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_subscript_tue_meal
        FOREIGN KEY (tue)
        REFERENCES meal_table(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_subscript_wed_meal
        FOREIGN KEY (wed)
        REFERENCES meal_table(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_subscript_thu_meal
        FOREIGN KEY (thu)
        REFERENCES meal_table(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_subscript_fri_meal
        FOREIGN KEY (fri)
        REFERENCES meal_table(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_subscript_sat_meal
        FOREIGN KEY (sat)
        REFERENCES meal_table(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_subscript_sun_meal
        FOREIGN KEY (sun)
        REFERENCES meal_table(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_subscript_store
        FOREIGN KEY (store_id)
        REFERENCES store_table(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_subscript_receive_type
        CHECK (receive_type = 'DELIVERY')
);

CREATE INDEX idx_subscript_group_id
    ON subscript_order_table(subscription_group_id);

CREATE INDEX idx_subscript_user_order_time
    ON subscript_order_table(user_id, order_time);

CREATE INDEX idx_subscript_date_start
    ON subscript_order_table(date_start);

-- 장바구니 테이블
CREATE TABLE cart_table (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,

    CONSTRAINT fk_cart_user
        FOREIGN KEY (user_id)
        REFERENCES user_table(id)
        ON DELETE CASCADE
);

-- 장바구니 아이템 테이블
CREATE TABLE cart_item_table (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cart_id BIGINT NOT NULL,
    meal_id BIGINT NOT NULL,
    quantity INT NOT NULL DEFAULT 1,

    CONSTRAINT fk_cart_item_cart
        FOREIGN KEY (cart_id)
        REFERENCES cart_table(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_cart_item_meal
        FOREIGN KEY (meal_id)
        REFERENCES meal_table(id)
        ON DELETE CASCADE
);

CREATE TABLE payment_table (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    payment_method VARCHAR(30) NOT NULL, -- MOCK
    payment_status VARCHAR(20) NOT NULL, -- SUCCESS, FAIL
    amount INT NOT NULL DEFAULT 0,

    CONSTRAINT fk_payment_order
        FOREIGN KEY (order_id)
        REFERENCES order_table(id)
        ON DELETE CASCADE
);

CREATE TABLE store_daily_capacity_table (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pickup_point_id BIGINT NOT NULL,
    pickup_date DATE NOT NULL,
    total_cnt INT NOT NULL,
    remain_cnt INT NOT NULL,

    CONSTRAINT fk_store_daily_capacity_pickup_point
        FOREIGN KEY (pickup_point_id)
        REFERENCES pickup_point_table(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_pickup_point_date
        UNIQUE (pickup_point_id, pickup_date)
);

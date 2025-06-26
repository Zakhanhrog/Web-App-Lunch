-- ===================================================
-- [KHỞI TẠO DATABASE]
-- ===================================================
DROP DATABASE IF EXISTS lunch_app_db;
CREATE DATABASE lunch_app_db CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE lunch_app_db;

SET FOREIGN_KEY_CHECKS = 0;

-- ===================================================
-- [BẢNG CATEGORIES]
-- ===================================================
DROP TABLE IF EXISTS `categories`;
CREATE TABLE `categories` (
                              `id` bigint NOT NULL AUTO_INCREMENT,
                              `name` varchar(100) NOT NULL,
                              PRIMARY KEY (`id`),
                              UNIQUE KEY `UK_t8o6pivur7nn124jehx7cygw5` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `categories` VALUES
                             (1,'Cơm'),
                             (3,'Đồ mặn'),
                             (2,'Rau');

-- ===================================================
-- [BẢNG FOOD_ITEMS]
-- ===================================================
DROP TABLE IF EXISTS `food_items`;
CREATE TABLE `food_items` (
                              `id` bigint NOT NULL AUTO_INCREMENT,
                              `availableToday` bit(1) NOT NULL,
                              `dailyQuantity` int DEFAULT NULL,
                              `imageUrl` varchar(500) DEFAULT NULL,
                              `name` varchar(255) NOT NULL,
                              `price` decimal(10,2) NOT NULL,
                              `quantity` int NOT NULL,
                              `category_id` bigint NOT NULL,
                              PRIMARY KEY (`id`),
                              KEY `FKo8kdgi0mj48avtof8r60nr3mj` (`category_id`),
                              CONSTRAINT `FKo8kdgi0mj48avtof8r60nr3mj` FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `food_items` VALUES
                             (1,b'1',34,'/uploaded-images/food/food_1750859551399_2-1200x676-2-1200x676-3.jpg','Thịt kho tàu',20000.00,100000,3),
                             (2,b'1',37,'/uploaded-images/food/food_1750866030688_2-1200x676-2-1200x676-3.jpg','Cơm trắng gạo tám',10000.00,250,1),
                             (3,b'1',36,'/uploaded-images/food/food_1750866036543_2-1200x676-2-1200x676-3.jpg','Cơm gạo lứt huyết rồng',18000.00,120,1),
                             (5,b'1',35,'/uploaded-images/food/food_1750866052461_admin1.jpg','Cơm cháy kho quẹt',35000.00,70,1),
                             (6,b'1',37,'/uploaded-images/food/food_1750866061668_z6669682685241_4623fef4b9b1c09defbfdea772eabebe.jpg','Xôi xéo',25000.00,100,1),
                             (7,b'1',35,'/uploaded-images/food/food_1750866068747_2-1200x676-2-1200x676-3.jpg','Rau muống luộc chấm mắm tỏi',15000.00,180,2),
                             (8,b'0',0,'/uploaded-images/food/food_1750866075169_2-1200x676-2-1200x676-3.jpg','Cải ngồng xào nấm',25000.00,130,2),
                             (9,b'0',0,'/uploaded-images/food/food_1750866081672_2-1200x676-2-1200x676-3.jpg','Đậu Hà Lan xào tôm',32000.00,100,2),
                             (10,b'0',0,'/uploaded-images/food/food_1750866087884_2-1200x676-2-1200x676-3.jpg','Salad cá ngừ',30000.00,90,2),
                             (11,b'0',0,'/uploaded-images/food/food_1750866095532_2-1200x676-2-1200x676-3.jpg','Nộm su hào cà rốt',22000.00,110,2),
                             (12,b'1',2,'/uploaded-images/food/food_1750907275449_2-1200x676-2-1200x676-3.jpg','Rau cải chíp luộc',18000.00,150,2),
                             (13,b'0',0,'/uploaded-images/food/food_1750907281936_2-1200x676-2-1200x676-3.jpg','Thịt ba chỉ rang cháy cạnh',38000.00,120,3),
                             (14,b'0',0,'/uploaded-images/food/food_1750907288202_2-1200x676-2-1200x676-3.jpg','Gà chiên mắm tỏi',45000.00,100,3),
                             (15,b'1',4,'/uploaded-images/food/food_1750907295395_2-1200x676-2-1200x676-3.jpg','Cá basa kho tộ',42000.00,80,3),
                             (16,b'0',0,'/uploaded-images/food/food_1750907304580_admin1.jpg','Sườn non rim mặn ngọt',50000.00,90,3),
                             (17,b'1',7,'/uploaded-images/food/food_1750907312131_2-1200x676-2-1200x676-3.jpg','Trứng chiên thịt bằm',28000.00,150,3),
                             (18,b'0',0,'/uploaded-images/food/food_1750907318429_2-1200x676-2-1200x676-3.jpg','Mực xào dứa cần tỏi',55000.00,70,3),
                             (19,b'0',0,'/uploaded-images/food/food_1750907325215_2-1200x676-2-1200x676-3.jpg','Bò xào lúc lắc',60000.00,60,3),
                             (20,b'0',0,'/uploaded-images/food/food_1750907331159_2-1200x676-2-1200x676-3.jpg','Chả lá lốt',33000.00,110,3),
                             (21,b'0',0,'/uploaded-images/food/food_1750907336943_2-1200x676-2-1200x676-3.jpg','Tôm sốt bơ tỏi',65000.00,50,3),
                             (22,b'0',0,'/uploaded-images/food/food_1750864407411_2-1200x676-2-1200x676-3.jpg','Thịt bằm rau muống',20000.00,9999,3);

-- ===================================================
-- [BẢNG ROLES]
-- ===================================================
DROP TABLE IF EXISTS `roles`;
CREATE TABLE `roles` (
                         `id` bigint NOT NULL AUTO_INCREMENT,
                         `name` varchar(50) NOT NULL,
                         PRIMARY KEY (`id`),
                         UNIQUE KEY `UK_ofx66keruapi6vyqpv6f2or37` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `roles` VALUES
                        (2,'ROLE_ADMIN'),
                        (1,'ROLE_USER');

-- ===================================================
-- [BẢNG USERS]
-- ===================================================
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
                         `id` bigint NOT NULL AUTO_INCREMENT,
                         `balance` decimal(19,2) NOT NULL,
                         `created_at` datetime(6) NOT NULL,
                         `department` varchar(100) NOT NULL,
                         `enabled` bit(1) NOT NULL,
                         `password` varchar(255) NOT NULL,
                         `updated_at` datetime(6) DEFAULT NULL,
                         `username` varchar(50) NOT NULL,
                         PRIMARY KEY (`id`),
                         UNIQUE KEY `UK_r43af9ap4edm43mmtq01oddj6` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `users` VALUES
                        (1,213161000.00,'2025-06-25 11:50:52.267447','c02',b'1','$2a$10$flYVc.ERzGZm4Am58M/0WuTG559QCPnvA3rC5DPnpWsVob.kGDUqe','2025-06-26 08:11:02.621052','khanh'),
                        (2,999772000.00,'2025-06-25 15:22:06.885021','admin',b'1','$2a$10$J6olwIuRgv8voZyueWV2kO/Kn63d7WHo6HE3i2qgUkzsxSpT.Eo46','2025-06-26 07:03:04.324917','admin'),
                        (3,2999613000.00,'2025-06-26 01:33:53.121992','c02',b'1','$2a$10$0hE1xeFejH.wc5xp9NUKouUiy4WD/aoEakB.xFeYNgTThPRd0.f5y','2025-06-26 07:12:19.825800','Hung');

-- ===================================================
-- [BẢNG ORDERS]
-- ===================================================
DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders` (
                          `id` bigint NOT NULL AUTO_INCREMENT,
                          `order_date` datetime(6) NOT NULL,
                          `total_amount` decimal(19,2) NOT NULL,
                          `user_id` bigint NOT NULL,
                          `note` varchar(500) DEFAULT NULL,
                          PRIMARY KEY (`id`),
                          KEY `FK32ql8ubntj5uh44ph9659tiih` (`user_id`),
                          CONSTRAINT `FK32ql8ubntj5uh44ph9659tiih` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `orders` VALUES
                         (7,'2025-06-25 16:10:55.912737',88000.00,1,NULL),
                         (8,'2025-06-25 16:30:15.650169',88000.00,1,NULL),
                         (9,'2025-06-25 16:46:23.082525',58000.00,2,NULL),
                         (26,'2025-06-26 07:12:19.802496',62000.00,3,NULL),
                         (27,'2025-06-26 08:11:02.587022',61000.00,1,NULL);

-- ===================================================
-- [BẢNG ORDER_ITEMS]
-- ===================================================
DROP TABLE IF EXISTS `order_items`;
CREATE TABLE `order_items` (
                               `id` bigint NOT NULL AUTO_INCREMENT,
                               `price` decimal(10,2) NOT NULL,
                               `quantity` int NOT NULL,
                               `food_item_id` bigint NOT NULL,
                               `order_id` bigint NOT NULL,
                               PRIMARY KEY (`id`),
                               KEY `FKbmnfj15j0ngros6mbdx7q5c01` (`food_item_id`),
                               KEY `FKbioxgbv59vetrxe0ejfubep1w` (`order_id`),
                               CONSTRAINT `FKbmnfj15j0ngros6mbdx7q5c01` FOREIGN KEY (`food_item_id`) REFERENCES `food_items` (`id`),
                               CONSTRAINT `FKbioxgbv59vetrxe0ejfubep1w` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `order_items` VALUES
                              (14,10000.00,1,2,7),
                              (15,60000.00,1,19,7),
                              (16,18000.00,1,12,7),
                              (17,28000.00,1,17,8),
                              (18,60000.00,1,19,8),
                              (19,20000.00,1,1,9),
                              (20,38000.00,1,13,9),
                              (73,20000.00,1,1,26),
                              (74,42000.00,1,15,26),
                              (75,18000.00,1,12,27),
                              (76,18000.00,1,3,27),
                              (77,25000.00,1,6,27);

-- ===================================================
-- [BẢNG USER_ROLES]
-- ===================================================
DROP TABLE IF EXISTS `user_roles`;
CREATE TABLE `user_roles` (
                              `user_id` bigint NOT NULL,
                              `role_id` bigint NOT NULL,
                              PRIMARY KEY (`user_id`,`role_id`),
                              KEY `FKh8ciramu9cc9q3qcqiv4ue8a6` (`role_id`),
                              CONSTRAINT `FKh8ciramu9cc9q3qcqiv4ue8a6` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`),
                              CONSTRAINT `FKhfh9dx7w3ubf1co1vdev94g3f` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `user_roles` VALUES
                             (2,1),
                             (1,2),
                             (3,2);

SET FOREIGN_KEY_CHECKS = 1;

-- ===================================================
-- [KẾT THÚC IMPORT]
-- ===================================================

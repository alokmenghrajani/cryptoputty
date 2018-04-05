DROP DATABASE IF EXISTS `cryptoputty`;

CREATE DATABASE `cryptoputty`;

USE `cryptoputty`;

CREATE TABLE `peers` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `address` VARCHAR(39) DEFAULT NULL,
  `port` SMALLINT UNSIGNED DEFAULT NULL,
  PRIMARY KEY (`id`)
);

INSERT INTO `peers` (`address`, `port`)
VALUES ('192.168.0.101', 18333),
  ('192.168.0.102', 18333),
  ('192.168.0.103', 18333),
  ('192.168.0.104', 18333),
  ('192.168.0.105', 18333);

-- 创建用户收藏表
CREATE TABLE IF NOT EXISTS `user_collections` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `news_id` BIGINT NOT NULL,
  `news_type` VARCHAR(30) NOT NULL COMMENT '新闻类型，记录来源于哪个表',
  `collect_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_news_type` (`user_id`, `news_id`, `news_type`),
  KEY `idx_user_collections_user_id` (`user_id`),
  FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建用户浏览历史表
CREATE TABLE IF NOT EXISTS `user_history` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `news_id` BIGINT NOT NULL,
  `news_type` VARCHAR(30) NOT NULL COMMENT '新闻类型，记录来源于哪个表',
  `view_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_news_type` (`user_id`, `news_id`, `news_type`),
  KEY `idx_user_history_user_id` (`user_id`),
  FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 添加注释
ALTER TABLE `user_collections` COMMENT = '用户收藏新闻表';
ALTER TABLE `user_history` COMMENT = '用户浏览历史表'; 
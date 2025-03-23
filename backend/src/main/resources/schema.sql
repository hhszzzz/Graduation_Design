-- 关键词表
CREATE TABLE IF NOT EXISTS `keyword` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `keyword` VARCHAR(100) NOT NULL COMMENT '关键词',
  `news_type` VARCHAR(50) DEFAULT NULL COMMENT '新闻类型',
  `news_id` BIGINT DEFAULT NULL COMMENT '新闻ID',
  `news_title` VARCHAR(255) DEFAULT NULL COMMENT '新闻标题',
  `record_time` DATETIME NOT NULL COMMENT '记录时间',
  `create_time` DATETIME NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  INDEX `idx_keyword` (`keyword`),
  INDEX `idx_news_type` (`news_type`),
  INDEX `idx_news_id` (`news_id`),
  INDEX `idx_record_time` (`record_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='关键词表'; 
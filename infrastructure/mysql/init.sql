-- 量化交易平台数据库初始化脚本

-- 创建数据库
CREATE DATABASE IF NOT EXISTS quant_trading CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE quant_trading;

-- 用户表
CREATE TABLE IF NOT EXISTS `users` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
  `password` VARCHAR(255) NOT NULL COMMENT '密码(加密)',
  `email` VARCHAR(100) COMMENT '邮箱',
  `phone` VARCHAR(20) COMMENT '手机号',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_username (username),
  INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 用户资金账户表
CREATE TABLE IF NOT EXISTS `user_accounts` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `balance` DECIMAL(20, 2) DEFAULT 0.00 COMMENT '可用余额',
  `frozen` DECIMAL(20, 2) DEFAULT 0.00 COMMENT '冻结金额',
  `total_assets` DECIMAL(20, 2) DEFAULT 0.00 COMMENT '总资产',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_id (user_id),
  FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户资金账户表';

-- 股票信息表
CREATE TABLE IF NOT EXISTS `stocks` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `symbol` VARCHAR(20) NOT NULL UNIQUE COMMENT '股票代码',
  `name` VARCHAR(100) NOT NULL COMMENT '股票名称',
  `exchange` VARCHAR(20) COMMENT '交易所: SH-上交所 SZ-深交所',
  `list_date` DATE COMMENT '上市日期',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 0-退市 1-正常',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_symbol (symbol),
  INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='股票信息表';

-- K线数据表 (建议按月分表)
CREATE TABLE IF NOT EXISTS `kline_daily` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `symbol` VARCHAR(20) NOT NULL COMMENT '股票代码',
  `trade_date` DATE NOT NULL COMMENT '交易日期',
  `open` DECIMAL(10, 2) NOT NULL COMMENT '开盘价',
  `high` DECIMAL(10, 2) NOT NULL COMMENT '最高价',
  `low` DECIMAL(10, 2) NOT NULL COMMENT '最低价',
  `close` DECIMAL(10, 2) NOT NULL COMMENT '收盘价',
  `volume` BIGINT COMMENT '成交量',
  `amount` DECIMAL(20, 2) COMMENT '成交额',
  `change_pct` DECIMAL(10, 4) COMMENT '涨跌幅%',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_symbol_date (symbol, trade_date),
  INDEX idx_trade_date (trade_date),
  INDEX idx_symbol (symbol)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='日K线数据表';

-- 策略配置表
CREATE TABLE IF NOT EXISTS `strategies` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `name` VARCHAR(100) NOT NULL COMMENT '策略名称',
  `type` VARCHAR(50) COMMENT '策略类型: MA-均线 MACD KDJ',
  `params` JSON COMMENT '策略参数(JSON格式)',
  `status` TINYINT DEFAULT 0 COMMENT '状态: 0-停用 1-启用',
  `description` TEXT COMMENT '策略描述',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_user_id (user_id),
  INDEX idx_status (status),
  FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='策略配置表';

-- 订单表
CREATE TABLE IF NOT EXISTS `orders` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `order_no` VARCHAR(50) NOT NULL UNIQUE COMMENT '订单编号',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `symbol` VARCHAR(20) NOT NULL COMMENT '股票代码',
  `direction` TINYINT NOT NULL COMMENT '方向: 0-买入 1-卖出',
  `price` DECIMAL(10, 2) NOT NULL COMMENT '委托价格',
  `volume` INT NOT NULL COMMENT '委托数量',
  `amount` DECIMAL(20, 2) NOT NULL COMMENT '委托金额',
  `filled_volume` INT DEFAULT 0 COMMENT '成交数量',
  `filled_amount` DECIMAL(20, 2) DEFAULT 0 COMMENT '成交金额',
  `status` TINYINT DEFAULT 0 COMMENT '状态: 0-待成交 1-部分成交 2-全部成交 3-已撤销',
  `strategy_id` BIGINT COMMENT '策略ID',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_user_id (user_id),
  INDEX idx_symbol (symbol),
  INDEX idx_status (status),
  INDEX idx_created_at (created_at),
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (strategy_id) REFERENCES strategies(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 持仓表
CREATE TABLE IF NOT EXISTS `positions` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `symbol` VARCHAR(20) NOT NULL COMMENT '股票代码',
  `volume` INT NOT NULL DEFAULT 0 COMMENT '持仓数量',
  `available_volume` INT NOT NULL DEFAULT 0 COMMENT '可用数量',
  `avg_price` DECIMAL(10, 2) COMMENT '持仓均价',
  `cost` DECIMAL(20, 2) COMMENT '持仓成本',
  `market_value` DECIMAL(20, 2) COMMENT '市值',
  `profit` DECIMAL(20, 2) COMMENT '浮动盈亏',
  `profit_rate` DECIMAL(10, 4) COMMENT '盈亏比例%',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_symbol (user_id, symbol),
  INDEX idx_user_id (user_id),
  FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='持仓表';

-- 交易记录表
CREATE TABLE IF NOT EXISTS `trades` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `trade_no` VARCHAR(50) NOT NULL UNIQUE COMMENT '成交编号',
  `order_no` VARCHAR(50) NOT NULL COMMENT '订单编号',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `symbol` VARCHAR(20) NOT NULL COMMENT '股票代码',
  `direction` TINYINT NOT NULL COMMENT '方向: 0-买入 1-卖出',
  `price` DECIMAL(10, 2) NOT NULL COMMENT '成交价格',
  `volume` INT NOT NULL COMMENT '成交数量',
  `amount` DECIMAL(20, 2) NOT NULL COMMENT '成交金额',
  `commission` DECIMAL(10, 2) DEFAULT 0 COMMENT '手续费',
  `trade_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '成交时间',
  INDEX idx_user_id (user_id),
  INDEX idx_order_no (order_no),
  INDEX idx_symbol (symbol),
  INDEX idx_trade_time (trade_time),
  FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交易记录表';

-- 插入测试用户
INSERT INTO `users` (`username`, `password`, `email`, `status`) VALUES
('test_user', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'test@example.com', 1);

-- 插入测试股票数据
INSERT INTO `stocks` (`symbol`, `name`, `exchange`, `list_date`, `status`) VALUES
('000001', '平安银行', 'SZ', '1991-04-03', 1),
('600000', '浦发银行', 'SH', '1999-11-10', 1),
('600519', '贵州茅台', 'SH', '2001-08-27', 1);

-- 创建测试账户
INSERT INTO `user_accounts` (`user_id`, `balance`, `total_assets`) VALUES
(1, 100000.00, 100000.00);

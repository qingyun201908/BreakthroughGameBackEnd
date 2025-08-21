-- 使用 gen_random_uuid() 需要 pgcrypto 扩展（一次性执行即可）
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 用户表（基础账号信息）
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), -- 主键：UUID，默认使用 pgcrypto 生成
    username VARCHAR(50) NOT NULL,                 -- 用户名（业务上要求唯一）
    email VARCHAR(120) NOT NULL,                   -- 邮箱（业务上要求唯一）
    password_hash VARCHAR(100) NOT NULL,           -- 密码哈希（禁止存明文）
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()  -- 创建时间（带时区）
);

-- 唯一索引：用户名
CREATE UNIQUE INDEX IF NOT EXISTS uk_users_username ON users(username);
-- 唯一索引：邮箱
CREATE UNIQUE INDEX IF NOT EXISTS uk_users_email ON users(email);

-- ========= 元数据注释（\d+ 查看更直观） =========
COMMENT ON TABLE users IS '用户表：存储登录账号的基础信息';
COMMENT ON COLUMN users.id IS '主键：UUID；默认 gen_random_uuid() 生成';
COMMENT ON COLUMN users.username IS '用户名；长度<=50；由唯一索引 uk_users_username 保证唯一';
COMMENT ON COLUMN users.email IS '邮箱；长度<=120；由唯一索引 uk_users_email 保证唯一';
COMMENT ON COLUMN users.password_hash IS '密码哈希（如 bcrypt/argon2）；严禁存储明文密码';
COMMENT ON COLUMN users.created_at IS '创建时间（带时区）；默认 now()';

-- ========= 可选建议 =========
-- 1) 若需要“忽略大小写的唯一性”，可将 email/username 改为 CITEXT：
--    CREATE EXTENSION IF NOT EXISTS citext;
--    ALTER TABLE users ALTER COLUMN email TYPE CITEXT;
--    ALTER

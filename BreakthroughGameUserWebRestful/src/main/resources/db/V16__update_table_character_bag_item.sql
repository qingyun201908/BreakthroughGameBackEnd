-- 文件：db/migration/V20250829__rename_bagitem_desc_to_description.sql
-- 中文备注：避免关键字冲突，把 "desc" 改成 description
ALTER TABLE character_bag_item RENAME COLUMN "desc" TO description;

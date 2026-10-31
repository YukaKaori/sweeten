#!/bin/bash
# 由 MySQL 镜像在首次初始化数据目录时执行；init_all.sql 里的 source 路径相对于 /
(cd / && mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" -e "source sql/init_all.sql")

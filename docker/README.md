# Docker 开发环境（M0）

## 启动（任选一种）

```bash
# 方式一：Windows 终端直接调 WSL 里的 Docker（推荐）
wsl docker compose -f "/mnt/e/作业/项目经验/购物系统/docker/docker-compose.yml" up -d

# 方式二：开始菜单打开 Ubuntu 终端
cd /mnt/e/作业/项目经验/购物系统/docker
docker compose up -d
```

## 首次拉取镜像慢 / 失败

配置国内镜像加速（WSL 内执行）：

```bash
sudo tee /etc/docker/daemon.json <<'EOF'
{ "registry-mirrors": ["https://docker.m.daocloud.io"] }
EOF
sudo service docker restart
```

## 端口与账号速查

| 服务 | 端口 | 账号 / 说明 |
|---|---|---|
| MySQL 8.0 | 3306 | root / meal_root_2026，默认库 `meal_order`，utf8mb4 |
| Redis 7.2 | 6379 | 无密码（本地开发） |
| RabbitMQ | 5672 / 15672 | guest / guest，控制台 http://localhost:15672；定制镜像 `meal-rabbitmq:3.13-delayed`（带延迟交换机插件，见 `rabbitmq/Dockerfile`） |
| RustFS（S3 兼容对象存储） | 9000 | mealadmin / mealadmin123；控制台暂不可用（上游 bug），建桶走后端代码或 mc 客户端 |

## 与 Dify 共存

先停掉 Dify 容器再启动本套，避免 6379 等端口冲突和内存占用：

```bash
wsl docker ps                      # 看哪些在跑
wsl docker stop $(wsl docker ps -q)
```

## 常用命令

```bash
docker compose ps                  # 状态
docker compose logs -f mysql       # 看日志
docker compose down                # 停止（数据卷保留）
docker compose down -v             # 停止并清空数据（慎用）
```

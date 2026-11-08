# Git 开发规范

> 目标：单人开发也保持干净的提交历史，杜绝 Git 污染。
> 本文件一经合并不得随意修改，改动需走 `docs/<主题>` 分支并说明理由。

## 1. 分支模型

| 分支类型 | 命名规则 | 用途 | 生命周期 |
|---|---|---|---|
| `main` | — | 唯一长期分支，任何时刻可运行、可演示 | 永久 |
| 功能分支 | `feature/<里程碑>-<模块短名>` | **一个新功能/新模块一个分支** | 合并后删除 |
| 修复分支 | `fix/<短描述>` | bug 修复 | 合并后删除 |
| 文档分支 | `docs/<主题>` | 文档较大改动 | 合并后删除 |
| 杂务分支 | `chore/<主题>` | 依赖升级、构建配置 | 合并后删除 |

**铁律**
1. 每开发一个新功能或新模块，**必须**从最新 `main` 拉新分支，禁止在 `main` 上直接提交功能代码
2. `main` 上只允许两类提交：里程碑合并（`merge:`）和文档进度更新（`docs(progress):`）
3. 一个分支只干一件事；分支存活不超过一个里程碑周期，完成即合并、即删

## 2. Commit 信息（Conventional Commits）

格式：`<type>(<scope>): <中文一句话描述>`

| type | 含义 |
|---|---|
| feat | 新功能 |
| fix | 修 bug |
| docs | 仅文档 |
| refactor | 重构（不改行为） |
| test | 测试 |
| build | 构建 / 依赖 |
| chore | 杂务（.gitignore、脚本等） |

示例：
```
feat(order): 订单状态机与 CAS 流转
fix(stock): 修复并发扣库存的条件判断
docs(progress): M0 完成
```

## 3. 合并与 Tag

```bash
git switch main
git merge --no-ff feature/m0-env-scaffold -m "merge: M0 环境与脚手架完成"
git branch -d feature/m0-env-scaffold
git tag -a v0.1-m0 -m "M0 完成"
```

- 一律 `--no-ff` 合并，保留功能边界
- 禁止对已推送分支 rebase；禁止 force push `main`
- 每个里程碑合并后打 `v0.<n>-m<n>` 标签

## 4. 防 Git 污染红线

1. 不提交构建产物：`target/`、`dist/`、`node_modules/`（.gitignore 已覆盖，勿删）
2. 不提交 IDE 配置：`.idea/`、`*.iml`、`.vscode/`
3. 不提交本地凭证与真实密钥：`.env`、`application-local.yml` 只提交 `.env.example`
4. 不提交日志与临时文件：`logs/`、`*.log`、`tmp/`
5. 单文件 > 1MB 不入库；大图片素材走 MinIO，小程序本地资源适度
6. 提交前必看 `git status`；用 `git add <明确路径>`，不用 `git add .` 盲加
7. commit 信息禁用 "update"、"修改" 等无信息量描述

## 5. 日常速查

```bash
# 开始新功能
git switch main && git switch -c feature/m2-cart-order

# 干活中随时检查
git status
git log --oneline --graph -15

# 功能完成合并
git switch main
git merge --no-ff feature/m2-cart-order -m "merge: M2 购物车与下单"
git branch -d feature/m2-cart-order

# 进度书更新（main 上直接提交）
git add docs/任务进度书.md
git commit -m "docs(progress): 更新进度"
```

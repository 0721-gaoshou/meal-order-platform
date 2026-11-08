# 接口约定（api-convention）

> `R.java` 与 `admin-web/src/api/request.ts` 的注释都指向本文件。

## 统一响应体

```json
{ "code": "00000", "message": "成功", "data": { } }
```

- HTTP 状态码恒为 200（含鉴权失败），前端只认 `code`——与 axios 拦截器约定一致
- `data` 为业务载荷，可为 null；雪花 ID 一律序列化为**字符串**（JS Number 53 位精度限制）

## 错误码分段（ResultCode）

| 段 | 含义 | 当前码 |
|---|---|---|
| 00000 | 成功 | SUCCESS |
| Axxxx | 参数错误 | A0001 参数 / A0002 缺失 |
| Bxxxx | 认证权限 | B0001 未登录过期 / B0002 无权限 / B0003 不存在 |
| Cxxxx | 业务失败 | C0001 通用（如"用户名或密码错误"） |
| Dxxxx | 系统错误 | D0001 系统 / D0002 第三方 |

## 认证（M1）

- 请求头 `Authorization: Bearer <accessToken>`；双 token：access 2h / refresh 7d，refresh 时双 token 轮换
- 登录失败用 C0001（不是 B0001——B0001 会触发前端清凭证跳登录页）
- 白名单：`/auth/login` `/auth/refresh` `/auth/logout` `/health` `/doc.html` `/v3/api-docs/**` 等
- RBAC：Controller 方法/类上 `@RequireRole("ADMIN")`，未标注 = 登录即可

## 幂等（对应计划书 §4.11）

| 接口 | 幂等手段 |
|---|---|
| 下单(M2) | `orders.uk_request_id` 唯一索引 |
| 支付回调(M4) | `pay_log.uk_out_trade_trade` + 状态 CAS |
| 退款回调(M4) | `refund_order.uk_refund_no` + 状态 CAS |
| 领券(M3) | `user_coupon.uk_user_coupon` + 余量条件扣减 |

## 前端调用范式

```ts
// api 文件统一：<never, ApiResult<T>> 泛型技巧；成功时 result.data 即载荷
export function listCategories() {
  return request.get<never, ApiResult<Category[]>>('/admin/categories')
}
```

- 拦截器：成功返回业务体；`B0001` 触发单飞刷新（并发只发一次 /auth/refresh）→ 重放原请求；刷新失败清凭证跳 /login
- 上传等长请求单独传 `timeout`

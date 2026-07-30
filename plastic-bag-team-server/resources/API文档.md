# 塑料袋子Team · API 接口文档（AlienV4 验证系统）

> 站点名：**塑料袋子Team**
> API 域名：**yh-team.org**
> 后端服务端口：**14639**
> 客户端类型：`alienv4`
> 文档语言：简体中文

---

## 一、概览

本系统为 AlienV4（魔改客户端）提供账号授权与机器码绑定验证。包含：

- **游戏端验证接口**：Fabric Mod 在游戏窗口出现前调用，校验用户名/密码、购买状态、机器码绑定。
- **管理后台接口**：管理员登录后增删账号、授权时长、解绑机器码。
- **管理后台网站**：浏览器访问 `http://<host>:14639/` 即可使用中文后台。
- **本接口文档**：访问 `http://<host>:14639/api-docs` 或阅读本文档。

### 基础地址

| 环境 | Base URL |
|------|----------|
| 生产 | `https://yh-team.org`（反向代理到 14639） |
| 本地 | `http://localhost:14639` |

所有接口返回 `application/json`，统一结构：

```json
{
  "success": true,
  "code": "OK",
  "message": "说明文字",
  "...其他字段": {}
}
```

失败时的 `code` 列表：

| code | 含义 |
|------|------|
| `BAD_REQUEST` | 缺少必填字段 |
| `NOT_PURCHASED` | 账号不存在 / 未购买该客户端 |
| `INVALID_CREDENTIALS` | 用户名或密码错误 |
| `MACHINE_MISMATCH` | 机器码不匹配（已绑定其他机器） |
| `EXPIRED` | 授权已过期 |
| `DISABLED` | 账号被禁用 |
| `UNAUTHORIZED` | 管理员未登录 / token 失效 |
| `WRONG_PASSWORD` | 管理员密码错误 |
| `ERROR` | 其他错误 |

---

## 二、游戏端验证接口

### 1. 启动验证

`POST /api/v1/auth/verify`

由 Fabric Mod 在启动游戏前调用。请求体：

```json
{
  "username": "玩家用户名",
  "password": "账号密码",
  "machineCode": "本机机器码(SHA-256)",
  "clientType": "alienv4"
}
```

响应（验证通过）：

```json
{
  "success": true,
  "code": "OK",
  "message": "Verified.",
  "token": "会话令牌",
  "expireAt": 1735689600000,
  "bound": true,
  "needsBind": false
}
```

响应（首次绑定，机器码为空）：

```json
{
  "success": true,
  "code": "OK",
  "message": "Verified. Machine bound.",
  "token": "会话令牌",
  "expireAt": 1735689600000,
  "bound": true,
  "needsBind": true
}
```

响应（失败示例）：

```json
{ "success": false, "code": "MACHINE_MISMATCH", "message": "..." }
```

**逻辑：**

1. 账号不存在 → `NOT_PURCHASED`（即未购买 AlienV4）。
2. 密码错误 → `INVALID_CREDENTIALS`。
3. `clientType` 不匹配 → `NOT_PURCHASED`。
4. 账号被禁用 → `DISABLED`。
5. 授权过期（`expireAt < now`）→ `EXPIRED`。
6. 机器码为空 → 自动绑定当前机器码，`needsBind=true`。
7. 机器码一致 → 通过。
8. 机器码不一致 → `MACHINE_MISMATCH`（需管理员解绑）。

> 游戏端策略：验证不通过一律直接崩溃退出（`System.exit` / 抛异常）。

---

## 三、账号信息接口

### 2. 查询会话

`GET /api/v1/account/info?token=会话令牌`

响应：

```json
{
  "success": true,
  "username": "玩家",
  "clientType": "alienv4",
  "expireAt": 1735689600000,
  "bound": true,
  "active": true
}
```

---

## 四、管理员接口

所有非登录的管理接口都需要在请求头携带：

```
X-Admin-Token: <管理员会话令牌>
```

### 3. 管理员登录

`POST /api/v1/admin/login`

```json
{ "password": "admin123999" }
```

响应：

```json
{ "success": true, "adminToken": "管理员会话令牌", "message": "Admin login OK" }
```

> 默认管理员密码：`admin123999`。会话有效期 24 小时。

### 4. 获取账号列表

`GET /api/v1/admin/accounts`

响应：

```json
{
  "success": true,
  "count": 2,
  "accounts": [
    {
      "username": "alice",
      "clientType": "alienv4",
      "machineCode": "a1b2c3d4****",
      "createdAt": 1700000000000,
      "expireAt": 1735689600000,
      "active": true,
      "expired": false
    }
  ]
}
```

### 5. 添加 / 授权账号

`POST /api/v1/admin/account`

```json
{
  "username": "bob",
  "password": "123456",
  "clientType": "alienv4",
  "durationDays": 30
}
```

| 字段 | 说明 |
|------|------|
| username | 登录用户名（唯一） |
| password | 登录密码 |
| clientType | 授权客户端，默认 `alienv4` |
| durationDays | 授权天数（默认 30） |

### 6. 续期

`POST /api/v1/admin/account/renew`

```json
{ "username": "bob", "durationDays": 30 }
```

在现有到期时间基础上追加天数（已过期则从当前时间起算）。

### 7. 删除账号

`POST /api/v1/admin/account/delete`

```json
{ "username": "bob" }
```

### 8. 解绑机器码

`POST /api/v1/admin/account/resetmachine`

```json
{ "username": "bob" }
```

清空该账号绑定的机器码与会话，玩家下次启动将重新绑定当前机器。

---

## 五、使用流程示例（curl）

```bash
# 1. 管理员登录
ADMIN=$(curl -s -X POST http://localhost:14639/api/v1/admin/login \
  -H 'Content-Type: application/json' \
  -d '{"password":"admin123999"}' | grep -o '"adminToken":"[^"]*"' | cut -d'"' -f4)

# 2. 添加账号并授权 30 天
curl -s -X POST http://localhost:14639/api/v1/admin/account \
  -H "X-Admin-Token: $ADMIN" -H 'Content-Type: application/json' \
  -d '{"username":"test","password":"test123","clientType":"alienv4","durationDays":30}'

# 3. 游戏端验证（模拟）
curl -s -X POST http://localhost:14639/api/v1/auth/verify \
  -H 'Content-Type: application/json' \
  -d '{"username":"test","password":"test123","machineCode":"ABC123","clientType":"alienv4"}'
```

---

## 六、数据持久化

服务端使用单文件 JSON 数据库 `yhteam_data.json`（与 jar 同目录），包含：

- `accounts`：账号数组
- `adminPasswordHash`：管理员密码哈希

首次启动会自动创建一个测试账号 `admin / admin123`（授权 365 天）。

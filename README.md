# 塑料袋子Team · AlienV4 验证系统

> 站点名：**塑料袋子Team**  
> API 域名：**yh-team.org** · 后端端口：**14639**  
> 客户端：AlienV4（魔改版）· Minecraft 1.21.1 Fabric

本仓库包含两部分，最终产出 **两个 jar**：

| 模块     | 目录                         | 产物                             | 说明                           |
| ------ | -------------------------- | ------------------------------ | ---------------------------- |
| 验证 Mod | `nyx-alienv4-verify-mod/`  | `nyx-alienv4-verify-1.0.0.jar` | Fabric 客户端 mod，游戏窗口出现前弹出验证界面 |
| 验证后端   | `plastic-bag-team-server/` | `plastic-bag-team-server.jar`  | 可独立运行的 Java Web 后端（含管理员后台网站） |

---

## 一、工作流程

1. 玩家启动游戏，Fabric Mod 在**游戏窗口创建之前**弹出验证窗口（默认英文，可切换 简体中文 / 日本語 / Русский）。
2. 玩家输入用户名、密码，Mod 计算本机**机器码**并请求 `yh-team.org/api/v1/auth/verify`。
3. 后端检查：是否购买 AlienV4、密码是否正确、是否过期、机器码是否匹配。
   - 机器码为空 → 自动绑定当前机器；
   - 机器码不一致 → 拒绝（`MACHINE_MISMATCH`）；
   - 任一验证不通过 → **游戏直接崩溃退出**。
4. 管理员通过浏览器后台（`http://<host>:14639/`）管理账号：添加账号并授权时长、续期、解绑机器码、删除账号。

---

## 二、Web 后端（塑料袋子Team）

```bash
cd plastic-bag-team-server
bash build.sh
java -jar plastic-bag-team-server.jar
# 管理后台: http://localhost:14639/
# 接口文档: http://localhost:14639/api-docs
```

- 默认管理员密码：`admin123999`
- 首次启动自动创建测试账号 `admin / admin123`（授权 365 天）
- 数据保存在同目录 `yhteam_data.json`
- 生产环境用 Nginx 反代 `yh-team.org` → `127.0.0.1:14639`

接口详情见 [`plastic-bag-team-server/resources/API文档.md`](plastic-bag-team-server/resources/API文档.md)。

---

## 三、Fabric Mod（1.21.1）

```bash
cd nyx-alienv4-verify-mod
# 需要 JDK 21
./gradlew build        # 若系统非 JDK21，请用 JDK21 运行 gradle
# 产物: build/libs/nyx-alienv4-verify-1.0.0.jar
```

把 jar 放进 `.minecraft/versions/<版本>/mods/` 即可。  
Mod 配置文件 `nyx-auth.properties`（游戏目录下）可改 API 地址：

```properties
api.base.url=https://yh-team.org
client.type=alienv4
lang=en_us
```



---

## 四、目录结构

```
.
├── nyx-alienv4-verify-mod/      # Fabric 1.21.1 验证 Mod
│   ├── src/main/java/com/nyxclient/verify/
│   │   ├── auth/                # 验证窗口 / API客户端 / 机器码 / 多语言 / 配置
│   │   └── mixin/MainMixin.java # 在游戏窗口前注入验证
│   ├── build.gradle
│   └── fabric.mod.json
└── plastic-bag-team-server/     # 验证后端（Java 原生 HttpServer）
    ├── src/org/yhteam/server/   # 服务器 / 鉴权 / 管理 / 静态资源
    ├── resources/               # 后台网站 + API 文档（中文）
    ├── build.sh
    └── API文档.md
```

© 塑料袋子Team

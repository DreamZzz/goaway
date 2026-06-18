# 运行模式

## 后端

- `cd backend && ./start.sh local`
- `cd backend && ./start.sh prod`

`start.sh` 会加载 `.env.<env>` 或 `.env`，并设置 `SPRING_PROFILES_ACTIVE`。

## 前端

- `./start.sh local`
- `./start.sh remote`
- `./start.sh device local "设备名"`
- `./start.sh device remote "设备名"`
- `./start.sh metro local`

补充说明：

- `device remote` 是当前真机联调主入口，连接生产环境 `https://eat.868299.com`
- 第一次在真机上跑 `device remote` 前，先用 Xcode 对同一 `Team + Bundle Identifier` 成功点一次 `Run`
- 当前审核/联调默认 bundle 为 `com.868299.eat`

## 默认策略

- `local`: 本机后端 + 本地代理
- `remote`: 远端环境，通过 `APP_REMOTE_API_BASE_URL` 与 `APP_REMOTE_PROXY_TARGET` 控制
- `device`: 真机模式
- `metro`: 只启动 bundler 并写运行时配置

## 当前审核期默认体验

- 冷启动直接进入首页，不再先强制跳登录页
- 未登录用户可直接体验“来点灵感”，也可通过文字/语音入口试吃 1 道菜
- guest token 只允许调用 `POST /api/auth/guest`、`POST /api/meals/guest-inspirations` 与语音转写 `POST /api/voice/transcriptions`
- 游客文字/语音试吃先做 catalog 强命中；弱匹配或无匹配时仅生成 1 道 LLM 试吃菜谱。收藏、推荐反馈、购物清单、推荐历史、订阅与购买能力仍要求登录

## 运行时配置

- 生成文件：`frontend/src/app/config/runtime.generated.js`
- 兼容出口：`frontend/src/config/runtime.generated.js`
- 生成脚本：`frontend/scripts/write-runtime-config.js`

# Backend Validation Guide

## 本地最小验证

```bash
cd backend
./start.sh local
```

另开终端：

```bash
./ops/scripts/smoke-api.sh
```

更完整的发布验收门见 [release-acceptance.md](/Users/zhaoqiang/Documents/Project/what-to-eat/memory/release-acceptance.md)。

## provider 验证建议

- 存储：切 `APP_MEDIA_STORAGE_PROVIDER=oss` 后，验证上传返回绝对地址。
- 邮件：切 `APP_AUTH_PASSWORD_RESET_PROVIDER=mail` 后，验证找回密码验证码投递。
- 短信：切 `APP_AUTH_SMS_PROVIDER=aliyun` 后，验证短信登录验证码投递。
- 语音：切 `APP_SPEECH_PROVIDER=aliyun` 后，验证 `/api/voice/transcriptions`。
- 大模型：切 `APP_LLM_PROVIDER=openai-compatible` 后，验证菜谱生成。
- 图片：切 `APP_LLM_IMAGE_PROVIDER=web-search` 或 `openai-compatible` 后，验证异步补图。

## 回归接口

- `GET /api/auth/captcha`
- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/password/forgot`
- `POST /api/voice/transcriptions`
- `GET /api/meals/catalog`
- `POST /api/meals/recommendations`
- `POST /api/meals/recommendations/stream`
- `POST /api/meals/recipes/{id}/image`
- `POST /api/meals/recipes/{id}/steps/stream`
- `POST /api/meals/recipes/{id}/video`
- `PUT /api/meals/recipes/{id}/preference`
- `GET /api/meals/favorites`
- `GET /api/subscription/status`

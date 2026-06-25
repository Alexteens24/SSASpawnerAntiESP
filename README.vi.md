# SSASpawnerAntiESP

**English:** [README.md](README.md)

[![Build](https://github.com/Alexteens24/SSASpawnerAntiESP/actions/workflows/build.yml/badge.svg)](https://github.com/Alexteens24/SSASpawnerAntiESP/actions/workflows/build.yml)

Plugin addon cho [SmartSpawner](https://github.com/NighterDevelopment/SmartSpawner): **ẩn block spawner khỏi player không có tầm nhìn thẳng** (chống ESP / x-ray spawner).

Player không nhìn thấy spawner qua tường — client chỉ nhận block giả (đá, deepslate, …). Khi có line of sight, spawner hiện lại bình thường.

> **Lưu ý:** Plugin chỉ thay đổi **giao diện phía client** từng player. Dữ liệu spawner trên server, SmartSpawner và lệnh admin (ví dụ `/ss list`) **không bị ảnh hưởng**.

Hỗ trợ **Paper** và **Folia**.

![Showcase](showcase.gif)

---

## Yêu cầu

| Thành phần | Phiên bản |
|------------|-----------|
| Server | **Paper** `1.21.11` hoặc `26.1.2` |
| SmartSpawner | `1.6.2+` (Paper 1.21.11) · `1.6.7+` (Paper 26.1.2) |
| PacketEvents | `2.12.1+` |
| Java | `21` (Paper 1.21.11) · `25` (Paper 26.1.2) |

**Bắt buộc** cài **SmartSpawner** và **PacketEvents** trước. SSASpawnerAntiESP sẽ tự tắt nếu không tìm thấy SmartSpawner API.

---

## Tải plugin

Một **JAR universal** dùng được cho mọi phiên bản Paper được hỗ trợ (không có hậu tố classifier):

| Tên file |
|----------|
| `SSASpawnerAntiESP-<version>.jar` |

Tải tại:

- [Releases](https://github.com/Alexteens24/SSASpawnerAntiESP/releases) (bản phát hành chính thức)
- [GitHub Actions](https://github.com/Alexteens24/SSASpawnerAntiESP/actions) → chọn workflow run mới nhất → mục **Artifacts**

Build local: `./gradlew shadowJar` → `build/libs/SSASpawnerAntiESP-<version>.jar`

---

## Cài đặt

1. Cài **SmartSpawner** và **PacketEvents**, khởi động server một lần.
2. Copy file JAR vào thư mục `plugins/`.
3. Khởi động lại server.
4. (Tuỳ chọn) Chỉnh `plugins/SSASpawnerAntiESP/config.yml` rồi dùng `/ssaspawnerantiesp reload`.

---

## Cách hoạt động

Plugin dùng kiến trúc tương tự [RayTraceAntiXray](https://github.com/AdvancedAntiXray/RayTraceAntiXray), chỉnh cho spawner:

1. **Chunk obfuscation** — khi Paper gửi chunk tới client, spawner được thay bằng block giả (đá, deepslate, …) trong packet; block entity spawner bị gỡ khỏi packet.
2. **PacketEvents** — đồng bộ danh sách spawner cần ray trace sau khi chunk packet đã gửi.
3. **Ray trace async** — kiểm tra line of sight từ mắt player (và góc third-person nếu bật) tới từng spawner trong chunk đã load.
4. **Block update** — có LOS → gửi spawner thật; không có → giữ block giả trên client.
5. **Join / teleport** — spawner gần player được ẩn ngay (từ index SmartSpawner) để tránh nháy trước khi chunk obfuscate kịp.

SmartSpawner index dùng để đồng bộ place/break và ẩn nhanh lúc join; ẩn/hiện runtime chủ yếu qua spawner thật trong chunk.

Block giả theo dimension:

| Dimension | Block giả |
|-----------|-----------|
| Overworld (y ≥ 0) | Stone |
| Overworld (y < 0) | Deepslate |
| Nether | Netherrack |
| The End | End Stone |

---

## Cấu hình

File: `plugins/SSASpawnerAntiESP/config.yml`

### `settings` — toàn server

| Tuỳ chọn | Mặc định | Mô tả |
|----------|----------|--------|
| `update-ticks` | `1` | Số tick giữa mỗi lần gửi packet cập nhật block cho player. |
| `ms-per-ray-trace-tick` | `50` | Khoảng thời gian (ms) giữa mỗi vòng kiểm tra tầm nhìn. |
| `ray-trace-threads` | `1` | Số luồng xử lý kiểm tra tầm nhìn. Tăng nếu server nhiều player online. |

### `world-settings` — theo từng world

Cấu hình mặc định nằm trong `world-settings.default`. Ghi đè cho world cụ thể: `world-settings.<tên-world>.<tuỳ-chọn>`.

| Tuỳ chọn | Mặc định | Mô tả |
|----------|----------|--------|
| `enabled` | `true` | Bật/tắt plugin trong world đó. |
| `ray-trace-distance` | `64.0` | Khoảng cách tối đa (block) để kiểm tra spawner quanh player. |
| `ray-trace-third-person` | `false` | Ray trace thêm từ góc third-person (F5) — hữu ích khi camera lệch khỏi mắt player. |
| `rehide-blocks` | `true` | Bật tối ưu: spawner xa hơn `rehide-distance` sẽ được ẩn mà không cần ray trace. |
| `rehide-distance` | `60.0` | Ngưỡng khoảng cách (block) cho tối ưu `rehide-blocks`. |
| `section-leap` | `false` | Bỏ qua các vùng 16×16×16 block toàn air khi ray trace (nhanh hơn). Chỉ bật sau khi đã test ổn trên server. |
| `max-ray-trace-block-count-per-chunk` | `64` | Số spawner tối đa ray trace mỗi chunk (obfuscation Paper). |

Ví dụ tắt ở world `spawn`:

```yaml
world-settings:
  spawn:
    enabled: false
```

---

## Lệnh & quyền

| Lệnh | Quyền | Mô tả |
|------|-------|--------|
| `/ssaspawnerantiesp reload` | `ssaspawnerantiesp.command.reload` | Tải lại config và danh sách spawner |

---

## Giới hạn cần biết

- Chỉ ẩn **block spawner** trên client — không phải giải pháp chống hack tuyệt đối (mod outline, particle, v.v. vẫn có thể là vector khác).
- **Không chạy cùng [RayTraceAntiXray](https://github.com/AdvancedAntiXray/RayTraceAntiXray)** trên cùng world — cả hai đều chiếm `chunkPacketBlockController` của Paper.
- Khi `enabled: false` cho một world, plugin khôi phục anti-xray ore mặc định của Paper (nếu server bật engine-mode `HIDE`).
- Block giả có thể **không khớp** block xung quanh (ví dụ đá giữa đất/sand) — đây là trade-off của cách ẩn bằng packet.
---

## Giấy phép

[MIT](LICENSE)

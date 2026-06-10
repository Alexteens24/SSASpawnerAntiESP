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
| Java | `21` (Paper 1.21.11) · `25` (Paper 26.1.2) |

**Bắt buộc** cài SmartSpawner trước. SSASpawnerAntiESP sẽ tự tắt nếu không tìm thấy SmartSpawner API.

---

## Tải plugin

Chọn **đúng file JAR** theo phiên bản Paper server:

| Phiên bản Paper | Tên file |
|-----------------|----------|
| 1.21.11 | `SSASpawnerAntiESP-*-1.21.11.jar` |
| 26.1.2 | `SSASpawnerAntiESP-*-26.1.2.jar` |

Tải tại:

- [Releases](https://github.com/Alexteens24/SSASpawnerAntiESP/releases) (bản phát hành chính thức)
- [GitHub Actions](https://github.com/Alexteens24/SSASpawnerAntiESP/actions) → chọn workflow run mới nhất → mục **Artifacts**

---

## Cài đặt

1. Cài **SmartSpawner** và khởi động server một lần.
2. Copy file JAR đúng phiên bản vào thư mục `plugins/`.
3. Khởi động lại server.
4. (Tuỳ chọn) Chỉnh `plugins/SSASpawnerAntiESP/config.yml` rồi dùng `/ssaspawnerantiesp reload`.

---

## Cách hoạt động

1. Khi bật plugin, lấy danh sách tọa độ spawner từ SmartSpawner.
2. Định kỳ kiểm tra từ vị trí mắt player tới các spawner gần đó — có bị block che hay không.
3. **Không nhìn thấy** → gửi packet thay block spawner bằng block giả trên client player đó.
4. **Nhìn thấy** → gửi lại block spawner thật.
5. Khi player join hoặc teleport, spawner gần đó được ẩn ngay để tránh lộ nháy trước khi kiểm tra xong.

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
| `rehide-blocks` | `true` | Bật tối ưu: spawner xa hơn `rehide-distance` sẽ được ẩn mà không cần ray trace. |
| `rehide-distance` | `60.0` | Ngưỡng khoảng cách (block) cho tối ưu `rehide-blocks`. |
| `section-leap` | `false` | Bỏ qua các vùng 16×16×16 block toàn air khi ray trace (nhanh hơn). Chỉ bật sau khi đã test ổn trên server. |

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
- Block giả có thể **không khớp** block xung quanh (ví dụ đá giữa đất/sand) — đây là trade-off của cách ẩn bằng packet.
- Cần **đúng JAR** đúng phiên bản Paper; dùng sai bản có thể không load hoặc lỗi.

---

## Giấy phép

[MIT](LICENSE)

# 智慧城管 · 违章建筑检测系统

利用无人机航拍影像与历史底图做时序对比，AI 自动识别新增违建图斑，形成
**航拍采集 → 影像入库 → 时序对比 → 自动标注 → 图斑审核 → 工单处置 → 地图查询** 的执法闭环。

## 系统架构

```
┌─────────────┐   航拍   ┌──────────────┐
│  无人机航线采集 │ ──────▶ │  MinIO 影像库  │
└─────────────┘         └──────┬───────┘
                               │ 预签名URL
┌─────────────┐  REST   ┌──────▼───────┐  HTTP   ┌──────────────┐
│ AntD 管理端  │ ◀─────▶ │ Spring Boot  │ ──────▶ │ Python AI 服务 │
│ (React+Vite)│         │  业务后端     │ 变化检测 │ FastAPI+OpenCV│
└─────────────┘         └──────┬───────┘        └──────────────┘
┌─────────────┐  REST          │ JPA/Spatial
│ UniApp 移动端│ ◀───────────── │ 
└─────────────┘         ┌──────▼───────┐        ┌──────────────┐
                        │ PostGIS 空间库 │        │ GeoServer(可选)│
                        └──────────────┘        └──────────────┘
```

| 模块 | 目录 | 技术栈 |
|---|---|---|
| 业务后端 | `backend/` | Spring Boot 3 + JPA + Hibernate Spatial |
| 空间数据 | `deploy/` | PostGIS 16（GeoServer 可选 profile） |
| 对象存储 | `deploy/` | MinIO（imagery / evidence 两个 bucket） |
| 变化检测 | `ai-service/` | FastAPI + OpenCV（ORB 配准 + 差分 + 轮廓提取） |
| 管理端 | `admin-web/` | React 18 + Vite + Ant Design 5 + Leaflet |
| 移动端 | `mobile/` | UniApp（Vue3，现场核查拍照回传） |

## 快速启动

### 1. 基础设施（PostGIS + MinIO）

```bash
cd deploy
docker compose up -d          # 可选 GeoServer: docker compose --profile geoserver up -d
```

### 2. AI 变化检测服务（端口 8000）

```bash
cd ai-service
pip install -r requirements.txt
uvicorn main:app --port 8000
# 或 docker build -t ucm-ai . && docker run -p 8000:8000 ucm-ai
```

### 3. 业务后端（端口 8080）

```bash
cd backend
./mvnw spring-boot:run        # 或 mvn spring-boot:run（需 JDK17+）
# 首次启动自动建表并初始化账号：admin/admin123（管理员）、zhangsan/123456（队员）
```

### 4. 管理端（端口 5173）

```bash
cd admin-web
npm install
npm run dev                   # 已配置 /api 代理到 8080
```

### 5. 移动端

用 HBuilderX 打开 `mobile/` 目录，运行到浏览器/模拟器/真机。
真机调试时把 `mobile/utils/request.js` 中 `BASE_URL` 改为电脑局域网 IP。

## 主链路联调（无真实航拍数据时）

```bash
# 1. 生成两期演示影像（新影像中含两处"新增违建"）
cd ai-service/demo && python gen_demo_images.py

# 2. 管理端「影像管理」分别入库 demo_base.png / demo_new.png
#    bbox 填: 114.30, 30.55, 114.36, 30.60

# 3. 「对比检测」发起对比：底图=demo_base，新影像=demo_new
#    任务状态自动轮询，SUCCESS 后点「查看图斑」

# 4. 「图斑审核」地图上点击图斑 → 确认违建 → 自动生成核查工单

# 5. 「工单处置」派单给 zhangsan → 移动端登录 zhangsan 现场核查、拍照、认定

# 6. 「地图查询」查看全部图斑与影像覆盖范围
```

## 核心 API（前缀 /api，除登录外均需请求头 X-Token）

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | /auth/login | 登录，返回 token |
| GET/POST/PUT/DELETE | /routes | 航线管理（region 支持 GeoJSON Polygon 或 bbox） |
| GET/POST/PUT/DELETE | /tasks | 航拍任务 |
| GET | /imagery | 影像列表 |
| POST | /imagery/upload | 影像入库（multipart：文件+名称+bbox+类型+日期） |
| GET | /imagery/{id}/preview | 影像预签名预览 URL |
| GET/POST | /compare | 对比任务列表 / 发起对比（异步 AI 检测） |
| GET | /spots | 图斑列表（status / compareTaskId 过滤） |
| POST | /spots/{id}/review | 图斑审核：approved=true 确认并自动生成工单 |
| GET | /orders?mine=true | 工单列表（mine=我的） |
| GET | /orders/{id} | 工单详情（含流转日志） |
| POST | /orders/{id}/action | 工单动作：ASSIGN/CONFIRM/EXCLUDE/RECTIFY/ARCHIVE |
| POST | /files/upload | 现场照片上传（evidence bucket） |
| GET | /map/spots、/map/imagery | GeoJSON 地图数据 |

## AI 检测服务接口

```
POST /detect
{
  "base_url": "<历史底图预签名URL>",
  "new_url":  "<新影像预签名URL>",
  "bbox": [minx, miny, maxx, maxy],   // 新影像地理范围 EPSG:4326
  "min_area_m2": 20
}
→ { "features": [ { "geometry": <GeoJSON Polygon>, "area_m2": 85.3,
                    "confidence": 0.87, "change_type": "NEW_CONSTRUCTION" } ] }
```

检测流程：影像缩放 → ORB 特征匹配 + 单应矩阵配准（历史图对齐到新影像坐标系）
→ 高斯滤波 + 差分 → Otsu 阈值 + 形态学开闭运算 → 轮廓提取与简化
→ 按 bbox 仿射换算地理坐标，面积/置信度过滤后输出。

> 首版用传统 CV 保证开箱即用；接口已按「输入两期影像、输出 GeoJSON 图斑」抽象，
> 后续可无缝替换为深度学习变化检测模型（如 ChangeFormer/BiT）。

## 工单状态机

```
PENDING(待核查) --ASSIGN--> INSPECTING(核查中)
INSPECTING --CONFIRM--> CONFIRMED(已认定) --RECTIFY--> RECTIFYING(整改中) --ARCHIVE--> ARCHIVED(已归档)
INSPECTING --EXCLUDE--> EXCLUDED(已排除)
CONFIRMED --ARCHIVE--> ARCHIVED
```

每次流转写入 `work_order_log`（操作人/意见/现场照片），全程留痕。

## 数据库表（PostGIS）

`sys_user` 用户、`flight_route` 航线（region 为 Polygon）、`flight_task` 航拍任务、
`imagery` 影像（footprint 为 Polygon）、`compare_task` 对比任务、
`change_spot` 变化图斑（geom 为 Polygon，含面积/置信度/审核状态）、
`work_order` 工单、`work_order_log` 流转日志。空间字段均为 EPSG:4326。

## 后续迭代方向

- 接入深度学习变化检测模型与建筑物语义分割，降低误报
- GeoServer 发布 WMS 图层，管理端切换为 WMS 叠加（大数据量影像）
- 倾斜摄影三维模型（3D Tiles）与违建体量估算
- 航线规划算法（区域覆盖 + 重叠度）与无人机云平台对接自动回传
- 消息推送（短信/钉钉）与超期督办

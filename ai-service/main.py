"""
智慧城管 - 影像变化检测服务
输入两期影像 URL + 新影像地理范围(bbox)，输出疑似违建图斑 GeoJSON。

流程：下载 -> 缩放 -> ORB 特征配准(历史底图对齐到新影像) -> 差分 -> 阈值/形态学
-> 轮廓提取 -> 像素坐标仿射换算为地理坐标(EPSG:4326)。
"""
import io
import math
from typing import List

import cv2
import numpy as np
import requests
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

app = FastAPI(title="UCM Change Detection Service", version="1.0.0")

MAX_DIM = 2048          # 检测工作分辨率（最长边）
METERS_PER_DEG_LAT = 111320.0


class DetectRequest(BaseModel):
    base_url: str = Field(..., description="历史底图下载地址")
    new_url: str = Field(..., description="新影像下载地址")
    bbox: List[float] = Field(..., description="新影像地理范围 [minx, miny, maxx, maxy] (EPSG:4326)")
    min_area_m2: float = Field(20.0, description="最小图斑面积（平方米）")


@app.get("/health")
def health():
    return {"status": "ok"}


def download_image(url: str) -> np.ndarray:
    resp = requests.get(url, timeout=120)
    if resp.status_code != 200:
        raise HTTPException(status_code=400, detail=f"影像下载失败: HTTP {resp.status_code}")
    data = np.frombuffer(resp.content, dtype=np.uint8)
    img = cv2.imdecode(data, cv2.IMREAD_COLOR)
    if img is None:
        raise HTTPException(status_code=400, detail="影像解码失败，请确认是 JPG/PNG/TIF 格式")
    return img


def resize_keep_ratio(img: np.ndarray, max_dim: int = MAX_DIM):
    h, w = img.shape[:2]
    scale = min(1.0, max_dim / max(h, w))
    if scale < 1.0:
        img = cv2.resize(img, (int(w * scale), int(h * scale)), interpolation=cv2.INTER_AREA)
    return img


def align(base: np.ndarray, new: np.ndarray) -> np.ndarray:
    """将历史底图配准到新影像坐标系（ORB + 单应矩阵）。失败则退化为直接缩放对齐。"""
    gray_base = cv2.cvtColor(base, cv2.COLOR_BGR2GRAY)
    gray_new = cv2.cvtColor(new, cv2.COLOR_BGR2GRAY)
    orb = cv2.ORB_create(5000)
    kp1, des1 = orb.detectAndCompute(gray_base, None)
    kp2, des2 = orb.detectAndCompute(gray_new, None)
    if des1 is None or des2 is None or len(kp1) < 8 or len(kp2) < 8:
        return cv2.resize(base, (new.shape[1], new.shape[0]))
    bf = cv2.BFMatcher(cv2.NORM_HAMMING)
    matches = bf.knnMatch(des1, des2, k=2)
    good = [m for m, n in matches if m.distance < 0.75 * n.distance]
    if len(good) < 8:
        return cv2.resize(base, (new.shape[1], new.shape[0]))
    src = np.float32([kp1[m.queryIdx].pt for m in good]).reshape(-1, 1, 2)
    dst = np.float32([kp2[m.trainIdx].pt for m in good]).reshape(-1, 1, 2)
    H, mask = cv2.findHomography(src, dst, cv2.RANSAC, 5.0)
    if H is None:
        return cv2.resize(base, (new.shape[1], new.shape[0]))
    return cv2.warpPerspective(base, H, (new.shape[1], new.shape[0]))


def detect_changes(base_aligned: np.ndarray, new: np.ndarray) -> tuple[np.ndarray, np.ndarray]:
    """差分 + 阈值 + 形态学，返回二值变化掩膜与差分强度图"""
    g1 = cv2.GaussianBlur(cv2.cvtColor(base_aligned, cv2.COLOR_BGR2GRAY), (5, 5), 0)
    g2 = cv2.GaussianBlur(cv2.cvtColor(new, cv2.COLOR_BGR2GRAY), (5, 5), 0)
    diff = cv2.absdiff(g1, g2)
    _, mask = cv2.threshold(diff, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)
    kernel = cv2.getStructuringElement(cv2.MORPH_RECT, (5, 5))
    mask = cv2.morphologyEx(mask, cv2.MORPH_OPEN, kernel, iterations=1)
    mask = cv2.morphologyEx(mask, cv2.MORPH_CLOSE, kernel, iterations=2)
    return mask, diff


def pixel_to_geo(px: float, py: float, img_w: int, img_h: int, bbox: List[float]):
    minx, miny, maxx, maxy = bbox
    lng = minx + px / img_w * (maxx - minx)
    lat = maxy - py / img_h * (maxy - miny)
    return lng, lat


@app.post("/detect")
def detect(req: DetectRequest):
    if len(req.bbox) != 4 or req.bbox[0] >= req.bbox[2] or req.bbox[1] >= req.bbox[3]:
        raise HTTPException(status_code=400, detail="bbox 不合法")

    base = resize_keep_ratio(download_image(req.base_url))
    new = resize_keep_ratio(download_image(req.new_url))

    base_aligned = align(base, new)
    mask, diff = detect_changes(base_aligned, new)

    img_h, img_w = mask.shape[:2]
    minx, miny, maxx, maxy = req.bbox
    center_lat = (miny + maxy) / 2
    # 每像素对应的实地米数（用于面积过滤与面积计算）
    m_per_px_x = (maxx - minx) * METERS_PER_DEG_LAT * math.cos(math.radians(center_lat)) / img_w
    m_per_px_y = (maxy - miny) * METERS_PER_DEG_LAT / img_h
    m2_per_px = abs(m_per_px_x * m_per_px_y)
    min_area_px = req.min_area_m2 / m2_per_px if m2_per_px > 0 else 0

    contours, _ = cv2.findContours(mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    features = []
    for cnt in contours:
        area_px = cv2.contourArea(cnt)
        if area_px < min_area_px:
            continue
        epsilon = 0.01 * cv2.arcLength(cnt, True)
        approx = cv2.approxPolyDP(cnt, epsilon, True)
        if len(approx) < 3:
            continue
        # 置信度：图斑内平均差分强度归一化，下限 0.5
        cmask = np.zeros_like(mask)
        cv2.drawContours(cmask, [cnt], -1, 255, -1)
        mean_diff = cv2.mean(diff, mask=cmask)[0] / 255.0
        confidence = round(min(0.99, max(0.5, 0.5 + mean_diff)), 2)

        ring = [list(pixel_to_geo(float(p[0][0]), float(p[0][1]), img_w, img_h, req.bbox)) for p in approx]
        ring.append(ring[0])  # 闭合
        features.append({
            "geometry": {"type": "Polygon", "coordinates": [ring]},
            "area_m2": round(area_px * m2_per_px, 1),
            "confidence": confidence,
            "change_type": "NEW_CONSTRUCTION",
        })

    features.sort(key=lambda f: f["area_m2"], reverse=True)
    return {"features": features, "image_size": [img_w, img_h], "m2_per_pixel": round(m2_per_px, 4)}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)

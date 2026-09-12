"""
生成两期演示影像：demo_base.png（历史底图）与 demo_new.png（新增两处"违建"）。
用于无真实航拍数据时联调「影像入库 -> 对比检测 -> 图斑标注」链路。

用法: python gen_demo_images.py [输出目录]
"""
import sys
from pathlib import Path

import cv2
import numpy as np

OUT = Path(sys.argv[1] if len(sys.argv) > 1 else ".")
SIZE = 1024
rng = np.random.default_rng(42)


def base_scene() -> np.ndarray:
    # 底噪 + 网格道路 + 若干既有建筑
    img = rng.integers(90, 130, (SIZE, SIZE, 3), dtype=np.uint8)
    for i in range(0, SIZE, 128):  # 道路
        cv2.line(img, (i, 0), (i, SIZE), (70, 70, 70), 6)
        cv2.line(img, (0, i), (SIZE, i), (70, 70, 70), 6)
    for _ in range(40):  # 既有建筑（两期都有）
        x, y = rng.integers(30, SIZE - 90, 2)
        w, h = rng.integers(30, 70, 2)
        c = int(rng.integers(140, 200))
        cv2.rectangle(img, (x, y), (x + w, y + h), (c, c, c), -1)
    return img


def main():
    base = base_scene()
    new = base.copy()
    # 新增"违建"1：大棚/构筑物
    cv2.rectangle(new, (200, 300), (290, 360), (220, 180, 120), -1)
    # 新增"违建"2：扩建块
    cv2.rectangle(new, (640, 620), (760, 700), (200, 200, 230), -1)
    # 轻微全局光照差异，模拟真实航拍
    new = cv2.convertScaleAbs(new, alpha=1.02, beta=3)

    OUT.mkdir(parents=True, exist_ok=True)
    cv2.imwrite(str(OUT / "demo_base.png"), base)
    cv2.imwrite(str(OUT / "demo_new.png"), new)
    print(f"已生成: {OUT/'demo_base.png'}, {OUT/'demo_new.png'}")
    print("建议入库 bbox: minx=114.30, miny=30.55, maxx=114.36, maxy=30.60")


if __name__ == "__main__":
    main()

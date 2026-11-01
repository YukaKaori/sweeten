# 设计源文件（不参与构建）

这里存放**不会被 Vite 打包**的原始素材。界面实际使用的图片在 `src/assets/`（打包）与 `public/`（原样发布）；
品牌标志与网站图标由 `scripts/brand/build_brand_assets.py` 生成到 `public/brand/`，见 [BRAND.md](../BRAND.md)。

原目录名为 `dessertshop-image/`（DessertShop 时期），2026-10-31 按用途整理到这里，文件内容未改动。

| 路径 | 说明 |
|---|---|
| `product-photos/strawberrycake.png` | 当前全站统一商品图的**原图**（2.1 MB）。线上使用的是由后端 `POST /upload` 上传到阿里云 OSS 的同一张图（SHA-256 一致，见 `backend/sql/migrations/2026-10-13_2_product_image_cache_headers.sql`）；前端离线兜底用的压缩版在 `src/assets/images/strawberrycake.jpg`。重新上传或换 CDN 时以此为准。 |
| `unreferenced/background_clothing_conf/icon/` | 22 张与商品无关的图片（婚纱人像、樱花等，部分扩展名为 `.jpg-lx2`），来自最初一次提交，代码中**没有任何引用**，来源不明。原样保留待确认：若没有授权依据，建议从公开仓库中移除。 |

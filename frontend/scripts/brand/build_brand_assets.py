"""
喜甜 SWEETEN 品牌资源生成脚本

一次生成全部品牌资源（结果已提交到仓库；只有改动标志或品牌文案时才需要重新运行）：

  public/brand/                      标志矢量母版（SVG）与导出图（PNG），见 BRAND.md「资源清单」
  public/favicon.svg|.ico|.png       网站图标（简化图形，16/32/48）
  public/apple-touch-icon.png        iOS 主屏图标 180×180（满版，系统自动切圆角）
  public/site.webmanifest            Android / 主屏图标清单（名称、主题色、192/512/maskable 图标）
  src/assets/brand/logo.js           BrandLogo.vue 使用的路径数据（与导出图同源）
  src/assets/fonts/sweeten-display.woff2  展示字体：思源宋体 600，子集 = src/config/brand.js 中出现的汉字
  src/assets/fonts/sweeten-latin.woff2    英文字体：思源黑体 500 拉丁字母子集（眉题 / 标语）

标志的来源与重建方式（详见 BRAND.md）：
  - 参考稿是 4 张 AI 生成的品牌展板（不入库）。展板里「甜」字缺了「舌」下面的「口」，不能直接使用；
    这里用思源宋体的正确字形重建「喜甜」，再按展板做两处定制：「甜」的「甘」底部改为圆润的杯形，
    杯中放一颗草莓粉爱心；「喜」下方「口」的底横改成微微上扬的笑弧。
  - SWEETEN 字标按参考稿实测数据重建：E/T/N/W 为几何构造（主笔 15、细笔 5.75、对角 0.39 斜率…），
    S 由参考稿高清图描摹（potrace），单位为参考图像素，下方常量保留了原始测量值。
  - 图形标志（两片叶子托起爱心）与分隔线上的三瓣芽，是对参考稿的贝塞尔拟合（IoU 0.98 / 0.96）。
  - 标语、品类行使用 Montserrat（展板字体规范）。

依赖：
  pip install fonttools brotli skia-pathops skia-python pillow numpy
  字体（均为 SIL Open Font License 1.1，可商用、可转曲用于标志）：
    Noto Serif SC 可变字体  https://fonts.google.com/noto/specimen/Noto+Serif+SC
    Noto Sans SC 可变字体   https://fonts.google.com/noto/specimen/Noto+Sans+SC
    Montserrat 可变字体     https://fonts.google.com/specimen/Montserrat （Montserrat[wght].ttf）
  默认读取 Windows 字体目录；可用环境变量 SWEETEN_SERIF_VF / SWEETEN_SANS_VF / SWEETEN_MONTSERRAT_VF 指定路径。

用法（在前端目录 frontend/ 下）：
  python scripts/brand/build_brand_assets.py               # 全部重新生成
  python scripts/brand/build_brand_assets.py --skip-fonts  # 只生成标志与图标（字体子集不变）
  python scripts/brand/build_brand_assets.py --skip-logo   # 只重新生成字体子集（改了 config/brand.js 的展示文案后）
"""
import argparse
import json
import math
import os
import re
from pathlib import Path

import numpy as np
import pathops
import skia
from fontTools import subset
from fontTools.pens.basePen import BasePen
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
from fontTools.ttLib import TTFont
from fontTools.varLib import instancer
from PIL import Image

ROOT = Path(os.environ.get('SWEETEN_FRONTEND_ROOT') or Path(__file__).resolve().parents[2])
SERIF_VF = os.environ.get('SWEETEN_SERIF_VF', 'C:/Windows/Fonts/NotoSerifSC-VF.ttf')
SANS_VF = os.environ.get('SWEETEN_SANS_VF', 'C:/Windows/Fonts/NotoSansSC-VF.ttf')
MONTSERRAT_VF = os.environ.get('SWEETEN_MONTSERRAT_VF', 'C:/Windows/Fonts/Montserrat[wght].ttf')

# ------------------------------------------------------------------ 色彩（与 src/assets/base.css 一致）
CREAM = '#F7F3EC'       # 暖奶油白：页面底色
PAPER = '#FFFEFC'       # 柔白
BLUSH = '#E6A4A1'       # 草莓腮红：爱心
MILK_TEA = '#D9C5AD'    # 奶茶
COCOA = '#493B35'       # 可可棕：字标与叶子
GRAY = '#8E8982'        # 柔灰
BLUSH_TINT = '#F2DBD6'  # 应用图标底色 = 30% 草莓腮红叠在暖奶油白上（展板实测 #F3DDD7）

TONES = {
    'default': {'ink': COCOA, 'accent': BLUSH},   # 浅色底
    'inverse': {'ink': CREAM, 'accent': BLUSH},   # 深色底（可可棕 / 照片）
}

# ------------------------------------------------------------------ 字标参数
CN_WGHT = 460           # 思源宋体字重：参考稿笔画介于 400 与 500 之间，460 兼顾小尺寸可读性
CN_TRACKING = 60        # 「喜」「甜」之间额外字距（字体单位，1000/em）
CN_SMILE_SAG = 18       # 「喜」底横的笑弧下沉量
TIAN_BOWL = dict(cut_top=-200, curve_start=-96, bottom=82, bottom_ratio=0.68)   # 「甘」杯形（y 向下，基线 0）
TIAN_HEART = dict(rotate=14, tip_gap=66, clearance=15)                         # 杯中爱心：倾角 / 离杯底 / 与笔画间距

TAGLINE = dict(text='SWEETEN YOUR DAY.', wght=400, tracking=0.20)
CATEGORY = dict(text='DESSERT · BAKERY · DRINKS', wght=450, tracking=0.33)

# SWEETEN：参考稿（1254px 主标志图）实测，单位为参考图像素；大写高度 83.75px
EN_CAP_TOP, EN_BASELINE = 10.25, 94.0
EN_STEM, EN_ARM, EN_ROUND = 15.0, 5.75, 1.6
EN_LETTERS = {'S': 10.06, 'W': 105.46, 'E1': 257.65, 'E2': 350.4, 'T': 436.6, 'E3': 543.75, 'N': 636.1}
EN_S = (  # S 描摹轮廓（potrace，平滑后 24 段）
    'M40 95.17C39.83 95.14 38.48 94.95 37 94.74C29.77 93.73 24.84 92.07 19.94 88.99C15.80 86.39 11.30 82.46 10.43 80.68'
    'C9.99 79.79 9.99 74.10 10.43 73.43C11.25 72.18 12.17 72.52 13.60 74.60C19.93 83.85 29.74 89.02 41.77 89.45'
    'C52.18 89.82 59.73 85.72 61.87 78.51C63.53 72.92 61.61 68.06 56.38 64.61C53.62 62.79 45.26 59.22 35.38 55.65'
    'C28.20 53.05 23.66 50.85 19.79 48.10C11.76 42.38 9.20 31.36 13.81 22.33C17.41 15.27 26.46 10.38 38.25 9.10'
    'C48.17 8.03 58.89 10.38 66.83 15.37C71.02 18 71.74 19.20 70.55 21.63C69.32 24.18 68.97 24.20 65.90 21.83'
    'C59.17 16.61 52.40 14.46 43.38 14.66C37.40 14.80 34.02 15.70 30.37 18.14C23.56 22.69 23.10 31.19 29.40 35.94'
    'C32.27 38.10 34.20 39 42.94 42.30C62.69 49.74 65.73 51.28 70.46 56.29C79.41 65.77 78.04 80.66 67.48 88.56'
    'C63.63 91.45 57.75 93.82 52.81 94.49C48.21 95.12 41.77 95.45 40 95.17Z'
)

# 图形标志：对第 4 张展板的拟合（单位为展板像素）。左叶 = 底尖 B、顶尖 T 与两条三次曲线的控制点偏移，右叶镜像
LEAF = dict(B=(90.28, 134.80), T=(19.83, 41.60), u1=(46.45, 8.07), u2=(0.97, -37.76), v1=(-37.07, -5.92), v2=(-0.13, 57.98))
SYMBOL_AXIS = 92.77
HEART = dict(yn=31.82, yt=23.99, a=12.15, w=24.20, yw=33.68, yb=87.03, c1=(0.34, 5.88), c2=4.69, c3=4.71, c4=9.41,
             c5=15.74, tip=(14.0, 70.0))
# 三瓣芽（主标志分隔线中央）：对 1254px 主标志图的拟合
SPROUT_CENTER = dict(cx=37.14, yb=43.88, yt=5.21, k1=9.34, m1=7.72, k2=6.53, m2=10.39)
SPROUT_SIDE = dict(B=(35.52, 49.93), T=(12.46, 20.31), u1=(14.05, 1.88), u2=(-1.11, -11.18), v1=(-12.28, -3.69), v2=(-0.04, 12.91))

KAPPA = 0.5522847498


# ================================================================== 路径工具（pathops，y 轴向下）
def new_path():
    return pathops.Path()


def poly(points):
    p = new_path()
    pen = p.getPen()
    pen.moveTo(points[0])
    for q in points[1:]:
        pen.lineTo(q)
    pen.closePath()
    return p


def rect(x0, y0, x1, y1):
    return poly([(x0, y0), (x1, y0), (x1, y1), (x0, y1)])


def union(*paths):
    out = new_path()
    for p in paths:
        out = pathops.op(out, p, pathops.PathOp.UNION)
    return out


def difference(a, b):
    return pathops.op(a, b, pathops.PathOp.DIFFERENCE)


def intersection(a, b):
    return pathops.op(a, b, pathops.PathOp.INTERSECTION)


def transformed(path, s=1.0, tx=0.0, ty=0.0, sy=None):
    out = new_path()
    path.draw(TransformPen(out.getPen(), (s, 0, 0, s if sy is None else sy, tx, ty)))
    return out


def svg_d(path, s=1.0, tx=0.0, ty=0.0, digits=2):
    pen = SVGPathPen(None, ntos=lambda v: _n(v, digits))
    path.draw(TransformPen(pen, (s, 0, 0, s, tx, ty)))
    return pen.getCommands()


def bounds(path):
    return path.bounds  # (x0, y0, x1, y1)


def is_empty(path):
    b = path.bounds
    return b is None or (b[2] - b[0]) * (b[3] - b[1]) < 1e-6


class _SkiaPen(BasePen):
    def __init__(self):
        super().__init__(None)
        self.path = skia.Path()

    def _moveTo(self, p): self.path.moveTo(*p)
    def _lineTo(self, p): self.path.lineTo(*p)
    def _qCurveToOne(self, p1, p2): self.path.quadTo(*p1, *p2)
    def _curveToOne(self, p1, p2, p3): self.path.cubicTo(*p1, *p2, *p3)
    def _closePath(self): self.path.close()


def to_skia(path):
    pen = _SkiaPen()
    path.draw(pen)
    return pen.path


def from_skia(sk_path):
    out = new_path()
    pen = out.getPen()
    it = skia.Path.Iter(sk_path, False)
    while True:
        verb, pts = it.next()
        if verb == skia.Path.kDone_Verb:
            break
        if verb == skia.Path.kMove_Verb:
            pen.moveTo(tuple(pts[0]))
        elif verb == skia.Path.kLine_Verb:
            pen.lineTo(tuple(pts[1]))
        elif verb == skia.Path.kQuad_Verb:
            pen.qCurveTo(tuple(pts[1]), tuple(pts[2]))
        elif verb == skia.Path.kConic_Verb:
            quads = skia.Path.ConvertConicToQuads(pts[0], pts[1], pts[2], it.conicWeight(), 2)
            for i in range(1, len(quads) - 1, 2):
                pen.qCurveTo(tuple(quads[i]), tuple(quads[i + 1]))
        elif verb == skia.Path.kCubic_Verb:
            pen.curveTo(tuple(pts[1]), tuple(pts[2]), tuple(pts[3]))
        elif verb == skia.Path.kClose_Verb:
            pen.closePath()
    return out


def dilate(path, r):
    paint = skia.Paint(AntiAlias=True, Style=skia.Paint.kStroke_Style, StrokeWidth=2 * r,
                       StrokeJoin=skia.Paint.kRound_Join)
    stroked = skia.Path()
    paint.getFillPath(to_skia(path), stroked)
    return union(from_skia(stroked), path)


# ================================================================== 字体
_fonts, _glyph_sets = {}, {}


def glyph_set(font_path, wght):
    key = (font_path, wght)
    if key not in _glyph_sets:
        if font_path not in _fonts:
            if not Path(font_path).exists():
                raise SystemExit(f'找不到字体 {font_path}（见脚本头部「依赖」）')
            _fonts[font_path] = TTFont(font_path, lazy=True)
        f = _fonts[font_path]
        _glyph_sets[key] = (f.getGlyphSet(location={'wght': wght}), f.getBestCmap(), f['head'].unitsPerEm)
    return _glyph_sets[key]


def glyph(font_path, wght, ch, x=0.0):
    """字形轮廓（y 向下，基线 0），返回 (path, advance)。"""
    gs, cmap, _ = glyph_set(font_path, wght)
    g = gs[cmap[ord(ch)]]
    p = new_path()
    g.draw(TransformPen(p.getPen(), (1, 0, 0, -1, x, 0)))
    return p, g.width


def text_line(font_path, wght, text, tracking_em):
    _, _, upem = glyph_set(font_path, wght)
    x, parts = 0.0, []
    for i, ch in enumerate(text):
        g, adv = glyph(font_path, wght, ch, x)
        parts.append(g)
        x += adv + (tracking_em * upem if i < len(text) - 1 else 0)
    return union(*parts)


def scan_runs(path, y):
    """路径在水平线 y 上的填充区间（整数精度，供定位笔画用）。"""
    w = 6000
    surf = skia.Surface.MakeRaster(skia.ImageInfo.MakeA8(w, 3))
    c = surf.getCanvas()
    c.clear(skia.ColorTRANSPARENT)
    c.translate(1000, -y + 1)
    c.drawPath(to_skia(path), skia.Paint(AntiAlias=False))
    row = surf.makeImageSnapshot().toarray(colorType=skia.kAlpha_8_ColorType).reshape(3, w)[1] > 127
    runs, start = [], None
    for x, v in enumerate(row):
        if v and start is None:
            start = x
        if not v and start is not None:
            runs.append((start - 1000, x - 1000))
            start = None
    return runs


# ================================================================== 图形：叶子、爱心、三瓣芽
def leaf(B, T, u1, u2, v1, v2, mirror=None):
    def X(x): return x if mirror is None else 2 * mirror - x
    def D(dx): return dx if mirror is None else -dx
    (bx, by), (tx, ty) = B, T
    p = new_path()
    pen = p.getPen()
    pen.moveTo((X(tx), ty))
    pen.curveTo((X(tx) + D(u1[0]), ty + u1[1]), (X(bx) + D(u2[0]), by + u2[1]), (X(bx), by))  # 内侧边 T→B
    pen.curveTo((X(bx) + D(v1[0]), by + v1[1]), (X(tx) + D(v2[0]), ty + v2[1]), (X(tx), ty))  # 外侧边 B→T
    pen.closePath()
    return p


def heart(cx, tip_y, width, rotate_deg=0.0):
    """标志里的爱心（对称、尖底），按宽度缩放，绕尖点旋转（度，顺时针）。"""
    h = HEART
    s = width / (2 * h['w'])
    th = math.radians(rotate_deg)
    ct, st = math.cos(th), math.sin(th)

    def P(x, y):
        lx, ly = x * s, (y - h['yb']) * s
        return (cx + lx * ct - ly * st, tip_y + lx * st + ly * ct)

    a, w, yt, yn, yw, yb = h['a'], h['w'], h['yt'], h['yn'], h['yw'], h['yb']
    c1x, c1y = h['c1']
    tx, ty = h['tip']
    p = new_path()
    pen = p.getPen()
    pen.moveTo(P(0, yn))
    pen.curveTo(P(c1x, yn - c1y), P(a - h['c2'], yt), P(a, yt))
    pen.curveTo(P(a + h['c3'], yt), P(w, yw - h['c4']), P(w, yw))
    pen.curveTo(P(w, yw + h['c5']), P(tx, ty), P(0, yb))
    pen.curveTo(P(-tx, ty), P(-w, yw + h['c5']), P(-w, yw))
    pen.curveTo(P(-w, yw - h['c4']), P(-(a + h['c3']), yt), P(-a, yt))
    pen.curveTo(P(-(a - h['c2']), yt), P(-c1x, yn - c1y), P(0, yn))
    pen.closePath()
    return p


def symbol_parts():
    """两片叶子托起一颗爱心（展板像素坐标）。"""
    leaves = union(leaf(**LEAF), leaf(**LEAF, mirror=SYMBOL_AXIS))
    heart_w = 2 * HEART['w']
    return leaves, heart(SYMBOL_AXIS, HEART['yb'], heart_w)


def sprout():
    c = SPROUT_CENTER
    p = new_path()
    pen = p.getPen()
    pen.moveTo((c['cx'], c['yt']))
    pen.curveTo((c['cx'] + c['k1'], c['yt'] + c['m1']), (c['cx'] + c['k2'], c['yb'] - c['m2']), (c['cx'], c['yb']))
    pen.curveTo((c['cx'] - c['k2'], c['yb'] - c['m2']), (c['cx'] - c['k1'], c['yt'] + c['m1']), (c['cx'], c['yt']))
    pen.closePath()
    return union(p, leaf(**SPROUT_SIDE), leaf(**SPROUT_SIDE, mirror=c['cx']))


# ================================================================== 中文字标「喜甜」
def _xi():
    """喜：底横改为笑弧。"""
    g, adv = glyph(SERIF_VF, CN_WGHT, '喜')
    stems = scan_runs(g, 50)                     # 底横下方只剩下方「口」的两竖
    (l0, l1), (r0, r1) = stems[0], stems[-1]
    bar = [y for y in range(-60, 60) if any(s <= (l1 + r0) / 2 < e for s, e in scan_runs(g, y))]
    top, bot = min(bar), max(bar) + 1
    g = difference(g, rect(l1, top - 1, r0, bot + 1))
    sag = CN_SMILE_SAG
    x0, x1, mid = l1 - 6, r0 + 6, (l1 + r0) / 2
    smile = new_path()
    pen = smile.getPen()
    pen.moveTo((x0, top))
    pen.qCurveTo((mid, top + 2 * sag), (x1, top))
    pen.lineTo((x1, bot))
    pen.qCurveTo((mid, bot + 2 * sag), (x0, bot))
    pen.closePath()
    return union(g, smile), adv


def _tian():
    """甜：「甘」的下半改为杯形；返回 (轮廓, advance, 杯内参数)。"""
    g, adv = glyph(SERIF_VF, CN_WGHT, '甜')
    b = TIAN_BOWL
    runs = scan_runs(g, -200)                    # 「甘」中横与底横之间：最右两段即两竖
    (l0, l1), (r0, r1) = runs[-2], runs[-1]
    stem = ((l1 - l0) + (r1 - r0)) / 2
    t_bottom = round(stem * b['bottom_ratio'])
    g = difference(g, rect(l0 - 12, b['cut_top'], r1 + 20, 200))

    def u_shape(xl, xr, y_top, y_bottom):
        cx, rx, ry = (xl + xr) / 2, (xr - xl) / 2, y_bottom - b['curve_start']
        p = new_path()
        pen = p.getPen()
        pen.moveTo((xl, y_top))
        pen.lineTo((xl, b['curve_start']))
        pen.curveTo((xl, b['curve_start'] + KAPPA * ry), (cx - KAPPA * rx, y_bottom), (cx, y_bottom))
        pen.curveTo((cx + KAPPA * rx, y_bottom), (xr, b['curve_start'] + KAPPA * ry), (xr, b['curve_start']))
        pen.lineTo((xr, y_top))
        pen.closePath()
        return p

    bowl = difference(u_shape(l0, r1, b['cut_top'] - 5, b['bottom']),
                      u_shape(l1, r0, b['cut_top'] - 15, b['bottom'] - t_bottom))
    return union(g, bowl), adv, {'inner': (l1, r0), 'inner_bottom': b['bottom'] - t_bottom}


def cn_wordmark():
    """「喜甜」：返回 {'ink', 'accent'}，字体单位，y 向下，基线 0。"""
    xi, adv = _xi()
    tian, _, bowl = _tian()
    dx = adv + CN_TRACKING
    tian = transformed(tian, 1, dx, 0)
    il, ir = bowl['inner'][0] + dx, bowl['inner'][1] + dx
    cx, tip_y = (il + ir) / 2, bowl['inner_bottom'] - TIAN_HEART['tip_gap']
    # 二分求杯中能放下的最大爱心（与笔画保持 clearance）
    lo, hi, best = 60.0, float(ir - il), None
    for _ in range(18):
        w = (lo + hi) / 2
        h = heart(cx, tip_y, w, TIAN_HEART['rotate'])
        hb = bounds(h)
        h = heart(cx + (cx - (hb[0] + hb[2]) / 2), tip_y, w, TIAN_HEART['rotate'])   # 旋转后按外框居中
        if is_empty(intersection(dilate(h, TIAN_HEART['clearance']), tian)):
            best, lo = h, w
        else:
            hi = w
    assert best is not None and lo > 120, f'杯中爱心过小（{lo:.0f}），字体版本可能不同，请检查 TIAN_BOWL'
    return {'ink': union(xi, tian), 'accent': best}


# ================================================================== 英文字标 SWEETEN
def _en_parts():
    YT, YB = EN_CAP_TOP, EN_BASELINE

    def rounded_bar(x0, y0, x1, y1, left=False, right=True, r=EN_ROUND):
        k = KAPPA * r
        p = new_path()
        pen = p.getPen()
        pen.moveTo((x0 + (r if left else 0), y0))
        if right:
            pen.lineTo((x1 - r, y0))
            pen.curveTo((x1 - r + k, y0), (x1, y0 + r - k), (x1, y0 + r))
            pen.lineTo((x1, y1 - r))
            pen.curveTo((x1, y1 - r + k), (x1 - r + k, y1), (x1 - r, y1))
        else:
            pen.lineTo((x1, y0))
            pen.lineTo((x1, y1))
        if left:
            pen.lineTo((x0 + r, y1))
            pen.curveTo((x0 + r - k, y1), (x0, y1 - r + k), (x0, y1 - r))
            pen.lineTo((x0, y0 + r))
            pen.curveTo((x0, y0 + r - k), (x0 + r - k, y0), (x0 + r, y0))
        else:
            pen.lineTo((x0, y1))
        pen.closePath()
        return p

    def E(L):
        return union(rect(L, YT, L + EN_STEM, YB),
                     rounded_bar(L, YT, L + 53.7, YT + EN_ARM),
                     rounded_bar(L, 46.65, L + 44.0, 52.35),
                     rounded_bar(L, YB - EN_ARM, L + 54.1, YB))

    def T(L, w=71.0, stem=14.6):
        c = L + w / 2
        return union(rounded_bar(L, YT, L + w, YT + EN_ARM, left=True), rect(c - stem / 2, YT, c + stem / 2, YB))

    def N(L, w=72.9, thin=5.9, diag=20.5, slope=0.871):
        R = L + w
        xr = lambda y: L + 31.28 + slope * (y - 30)
        d = poly([(xr(YT) - diag, YT), (xr(YT), YT), (xr(YB), YB), (xr(YB) - diag, YB)])
        return intersection(union(rect(L, YT, L + thin, YB), rect(R - thin, YT, R, YB), d), rect(L, YT, R, YB))

    def W(L, s=0.39, thick=16.0):
        o = L - 105.46

        def edge(x_at_top, slope):
            return lambda y: x_at_top + slope * (y - YT)

        def band(xl, xr, y0=YT, y1=YB):
            return poly([(xl(y0), y0), (xr(y0), y0), (xr(y1), y1), (xl(y1), y1)])

        def vee(thick_left, thin_left_at_top, thin_w, thin_top):
            tl, tr = edge(thick_left, s), edge(thick_left + thick, s)
            nl, nr = edge(thin_left_at_top, -s), edge(thin_left_at_top + thin_w, -s)
            wedge = poly([(tl(YT - 5), YT - 5), (nr(YT - 5), YT - 5), (nr(YB), YB), (tl(YB), YB)])   # 两条外边之间
            return intersection(union(band(tl, tr), band(nl, nr, thin_top, YB)), wedge)

        v1 = vee(105.46 + o, 145.62 + o + s * (70 - YT), 5.4, 28)
        v2 = vee(153.59 + o, 216.25 + o + s * (11 - YT), 5.6, YT)
        return intersection(union(v1, v2), rect(L - 1, YT, L + 120, YB))

    s_path = new_path()
    pen = s_path.getPen()
    for cmd, args in re.findall(r'([MLCZ])([^MLCZ]*)', EN_S):
        nums = [float(v) for v in args.split()]
        pts = list(zip(nums[0::2], nums[1::2]))
        if cmd == 'M':
            pen.moveTo(pts[0])
        elif cmd == 'L':
            pen.lineTo(pts[0])
        elif cmd == 'C':
            pen.curveTo(*pts)
        else:
            pen.closePath()
    L = EN_LETTERS
    return union(s_path, W(L['W']), E(L['E1']), E(L['E2']), T(L['T']), E(L['E3']), N(L['N']))


def en_wordmark():
    """SWEETEN：单位为大写高度 = 700，y 向下，基线 0，左缘 0。"""
    p = _en_parts()
    k = 700 / (EN_BASELINE - EN_CAP_TOP)
    return transformed(p, k, -bounds(p)[0] * k, -EN_BASELINE * k)


# ================================================================== 组合（版式单位：SWEETEN 大写高 H = 100）
class Lockup:
    def __init__(self, name):
        self.name = name
        self.items = []        # (part_name, scale, tx, ty)

    def add(self, part, s, tx, ty):
        self.items.append((part, s, tx, ty))

    def layers(self, parts):
        ink, accent = new_path(), new_path()
        for name, s, tx, ty in self.items:
            part = parts[name]
            ink = union(ink, transformed(part['ink'], s, tx, ty))
            if part.get('accent') is not None:
                accent = union(accent, transformed(part['accent'], s, tx, ty))
        return ink, accent

    def box(self, parts):
        ink, accent = self.layers(parts)
        b0, b1 = bounds(ink), bounds(accent) if not is_empty(accent) else bounds(ink)
        return (min(b0[0], b1[0]), min(b0[1], b1[1]), max(b0[2], b1[2]), max(b0[3], b1[3]))


def build_parts():
    cn = cn_wordmark()
    en = en_wordmark()
    tagline = text_line(MONTSERRAT_VF, TAGLINE['wght'], TAGLINE['text'], TAGLINE['tracking'])
    category = text_line(MONTSERRAT_VF, CATEGORY['wght'], CATEGORY['text'], CATEGORY['tracking'])
    leaves, sym_heart = symbol_parts()
    sp = sprout()
    # 分隔线：三瓣芽居中，两侧细线（高度按芽的 59% 处，展板实测）
    sb = bounds(sp)
    sw, sh = sb[2] - sb[0], sb[3] - sb[1]
    cx, line_y, gap, line_len, line_w = (sb[0] + sb[2]) / 2, sb[1] + sh * 0.59, sw * 0.62, sw * 3.1, sh * 0.034
    divider = union(sp,
                    rect(cx - gap - line_len, line_y - line_w / 2, cx - gap, line_y + line_w / 2),
                    rect(cx + gap, line_y - line_w / 2, cx + gap + line_len, line_y + line_w / 2))
    parts = {
        'cn': {'ink': cn['ink'], 'accent': cn['accent']},
        'en': {'ink': en},
        'tagline': {'ink': tagline},
        'category': {'ink': category},
        'divider': {'ink': divider},
        'symbol': {'ink': leaves, 'accent': sym_heart},
    }
    for p in parts.values():   # 每个部件平移到墨迹左上角为原点，便于组合
        x0 = min(bounds(v)[0] for v in p.values() if v is not None)
        y0 = min(bounds(v)[1] for v in p.values() if v is not None)
        for k in list(p):
            p[k] = transformed(p[k], 1, -x0, -y0)
    return parts


def size(parts, name):
    b = bounds(parts[name]['ink'])
    if parts[name].get('accent') is not None:
        a = bounds(parts[name]['accent'])
        b = (min(b[0], a[0]), min(b[1], a[1]), max(b[2], a[2]), max(b[3], a[3]))
    return b[2] - b[0], b[3] - b[1]


def build_lockups(parts):
    H = 100.0
    en_w, en_h = size(parts, 'en')
    en_s = H / 700 * 1.0                          # en 部件的大写高 700 → H（墨迹框包含 S 的上下溢出）
    en_W = en_w * en_s
    cn_w, cn_h = size(parts, 'cn')
    tag_w, tag_h = size(parts, 'tagline')
    cat_w, cat_h = size(parts, 'category')
    div_w, div_h = size(parts, 'divider')
    sym_w, sym_h = size(parts, 'symbol')
    en_over = (en_h - 700) / 2 * en_s             # S 的上下溢出（版式按大写高对齐）
    L = {}

    # 主标志（竖版）：喜甜 / SWEETEN / 标语 / 分隔线 / 品类，居中（展板 4 比例）
    lk = Lockup('primary')
    cn_s = 0.75 * en_W / cn_w
    y = 0.0
    lk.add('cn', cn_s, -cn_w * cn_s / 2, y)
    y += cn_h * cn_s + 0.56 * H
    lk.add('en', en_s, -en_W / 2, y - en_over)
    y += H + 0.66 * H
    tag_s = 0.80 * en_W / tag_w
    lk.add('tagline', tag_s, -tag_w * tag_s / 2, y)
    y += tag_h * tag_s + 0.68 * H
    div_s = 0.74 * en_W / div_w
    lk.add('divider', div_s, -div_w * div_s / 2, y)
    y += div_h * div_s + 0.74 * H
    cat_s = 0.78 * en_W / cat_w
    lk.add('category', cat_s, -cat_w * cat_s / 2, y)
    L['primary'] = lk

    # 横版：喜甜 | SWEETEN + 标语（标语居中于 SWEETEN 之下，基线与喜甜底部对齐）
    lk = Lockup('horizontal')
    cn_s_h = 2.75 * H / cn_h
    lk.add('cn', cn_s_h, 0, 0)
    x_en = cn_w * cn_s_h + 0.75 * H
    tag_s_h = 0.81 * en_W / tag_w
    tag_top = cn_h * cn_s_h - tag_h * tag_s_h
    en_top = tag_top - 0.64 * H - H
    lk.add('en', en_s, x_en, en_top - en_over)
    lk.add('tagline', tag_s_h, x_en + (en_W - tag_w * tag_s_h) / 2, tag_top)
    L['horizontal'] = lk

    # 单行（导航 / 侧栏）：喜甜 + SWEETEN，无标语；SWEETEN 与喜甜视觉居中
    lk = Lockup('inline')
    cn_s_i = 2.6 * H / cn_h
    lk.add('cn', cn_s_i, 0, 0)
    en_s_i = en_s * 0.86
    lk.add('en', en_s_i, cn_w * cn_s_i + 0.62 * H, (cn_h * cn_s_i - H * 0.86) / 2 - en_over * 0.86 + 0.06 * H)
    L['inline'] = lk

    # 两行：喜甜 / SWEETEN（小票、紧凑竖排）
    lk = Lockup('stacked')
    lk.add('cn', cn_s, -cn_w * cn_s / 2, 0)
    lk.add('en', en_s, -en_W / 2, cn_h * cn_s + 0.56 * H - en_over)
    L['stacked'] = lk

    # 图形标志
    lk = Lockup('symbol')
    lk.add('symbol', 1000 / sym_w, 0, 0)
    L['symbol'] = lk
    return L


# ================================================================== 输出：SVG / PNG / ICO
def lockup_geometry(lockup, parts, pad=0.0):
    ink, accent = lockup.layers(parts)
    x0, y0, x1, y1 = lockup.box(parts)
    w, h = x1 - x0, y1 - y0
    p = pad * max(w, h) if pad < 1 else pad
    return ink, accent, (x0 - p, y0 - p, w + 2 * p, h + 2 * p)


def svg_document(layers, view_box, title, background=None, rx=0):
    vx, vy, vw, vh = view_box
    out = [f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="{_n(vx)} {_n(vy)} {_n(vw)} {_n(vh)}" role="img" aria-label="{title}">',
           f'<title>{title}</title>']
    if background:
        out.append(f'<rect x="{_n(vx)}" y="{_n(vy)}" width="{_n(vw)}" height="{_n(vh)}"{f" rx=\"{_n(rx)}\"" if rx else ""} fill="{background}"/>')
    for path, color in layers:
        if path is not None and not is_empty(path):
            out.append(f'<path fill="{color}" d="{svg_d(path)}"/>')
    out.append('</svg>')
    return '\n'.join(out) + '\n'


def _n(v, digits=2):
    """数字转文本：保留 digits 位小数，只去掉小数部分末尾的 0（整数里的 0 不动）。"""
    t = f'{v:.{digits}f}'
    if '.' in t:
        t = t.rstrip('0').rstrip('.')
    return '0' if t in ('-0', '') else t


def _coverage(sk_path, width, height, matrix=None):
    """抗锯齿覆盖率（0–1 浮点），用于按图层上色。"""
    surf = skia.Surface.MakeRaster(skia.ImageInfo.MakeA8(width, height))
    c = surf.getCanvas()
    c.clear(skia.ColorTRANSPARENT)
    if matrix:
        c.concat(matrix)
    c.drawPath(sk_path, skia.Paint(AntiAlias=True))
    a = surf.makeImageSnapshot().toarray(colorType=skia.kAlpha_8_ColorType)
    return a.reshape(height, width).astype(np.float64) / 255.0


def render_png(layers, view_box, width, height=None, background=None, rx=0):
    """按图层覆盖率在浮点空间合成后输出直通（非预乘）alpha：
    边缘像素的 RGB 始终等于图层本色，只有 alpha 渐变，不会出现 8 位预乘换算带来的偏色或白边。"""
    vx, vy, vw, vh = view_box
    height = height or round(width * vh / vw)
    s = min(width / vw, height / vh)
    matrix = skia.Matrix()
    matrix.setScaleTranslate(s, s, (width - vw * s) / 2 - vx * s, (height - vh * s) / 2 - vy * s)
    rgb = lambda col: np.array([int(col[i:i + 2], 16) for i in (1, 3, 5)], dtype=np.float64) / 255.0
    stack = []
    if background:
        tile = skia.Path()
        if rx:
            tile.addRRect(skia.RRect.MakeRectXY(skia.Rect.MakeWH(width, height), rx * s, rx * s))
        else:
            tile.addRect(skia.Rect.MakeWH(width, height))
        stack.append((_coverage(tile, width, height), rgb(background)))
    for path, color in layers:
        if path is not None and not is_empty(path):
            stack.append((_coverage(to_skia(path), width, height, matrix), rgb(color)))
    premul = np.zeros((height, width, 3))
    alpha = np.zeros((height, width))
    for cov, col in stack:           # source-over，预乘空间
        premul = col * cov[..., None] + premul * (1 - cov[..., None])
        alpha = cov + alpha * (1 - cov)
    straight = np.divide(premul, alpha[..., None], out=np.zeros_like(premul), where=alpha[..., None] > 0)
    out = np.dstack([straight, alpha])
    return Image.fromarray(np.round(out * 255).astype(np.uint8), 'RGBA')


def save_png(img, path):
    path.parent.mkdir(parents=True, exist_ok=True)
    if img.getextrema()[3][0] == 255:     # 不透明图存为 RGB
        img = img.convert('RGB')
    img.save(path, optimize=True)
    print(f'  {path.relative_to(ROOT)}  {img.size[0]}×{img.size[1]}  {path.stat().st_size // 1024 or 1}KB')


def write_text(path, text):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding='utf-8', newline='\n')
    print(f'  {path.relative_to(ROOT)}  {len(text.encode()) // 1024 or 1}KB')


# ================================================================== 应用图标 / 网站图标
def app_icon_layers(parts, size_px, full_bleed=False, content=0.62):
    """展板 4 的应用图标：浅草莓底 + 喜甜 + SWEETEN（竖排、同宽）。"""
    cn_w, cn_h = size(parts, 'cn')
    en_w, en_h = size(parts, 'en')
    W = content * size_px
    cn_s, en_s = W / cn_w, W / en_w
    gap = 0.058 * size_px
    block_h = cn_h * cn_s + gap + 700 * en_s
    y0 = (size_px - block_h) / 2 - 0.01 * size_px
    en_over = (en_h - 700) / 2 * en_s
    x0 = (size_px - W) / 2
    ink = union(transformed(parts['cn']['ink'], cn_s, x0, y0),
                transformed(parts['en']['ink'], en_s, x0, y0 + cn_h * cn_s + gap - en_over))
    accent = transformed(parts['cn']['accent'], cn_s, x0, y0)
    radius = 0 if full_bleed else 0.2237 * size_px
    return [(ink, COCOA), (accent, BLUSH)], (0, 0, size_px, size_px), radius


def favicon_layers(parts, size_px):
    """网站图标：暖奶油白圆角底 + 简化图形（小尺寸时图形放大、爱心加大）。"""
    sym_w, sym_h = size(parts, 'symbol')
    fill = {16: 0.90, 32: 0.80}.get(size_px, 0.76)
    s = fill * size_px / sym_w
    ox, oy = (size_px - sym_w * s) / 2, (size_px - sym_h * s) / 2 + 0.01 * size_px
    leaves = transformed(parts['symbol']['ink'], s, ox, oy)
    heart_path = transformed(parts['symbol']['accent'], s, ox, oy)
    if size_px <= 32:                      # 16/32px：爱心放大 12%（以尖点为基准），避免缩成一个点
        hb = bounds(heart_path)
        cx, cy = (hb[0] + hb[2]) / 2, hb[3]
        heart_path = transformed(transformed(heart_path, 1, -cx, -cy), 1.12, cx, cy)
    return [(leaves, COCOA), (heart_path, BLUSH)], (0, 0, size_px, size_px), 0.22 * size_px


# ================================================================== Web App Manifest
def write_manifest():
    manifest = {
        'name': '喜甜 SWEETEN',
        'short_name': '喜甜',
        'description': '喜甜 SWEETEN — 甜品 · 烘焙 · 饮品。SWEETEN YOUR DAY.',
        'lang': 'zh-CN',
        'theme_color': CREAM,
        'background_color': CREAM,
        'display': 'browser',      # 只提供主屏图标与名称，不把站点声明为可安装应用（没有离线能力）
        'icons': [
            {'src': '/brand/sweeten-icon-192.png', 'sizes': '192x192', 'type': 'image/png', 'purpose': 'any'},
            {'src': '/brand/sweeten-icon-512.png', 'sizes': '512x512', 'type': 'image/png', 'purpose': 'any'},
            {'src': '/brand/sweeten-icon-maskable-512.png', 'sizes': '512x512', 'type': 'image/png', 'purpose': 'maskable'},
        ],
    }
    write_text(ROOT / 'public/site.webmanifest', json.dumps(manifest, ensure_ascii=False, indent=2) + '\n')


# ================================================================== 组件路径数据（BrandLogo.vue）
def write_logo_js(parts, lockups):
    lines = ['// 由 scripts/brand/build_brand_assets.py 生成，请勿手改。',
             '// 「喜甜」取自思源宋体并定制，标语 / 品类为 Montserrat（均为 SIL OFL 1.1），已转为路径，不依赖字体文件。',
             '',
             '/** 部件：ink = 主色（可可棕；深色底为奶油白），accent = 爱心（草莓腮红） */',
             'export const LOGO_PARTS = {']
    for name, p in parts.items():
        digits = 1 if name in ('symbol', 'divider') else 0
        fields = [f"ink: '{svg_d(p['ink'], digits=digits)}'"]
        if p.get('accent') is not None:
            fields.append(f"accent: '{svg_d(p['accent'], digits=digits)}'")
        lines.append(f'  {name}: {{ {", ".join(fields)} }},')
    lines += ['}', '', '/** 组合：viewBox + 各部件的变换矩阵（与 public/brand 导出图同源） */', 'export const LOGO_LOCKUPS = {']
    for name, lk in lockups.items():
        x0, y0, x1, y1 = lk.box(parts)
        items = ', '.join(f"['{part}', 'matrix({_n(s, 6)} 0 0 {_n(s, 6)} {_n(tx - x0)} {_n(ty - y0)})']"
                          for part, s, tx, ty in lk.items)
        lines.append(f"  {name}: {{ viewBox: '0 0 {_n(x1 - x0)} {_n(y1 - y0)}', items: [{items}] }},")
    lines += ['}', '']
    write_text(ROOT / 'src/assets/brand/logo.js', '\n'.join(lines))


# ================================================================== 字体子集（展示字体 / 拉丁字体）
DISPLAY_WGHT = 600
LATIN_WGHT = 500
PUNCT = '，。、！？：；“”‘’（）《》·—…「」'
LATIN = ''.join(chr(c) for c in range(0x20, 0x7F)) + '·—’'


def write_subset(font, wght, text, out_name):
    opts = subset.Options()
    opts.flavor = 'woff2'
    opts.layout_features = ['kern', 'palt', 'liga']
    opts.name_IDs = [0, 1, 2, 3, 4, 5, 6, 13, 14]  # 保留版权与 OFL 许可声明
    opts.notdef_outline = True
    sub = subset.Subsetter(opts)
    sub.populate(text=text)
    # 子集化会原地修改字体对象，用新实例
    f = instancer.instantiateVariableFont(TTFont(font), {'wght': wght}, inplace=False)
    sub.subset(f)
    out = ROOT / 'src/assets/fonts' / out_name
    f.flavor = 'woff2'
    f.save(out)
    print(f'  {out.relative_to(ROOT)}  {out.stat().st_size // 1024}KB, {len(set(text))} chars')


def display_chars():
    """brand.js 里字符串字面量中的汉字（注释不计入，避免子集膨胀）。"""
    src = (ROOT / 'src/config/brand.js').read_text(encoding='utf-8')
    literals = re.findall(r"'([^'\n]*)'|`([^`]*)`", src)
    text = ''.join(a + b for a, b in literals)
    han = set(re.findall(r'[\u3400-\u9fff]', text))
    return ''.join(sorted(han)) + PUNCT + ' '


# ================================================================== 主流程
def main(logo=True, fonts=True):
    if logo:
        build_logo_assets()
    if fonts:
        print('字体子集 …')
        write_subset(SERIF_VF, DISPLAY_WGHT, display_chars(), 'sweeten-display.woff2')
        write_subset(SANS_VF, LATIN_WGHT, LATIN, 'sweeten-latin.woff2')


def build_logo_assets():
    print('构建矢量部件 …')
    parts = build_parts()
    for name in ('symbol', 'divider'):     # 小单位部件统一到 1000 宽，路径数据更整洁
        w, _ = size(parts, name)
        parts[name] = {k: transformed(v, 1000 / w) for k, v in parts[name].items()}
    lockups = build_lockups(parts)
    brand = ROOT / 'public/brand'

    print('标志（SVG 母版 + PNG）…')
    exports = [  # 文件名, 组合, 透明版留白（相对长边）, PNG 宽度
        ('sweeten-logo', 'primary', 0.05, 1600),
        ('sweeten-logo-horizontal', 'horizontal', 0.04, 2400),
        ('sweeten-symbol', 'symbol', 0.08, 1024),
    ]
    for base, key, pad, width in exports:
        ink, accent, vb = lockup_geometry(lockups[key], parts, pad)
        for tone in ('default', 'inverse'):
            t = TONES[tone]
            layers = [(ink, t['ink']), (accent, t['accent'])]
            suffix = '' if tone == 'default' else '-inverse'
            write_text(brand / f'{base}{suffix}.svg', svg_document(layers, vb, '喜甜 SWEETEN'))
            if key == 'symbol':
                save_png(render_png(layers, vb, width, width), brand / f'{base}{suffix}.png')
                continue
            save_png(render_png(layers, vb, width), brand / f'{base}{suffix}-transparent.png')
            # 带底色版本：留出更宽的安全区
            bg = CREAM if tone == 'default' else COCOA
            ink_b, accent_b, vb_b = lockup_geometry(lockups[key], parts, 0.14 if key == 'primary' else 0.10)
            save_png(render_png([(ink_b, t['ink']), (accent_b, t['accent'])], vb_b, width, background=bg),
                     brand / f'{base}{suffix}.png')

    print('应用图标 …')
    layers, vb, radius = app_icon_layers(parts, 512)
    write_text(brand / 'sweeten-icon.svg', svg_document(layers, vb, '喜甜 SWEETEN', background=BLUSH_TINT, rx=radius))
    for px in (512, 192):
        save_png(render_png(layers, vb, px, px, background=BLUSH_TINT, rx=radius), brand / f'sweeten-icon-{px}.png')
    layers_m, vb_m, _ = app_icon_layers(parts, 512, full_bleed=True, content=0.50)   # 内容落在 80% 安全圆内
    save_png(render_png(layers_m, vb_m, 512, 512, background=BLUSH_TINT), brand / 'sweeten-icon-maskable-512.png')
    layers_a, vb_a, _ = app_icon_layers(parts, 512, full_bleed=True, content=0.60)
    save_png(render_png(layers_a, vb_a, 180, 180, background=BLUSH_TINT), ROOT / 'public/apple-touch-icon.png')

    print('网站图标 …')
    layers, vb, radius = favicon_layers(parts, 64)
    write_text(ROOT / 'public/favicon.svg', svg_document(layers, vb, '喜甜 SWEETEN', background=CREAM, rx=radius))
    frames = {}
    for px in (16, 32, 48):
        layers, vb, radius = favicon_layers(parts, px)
        frames[px] = render_png(layers, vb, px, px, background=CREAM, rx=radius)
    save_png(frames[32], ROOT / 'public/favicon.png')
    ico = ROOT / 'public/favicon.ico'
    frames[48].save(ico, format='ICO', sizes=[(16, 16), (32, 32), (48, 48)], append_images=[frames[16], frames[32]])
    print(f'  {ico.relative_to(ROOT)}  16/32/48  {ico.stat().st_size // 1024 or 1}KB')

    print('图标清单 …')
    write_manifest()

    print('组件路径数据 …')
    write_logo_js(parts, lockups)
    return parts, lockups


if __name__ == '__main__':
    ap = argparse.ArgumentParser(description='喜甜 SWEETEN 品牌资源生成')
    ap.add_argument('--skip-logo', action='store_true', help='不生成标志、图标与组件路径数据')
    ap.add_argument('--skip-fonts', action='store_true', help='不生成字体子集')
    args = ap.parse_args()
    main(logo=not args.skip_logo, fonts=not args.skip_fonts)

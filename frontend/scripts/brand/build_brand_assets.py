"""
喜甜 SWEETEN 品牌资源生成脚本

生成（结果已提交到仓库，平时不需要运行）：
  src/assets/brand/wordmark.js          「喜甜」「SWEETEN」字标轮廓（SVG path，不依赖字体）
  src/assets/fonts/sweeten-display.woff2 展示字体：思源宋体 600，子集 = src/config/brand.js 中出现的汉字
  src/assets/fonts/sweeten-latin.woff2   英文字体：思源黑体 500 拉丁字母子集（眉题 / 标语）

何时需要重新运行：修改了 src/config/brand.js 里使用展示字体的中文文案。

依赖：
  pip install fonttools brotli
  字体（SIL Open Font License 1.1，可免费商用与嵌入）：
    Noto Serif SC 可变字体  https://fonts.google.com/noto/specimen/Noto+Serif+SC
    Noto Sans SC 可变字体   https://fonts.google.com/noto/specimen/Noto+Sans+SC
  默认读取 Windows 字体目录，可用环境变量 SWEETEN_SERIF_VF / SWEETEN_SANS_VF 指定路径。

用法（在前端仓库根目录）：
  python scripts/brand/build_brand_assets.py
"""
import os
import re
from pathlib import Path

from fontTools import subset
from fontTools.pens.boundsPen import BoundsPen
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
from fontTools.ttLib import TTFont
from fontTools.varLib import instancer

ROOT = Path(__file__).resolve().parents[2]
SERIF_VF = os.environ.get('SWEETEN_SERIF_VF', 'C:/Windows/Fonts/NotoSerifSC-VF.ttf')
SANS_VF = os.environ.get('SWEETEN_SANS_VF', 'C:/Windows/Fonts/NotoSansSC-VF.ttf')

# 字标参数（改动会影响全站 Logo 观感，需同步检查 BrandLogo.vue 的排版）
WORDMARK_CN = dict(font=SERIF_VF, wght=700, text='喜甜', tracking=0.04)
WORDMARK_EN = dict(font=SANS_VF, wght=500, text='SWEETEN', tracking=0.30)

DISPLAY_WGHT = 600
LATIN_WGHT = 500
PUNCT = '，。、！？：；“”‘’（）《》·—…「」'
LATIN = ''.join(chr(c) for c in range(0x20, 0x7F)) + '·—’'

_instances = {}


def instance(path, wght):
    key = (path, wght)
    if key not in _instances:
        _instances[key] = instancer.instantiateVariableFont(TTFont(path), {'wght': wght}, inplace=False)
    return _instances[key]


def outline(font, wght, text, tracking):
    """排版一行文字并返回 (viewBox, d)；坐标为字体单位、y 轴向下，viewBox 紧贴墨迹。"""
    f = instance(font, wght)
    glyph_set, cmap, hmtx = f.getGlyphSet(), f.getBestCmap(), f['hmtx']
    upem = f['head'].unitsPerEm
    x, parts, x0, y0, x1, y1 = 0, [], None, None, None, None
    for i, ch in enumerate(text):
        name = cmap[ord(ch)]
        pen = SVGPathPen(glyph_set, ntos=lambda v: str(round(v)))
        glyph_set[name].draw(TransformPen(pen, (1, 0, 0, -1, x, 0)))
        parts.append(pen.getCommands())
        bp = BoundsPen(glyph_set)
        glyph_set[name].draw(bp)
        bx0, by0, bx1, by1 = bp.bounds
        x0 = min(x0, x + bx0) if x0 is not None else x + bx0
        x1 = max(x1, x + bx1) if x1 is not None else x + bx1
        y0 = min(y0, -by1) if y0 is not None else -by1
        y1 = max(y1, -by0) if y1 is not None else -by0
        x += hmtx[name][0] + (tracking * upem if i < len(text) - 1 else 0)
    view_box = f'{round(x0)} {round(y0)} {round(x1 - x0)} {round(y1 - y0)}'
    return view_box, ''.join(parts)


def write_wordmark():
    cn_box, cn_d = outline(WORDMARK_CN['font'], WORDMARK_CN['wght'], WORDMARK_CN['text'], WORDMARK_CN['tracking'])
    en_box, en_d = outline(WORDMARK_EN['font'], WORDMARK_EN['wght'], WORDMARK_EN['text'], WORDMARK_EN['tracking'])
    out = ROOT / 'src/assets/brand/wordmark.js'
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(
        '// 由 scripts/brand/build_brand_assets.py 生成，请勿手改。\n'
        '// 字形轮廓取自 Noto Serif SC / Noto Sans SC（SIL OFL 1.1），转为路径后不依赖任何字体文件。\n'
        f"export const WORDMARK_CN = {{ viewBox: '{cn_box}', d: '{cn_d}' }}\n"
        f"export const WORDMARK_EN = {{ viewBox: '{en_box}', d: '{en_d}' }}\n",
        encoding='utf-8', newline='\n')
    print('wordmark.js', out.stat().st_size, 'bytes')


def write_subset(font, wght, text, out_name):
    opts = subset.Options()
    opts.flavor = 'woff2'
    opts.layout_features = ['kern', 'palt', 'liga']
    opts.name_IDs = [0, 1, 2, 3, 4, 5, 6, 13, 14]  # 保留版权与 OFL 许可声明
    opts.notdef_outline = True
    sub = subset.Subsetter(opts)
    sub.populate(text=text)
    # 子集化会原地修改字体对象，用新实例而不是缓存里的实例
    f = instancer.instantiateVariableFont(TTFont(font), {'wght': wght}, inplace=False)
    sub.subset(f)
    out = ROOT / 'src/assets/fonts' / out_name
    f.flavor = 'woff2'
    f.save(out)
    print(out_name, out.stat().st_size, 'bytes,', len(set(text)), 'chars')


def display_chars():
    """brand.js \u91cc\u5b57\u7b26\u4e32\u5b57\u9762\u91cf\u4e2d\u7684\u6c49\u5b57\uff08\u6ce8\u91ca\u4e0d\u8ba1\u5165\uff0c\u907f\u514d\u5b50\u96c6\u81a8\u80c0\uff09\u3002"""
    src = (ROOT / 'src/config/brand.js').read_text(encoding='utf-8')
    literals = re.findall(r"'([^'\n]*)'|`([^`]*)`", src)
    text = ''.join(a + b for a, b in literals)
    han = set(re.findall(r'[\u3400-\u9fff]', text))
    return ''.join(sorted(han)) + PUNCT + ' '


if __name__ == '__main__':
    write_wordmark()
    write_subset(SERIF_VF, DISPLAY_WGHT, display_chars(), 'sweeten-display.woff2')
    write_subset(SANS_VF, LATIN_WGHT, LATIN, 'sweeten-latin.woff2')

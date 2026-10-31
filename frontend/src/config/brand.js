/**
 * 喜甜 SWEETEN — 品牌常量与固定文案（单一来源）
 *
 * 展示字体（assets/fonts/sweeten-display.woff2，思源宋体子集）只包含本文件里出现过的汉字。
 * 修改或新增使用展示字体（.sw-display / var(--font-display)）的文案时，改这里，
 * 然后运行 `python scripts/brand/build_brand_assets.py` 重新生成字体子集。
 * 动态内容（商品名、人名、金额）一律使用系统字体，不要套展示字体。
 */

export const BRAND = {
  nameCn: '喜甜',
  nameEn: 'SWEETEN',
  fullName: '喜甜 SWEETEN',
  tagline: 'SWEETEN YOUR DAY.',
  taglineCn: '让每一天，甜一点。',
  category: '甜品 · 烘焙 · 饮品',
  categoryEn: 'DESSERT · BAKERY · DRINKS',
  workspace: '门店工作台',
}

/** 浏览器标题：「页面 · 喜甜 SWEETEN」 */
export const pageTitle = (title) => (title ? `${title} · ${BRAND.fullName}` : `${BRAND.fullName} · ${BRAND.category}`)

/** 顾客端文案 */
export const STORE_COPY = {
  hero: {
    eyebrow: BRAND.categoryEn,
    title: BRAND.taglineCn,
    lead: '蛋糕、面包与饮品，每日现做。为平常日子，添一份刚刚好的甜。',
    cta: '浏览今日甜品',
  },
  promises: [
    { key: 'fresh', title: '每日现做', desc: '下单后由门店确认制作' },
    { key: 'delivery', title: '同城配送', desc: '配送费全免' },
    { key: 'points', title: '会员积分', desc: '订单完成后积分自动到账' },
  ],
  sections: {
    bestsellers: { eyebrow: 'BESTSELLERS', title: '人气甜品' },
    menu: { eyebrow: 'THE MENU', title: '今日菜单' },
  },
  titles: {
    cart: '购物车',
    checkout: '确认订单',
    success: '订单已提交',
    orders: '我的订单',
    member: '喜甜会员',
  },
  /** 支付现状如实告知：系统没有接入在线支付，订单提交后进入门店「待接单」 */
  paymentNote: '在线支付暂未开通，订单提交后由门店确认制作。',
}

/** 员工端（登录页 / 工作台）文案 */
export const STAFF_COPY = {
  login: {
    eyebrow: 'SWEETEN · STAFF',
    title: '欢迎回到喜甜',
    lead: '登录门店工作台，打理商品、订单、库存与会员。',
    heroTitle: BRAND.taglineCn,
    heroSub: BRAND.tagline,
  },
}

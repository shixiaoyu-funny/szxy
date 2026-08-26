/**
 * 生成台湾省静态区划 JSON（数据来源见同目录 SOURCE.md）
 * 运行：node scripts/gen-taiwan-regions.mjs
 */
import { writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const __dirname = dirname(fileURLToPath(import.meta.url));

function districts(names, parentCode, start = 1) {
  return names.map((name, i) => {
    const seq = String(start + i).padStart(2, '0');
    // 12 位：前 4 位市码 + 2 位区序 + 000000
    return {
      code: `${parentCode.slice(0, 4)}${seq}000000`,
      name,
      level: 3,
      type: '区',
      children: []
    };
  });
}

function city(code, name, type, areas) {
  const kids = districts(areas, code);
  return { code, name, level: 2, type, children: kids, _expect: areas.length };
}

function county(code, name) {
  return { code, name, level: 2, type: '县', children: [] };
}

const cities = [
  city('710100000000', '台北市', '直辖市', [
    '中正区', '大同区', '中山区', '松山区', '大安区', '万华区',
    '信义区', '士林区', '北投区', '内湖区', '南港区', '文山区'
  ]),
  city('710200000000', '新北市', '直辖市', [
    '板桥区', '新庄区', '中和区', '永和区', '土城区', '树林区', '三峡区', '莺歌区',
    '三重区', '芦洲区', '五股区', '泰山区', '林口区', '八里区', '淡水区', '三芝区',
    '石门区', '金山区', '万里区', '汐止区', '瑞芳区', '贡寮区', '平溪区', '双溪区',
    '新店区', '深坑区', '石碇区', '坪林区', '乌来区'
  ]),
  city('710300000000', '桃园市', '直辖市', [
    '桃园区', '中坜区', '平镇区', '八德区', '杨梅区', '芦竹区', '大溪区',
    '龙潭区', '龟山区', '大园区', '观音区', '新屋区', '复兴区'
  ]),
  city('710400000000', '台中市', '直辖市', [
    '中区', '东区', '南区', '西区', '北区', '北屯区', '西屯区', '南屯区',
    '太平区', '大里区', '雾峰区', '乌日区', '丰原区', '后里区', '石冈区', '东势区',
    '新社区', '潭子区', '大雅区', '神冈区', '大肚区', '沙鹿区', '龙井区', '梧栖区',
    '清水区', '大甲区', '外埔区', '大安区', '和平区'
  ]),
  city('710500000000', '台南市', '直辖市', [
    '中西区', '东区', '南区', '北区', '安平区', '安南区', '永康区', '归仁区',
    '新化区', '左镇区', '玉井区', '楠西区', '南化区', '仁德区', '关庙区', '龙崎区',
    '官田区', '麻豆区', '佳里区', '西港区', '七股区', '将军区', '学甲区', '北门区',
    '新营区', '后壁区', '白河区', '东山区', '六甲区', '下营区', '柳营区', '盐水区',
    '善化区', '大内区', '山上区', '新市区', '安定区'
  ]),
  city('710600000000', '高雄市', '直辖市', [
    '楠梓区', '左营区', '鼓山区', '三民区', '盐埕区', '前金区', '新兴区', '苓雅区',
    '前镇区', '旗津区', '小港区', '凤山区', '大寮区', '鸟松区', '林园区', '仁武区',
    '大树区', '大社区', '冈山区', '路竹区', '桥头区', '梓官区', '弥陀区', '永安区',
    '燕巢区', '田寮区', '阿莲区', '茄萣区', '湖内区', '旗山区', '美浓区', '内门区',
    '杉林区', '甲仙区', '六龟区', '茂林区', '桃源区', '那玛夏区'
  ]),
  city('710700000000', '基隆市', '市', [
    '仁爱区', '中正区', '信义区', '中山区', '安乐区', '暖暖区', '七堵区'
  ]),
  city('710800000000', '新竹市', '市', ['东区', '北区', '香山区']),
  city('710900000000', '嘉义市', '市', ['东区', '西区'])
];

const counties = [
  county('711000000000', '新竹县'),
  county('711100000000', '苗栗县'),
  county('711200000000', '彰化县'),
  county('711300000000', '南投县'),
  county('711400000000', '云林县'),
  county('711500000000', '嘉义县'),
  county('711600000000', '屏东县'),
  county('711700000000', '宜兰县'),
  county('711800000000', '花莲县'),
  county('711900000000', '台东县'),
  county('712000000000', '澎湖县'),
  county('712100000000', '金门县'),
  county('712200000000', '连江县')
];

const expectDistricts = {
  台北市: 12,
  新北市: 29,
  桃园市: 13,
  台中市: 29,
  台南市: 37,
  高雄市: 38,
  基隆市: 7,
  新竹市: 3,
  嘉义市: 2
};

for (const c of cities) {
  const n = c.children.length;
  const exp = expectDistricts[c.name];
  if (n !== exp) {
    throw new Error(`${c.name} 区数应为 ${exp}，实际 ${n}`);
  }
  delete c._expect;
}

const children = [...cities, ...counties];
if (children.length !== 22) {
  throw new Error(`二级行政区应为 22，实际 ${children.length}`);
}

const root = {
  code: '710000000000',
  name: '台湾省',
  level: 1,
  type: '省',
  children
};

const out = join(__dirname, '../src/data/taiwanRegions.json');
writeFileSync(out, JSON.stringify(root, null, 2), 'utf8');
console.log('wrote', out);
console.log('level2', children.length, 'districts total', cities.reduce((s, c) => s + c.children.length, 0));

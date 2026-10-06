// A deliberately narrow, deterministic route for published area records.
// Other questions continue to use the knowledge assistant.
const AREA = '红树林面积';
const RECORD_ID = /\bDEMO-\d{3}\b/i;
const REGIONS = ['示例区域甲', '示例区域乙'];

export function isPublicFactQuery(question) {
  const text = String(question || '').trim();
  if (RECORD_ID.test(text) && /来源|出处|依据|链接|记录/.test(text)) return true;
  return text.includes(AREA) && REGIONS.some((name) => text.includes(name));
}

export function answerPublicFact(question, cards = []) {
  if (!isPublicFactQuery(question)) return null;
  const text = String(question).trim();
  const id = text.match(RECORD_ID)?.[0]?.toUpperCase();
  let matches;
  if (id) {
    matches = cards.filter((card) => card.record_id === id);
  } else {
    const mentioned = REGIONS.filter((name) => text.includes(name));
    const year = text.match(/\b(?:19|20)\d{2}\b/)?.[0];
    if (mentioned.length !== 1 || !year) {
      return {
        answer: '请明确一个合成地区（示例区域甲或示例区域乙）和一个年份后再查询测试记录。',
        sources: [],
        evidenceStatus: 'needs_clarification',
        route: 'public_data',
      };
    }
    const region = mentioned[0];
    matches = cards.filter((card) => card.title === `${region} · ${AREA}` && card.year_or_period === year);
  }
  const valid = matches.filter((card) => Number.isFinite(Number(card.value)) && card.unit && /^https:\/\//.test(card.source_url || ''));
  if (!valid.length) {
    return {
      answer: '当前公开资料中未找到与该地区、年份或记录编号完全匹配且可追溯的记录，不能给出确定数值。请到公开数据页核对筛选条件。',
      sources: [],
      evidenceStatus: 'not_found',
      route: 'public_data',
    };
  }
  const shown = valid.slice(0, 5);
  const lines = shown.map((card) => `${card.title}（${card.year_or_period}）：**${card.value} ${card.unit}**。记录 ${card.record_id}；质量等级 ${card.quality_level || '未标注'}${card.is_proxy ? '；代理指标' : ''}。`);
  return {
    answer: `${lines.join('\n\n')}\n\n以上均为完全合成的界面测试记录，不得用于科研、核算或业务判断。`,
    sources: shown.map((card) => ({ title: card.subtitle || card.title, url: card.source_url, recordId: card.record_id })),
    evidenceStatus: 'record_linked',
    route: 'public_data',
  };
}

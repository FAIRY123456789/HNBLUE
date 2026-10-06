from __future__ import annotations

import csv
import hashlib
import json
import math
import re
import unicodedata
from collections import Counter, defaultdict
from dataclasses import dataclass, asdict
from pathlib import Path
from typing import Iterable, Sequence

import numpy as np
from sklearn.decomposition import TruncatedSVD
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity
from sklearn.preprocessing import Normalizer


SUPPORTED_EXTENSIONS = {".md", ".txt", ".csv", ".json"}
ATOMIC_EXTENSIONS = {".csv", ".json", ".txt"}
EXCLUDED_PATHS = {
    "00_overview/all_in_one_hnblue_kb_v2.md": "duplicates the individual source documents",
}
DOMAIN_ALIASES = {
    "可解释": ("SHAP", "特征贡献", "解释方法"),
    "解释性": ("SHAP", "特征贡献"),
    "碳存量": ("碳储量", "碳储"),
    "碳储": ("碳储量", "地上生物量"),
    "接口": ("API", "端点"),
    "API": ("接口", "端点"),
    "真实性": ("is_simulated", "proxy", "来源追溯"),
    "真实数据": ("is_simulated", "proxy", "来源"),
    "是否真实": ("is_simulated", "proxy", "来源追溯"),
    "标签": ("is_simulated", "proxy", "结果类型"),
    "交易": ("核证", "MRV", "碳市场"),
    "无人机": ("UAV", "遥感"),
    "UAV": ("无人机", "遥感"),
    "大屏": ("深色主题", "驾驶舱"),
    "政府": ("政府端", "治理", "决策支持"),
    "OCR": ("图片识别", "扫描件", "原图复核", "机器识别"),
    "图片识别": ("OCR", "原图复核"),
    "野外调查": ("样方照片", "调查影像", "现场影像"),
    "记忆": ("对话历史", "会话上下文", "隐私", "长期记忆"),
    "历史": ("对话历史", "会话", "删除"),
    "隐私": ("最小化", "敏感信息", "会话删除"),
    "虚拟样方": ("情景模拟", "模型估算", "is_simulated"),
    "虚拟样地": ("虚拟样方", "情景模拟", "is_simulated"),
    "第二项": ("时间字段", "采集日期", "发布日期"),
    "页面上下文": ("AI 上下文", "字段含义", "空值", "缺失"),
    "后端没传": ("AI 上下文", "字段缺失", "空值", "不得自行补全"),
    "补齐": ("字段缺失", "空值", "不得自行补全"),
    "补上": ("字段缺失", "空值", "不得自行补全"),
    "InVEST-PLUS": ("情景预测", "未来趋势", "土地利用假设", "模型估算"),
    "安全审计": ("技术事件", "隐私最小化", "敏感问题", "响应状态"),
    "外部数据": ("外部科研数据集", "元数据索引", "官方链接"),
    "tco2e": ("二氧化碳当量", "44/12", "3.667"),
    "tc/ha": ("碳质量", "44/12", "3.667"),
}
CORE_DOMAIN_TERMS = (
    "hnblue", "mycarbonai", "蓝碳", "碳", "红树林", "海南", "生态", "模型", "数据",
    "平台", "系统", "政府", "来源", "追溯", "遥感", "无人机", "uav", "样地", "通量",
    "估值", "核证", "catboost", "shap", "baad", "api", "接口", "碳汇", "碳储量",
    "vm0033", "方法学", "湿地", "海草床", "global mangrove watch", "gmw",
    "ocr", "图片", "影像", "野外调查", "对话", "记忆", "隐私", "会话", "虚拟样方",
    "虚拟样地", "情景模拟", "单位", "tco2e", "tc/ha", "外部数据集", "文件哈希",
    "知识库", "证据", "回答", "模拟", "代理", "示例", "页面", "上下文", "字段", "后端",
    "invest", "未来趋势", "审计", "安全", "敏感",
)

SOURCE_HINTS = (
    (r"来源|追溯|出处|最初来自", ("source_traceability",)),
    (r"回答顺序|说明口径|怎样回答", ("answering_policy",)),
    (r"(?:在线|独立).{0,8}shap|shap.{0,8}(?:api|端点|接口)", ("model_service_boundary",)),
    (r"模型.{0,12}(?:使用限制|有什么限制|核证)|预测结果.{0,12}限制|算法.{0,16}(?:核证|报告)", ("model_service_boundary",)),
    (r"政府材料|正式政策材料|治理边界", ("governance_answering_principles",)),
    (r"碳价|市场价", ("answering_policy", "carbon_valuation_boundary")),
    (r"baad", ("dataset_origin",)),
    (r"ocr|图片识别|扫描件|原图", ("image_ocr",)),
    (r"无人机|uav|野外调查|样方照片|调查影像", ("image_ocr_uav_field_assets", "external_datasets_and_hainan_research")),
    (r"对话记忆|聊天历史|会话历史|长期记忆|隐私|删除测试会话", ("conversation_memory_privacy",)),
    (r"虚拟样方|虚拟样地|情景模拟|冒充实测", ("virtual_plots_simulation_boundary", "policy_simulation_plan")),
    (r"对外演示|模拟.{0,12}代理|代理.{0,12}示例|哪些内容.{0,12}标注", ("virtual_plots_simulation_boundary", "evidence_answering_and_source_traceability")),
    (r"tc/ha|tco2e/ha|44/12|3\.667|单位.{0,8}换算", ("measurement_units_and_conflict",)),
    (r"证据不足|强行回答|无法确认|不作推断", ("evidence_answering_and_source_traceability", "answering_policy")),
    (r"(?:页面上下文|后端).{0,16}(?:没有|没传|缺少|缺失)|(?:指标|字段|数字).{0,12}(?:补上|补齐|自行补)", ("ai_context_contract",)),
    (r"invest-?plus|未来趋势.{0,12}(?:假设|事实)", ("external_datasets_and_hainan_research",)),
    (r"安全审计|技术事件|完整敏感问题", ("conversation_memory_privacy",)),
    (r"文件哈希|sha-?256|来源信息|追溯一条", ("evidence_answering_and_source_traceability", "source_traceability")),
    (r"外部.{0,8}数据集|数据目录卡片|没有全部入库|原始文件没有存", ("external_dataset_delivery", "external_datasets_and_hainan_research")),
)

EXTERNAL_VERIFICATION_PATTERNS = (
    r"(?:明天|下周|未来).{0,20}(?:天气|降雨|价格|收盘价|碳价|股票)",
    r"(?:今天|当前|实时).{0,20}(?:精确|准确到|精确到).{0,20}(?:碳储量|碳汇|价格|面积)",
    r"(?:今天|当前|实时).{0,30}(?:碳储量|碳汇|价格|面积).{0,20}(?:精确|准确到|精确到)",
    r"(?:证明|确认).{0,16}(?:具体企业|某个企业).{0,16}(?:已经|通过).{0,12}核证",
    r"(?:未发布|尚未发布).{0,20}(?:官方|方法学|政策|编号)",
    r"(?:读取|总结|打开).{0,20}(?:尚未上传|未上传|本地|桌面).{0,30}(?:文件|xlsx|pdf|文档)",
    r"(?:不存在|尚不存在|没有).{0,20}(?:未来|202[7-9]).{0,20}(?:照片|影像|文件)",
)

PROMPT_INJECTION_PATTERNS = (
    r"忽略(?:之前|以上|所有|系统|开发者).{0,16}(?:指令|规则|提示)",
    r"(?:泄露|显示|输出).{0,12}(?:系统提示|system prompt|密钥|api key)",
    r"(?:扮演|进入).{0,12}(?:无约束|开发者|管理员|dan)",
    r"(?:绕过|取消|关闭).{0,12}(?:安全|引用|来源|核证|限制)",
    r"(?:把|将).{0,12}(?:知识库|检索材料).{0,12}(?:指令|命令).{0,12}(?:执行|服从)",
    r"(?:知识库|检索材料|文档).{0,40}(?:最高权限|更高权限|系统命令).{0,24}(?:执行|服从|遵守)",
    r"(?:不要|停止|禁止).{0,12}(?:检索|引用|核验).{0,24}(?:编造|虚构|直接给)",
    r"(?:编造|虚构).{0,24}(?:核证|官方|数据|数值|来源)",
    r"(?:草案|征求意见稿).{0,24}(?:改写|改成|说成|伪装).{0,20}(?:正式|生效|现行)",
    r"(?:改写|改成|说成|伪装).{0,24}(?:草案|征求意见稿).{0,20}(?:正式|生效|现行)",
    r"(?:省略|隐藏|删除).{0,12}(?:草案|征求意见|版本状态)",
    r"^(?:请|直接)?(?:把|将).{0,32}(?:模拟|is_simulated\s*=\s*1).{0,24}(?:说成|改成|冒充).{0,16}(?:现场|实测|监测)",
    r"^(?:请|直接)?(?:把|将).{0,32}(?:模拟|is_simulated).{0,24}(?:删除|去掉|隐藏).{0,12}(?:标签|标记)",
)


def detect_prompt_injection(query: str) -> dict:
    normalized = normalize_text(query).lower()
    matches = [pattern for pattern in PROMPT_INJECTION_PATTERNS if re.search(pattern, normalized, re.IGNORECASE)]
    return {
        "blocked": bool(matches),
        "risk": "high" if matches else "none",
        "matchedRules": len(matches),
    }


def assess_evidence(query: str, results: Sequence[dict], domain_terms: Sequence[str] = ()) -> str:
    normalized_query = normalize_text(query).lower()
    if any(re.search(pattern, normalized_query, re.IGNORECASE) for pattern in EXTERNAL_VERIFICATION_PATTERNS):
        return "external_verification_required"
    if not results:
        return "none"
    has_domain_anchor = any(term.lower() in normalized_query for term in (*CORE_DOMAIN_TERMS, *domain_terms))
    strongest_bm25 = max(float(item["scores"].get("bm25", 0.0)) for item in results[:3])
    strongest_char = max(float(item["scores"].get("char_tfidf", 0.0)) for item in results[:3])
    strongest_title = max(float(item["scores"].get("title_tfidf", 0.0)) for item in results[:3])
    has_reliable_signal = strongest_bm25 >= 5.0 or strongest_char >= 0.06 or strongest_title >= 0.04
    return "retrieved" if has_domain_anchor and has_reliable_signal else "insufficient"


@dataclass(frozen=True)
class Chunk:
    chunk_id: str
    source_path: str
    title: str
    section: str
    content: str
    contextual_text: str
    previous_context: str
    next_context: str
    kind: str
    source_sha256: str

    def to_dict(self) -> dict:
        return asdict(self)


def normalize_text(value: str) -> str:
    value = unicodedata.normalize("NFKC", value or "")
    value = value.replace("\r\n", "\n").replace("\r", "\n")
    value = re.sub(r"[ \t]+", " ", value)
    value = re.sub(r"\n{3,}", "\n\n", value)
    return value.strip()


def load_domain_terms(source_root: Path) -> list[str]:
    path = source_root / "chat_ready_qa" / "knowledge_map_keywords.txt"
    if not path.exists():
        return []
    terms = [item.strip() for item in re.split(r"[,，\n]", path.read_text(encoding="utf-8"))]
    return sorted({term for term in terms if term}, key=lambda item: (-len(item), item))


def tokenize(text: str, domain_terms: Sequence[str] = ()) -> list[str]:
    normalized = normalize_text(text).lower()
    tokens = re.findall(r"[a-z][a-z0-9_.-]*|\d+(?:\.\d+)?", normalized)
    for sequence in re.findall(r"[\u3400-\u9fff]+", normalized):
        if len(sequence) == 1:
            tokens.append(sequence)
        else:
            tokens.extend(sequence[i : i + 2] for i in range(len(sequence) - 1))
            if len(sequence) >= 3:
                tokens.extend(sequence[i : i + 3] for i in range(len(sequence) - 2))
    for term in domain_terms:
        if term.lower() in normalized:
            tokens.append(f"kw:{term.lower()}")
    for key, aliases in DOMAIN_ALIASES.items():
        if key.lower() in normalized:
            tokens.extend(f"alias:{item.lower()}" for item in aliases)
    return tokens


def markdown_sections(text: str, fallback_title: str) -> tuple[str, list[tuple[str, str]]]:
    heading_stack: list[str] = []
    document_title = fallback_title
    current_lines: list[str] = []
    sections: list[tuple[str, str]] = []

    def flush() -> None:
        content = normalize_text("\n".join(current_lines))
        if content:
            sections.append((" > ".join(heading_stack) or document_title, content))
        current_lines.clear()

    for raw_line in text.splitlines():
        match = re.match(r"^(#{1,6})\s+(.+?)\s*$", raw_line)
        if not match:
            current_lines.append(raw_line)
            continue
        flush()
        level = len(match.group(1))
        heading = normalize_text(match.group(2))
        if level == 1 and document_title == fallback_title:
            document_title = heading
        heading_stack[:] = heading_stack[: level - 1]
        while len(heading_stack) < level - 1:
            heading_stack.append(document_title)
        heading_stack.append(heading)
    flush()
    return document_title, sections


def split_paragraphs(content: str) -> list[str]:
    paragraphs = [normalize_text(item) for item in re.split(r"\n\s*\n", content)]
    return [item for item in paragraphs if item]


def _paragraph_similarities(paragraphs: Sequence[str]) -> list[float]:
    if len(paragraphs) < 2:
        return []
    try:
        matrix = TfidfVectorizer(analyzer="char", ngram_range=(2, 4), min_df=1).fit_transform(paragraphs)
    except ValueError:
        return [0.0] * (len(paragraphs) - 1)
    return [float(cosine_similarity(matrix[i], matrix[i + 1])[0, 0]) for i in range(len(paragraphs) - 1)]


def semantic_groups(paragraphs: Sequence[str], target_chars: int = 520, max_chars: int = 820) -> list[list[str]]:
    if not paragraphs:
        return []
    if sum(len(item) for item in paragraphs) <= max_chars:
        return [list(paragraphs)]
    similarities = _paragraph_similarities(paragraphs)
    positive = [value for value in similarities if value > 0]
    threshold = float(np.quantile(positive, 0.35)) if positive else 0.0
    groups: list[list[str]] = []
    current: list[str] = []
    current_chars = 0
    for index, paragraph in enumerate(paragraphs):
        previous_similarity = similarities[index - 1] if index > 0 else 1.0
        semantic_break = index > 0 and previous_similarity < threshold and current_chars >= target_chars // 2
        size_break = current and current_chars + len(paragraph) > max_chars
        if semantic_break or size_break:
            groups.append(current)
            current = []
            current_chars = 0
        current.append(paragraph)
        current_chars += len(paragraph)
        if current_chars >= target_chars and index < len(paragraphs) - 1:
            next_similarity = similarities[index]
            if next_similarity <= threshold:
                groups.append(current)
                current = []
                current_chars = 0
    if current:
        groups.append(current)
    return groups


def _context_excerpt(value: str, limit: int = 120) -> str:
    compact = re.sub(r"\s+", " ", value).strip()
    return compact[:limit]


def _read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8-sig", errors="replace")


def build_chunks(source_root: Path) -> tuple[list[Chunk], dict]:
    source_root = source_root.resolve()
    domain_terms = load_domain_terms(source_root)
    chunks: list[Chunk] = []
    exclusions: list[dict] = []
    indexed_files: list[dict] = []

    for path in sorted(source_root.rglob("*")):
        if not path.is_file() or path.suffix.lower() not in SUPPORTED_EXTENSIONS:
            continue
        relative = path.relative_to(source_root).as_posix()
        if relative in EXCLUDED_PATHS:
            exclusions.append({"source_path": relative, "reason": EXCLUDED_PATHS[relative]})
            continue
        raw = _read_text(path)
        source_hash = hashlib.sha256(raw.encode("utf-8")).hexdigest()
        fallback_title = path.stem.replace("_", " ")
        if path.suffix.lower() == ".md":
            title, sections = markdown_sections(raw, fallback_title)
        else:
            title, sections = fallback_title, [(fallback_title, normalize_text(raw))]
        indexed_files.append({"source_path": relative, "sha256": source_hash, "characters": len(raw)})

        for section, section_content in sections:
            if not section_content:
                continue
            paragraphs = split_paragraphs(section_content)
            atomic = path.suffix.lower() in ATOMIC_EXTENSIONS
            groups = [paragraphs] if atomic else semantic_groups(paragraphs)
            for group_index, group in enumerate(groups):
                content = "\n\n".join(group).strip()
                if not content:
                    continue
                previous_context = ""
                next_context = ""
                if group_index > 0:
                    previous_context = _context_excerpt(" ".join(groups[group_index - 1][-1:]))
                if group_index + 1 < len(groups):
                    next_context = _context_excerpt(" ".join(groups[group_index + 1][:1]))
                prefix_parts = [f"文档：{title}", f"章节：{section}"]
                if previous_context:
                    prefix_parts.append(f"上文：{previous_context}")
                if next_context:
                    prefix_parts.append(f"下文：{next_context}")
                contextual_text = "\n".join(prefix_parts) + "\n正文：" + content
                digest = hashlib.sha256(f"{relative}\0{section}\0{content}".encode("utf-8")).hexdigest()[:16]
                chunks.append(Chunk(
                    chunk_id=f"rag-{digest}",
                    source_path=relative,
                    title=title,
                    section=section,
                    content=content,
                    contextual_text=contextual_text,
                    previous_context=previous_context,
                    next_context=next_context,
                    kind=path.suffix.lower().lstrip("."),
                    source_sha256=source_hash,
                ))

    manifest = {
        "schema_version": 1,
        "source_root": source_root.name,
        "source_files": len(indexed_files),
        "chunks": len(chunks),
        "domain_terms": len(domain_terms),
        "indexed_files": indexed_files,
        "excluded_files": exclusions,
        "chunking": {
            "strategy": "markdown headings plus adjacency semantic boundaries",
            "target_chars": 520,
            "max_chars": 820,
            "atomic_extensions": sorted(ATOMIC_EXTENSIONS),
            "context": "document title, heading path, previous and next paragraph excerpts",
        },
    }
    return chunks, manifest


class HybridRagIndex:
    def __init__(self, chunks: Sequence[Chunk], domain_terms: Sequence[str] = ()):
        if not chunks:
            raise ValueError("RAG index requires at least one chunk")
        self.chunks = list(chunks)
        self.domain_terms = list(domain_terms)
        self.texts = [chunk.contextual_text for chunk in self.chunks]
        self.tokens = [tokenize(text, self.domain_terms) for text in self.texts]
        self.doc_lengths = np.array([max(1, len(tokens)) for tokens in self.tokens], dtype=float)
        self.avg_doc_length = float(self.doc_lengths.mean())
        self.term_frequencies = [Counter(tokens) for tokens in self.tokens]
        document_frequency: Counter[str] = Counter()
        for tokens in self.tokens:
            document_frequency.update(set(tokens))
        total = len(self.chunks)
        self.bm25_idf = {
            term: math.log(1.0 + (total - count + 0.5) / (count + 0.5))
            for term, count in document_frequency.items()
        }
        self.char_vectorizer = TfidfVectorizer(analyzer="char", ngram_range=(2, 4), min_df=1, sublinear_tf=True)
        self.char_matrix = self.char_vectorizer.fit_transform(self.texts)
        self.title_texts = [f"{chunk.title} {chunk.section} {chunk.source_path}" for chunk in self.chunks]
        self.title_vectorizer = TfidfVectorizer(analyzer="char", ngram_range=(2, 4), min_df=1, sublinear_tf=True)
        self.title_matrix = self.title_vectorizer.fit_transform(self.title_texts)
        components = min(48, self.char_matrix.shape[0] - 1, self.char_matrix.shape[1] - 1)
        self.svd = None
        self.normalizer = None
        self.semantic_matrix = None
        if components >= 2:
            self.svd = TruncatedSVD(n_components=components, random_state=42)
            self.normalizer = Normalizer(copy=False)
            self.semantic_matrix = self.normalizer.fit_transform(self.svd.fit_transform(self.char_matrix))

    @classmethod
    def from_jsonl(cls, path: Path, domain_terms: Sequence[str] = ()) -> "HybridRagIndex":
        chunks = []
        with path.open("r", encoding="utf-8") as handle:
            for line in handle:
                if line.strip():
                    chunks.append(Chunk(**json.loads(line)))
        return cls(chunks, domain_terms)

    def bm25_scores(self, query: str, k1: float = 1.5, b: float = 0.75) -> np.ndarray:
        query_terms = tokenize(query, self.domain_terms)
        scores = np.zeros(len(self.chunks), dtype=float)
        for index, frequencies in enumerate(self.term_frequencies):
            length_factor = k1 * (1.0 - b + b * self.doc_lengths[index] / self.avg_doc_length)
            score = 0.0
            for term in query_terms:
                frequency = frequencies.get(term, 0)
                if frequency:
                    score += self.bm25_idf.get(term, 0.0) * frequency * (k1 + 1.0) / (frequency + length_factor)
            scores[index] = score
        return scores

    def component_scores(self, query: str) -> dict[str, np.ndarray]:
        char_query = self.char_vectorizer.transform([normalize_text(query)])
        char_scores = cosine_similarity(char_query, self.char_matrix).ravel()
        title_query = self.title_vectorizer.transform([normalize_text(query)])
        title_scores = cosine_similarity(title_query, self.title_matrix).ravel()
        if self.svd is None or self.semantic_matrix is None:
            semantic_scores = np.zeros(len(self.chunks), dtype=float)
        else:
            semantic_query = self.normalizer.transform(self.svd.transform(char_query))
            semantic_scores = cosine_similarity(semantic_query, self.semantic_matrix).ravel()
        return {
            "bm25": self.bm25_scores(query),
            "char_tfidf": char_scores,
            "semantic_lsa": semantic_scores,
            "title_tfidf": title_scores,
        }

    @staticmethod
    def _rrf(scores: dict[str, np.ndarray], weights: dict[str, float], constant: int = 50) -> np.ndarray:
        size = len(next(iter(scores.values())))
        fused = np.zeros(size, dtype=float)
        for name, values in scores.items():
            order = np.argsort(-values, kind="stable")
            ranks = np.empty(size, dtype=int)
            ranks[order] = np.arange(1, size + 1)
            fused += weights.get(name, 1.0) / (constant + ranks)
        return fused

    def search(self, query: str, top_k: int = 5, mode: str = "hybrid", diversify: bool = True) -> list[dict]:
        query = normalize_text(query)
        if not query:
            return []
        components = self.component_scores(query)
        if mode == "hybrid":
            final_scores = self._rrf(components, {
                "bm25": 1.0,
                "char_tfidf": 0.85,
                "semantic_lsa": 0.8,
                "title_tfidf": 0.65,
            })
            lowered_query = query.lower()
            for pattern, path_hints in SOURCE_HINTS:
                if not re.search(pattern, lowered_query, re.IGNORECASE):
                    continue
                for index, chunk in enumerate(self.chunks):
                    if any(hint in chunk.source_path.lower() for hint in path_hints):
                        final_scores[index] += 0.012
        elif mode in components:
            final_scores = components[mode]
        else:
            raise ValueError(f"Unknown retrieval mode: {mode}")
        order = np.argsort(-final_scores, kind="stable")
        if re.search(r"冲突|版本|正式有效|替代|还是|(?:v?\d+(?:\.\d+)?).{0,12}(?:v?\d+(?:\.\d+)?)", query, re.IGNORECASE):
            diversify = False
        selected: list[int] = []
        used_sources: set[str] = set()
        if diversify:
            for index in order:
                source = self.chunks[int(index)].source_path
                if source in used_sources:
                    continue
                selected.append(int(index))
                used_sources.add(source)
                if len(selected) >= top_k:
                    break
        for index in order:
            if len(selected) >= top_k:
                break
            if int(index) not in selected:
                selected.append(int(index))

        results = []
        for rank, index in enumerate(selected, start=1):
            chunk = self.chunks[index]
            results.append({
                **chunk.to_dict(),
                "rank": rank,
                "score": round(float(final_scores[index]), 8),
                "scores": {name: round(float(values[index]), 8) for name, values in components.items()},
            })
        return results

    def answer(self, query: str, top_k: int = 4) -> dict:
        injection = detect_prompt_injection(query)
        if injection["blocked"]:
            return {
                "answer": "检测到试图绕过来源、引用或安全边界的提示。HNBLUE 碳助手不会执行该指令；请改为直接询问蓝碳、数据来源、模型或平台使用问题。",
                "sources": [],
                "evidenceStatus": "blocked_prompt_injection",
                "injection": injection,
            }
        results = self.search(query, top_k=top_k, mode="hybrid", diversify=True)
        if not results:
            return {"answer": "知识库中没有检索到可用资料。", "sources": [], "evidenceStatus": "none"}
        evidence_status = assess_evidence(query, results, self.domain_terms)
        if evidence_status != "retrieved":
            return {
                "answer": "当前 HNBLUE 知识库中没有可直接支持该结论的可靠资料，或该问题需要外部实时/正式核验，暂不生成推断性回答。",
                "sources": [],
                "evidenceStatus": evidence_status,
                "retrieval": results,
            }
        evidence = []
        for item in results[:3]:
            excerpt = re.sub(r"\s+", " ", item["content"]).strip()
            evidence.append(f"- **{item['title']}｜{item['section']}**：{excerpt[:320]}")
        answer = "根据 HNBLUE 本地知识库检索到的资料：\n\n" + "\n".join(evidence)
        answer += "\n\n以上为知识库原文摘录式回答；涉及实时数值、最新政策或正式核证时，应再以数据库和主管部门材料复核。"
        sources = [{
            "title": item["title"],
            "chunkSource": item["source_path"],
            "recordId": item["chunk_id"],
            "section": item["section"],
            "score": item["score"],
        } for item in results[:3]]
        return {"answer": answer, "sources": sources, "evidenceStatus": "retrieved", "retrieval": results}

    def context(self, query: str, top_k: int = 5) -> dict:
        injection = detect_prompt_injection(query)
        if injection["blocked"]:
            return {
                "results": [],
                "sources": [],
                "evidenceStatus": "blocked_prompt_injection",
                "injection": injection,
            }
        results = self.search(query, top_k=top_k, mode="hybrid", diversify=True)
        status = assess_evidence(query, results, self.domain_terms)
        accepted = results if status == "retrieved" else []
        sources = []
        for item in accepted:
            url_match = re.search(r"https://[^\s)）]+", item["content"])
            excerpt = re.sub(r"\s+", " ", item["content"]).strip()[:420]
            sources.append({
                "title": item["title"],
                "chunkSource": item["source_path"],
                "recordId": item["chunk_id"],
                "section": item["section"],
                "score": item["score"],
                "url": url_match.group(0).rstrip(".,，。") if url_match else "",
                "excerpt": excerpt,
            })
        return {
            "results": accepted,
            "sources": sources,
            "evidenceStatus": status,
            "injection": injection,
        }


def write_artifacts(source_root: Path, output_dir: Path) -> dict:
    chunks, manifest = build_chunks(source_root)
    output_dir.mkdir(parents=True, exist_ok=True)
    chunks_path = output_dir / "chunks.jsonl"
    with chunks_path.open("w", encoding="utf-8", newline="\n") as handle:
        for chunk in chunks:
            handle.write(json.dumps(chunk.to_dict(), ensure_ascii=False, separators=(",", ":")) + "\n")
    manifest["chunks_sha256"] = hashlib.sha256(chunks_path.read_bytes()).hexdigest()
    manifest_path = output_dir / "index_manifest.json"
    manifest_path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return manifest

const fs = require('fs');
const path = require('path');

// 配色
const C = {
    primary: '1a1a2e',
    secondary: '667eea', 
    accent: '00d4ff',
    highlight: 'e94560',
    gold: 'ffd700',
    white: 'FFFFFF'
};

// 幻灯片内容
const slides = [
    { type: 'title', title: 'VisionVoice 音素识别系统', subtitle: '端侧AI发音评估技术详解', footer: '2024 IEEE APCCAS' },
    { type: 'section', num: '01', title: '项目概述与核心挑战' },
    { type: 'content', title: 'VisionVoice 简介', bullets: ['核心目标：零网络延迟、高隐私安全的专业级发音评估', '技术方案：端侧离线推理，无需网络连接', '应用场景：AR物体识别 → 单词学习 → 发音练习', '性能指标：推理时间 < 320ms，模型体积 ~300MB'] },
    { type: 'content', title: '核心技术栈', bullets: ['语音模型：Wav2Vec2-XLS-R 300M（多语言音素识别）', '推理引擎：ONNX Runtime Mobile（端侧高效推理）', '对齐算法：Needleman-Wunsch（序列动态规划）', '评分系统：非线性分段映射（心理学反馈曲线）'] },
    { type: 'content', title: '挑战一：模型领域错位', subtitle: 'ASR模型输出字母，评估基准使用音标', bullets: ['输入音频 → Wav2Vec2-base → "A-P-P-L-E" (英文字母)', '标准答案 → CMU Dict → "AE P L" (Arpabet音素)', '问题：跨维度特征不匹配，无法对齐', '结果：对所有发音均打出 0分'] },
    { type: 'content', title: '挑战二：符号域冲突', subtitle: 'Unicode IPA vs ASCII Arpabet', bullets: ['词典路径：ɹ, æ, ɔ (Unicode 国际音标)', 'AI模型路径：r, ae, ao (ASCII Arpabet音标)', 'Java字符串比较："ɹ".equals("r") → false', '即使发音正确，系统判定为错读！'] },
    { type: 'content', title: '挑战三：声学物理干扰', subtitle: '清塞音气流冲击麦克风', bullets: ['用户发音 /k/, /p/, /t/ → 瞬间释放高压气流', '冲击手机麦克风振膜 → 非线性物理失真', 'AI提取特征偏离声带振动', '结果：被识别为 /hh/ 而非 /k/'] },
    { type: 'section', num: '02', title: '声学模型选型演进' },
    { type: 'compare', title: '初代方案 vs 重构方案', left: { title: '❌ Wav2Vec2-Base', items: ['参数量：95M', '输出：英文字母 (A-Z)', '问题：无法与音标对齐', '结果：测试0分'] }, right: { title: '✅ Wav2Vec2-XLS-R', items: ['参数量：300M', '输出：Arpabet音素 (40类)', '优势：直接输出音标特征', '结果：重构成功'] } },
    { type: 'content', title: '端侧性能优化', subtitle: 'INT8动态量化：体积压缩75%', bullets: ['原始模型 (FP32)：1.2 GB', '优化模型 (INT8)：~300 MB', '完美适配移动端内存限制', '避免Android OOM异常', '推理精度衰减 < 1%'] },
    { type: 'section', num: '03', title: '信号预处理与CTC解码' },
    { type: 'content', title: 'AGC自动增益控制', subtitle: '防"AI幻觉"拦截机制', bullets: ['问题：用户未发声时，模型将噪音解码为无意义音素', '算法：计算PCM数据峰值振幅', 'if 振幅 < 0.015 → 拦截推理 → 判定为全局漏读', 'else → 执行归一化处理'] },
    { type: 'content', title: 'CTC解码去噪', subtitle: '过滤非发音特征', bullets: ['h# (静音帧) → 过滤', 'spn (环境杂音) → 过滤', '[UNK] (未知符号) → 过滤', '确保送入对比算法的数据是纯净的真实音素序列'] },
    { type: 'section', num: '04', title: '双重归一化架构' },
    { type: 'content', title: '巴别塔难题', subtitle: '完全正确的发音被判定为错读', bullets: ['UI显示：标准音标 i ← 完全匹配 → i :识别音标', '判定结果：❌ 错读 (标红)', '根本原因：Unicode与ASCII编码不同', 'Java字符串比较失败导致伪错误'] },
    { type: 'content', title: '双层翻译机制', subtitle: '彻底剥离比对逻辑与渲染逻辑', bullets: ['Level 1 底层降维：Unicode IPA → ASCII Arpabet', 'Level 2 对齐运算：Needleman-Wunsch Alignment', 'Level 3 表层升维：ASCII Arpabet → Unicode IPA', '核心思想：算法用Arpabet计算，UI显示国际音标'] },
    { type: 'section', num: '05', title: '序列对齐与评分算法' },
    { type: 'content', title: 'Needleman-Wunsch全局对齐', subtitle: '定制化序列对齐算法', bullets: ['MATCH = +1.0 (完全匹配)', 'FLAW = +0.5 (发音瑕疵)', 'MISMATCH = -1.0 (错读)', 'GAP = -1.0 (漏读/多读)'] },
    { type: 'content', title: '非线性评分曲线', subtitle: '基于教学反馈心理学的分段映射', bullets: ['< 0.5：严格惩罚区 (0-59分)', '0.5-0.8：进步空间大 (60-89分) ← 激励学习', '≥ 0.8：优秀鼓励区 (90-100分)', '核心思想：让微小进步也能被感知'] },
    { type: 'section', num: '06', title: '声学容错矩阵' },
    { type: 'content', title: 'Plosive Puff Effect', subtitle: '物理抗噪：爆破音气流干扰', bullets: ['问题：清塞音被识别为 /hh/', '物理原因：高压气流冲击麦克风振膜', '工程解决：硬编码豁免路径', '效果：k → hh 判定为完全匹配'] },
    { type: 'content', title: 'SLA理论分级惩罚', subtitle: '基于第二语言习得理论', bullets: ['一级：完全豁免 (100分) - 长短元音偏差', '二级：发音瑕疵 (60分) - 梅花音张口不足', '三级：严重错读 (0分) - 破坏语义', '核心：可理解度 > 母语者口音'] },
    { type: 'section', num: '07', title: '性能指标与总结' },
    { type: 'content', title: '性能指标', subtitle: '端侧推理效率', bullets: ['模型加载时间：~800ms (mmap内存映射)', '推理时间：~320ms (1秒音频)', 'Java堆占用：~11MB', '模型体积：~300MB (INT8量化)'] },
    { type: 'content', title: '技术架构总览', bullets: ['用户音频 → AudioProcessor (解码+重采样)', '→ Wav2Vec2Scorer (ONNX推理+CTC解码)', '→ 双重归一化 (IPA ↔ Arpabet)', '→ Needleman-Wunsch + 三级容错矩阵', '→ 最终评分 (0-100分)'] },
    { type: 'content', title: '核心创新总结', bullets: ['#1 模型领域适配：Wav2Vec2-XLS-R + INT8量化', '#2 符号域统一：双重归一化架构', '#3 物理补偿：三级声学容错矩阵'] },
    { type: 'content', title: '项目成果', bullets: ['✅ 零网络延迟：端侧离线推理', '✅ 高隐私安全：音频数据不出设备', '✅ 媲美云端精度：双重归一化 + 容错矩阵', '✅ 移动端适配：300MB模型 + 11MB内存'] },
    { type: 'thanks', title: '谢谢！', subtitle: 'VisionVoice - 让每个学习者都能获得专业的发音指导' }
];

// 创建PPTX XML
function createSlideXML(s, idx) {
    const slideXML = `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sld xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main" xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main">
  <p:cSld>
    <p:bg>
      <p:bgPr>
        <a:solidFill><a:srgbClr val="${s.type === 'section' ? '0f3460' : '1a1a2e'}"/></a:solidFill>
      </p:bgPr>
    </p:bg>
    <p:spTree>
      <p:nvGrpSpPr><p:cNvPr id="0"/><p:nvPr/></p:nvGrpSpPr>
      <p:grpSpPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="0" cy="0"/><a:chOff x="0" y="0"/><a:chExt cx="0" cy="0"/></a:xfrm></p:grpSpPr>
${createContent(s)}
    </p:spTree>
  </p:cSld>
  <p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr>
</p:sld>`;
    return slideXML;
}

function createContent(s) {
    if (s.type === 'title') {
        return `      <p:sp>
        <p:nvSpPr><p:cNvPr id="1"/><p:nvPr/></p:nvSpPr>
        <p:spPr><a:xfrm><a:off x="838200" y="2743200"/><a:ext cx="11388600" cy="1200000"/></a:xfrm></p:spPr>
        <p:txBody><a:bodyPr/><a:lstStyle/><a:p><a:pPr algn="ctr"/><a:r><a:rPr lang="zh-CN" sz="4400" b="1"><a:solidFill><a:srgbClr val="${C.accent}"/></a:solidFill></a:rPr><a:t>${escape(s.title)}</a:t></a:r></a:p></p:txBody>
      </p:sp>
      <p:sp>
        <p:nvSpPr><p:cNvPr id="2"/><p:nvPr/></p:nvSpPr>
        <p:spPr><a:xfrm><a:off x="838200" y="4000000"/><a:ext cx="11388600" cy="800000"/></a:xfrm></p:spPr>
        <p:txBody><a:bodyPr/><a:lstStyle/><a:p><a:pPr algn="ctr"/><a:r><a:rPr lang="zh-CN" sz="2400"><a:solidFill><a:srgbClr val="${C.highlight}"/></a:solidFill></a:rPr><a:t>${escape(s.subtitle)}</a:t></a:r></a:p></p:txBody>
      </p:sp>
      <p:sp>
        <p:nvSpPr><p:cNvPr id="3"/><p:nvPr/></p:nvSpPr>
        <p:spPr><a:xfrm><a:off x="838200" y="5000000"/><a:ext cx="11388600" cy="600000"/></a:xfrm></p:spPr>
        <p:txBody><a:bodyPr/><a:lstStyle/><a:p><a:pPr algn="ctr"/><a:r><a:rPr lang="zh-CN" sz="1200"><a:solidFill><a:srgbClr val="888888"/></a:solidFill></a:rPr><a:t>${escape(s.footer || '')}</a:t></a:r></a:p></p:txBody>
      </p:sp>`;
    }
    
    if (s.type === 'section') {
        return `      <p:sp>
        <p:nvSpPr><p:cNvPr id="1"/><p:nvPr/></p:nvSpPr>
        <p:spPr><a:xfrm><a:off x="4000000" y="1500000"/><a:ext cx="4000000" cy="1500000"/></a:xfrm></p:spPr>
        <p:txBody><a:bodyPr/><a:lstStyle/><a:p><a:pPr algn="ctr"/><a:r><a:rPr lang="en-US" sz="9600" b="1"><a:solidFill><a:srgbClr val="${C.highlight}"/></a:solidFill></a:rPr><a:t>${s.num}</a:t></a:r></a:p></p:txBody>
      </p:sp>
      <p:sp>
        <p:nvSpPr><p:cNvPr id="2"/><p:nvPr/></p:nvSpPr>
        <p:spPr><a:xfrm><a:off x="838200" y="3500000"/><a:ext cx="11388600" cy="900000"/></a:xfrm></p:spPr>
        <p:txBody><a:bodyPr/><a:lstStyle/><a:p><a:pPr algn="ctr"/><a:r><a:rPr lang="zh-CN" sz="3200"><a:solidFill><a:srgbClr val="${C.white}"/></a:solidFill></a:rPr><a:t>${escape(s.title)}</a:t></a:r></a:p></p:txBody>
      </p:sp>`;
    }
    
    if (s.type === 'content') {
        const bullets = s.bullets.map((b, i) => {
            const bulletChar = b.startsWith('✅') || b.startsWith('#') || b.startsWith('→') ? '' : '• ';
            return `<a:p><a:pPr marL="457200" indent="-457200"/><a:r><a:rPr lang="zh-CN" sz="1800"><a:solidFill><a:srgbClr val="${C.white}"/></a:solidFill></a:rPr><a:t>${bulletChar}${escape(b)}</a:t></a:r></a:p>`;
        }).join('\n');
        
        return `      <p:sp>
        <p:nvSpPr><p:cNvPr id="1"/><p:nvPr/></p:nvSpPr>
        <p:spPr><a:xfrm><a:off x="838200" y="500000"/><a:ext cx="11388600" cy="700000"/></a:xfrm></p:spPr>
        <p:txBody><a:bodyPr/><a:lstStyle/><a:p><a:r><a:rPr lang="zh-CN" sz="2800" b="1"><a:solidFill><a:srgbClr val="${C.accent}"/></a:solidFill></a:rPr><a:t>${escape(s.title)}</a:t></a:r></a:p></p:txBody>
      </p:sp>
      ${s.subtitle ? `<p:sp>
        <p:nvSpPr><p:cNvPr id="2"/><p:nvPr/></p:nvSpPr>
        <p:spPr><a:xfrm><a:off x="838200" y="1200000"/><a:ext cx="11388600" cy="500000"/></a:xfrm></p:spPr>
        <p:txBody><a:bodyPr/><a:lstStyle/><a:p><a:r><a:rPr lang="zh-CN" sz="1600"><a:solidFill><a:srgbClr val="${C.gold}"/></a:solidFill></a:rPr><a:t>${escape(s.subtitle)}</a:t></a:r></a:p></p:txBody>
      </p:sp>` : ''}
      <p:sp>
        <p:nvSpPr><p:cNvPr id="3"/><p:nvPr/></p:nvSpPr>
        <p:spPr><a:xfrm><a:off x="838200" y="${s.subtitle ? '1800000' : '1400000'}"/><a:ext cx="11388600" cy="3500000"/></a:xfrm></p:spPr>
        <p:txBody><a:bodyPr/><a:lstStyle/>
${bullets}
        </p:txBody>
      </p:sp>`;
    }
    
    if (s.type === 'compare') {
        const leftItems = s.left.items.map(b => `<a:p><a:r><a:rPr lang="zh-CN" sz="1600"><a:solidFill><a:srgbClr val="${C.white}"/></a:solidFill></a:rPr><a:t>• ${escape(b)}</a:t></a:r></a:p>`).join('\n');
        const rightItems = s.right.items.map(b => `<a:p><a:r><a:rPr lang="zh-CN" sz="1600"><a:solidFill><a:srgbClr val="${C.white}"/></a:solidFill></a:rPr><a:t>• ${escape(b)}</a:t></a:r></a:p>`).join('\n');
        
        return `      <p:sp>
        <p:nvSpPr><p:cNvPr id="1"/><p:nvPr/></p:nvSpPr>
        <p:spPr><a:xfrm><a:off x="838200" y="500000"/><a:ext cx="11388600" cy="700000"/></a:xfrm></p:spPr>
        <p:txBody><a:bodyPr/><a:lstStyle/><a:p><a:r><a:rPr lang="zh-CN" sz="2800" b="1"><a:solidFill><a:srgbClr val="${C.accent}"/></a:solidFill></a:rPr><a:t>${escape(s.title)}</a:t></a:r></a:p></p:txBody>
      </p:sp>
      <p:sp>
        <p:nvSpPr><p:cNvPr id="2"/><p:nvPr/></p:nvSpPr>
        <p:spPr><a:xfrm><a:off x="500000" y="1400000"/><a:ext cx="5400000" cy="3000000"/></a:xfrm><a:prstGeom prst="roundRect"><a:avLst><a:gd name="adj" fmla="val 10000"/></a:avLst></a:prstGeom><a:solidFill><a:srgbClr val="442222"/></a:solidFill><a:ln w="28575"><a:solidFill><a:srgbClr val="${C.highlight}"/></a:solidFill></a:ln></p:spPr>
        <p:txBody><a:bodyPr/><a:lstStyle/><a:p><a:pPr algn="ctr"/><a:r><a:rPr lang="zh-CN" sz="2000" b="1"><a:solidFill><a:srgbClr val="${C.highlight}"/></a:solidFill></a:rPr><a:t>${escape(s.left.title)}</a:t></a:r></a:p>${leftItems}</p:txBody>
      </p:sp>
      <p:sp>
        <p:nvSpPr><p:cNvPr id="3"/><p:nvPr/></p:nvSpPr>
        <p:spPr><a:xfrm><a:off x="6100000" y="1400000"/><a:ext cx="5400000" cy="3000000"/></a:xfrm><a:prstGeom prst="roundRect"><a:avLst><a:gd name="adj" fmla="val 10000"/></a:avLst></a:prstGeom><a:solidFill><a:srgbClr val="224422"/></a:solidFill><a:ln w="28575"><a:solidFill><a:srgbClr val="${C.accent}"/></a:solidFill></a:ln></p:spPr>
        <p:txBody><a:bodyPr/><a:lstStyle/><a:p><a:pPr algn="ctr"/><a:r><a:rPr lang="zh-CN" sz="2000" b="1"><a:solidFill><a:srgbClr val="${C.accent}"/></a:solidFill></a:rPr><a:t>${escape(s.right.title)}</a:t></a:r></a:p>${rightItems}</p:txBody>
      </p:sp>`;
    }
    
    if (s.type === 'thanks') {
        return `      <p:sp>
        <p:nvSpPr><p:cNvPr id="1"/><p:nvPr/></p:nvSpPr>
        <p:spPr><a:xfrm><a:off x="838200" y="2000000"/><a:ext cx="11388600" cy="1200000"/></a:xfrm></p:spPr>
        <p:txBody><a:bodyPr/><a:lstStyle/><a:p><a:pPr algn="ctr"/><a:r><a:rPr lang="zh-CN" sz="6000" b="1"><a:solidFill><a:srgbClr val="${C.accent}"/></a:solidFill></a:rPr><a:t>${escape(s.title)}</a:t></a:r></a:p></p:txBody>
      </p:sp>
      <p:sp>
        <p:nvSpPr><p:cNvPr id="2"/><p:nvPr/></p:nvSpPr>
        <p:spPr><a:xfrm><a:off x="838200" y="3400000"/><a:ext cx="11388600" cy="800000"/></a:xfrm></p:spPr>
        <p:txBody><a:bodyPr/><a:lstStyle/><a:p><a:pPr algn="ctr"/><a:r><a:rPr lang="zh-CN" sz="2000"><a:solidFill><a:srgbClr val="${C.highlight}"/></a:solidFill></a:rPr><a:t>${escape(s.subtitle)}</a:t></a:r></a:p></p:txBody>
      </p:sp>`;
    }
    
    return '';
}

function escape(str) {
    if (!str) return '';
    return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

// 创建PPTX文件结构
function createPPTX() {
    const outputDir = 'C:\\Users\\Dell\\.qclaw\\workspace\\VisionVoice_Phoneme.pptx';
    const tmpDir = 'C:\\Users\\Dell\\.qclaw\\workspace\\pptx_tmp';
    
    // 创建目录结构
    const dirs = [
        tmpDir,
        `${tmpDir}\\_rels`,
        `${tmpDir}\\docProps`,
        `${tmpDir}\\ppt`,
        `${tmpDir}\\ppt\\_rels`,
        `${tmpDir}\\ppt\\slideLayouts`,
        `${tmpDir}\\ppt\\slideMasters`,
        `${tmpDir}\\ppt\\slides`,
        `${tmpDir}\\ppt\\slides\\_rels`,
        `${tmpDir}\\ppt\\theme`
    ];
    
    dirs.forEach(d => {
        if (!fs.existsSync(d)) fs.mkdirSync(d, { recursive: true });
    });
    
    // [Content_Types].xml
    const contentTypes = `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/ppt/presentation.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml"/>
  ${slides.map((_, i) => `<Override PartName="/ppt/slides/slide${i+1}.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slide+xml"/>`).join('\n  ')}
  <Override PartName="/ppt/slideMasters/slideMaster1.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slideMaster+xml"/>
  <Override PartName="/ppt/theme/theme1.xml" ContentType="application/vnd.openxmlformats-officedocument.theme+xml"/>
  <Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/>
</Types>`;
    fs.writeFileSync(`${tmpDir}\\[Content_Types].xml`, contentTypes);
    
    // _rels/.rels
    const rels = `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="ppt/presentation.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/>
</Relationships>`;
    fs.writeFileSync(`${tmpDir}\\_rels\\.rels`, rels);
    
    // docProps/core.xml
    const core = `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dcterms="http://purl.org/dc/terms/">
  <dc:title>VisionVoice 音素识别系统</dc:title>
  <dc:creator>VisionVoice Team</dc:creator>
  <dcterms:created>2026-03-20T00:00:00Z</dcterms:created>
</cp:coreProperties>`;
    fs.writeFileSync(`${tmpDir}\\docProps\\core.xml`, core);
    
    // ppt/presentation.xml
    const presentation = `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:presentation xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main" sldMasterIdLst="" sldIdLst="">
  <p:sldMasterIdLst><p:sldMasterId id="2147483648" r:id="rId1" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"/></p:sldMasterIdLst>
  <p:sldIdLst>
    ${slides.map((_, i) => `<p:sldId id="${256+i}" r:id="rId${i+2}" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"/>`).join('\n    ')}
  </p:sldIdLst>
  <p:sz cx="12192000" cy="6858000"/>
</p:presentation>`;
    fs.writeFileSync(`${tmpDir}\\ppt\\presentation.xml`, presentation);
    
    // ppt/_rels/presentation.xml.rels
    const presRels = `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideMaster" Target="slideMasters/slideMaster1.xml"/>
  ${slides.map((_, i) => `<Relationship Id="rId${i+2}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slide" Target="slides/slide${i+1}.xml"/>`).join('\n  ')}
</Relationships>`;
    fs.writeFileSync(`${tmpDir}\\ppt\\_rels\\presentation.xml.rels`, presRels);
    
    // slideMaster
    const slideMaster = `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sldMaster xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
  <p:cSld><p:bg><p:bgPr><a:solidFill xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"><a:srgbClr val="1a1a2e"/></a:solidFill></p:bgPr></p:bg><p:spTree/></p:cSld>
</p:sldMaster>`;
    fs.writeFileSync(`${tmpDir}\\ppt\\slideMasters\\slideMaster1.xml`, slideMaster);
    
    // theme
    const theme = `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<a:theme xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" name="VisionVoice">
  <a:themeElements><a:clrScheme name="Dark"><a:dk1><a:sysClr val="windowText" lastClr="000000"/></a:dk1><a:lt1><a:sysClr val="window" lastClr="FFFFFF"/></a:lt1><a:dk2><a:srgbClr val="1a1a2e"/></a:dk2><a:lt2><a:srgbClr val="667eea"/></a:lt2><a:accent1><a:srgbClr val="00d4ff"/></a:accent1><a:accent2><a:srgbClr val="e94560"/></a:accent2><a:accent3><a:srgbClr val="ffd700"/></a:accent3></a:clrScheme><a:fontScheme name="Default"/><a:fmtScheme name="Default"/></a:themeElements>
</a:theme>`;
    fs.writeFileSync(`${tmpDir}\\ppt\\theme\\theme1.xml`, theme);
    
    // 创建所有幻灯片
    slides.forEach((s, i) => {
        const slideXML = createSlideXML(s, i);
        fs.writeFileSync(`${tmpDir}\\ppt\\slides\\slide${i+1}.xml`, slideXML);
        
        // slide rels
        const slideRels = `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideLayout" Target="../slideLayouts/slideLayout1.xml"/>
</Relationships>`;
        fs.writeFileSync(`${tmpDir}\\ppt\\slides\\_rels\\slide${i+1}.xml.rels`, slideRels);
    });
    
    // slideLayout
    const slideLayout = `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sldLayout xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
  <p:cSld><p:spTree/></p:cSld>
</p:sldLayout>`;
    fs.writeFileSync(`${tmpDir}\\ppt\\slideLayouts\\slideLayout1.xml`, slideLayout);
    
    console.log(`✅ PPTX源文件已创建在: ${tmpDir}`);
    console.log(`📁 请使用7-Zip或其他工具将 ${tmpDir} 目录打包成ZIP，然后重命名为 .pptx`);
    console.log(`\n或者直接运行以下命令创建PPTX文件...`);
    
    // 尝试创建PPTX (使用Node.js内置压缩)
    createZipFromDir(tmpDir, outputDir);
}

function createZipFromDir(sourceDir, outputPath) {
    const AdmZip = require('adm-zip');
    try {
        const zip = new AdmZip();
        const files = getAllFiles(sourceDir);
        files.forEach(file => {
            const relativePath = path.relative(sourceDir, file);
            zip.addLocalFile(file, path.dirname(relativePath));
        });
        zip.writeZip(outputPath);
        console.log(`\n🎉 PPTX文件创建成功: ${outputPath}`);
    } catch (e) {
        console.log('adm-zip不可用，尝试手动创建...');
        // 如果adm-zip不可用，提供手动指引
        console.log(`\n请手动执行以下步骤创建PPTX:`);
        console.log(`1. 打开 ${sourceDir}`);
        console.log(`2. 选中所有文件和文件夹`);
        console.log(`3. 右键 → 发送到 → 压缩(zipped)文件夹`);
        console.log(`4. 将生成的ZIP文件重命名为 VisionVoice_Phoneme.pptx`);
    }
}

function getAllFiles(dir) {
    let files = [];
    fs.readdirSync(dir).forEach(file => {
        const fullPath = path.join(dir, file);
        if (fs.statSync(fullPath).isDirectory()) {
            files = files.concat(getAllFiles(fullPath));
        } else {
            files.push(fullPath);
        }
    });
    return files;
}

// 运行
createPPTX();

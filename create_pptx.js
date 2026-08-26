// VisionVoice 音素识别PPT生成器
// 使用纯JavaScript构建PPTX（PPTX是ZIP格式）

const fs = require('fs');
const path = require('path');
const { createGzip } = require('zlib');

// 配色方案
const COLORS = {
    primary: '1a1a2e',      // 深蓝黑
    secondary: '667eea',    // 紫蓝
    accent: '00d4ff',       // 青色
    highlight: 'e94560',    // 红色
    gold: 'ffd700',         // 金色
    white: 'FFFFFF',
    gray: 'AAAAAA',
    lightBg: 'f5f5f5'
};

// 幻灯片内容
const slides = [
    // Slide 1: 标题页
    {
        type: 'title',
        title: 'VisionVoice 音素识别系统',
        subtitle: '端侧AI发音评估技术详解',
        footer: 'VisionVoice Team · 2024 IEEE APCCAS'
    },
    // Slide 2: 目录
    {
        type: 'toc',
        title: '目录',
        items: [
            '1. 项目概述与核心挑战',
            '2. 声学模型选型演进',
            '3. 信号预处理与CTC解码',
            '4. 双重归一化架构',
            '5. 序列对齐与评分算法',
            '6. 声学容错矩阵',
            '7. 性能指标与总结'
        ]
    },
    // Slide 3: 章节页 - 第一部分
    {
        type: 'section',
        number: '01',
        title: '项目概述与核心挑战'
    },
    // Slide 4: VisionVoice简介
    {
        type: 'content',
        title: 'VisionVoice 简介',
        subtitle: 'AR英语学习应用的端侧AI发音评估',
        bullets: [
            '核心目标：零网络延迟、高隐私安全的专业级发音评估',
            '技术方案：端侧离线推理，无需网络连接',
            '应用场景：AR物体识别 → 单词学习 → 发音练习',
            '性能指标：推理时间 < 320ms，模型体积 ~300MB'
        ]
    },
    // Slide 5: 核心技术栈
    {
        type: 'table',
        title: '核心技术栈',
        headers: ['层级', '技术选型', '说明'],
        rows: [
            ['语音模型', 'Wav2Vec2-XLS-R 300M', '多语言音素识别'],
            ['推理引擎', 'ONNX Runtime Mobile', '端侧高效推理'],
            ['对齐算法', 'Needleman-Wunsch', '序列动态规划对齐'],
            ['评分系统', '非线性分段映射', '心理学反馈曲线']
        ]
    },
    // Slide 6: 挑战一
    {
        type: 'content',
        title: '挑战一：模型领域错位 (Domain Mismatch)',
        subtitle: 'ASR模型输出字母，评估基准使用音标',
        bullets: [
            '输入音频 → Wav2Vec2-base → "A-P-P-L-E" (英文字母)',
            '标准答案 → CMU Dict → "AE P L" (Arpabet音素)',
            '问题：跨维度特征不匹配，无法对齐',
            '结果：对所有发音均打出 0分'
        ],
        highlight: 'error'
    },
    // Slide 7: 挑战二
    {
        type: 'content',
        title: '挑战二：异构数据源符号域冲突',
        subtitle: 'Unicode IPA vs ASCII Arpabet 字符不匹配',
        bullets: [
            '词典路径：ɹ, æ, ɔ (Unicode 国际音标)',
            'AI模型路径：r, ae, ao (ASCII Arpabet音标)',
            'Java字符串比较："ɹ".equals("r") → false',
            '问题：即使发音完全正确，系统依然判定为错读！'
        ],
        highlight: 'error'
    },
    // Slide 8: 挑战三
    {
        type: 'content',
        title: '挑战三：声学物理干扰',
        subtitle: '清塞音(/p/, /t/, /k/)气流冲击麦克风',
        bullets: [
            '用户发音清塞音 → 瞬间释放高压气流',
            '冲击手机麦克风振膜 → 产生非线性物理失真',
            'AI提取的声学特征偏离了声带振动',
            '结果：/k/, /p/, /t/ 被识别为 /hh/'
        ],
        highlight: 'warning'
    },
    // Slide 9: 章节页 - 第二部分
    {
        type: 'section',
        number: '02',
        title: '声学模型选型演进'
    },
    // Slide 10: 模型对比
    {
        type: 'twoColumn',
        title: '2.1 初代方案 vs 重构方案',
        left: {
            title: '❌ Wav2Vec2-Base',
            color: COLORS.highlight,
            items: ['参数量：95M', '输出：英文字母 (A-Z)', '问题：无法与音标对齐'],
            badge: '测试结果：0分'
        },
        right: {
            title: '✅ Wav2Vec2-XLS-R',
            color: COLORS.accent,
            items: ['参数量：300M', '输出：Arpabet音素 (40类)', '优势：直接输出音标特征'],
            badge: '重构成功'
        }
    },
    // Slide 11: 性能优化
    {
        type: 'content',
        title: '2.2 端侧性能优化',
        subtitle: 'INT8动态量化：体积压缩75%',
        bullets: [
            '原始模型 (FP32)：1.2 GB',
            '优化模型 (INT8)：~300 MB',
            '完美适配移动端内存限制',
            '避免Android OOM异常',
            '推理精度衰减 < 1%'
        ],
        highlight: 'success'
    },
    // Slide 12: 章节页 - 第三部分
    {
        type: 'section',
        number: '03',
        title: '信号预处理与CTC解码'
    },
    // Slide 13: AGC
    {
        type: 'content',
        title: '3.1 AGC自动增益控制',
        subtitle: '防"AI幻觉"拦截机制',
        bullets: [
            '问题：用户未发声时，模型将背景噪音解码为无意义音素碎片',
            '算法：计算PCM数据峰值振幅',
            'if 振幅 < 0.015 → 直接拦截推理 → 判定为全局漏读',
            'else → 执行归一化处理 → 确保微弱特征被捕获'
        ],
        highlight: 'warning'
    },
    // Slide 14: CTC解码
    {
        type: 'table',
        title: '3.2 CTC解码去噪',
        subtitle: '过滤非发音特征',
        headers: ['杂质符号', '含义', '处理方式'],
        rows: [
            ['h#', '静音帧', '过滤'],
            ['spn', '环境杂音', '过滤'],
            ['[UNK]', '未知符号', '过滤']
        ],
        highlight: 'success'
    },
    // Slide 15: 章节页 - 第四部分
    {
        type: 'section',
        number: '04',
        title: '双重归一化架构'
    },
    // Slide 16: 巴别塔问题
    {
        type: 'content',
        title: '4.1 跨服聊天的"巴别塔难题"',
        subtitle: '现象：完全正确的发音被判定为错读',
        bullets: [
            'UI显示：标准音标 i ← 完全匹配 → i :识别音标',
            '判定结果：❌ 错读 (标红)',
            '根本原因：Unicode与ASCII编码不同',
            'Java字符串比较失败导致伪错误'
        ],
        highlight: 'error'
    },
    // Slide 17: 双层翻译
    {
        type: 'content',
        title: '4.2 双层翻译机制',
        subtitle: '彻底剥离比对逻辑与渲染逻辑',
        bullets: [
            'Level 1 底层降维：Unicode IPA → ASCII Arpabet',
            'Level 2 对齐运算：Needleman-Wunsch Alignment',
            'Level 3 表层升维：ASCII Arpabet → Unicode IPA',
            '核心思想：算法用粗糙的Arpabet计算，UI显示专业的国际音标'
        ],
        highlight: 'success'
    },
    // Slide 18: 章节页 - 第五部分
    {
        type: 'section',
        number: '05',
        title: '序列对齐与评分算法'
    },
    // Slide 19: Needleman-Wunsch
    {
        type: 'table',
        title: '5.1 Needleman-Wunsch全局对齐',
        subtitle: '定制化序列对齐算法',
        headers: ['权重参数', '分值', '说明'],
        rows: [
            ['MATCH', '+1.0', '完全匹配'],
            ['FLAW', '+0.5', '发音瑕疵'],
            ['MISMATCH', '-1.0', '错读 Substitution'],
            ['GAP', '-1.0', '漏读/多读']
        ],
        highlight: 'warning'
    },
    // Slide 20: 对齐示例
    {
        type: 'content',
        title: '5.2 对齐矩阵示意',
        subtitle: '标准序列 vs 用户序列',
        bullets: [
            '标准音素: A  E  P  L',
            '用户音素: A     P  L',
            '对齐分析：A → MATCH ✓ | [GAP] → Gap(-1) | P → MATCH ✓ | L → MATCH ✓',
            '计算得分：(3 - 1) / 3 = 0.67'
        ]
    },
    // Slide 21: 非线性评分
    {
        type: 'content',
        title: '5.3 非线性评分曲线',
        subtitle: '基于教学反馈心理学的分段映射',
        bullets: [
            '< 0.5：严格惩罚区 (0-59分)',
            '0.5-0.8：进步空间大 (60-89分) ← 激励学习',
            '≥ 0.8：优秀鼓励区 (90-100分)',
            '核心思想：扩大中间分数段，让微小进步也能被感知'
        ],
        highlight: 'success'
    },
    // Slide 22: 章节页 - 第六部分
    {
        type: 'section',
        number: '06',
        title: '声学容错矩阵'
    },
    // Slide 23: Plosive Puff
    {
        type: 'content',
        title: '6.1 物理抗噪：爆破音气流干扰',
        subtitle: 'Plosive Puff Effect 解决方案',
        bullets: [
            '问题：清塞音(/k/, /p/, /t/)被识别为 /hh/',
            '物理原因：高压气流冲击麦克风振膜产生非线性失真',
            '工程解决：硬编码豁免路径',
            '效果：k → hh 判定为完全匹配'
        ],
        highlight: 'success'
    },
    // Slide 24: SLA理论
    {
        type: 'table',
        title: '6.2 SLA理论分级惩罚',
        subtitle: '基于Munro & Derwing (1995) 第二语言习得理论',
        headers: ['等级', '类型', '判定', '分数', '示例'],
        rows: [
            ['一级', '完全豁免', 'Ignored→Match', '100分', '长短元音偏差'],
            ['二级', '发音瑕疵', 'Flaw', '60分', '梅花音张口不足'],
            ['三级', '严重错读', 'Substitution', '0分', '破坏语义']
        ]
    },
    // Slide 25: 章节页 - 第七部分
    {
        type: 'section',
        number: '07',
        title: '性能指标与总结'
    },
    // Slide 26: 性能指标
    {
        type: 'table',
        title: '性能指标',
        subtitle: '端侧推理效率',
        headers: ['指标', '数值', '说明'],
        rows: [
            ['模型加载时间', '~800ms', 'mmap内存映射'],
            ['推理时间', '~320ms', '1秒音频'],
            ['Java堆占用', '~11MB', '内存优化'],
            ['模型体积', '~300MB', 'INT8量化']
        ],
        highlight: 'success'
    },
    // Slide 27: 技术架构
    {
        type: 'content',
        title: '技术架构总览',
        bullets: [
            '📥 用户音频 (m4a/AAC)',
            '↓ AudioProcessor: AAC解码 → 16kHz → 归一化',
            '↓ Wav2Vec2Scorer (ONNX): CTC解码 → 杂质过滤',
            '↓ 双重归一化: IPA ↔ Arpabet',
            '↓ Needleman-Wunsch + 三级容错矩阵',
            '✅ 最终评分 (0-100分)'
        ]
    },
    // Slide 28: 核心创新
    {
        type: 'content',
        title: '核心创新总结',
        bullets: [
            '#1 模型领域适配：Wav2Vec2-XLS-R + INT8量化',
            '    95M字母模型 → 300M音素模型',
            '#2 符号域统一：双重归一化架构 + 状态机',
            '    消除Unicode/ASCII冲突',
            '#3 物理补偿：三级声学容错矩阵 + SLA理论',
            '    弥补硬件麦克风缺陷'
        ]
    },
    // Slide 29: 项目成果
    {
        type: 'content',
        title: '项目成果',
        bullets: [
            '✅ 零网络延迟：端侧离线推理，无需联网',
            '✅ 高隐私安全：音频数据不出设备',
            '✅ 媲美云端精度：双重归一化 + 容错矩阵',
            '✅ 移动端适配：300MB模型 + 11MB内存'
        ],
        highlight: 'success'
    },
    // Slide 30: 谢谢
    {
        type: 'thanks',
        title: '谢谢！',
        subtitle: 'VisionVoice - 让每个学习者都能获得专业的发音指导',
        footer: '2024 IEEE APCCAS 论文支撑材料'
    }
];

// 创建XML内容
function createSlideXML(slideData, index) {
    const slideNum = index + 1;
    let content = '';
    
    if (slideData.type === 'title') {
        content = `
            <p:sp>
                <p:nvSpPr><p:cNvPr id="1" name="Title"/><p:nvPr/></p:nvSpPr>
                <p:spPr>
                    <a:xfrm><a:off x="838200" y="2743200"/><a:ext cx="11388600" cy="1200000"/></a:xfrm>
                </p:spPr>
                <p:txBody>
                    <a:bodyPr/>
                    <a:lstStyle/>
                    <a:p><a:pPr algn="ctr"/><a:r><a:rPr lang="zh-CN" sz="4400" b="1"><a:solidFill><a:srgbClr val="${COLORS.accent.substring(1)}"/></a:solidFill></a:rPr><a:t>${slideData.title}</a:t></a:r></a:p>
                </p:txBody>
            </p:sp>
            <p:sp>
                <p:nvSpPr><p:cNvPr id="2" name="Subtitle"/><p:nvPr/></p:nvSpPr>
                <p:spPr>
                    <a:xfrm><a:off x="838200" y="4000000"/><a:ext cx="11388600" cy="800000"/></a:xfrm>
                </p:spPr>
                <p:txBody>
                    <a:bodyPr/>
                    <a:lstStyle/>
                    <a:p><a:pPr algn="ctr"/><a:r><a:rPr lang="zh-CN" sz="2400"><a:solidFill><a:srgbClr val="${COLORS.highlight.substring(1)}"/></a:solidFill></a:rPr><a:t>${slideData.subtitle}</a:t></a:r></a:p>
                </p:txBody>
            </p:sp>
        `;
    } else if (slideData.type === 'section') {
        content = `
            <p:sp>
                <p:nvSpPr><p:cNvPr id="1" name="SectionNum"/><p:nvPr/></p:nvSpPr>
                <p:spPr>
                    <a:xfrm><a:off x="2400000" y="2000000"/><a:ext cx="7200000" cy="1400000"/></a:xfrm>
                </p:spPr>
                <p:txBody>
                    <a:bodyPr/>
                    <a:lstStyle/>
                    <a:p><a:pPr algn="ctr"/><a:r><a:rPr lang="en-US" sz="7200" b="1"><a:solidFill><a:srgbClr val="${COLORS.highlight.substring(1)}"/></a:solidFill></a:rPr><a:t>${slideData.number}</a:t></a:r></a:p>
                </p:txBody>
            </p:sp>
            <p:sp>
                <p:nvSpPr><p:cNvPr id="2" name="SectionTitle"/><p:nvPr/></p:nvSpPr>
                <p:spPr>
                    <a:xfrm><a:off x="1200000" y="3600000"/><a:ext cx="9600000" cy="900000"/></a:xfrm>
                </p:spPr>
                <p:txBody>
                    <a:bodyPr/>
                    <a:lstStyle/>
                    <a:p><a:pPr algn="ctr"/><a:r><a:rPr lang="zh-CN" sz="3200"><a:solidFill><a:srgbClr val="${COLORS.white.substring(1)}"/></a:solidFill></a:rPr><a:t>${slideData.title}</a:t></a:r></a:p>
                </p:txBody>
            </p:sp>
        `;
    } else if (slideData.type === 'toc') {
        let items = '';
        slideData.items.forEach((item, i) => {
            items += `<a:p><a:pPr marL="3429000" indent="-3429000"/><a:r><a:rPr lang="zh-CN" sz="2000"><a:solidFill><a:srgbClr val="${COLORS.white.substring(1)}"/></a:solidFill></a:rPr><a:t>${item}</a:t></a:r></a:p>`;
        });
        content = `
            <p:sp>
                <p:nvSpPr><p:cNvPr id="1" name="Title"/><p:nvPr/></p:nvSpPr>
                <p:spPr>
                    <a:xfrm><a:off x="838200" y="600000"/><a:ext cx="11388600" cy="700000"/></a:xfrm>
                </p:spPr>
                <p:txBody>
                    <a:bodyPr/>
                    <a:lstStyle/>
                    <a:p><a:pPr algn="l"/><a:r><a:rPr lang="zh-CN" sz="3200" b="1"><a:solidFill><a:srgbClr val="${COLORS.accent.substring(1)}"/></a:solidFill></a:rPr><a:t>${slideData.title}</a:t></a:r></a:p>
                </p:txBody>
            </p:sp>
            <p:sp>
                <p:nvSpPr><p:cNvPr id="2" name="Content"/><p:nvPr/></p:nvSpPr>
                <p:spPr>
                    <a:xfrm><a:off x="1200000" y="1400000"/><a:ext cx="9600000" cy="4000000"/></a:xfrm>
                </p:spPr>
                <p:txBody>
                    <a:bodyPr/>
                    <a:lstStyle/>
                    ${items}
                </p:txBody>
            </p:sp>
        `;
    } else if (slideData.type === 'content') {
        let bullets = '';
        slideData.bullets.forEach((bullet, i) => {
            const bulletChar = bullet.startsWith('📥') || bullet.startsWith('↓') || bullet.startsWith('✅') || bullet.startsWith('#') ? '' : '• ';
            bullets += `<a:p><a:pPr marL="457200" indent="-457200"/><a:r><a:rPr lang="zh-CN" sz="1800"><a:solidFill><a:srgbClr val="${COLORS.white.substring(1)}"/></a:solidFill></a:rPr><a:t>${bulletChar}${bullet}</a:t></a:r></a:p>`;
        });
        
        const bgColor = slideData.highlight === 'error' ? '660000' : 
                       slideData.highlight === 'warning' ? '665500' : 
                       slideData.highlight === 'success' ? '006633' : COLORS.secondary;
        
        content = `
            <p:sp>
                <p:nvSpPr><p:cNvPr id="1" name="Title"/><p:nvPr/></p:nvSpPr>
                <p:spPr>
                    <a:xfrm><a:off x="838200" y="500000"/><a:ext cx="11388600" cy="700000"/></a:xfrm>
                </p:spPr>
                <p:txBody>
                    <a:bodyPr/>
                    <a:lstStyle/>
                    <a:p><a:pPr algn="l"/><a:r><a:rPr lang="zh-CN" sz="2800" b="1"><a:solidFill><a:srgbClr val="${COLORS.accent.substring(1)}"/></a:solidFill></a:rPr><a:t>${slideData.title}</a:t></a:r></a:p>
                </p:txBody>
            </p:sp>
            ${slideData.subtitle ? `
            <p:sp>
                <p:nvSpPr><p:cNvPr id="2" name="Subtitle"/><p:nvPr/></p:nvSpPr>
                <p:spPr>
                    <a:xfrm><a:off x="838200" y="1200000"/><a:ext cx="11388600" cy="500000"/></a:xfrm>
                </p:spPr>
                <p:txBody>
                    <a:bodyPr/>
                    <a:lstStyle/>
                    <a:p><a:pPr algn="l"/><a:r><a:rPr lang="zh-CN" sz="1600"><a:solidFill><a:srgbClr val="${COLORS.gold.substring(1)}"/></a:solidFill></a:rPr><a:t>${slideData.subtitle}</a:t></a:r></a:p>
                </p:txBody>
            </p:sp>
            ` : ''}
            <p:sp>
                <p:nvSpPr><p:cNvPr id="3" name="Content"/><p:nvPr/></p:nvSpPr>
                <p:spPr>
                    <a:xfrm><a:off x="838200" y="${slideData.subtitle ? '1800000' : '1400000'}"/><a:ext cx="11388600" cy="3200000"/></a:xfrm>
                </p:spPr>
                <p:txBody>
                    <a:bodyPr/>
                    <a:lstStyle/>
                    ${bullets}
                </p:txBody>
            </p:sp>
        `;
    } else if (slideData.type === 'table') {
        let rows = '';
        slideData.rows.forEach((row, i) => {
            const cells = row.map((cell, j) => 
                `<a:tc><a:txBody><a:bodyPr/><a:lstStyle/><a:p><a:r><a:rPr lang="zh-CN" sz="1400"><a:solidFill><a:srgbClr val="${COLORS.white.substring(1)}"/></a:solidFill></a:rPr><a:t>${cell}</a:t></a:r></a:p></a:txBody><a:tcPr/></a:tc>`
            ).join('');
            rows += `<a:tr>${cells}</a:tr>`;
        });
        
        const headerCells = slideData.headers.map(h => 
            `<a:tc><a:txBody><a:bodyPr/><a:lstStyle/><a:p><a:r><a:rPr lang="zh-CN" sz="1600" b="1"><a:solidFill><a:srgbClr val="${COLORS.accent.substring(1)}"/></a:solidFill></a:rPr><a:t>${h}</a:t></a:r></a:p></a:txBody><a:tcPr/></a:tc>`
        ).join('');
        
        content = `
            <p:sp>
                <p:nvSpPr><p:cNvPr id="1" name="Title"/><p:nvPr/></p:nvSpPr>
                <p:spPr>
                    <a:xfrm><a:off x="838200" y="500000"/><a:ext cx="11388600" cy="700000"/></a:xfrm>
                </p:spPr>
                <p:txBody>
                    <a:bodyPr/>
                    <a:lstStyle/>
                    <a:p><a:pPr algn="l"/><a:r><a:rPr lang="zh-CN" sz="2800" b="1"><a:solidFill><a:srgbClr val="${COLORS.accent.substring(1)}"/></a:solidFill></a:rPr><a:t>${slideData.title}</a:t></a:r></a:p>
                </p:txBody>
            </p:sp>
            ${slideData.subtitle ? `
            <p:sp>
                <p:nvSpPr><p:cNvPr id="2" name="Subtitle"/><p:nvPr/></p:nvSpPr>
                <p:spPr>
                    <a:xfrm><a:off x="838200" y="1200000"/><a:ext cx="11388600" cy="500000"/></a:xfrm>
                </p:spPr>
                <p:txBody>
                    <a:bodyPr/>
                    <a:lstStyle/>
                    <a:p><a:pPr algn="l"/><a:r><a:rPr lang="zh-CN" sz="1600"><a:solidFill><a:srgbClr val="${COLORS.gold.substring(1)}"/></a:solidFill></a:rPr><a:t>${slideData.subtitle}</a:t></a:r></a:p>
                </p:txBody>
            </p:sp>
            ` : ''}
            <a:tbl>
                <a:tblPr firstRow="1">
                    <a:tableStyleId>{5C22544A-7EE6-4342-B048-85BDC9FD1C3A}</a:tableStyleId>
                </a:tblPr>
                <a:tblGrid>
                    ${slideData.headers.map(() => '<a:gridCol><a:xfrm><a:off x="0"/><a:ext cx="2400000"/></a:xfrm></a:gridCol>').join('')}
                </a:tblGrid>
                <a:tr h="500000">${headerCells}</a:tr>
                ${rows}
            </a:tbl>
        `;
    } else if (slideData.type === 'twoColumn') {
        const leftColor = slideData.left.color;
        const rightColor = slideData.right.color;
        
        let leftItems = slideData.left.items.map(item => 
            `<a:p><a:pPr marL="228600" indent="-228600"/><a:r><a:rPr lang="zh-CN" sz="1600"><a:solidFill><a:srgbClr val="${COLORS.white.substring(1)}"/></a:solidFill></a:rPr><a:t>• ${item}</a:t></a:r></a:p>`
        ).join('');
        let rightItems = slideData.right.items.map(item => 
            `<a:p><a:pPr marL="228600" indent="-228600"/><a:r><a:rPr lang="zh-CN" sz="1600"><a:solidFill><a:srgbClr val="${COLORS.white.substring(1)}"/></a:solidFill></a:rPr><a:t>• ${item}</a:t></a:r></a:p>`
        ).join('');
        
        content = `
            <p:sp>
                <p:nvSpPr><p:cNvPr id="1" name="Title"/><p:nvPr/></p:nvSpPr>
                <p:spPr>
                    <a:xfrm><a:off x="838200" y="500000"/><a:ext cx="11388600" cy="700000"/></a:xfrm>
                </p:spPr>
                <p:txBody>
                    <a:bodyPr/>
                    <a:lstStyle/>
                    <a:p><a:pPr algn="l"/><a:r><a:rPr lang="zh-CN" sz="2800" b="1"><a:solidFill><a:srgbClr val="${COLORS.accent.substring(1)}"/></a:solidFill></a:rPr><a:t>${slideData.title}</a:t></a:r></a:p>
                </p:txBody>
            </p:sp>
            <p:sp>
                <p:nvSpPr><p:cNvPr id="2" name="LeftBox"/><p:nvPr/></p:nvSpPr>
                <p:spPr>
                    <a:xfrm><a:off x="500000" y="1400000"/><a:ext cx="5400000" cy="3200000"/></a:xfrm>
                    <a:prstGeom prst="roundRect"><a:avLst><a:gd name="adj" fmla="val 5000"/></a:avLst></a:prstGeom>
                    <a:solidFill><a:srgbClr val="333333"/></a:solidFill>
                    <a:ln w="28575"><a:solidFill><a:srgbClr val="${leftColor.substring(1)}"/></a:solidFill></a:ln>
                </p:spPr>
                <p:txBody>
                    <a:bodyPr/>
                    <a:lstStyle/>
                    <a:p><a:pPr algn="ctr"/><a:r><a:rPr lang="zh-CN" sz="2000" b="1"><a:solidFill><a:srgbClr val="${leftColor.substring(1)}"/></a:solidFill></a:rPr><a:t>${slideData.left.title}</a:t></a:r></a:p>
                    ${leftItems}
                    <a:p><a:pPr algn="ctr"/><a:r><a:rPr lang="zh-CN" sz="1400"><a:solidFill><a:srgbClr val="${leftColor.substring(1)}"/></a:solidFill></a:rPr><a:t>${slideData.left.badge}</a:t></a:r></a:p>
                </p:txBody>
            </p:sp>
            <p:sp>
                <p:nvSpPr><p:cNvPr id="3" name="RightBox"/><p:nvPr/></p:nvSpPr>
                <p:spPr>
                    <a:xfrm><a:off x="6100000" y="1400000"/><a:ext cx="5400000" cy="3200000"/></a:xfrm>
                    <a:prstGeom prst="roundRect"><a:avLst><a:gd name="adj" fmla="val 5000"/></a:avLst></a:prstGeom>
                    <a:solidFill><a:srgbClr val="333333"/></a:solidFill>
                    <a:ln w="28575"><a:solidFill><a:srgbClr val="${rightColor.substring(1)}"/></a:solidFill></a:ln>
                </p:spPr>
                <p:txBody>
                    <a:bodyPr/>
                    <a:lstStyle/>
                    <a:p><a:pPr algn="ctr"/><a:r><a:rPr lang="zh-CN" sz="2000" b="1"><a:solidFill><a:srgbClr val="${rightColor.substring(1)}"/></a:solidFill></a:rPr><a:t>${slideData.right.title}</a:t></a:r></a:p>
                    ${rightItems}
                    <a:p><a:pPr algn="ctr"/><a:r><a:rPr lang="zh-CN" sz="1400"><a:solidFill><a:srgbClr val="${rightColor.substring(1)}"/></a:solidFill></a:rPr><a:t>${slideData.right.badge}</a:t></a:r></a:p>
                </p:txBody>
            </p:sp>
        `;
    } else if (slideData.type === 'thanks') {
        content = `
            <p:sp>
                <p:nvSpPr><p:cNvPr id="1" name="Title"/><p:nvPr/></p:nvSpPr>
                <p:spPr>
                    <a:xfrm><a:off x="838200" y="2000000"/><a:ext cx="11388600" cy="1200000"/></a:xfrm>
                </p:spPr>
                <p:txBody>
                    <a:bodyPr/>
                    <a:lstStyle/>
                    <a:p><a:pPr algn="ctr"/><a:r><a:rPr lang="zh-CN" sz="6000" b="1"><a:solidFill><a:srgbClr val="${COLORS.accent.substring(1)}"/></a:solidFill></a:rPr><a:t>${slideData.title}</a:t></a:r></a:p>
                </p:txBody>
            </p:sp>
            <p:sp>
                <p:nvSpPr><p:cNvPr id="2" name="Subtitle"/><p:nvPr/></p:nvSpPr>
                <p:spPr>
                    <a:xfrm><a:off x="838200" y="3400000"/><a:ext cx="11388600" cy="800000"/></a:xfrm>
                </p:spPr>
                <p:txBody>
                    <a:bodyPr/>
                    <a:lstStyle/>
                    <a:p><a:pPr algn="ctr"/><a:r><a:rPr lang="zh-CN" sz="2000"><a:solidFill><a:srgbClr val="${COLORS.highlight.substring(1)}"/></a:solidFill></a:rPr><a:t>${slideData.subtitle}</a:t></a:r></a:p>
                </p:txBody>
            </p:sp>
        `;
    }
    
    return `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sps xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main" xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main">
    <p:cSld>
        <p:spTree>
            <p:nvGrpSpPr><p:cNvPr id="0" name=""/><p:nvPr/></p:nvGrpSpPr>
            <p:grpSpPr>
                <a:xfrm><a:off x="0" y="0"/><a:ext cx="0" cy="0"/><a:
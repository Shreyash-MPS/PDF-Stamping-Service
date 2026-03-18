import { useState, useRef, useEffect, useCallback } from 'react';
import CodeMirror from '@uiw/react-codemirror';
import { html as htmlLang } from '@codemirror/lang-html';
import { Code, Eye, X, Maximize2, LayoutTemplate } from 'lucide-react';
import { TEMPLATES } from '../models/templates';
import stampingConfigData from '../data/stamping_config.json';

const inputClass = 'w-full px-3 py-1.5 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-[#a81732]/20 focus:border-[#a81732]';

/* ── Inline HTML Editor Modal ── */
const HtmlEditorModal = ({ value, onChange, onClose }) => {
    const [code, setCode] = useState(value || '');
    const [leftWidth, setLeftWidth] = useState(50);
    const splitRef = useRef(null);
    const dragging = useRef(false);
    const iframeRef = useRef(null);

    useEffect(() => {
        if (iframeRef.current) {
            iframeRef.current.srcdoc = code || '<p style="color:#999;text-align:center;margin-top:40px;">Start typing HTML…</p>';
        }
    }, [code]);

    const onMouseDown = (e) => { dragging.current = true; document.body.style.cursor = 'col-resize'; e.preventDefault(); };
    const onMouseMove = useCallback((e) => {
        if (!dragging.current || !splitRef.current) return;
        const rect = splitRef.current.getBoundingClientRect();
        setLeftWidth(Math.max(20, Math.min(80, ((e.clientX - rect.left) / rect.width) * 100)));
    }, []);
    const onMouseUp = useCallback(() => { dragging.current = false; document.body.style.cursor = ''; }, []);

    useEffect(() => {
        document.addEventListener('mousemove', onMouseMove);
        document.addEventListener('mouseup', onMouseUp);
        return () => { document.removeEventListener('mousemove', onMouseMove); document.removeEventListener('mouseup', onMouseUp); };
    }, [onMouseMove, onMouseUp]);

    const handleApply = () => { onChange(code); onClose(); };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50" onClick={onClose}>
            <div className="bg-white rounded-lg shadow-2xl w-[90vw] h-[80vh] flex flex-col" onClick={e => e.stopPropagation()}>
                {/* Header */}
                <div className="flex items-center justify-between px-5 py-3 border-b border-gray-200 bg-[#fafafa] rounded-t-lg">
                    <div className="flex items-center gap-2 text-gray-800 font-bold text-base">
                        <Code className="w-5 h-5 text-[#a81732]" />
                        HTML Editor
                    </div>
                    <div className="flex items-center gap-2">
                        <button onClick={handleApply}
                            className="px-4 py-1.5 text-sm font-semibold text-white bg-[#a81732] rounded-md hover:bg-[#851227] transition-colors cursor-pointer">
                            Apply
                        </button>
                        <button onClick={onClose}
                            className="p-1.5 text-gray-400 hover:text-gray-700 rounded-md hover:bg-gray-100 cursor-pointer">
                            <X className="w-5 h-5" />
                        </button>
                    </div>
                </div>

                {/* Split pane */}
                <div className="flex flex-1 min-h-0" ref={splitRef}>
                    {/* Editor */}
                    <div className="flex flex-col overflow-hidden" style={{ width: `calc(${leftWidth}% - 4px)` }}>
                        <div className="flex items-center gap-1.5 px-3 py-1.5 bg-[#1e1e1e] text-xs font-semibold text-gray-400 tracking-wider uppercase">
                            <Code className="w-3.5 h-3.5" /> Source
                        </div>
                        <div className="flex-1 overflow-auto">
                            <CodeMirror
                                value={code}
                                height="100%"
                                theme="dark"
                                extensions={[htmlLang()]}
                                onChange={setCode}
                                style={{ height: '100%', fontSize: '0.85rem' }}
                            />
                        </div>
                    </div>

                    {/* Drag handle */}
                    <div className="w-2 bg-gray-200 cursor-col-resize flex items-center justify-center shrink-0 hover:bg-gray-300 transition-colors"
                        onMouseDown={onMouseDown}>
                        <div className="w-0.5 h-8 rounded bg-gray-400" />
                    </div>

                    {/* Preview */}
                    <div className="flex flex-col" style={{ width: `calc(${100 - leftWidth}% - 4px)` }}>
                        <div className="flex items-center gap-1.5 px-3 py-1.5 bg-[#f0f0f0] border-b border-gray-200 text-xs font-semibold text-gray-500 tracking-wider uppercase">
                            <Eye className="w-3.5 h-3.5" /> Live Preview
                        </div>
                        <iframe ref={iframeRef} className="flex-1 w-full border-none bg-white" sandbox="allow-same-origin" />
                    </div>
                </div>
            </div>
        </div>
    );
};

const ARTICLE_FIELDS = [
    { key: 'articleTitle', label: 'Article Title' },
    { key: 'articleAuthors', label: 'Article Authors' },
    { key: 'articleDoi', label: 'Article DOI' },
    { key: 'articleCopyright', label: 'Article Copyright' },
    { key: 'articleIssn', label: 'Article ISSN' },
    { key: 'articleId', label: 'Article ID' },
];

const OTHER_FIELDS = [
    { key: 'dateOfDownload', label: 'Date of Download' },
    { key: 'downloadBy', label: 'Downloaded By' },
];

const StampSectionPanel = ({ section, onChange, sectionKey, templateName, onTemplateChange }) => {
    const [showHtmlEditor, setShowHtmlEditor] = useState(false);

    const update = (fieldKey, value) => {
        onChange({ ...section, [fieldKey]: value });
    };

    const handleFileChange = (e) => {
        const file = e.target.files[0];
        if (!file) return;
        const reader = new FileReader();
        reader.onload = (ev) => update('logo', { enabled: true, value: ev.target.result.split(',')[1] });
        reader.readAsDataURL(file);
    };

    const handleTemplateSelect = (newTemplateName) => {
        if (onTemplateChange) onTemplateChange(newTemplateName);
        const template = TEMPLATES[newTemplateName];
        if (template?.defaultSection) {
            // Preserve logo value if already uploaded
            const currentLogo = section.logo;
            const newSection = { ...template.defaultSection };
            if (currentLogo?.value && newSection.logo?.enabled) {
                newSection.logo = { ...newSection.logo, value: currentLogo.value };
            }
            onChange(newSection);
        }
    };

    return (
        <div className="space-y-6 animate-[fadeIn_0.2s_ease-out]">
            {/* Enable toggle */}
            <div className="flex items-center justify-between pb-4 border-b border-gray-200">
                <span className="text-sm font-semibold text-gray-700">
                    Enable this {sectionKey === 'newPage' ? 'section' : 'position'}
                </span>
                <label className="relative inline-flex items-center cursor-pointer shrink-0">
                    <input
                        type="checkbox" className="sr-only peer"
                        checked={section.enabled}
                        onChange={e => update('enabled', e.target.checked)}
                    />
                    <div className="w-9 h-5 bg-gray-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-4 after:w-4 after:transition-all peer-checked:bg-[#a81732]" />
                </label>
            </div>

            <div className={`space-y-6 transition-opacity duration-200 ${section.enabled ? 'opacity-100' : 'opacity-40 pointer-events-none'}`}>
                {/* New Page specific: page position */}
                {sectionKey === 'newPage' && (
                    <div>
                        <div className="text-xs font-bold text-gray-500 uppercase tracking-wider mb-3">Page Settings</div>
                        <div className="bg-gray-50 border border-gray-200 rounded-md p-4 space-y-4">
                            <div>
                                <span className="text-sm text-gray-700 font-medium">Page Position</span>
                                <div className="flex gap-4 mt-2">
                                    {[{ value: 'front', label: 'Front (Before PDF)' }, { value: 'back', label: 'Back (After PDF)' }].map(opt => (
                                        <label key={opt.value} className="flex items-center gap-2 cursor-pointer text-sm">
                                            <input
                                                type="radio"
                                                name="pagePosition"
                                                className="w-4 h-4 accent-[#a81732] cursor-pointer"
                                                checked={(section.pagePosition ?? 'front') === opt.value}
                                                onChange={() => update('pagePosition', opt.value)}
                                            />
                                            <span className="text-gray-700 select-none">{opt.label}</span>
                                        </label>
                                    ))}
                                </div>
                            </div>
                            <div>
                                <span className="text-sm text-gray-700 font-medium flex items-center gap-1.5">
                                    <LayoutTemplate className="w-4 h-4 text-gray-500" /> Page Template
                                </span>
                                <select
                                    className="mt-2 w-full px-3 py-2 border border-gray-300 rounded-md text-sm bg-white focus:outline-none focus:ring-2 focus:ring-[#a81732]/20 focus:border-[#a81732]"
                                    value={templateName || 'journal_article'}
                                    onChange={e => handleTemplateSelect(e.target.value)}
                                >
                                    {stampingConfigData.map(t => (
                                        <option key={t.templateName} value={t.templateName}>{t.displayName}</option>
                                    ))}
                                </select>
                                {TEMPLATES[templateName]?.defaultSection && (
                                    <p className="mt-1.5 text-xs text-gray-400">
                                        Selecting a template pre-fills the section fields. You can still customize them below.
                                    </p>
                                )}
                            </div>
                        </div>
                    </div>
                )}

                {/* Client-provided content: Logo, HTML, Text */}
                <div>
                    <div className="text-xs font-bold text-gray-500 uppercase tracking-wider mb-3">Client-Provided Content</div>
                    <div className="bg-gray-50 border border-gray-200 rounded-md p-4 space-y-4">
                        {/* Logo */}
                        <div>
                            <label className="flex items-center gap-2.5 cursor-pointer text-sm py-1">
                                <input type="checkbox" className="w-4 h-4 accent-[#a81732] cursor-pointer"
                                    checked={section.logo?.enabled ?? false}
                                    onChange={e => update('logo', { ...section.logo, enabled: e.target.checked, value: e.target.checked ? section.logo?.value : null })}
                                />
                                <span className="text-gray-700 select-none">Logo Image</span>
                            </label>
                            <div className={`overflow-hidden transition-all duration-200 ${section.logo?.enabled ? 'max-h-40 opacity-100 mt-1.5 ml-6.5' : 'max-h-0 opacity-0'}`}>
                                <div className="flex items-center gap-3">
                                    <input type="file"
                                        className="block w-full text-sm text-gray-500 file:mr-3 file:py-1.5 file:px-3 file:rounded-md file:border-0 file:text-sm file:font-semibold file:bg-[#fdf2f4] file:text-[#a81732] hover:file:bg-[#fbdde1] border border-gray-300 rounded-md bg-white p-1"
                                        accept="image/png, image/jpeg" onChange={handleFileChange}
                                    />
                                    {section.logo?.value && (
                                        <button onClick={() => update('logo', { enabled: true, value: null })}
                                            className="text-xs text-red-500 hover:text-red-700 whitespace-nowrap cursor-pointer">Remove</button>
                                    )}
                                </div>
                                {section.logo?.value && (
                                    <img src={`data:image/png;base64,${section.logo.value}`} alt="Logo preview"
                                        className="mt-2 max-h-12 object-contain border border-gray-200 rounded p-1 bg-white" />
                                )}
                            </div>
                        </div>

                        {/* HTML */}
                        <div>
                            <label className="flex items-center gap-2.5 cursor-pointer text-sm py-1">
                                <input type="checkbox" className="w-4 h-4 accent-[#a81732] cursor-pointer"
                                    checked={section.html?.enabled ?? false}
                                    onChange={e => update('html', { ...section.html, enabled: e.target.checked })}
                                />
                                <span className="text-gray-700 select-none">Custom HTML</span>
                            </label>
                            <div className={`overflow-hidden transition-all duration-200 ${section.html?.enabled ? 'max-h-[300px] opacity-100 mt-1.5 ml-6.5' : 'max-h-0 opacity-0'}`}>
                                {/* Inline preview */}
                                {section.html?.value ? (
                                    <div className="border border-gray-200 rounded-md bg-white mb-2 overflow-hidden">
                                        <div className="px-2.5 py-1 bg-gray-50 border-b border-gray-200 flex items-center justify-between">
                                            <span className="text-[10px] font-semibold text-gray-400 uppercase tracking-wider">Preview</span>
                                        </div>
                                        <iframe
                                            srcDoc={section.html.value}
                                            className="w-full border-none bg-white"
                                            style={{ height: '80px', pointerEvents: 'none' }}
                                            sandbox="allow-same-origin"
                                            title="HTML preview"
                                        />
                                    </div>
                                ) : (
                                    <div className="border border-dashed border-gray-300 rounded-md bg-gray-50 px-3 py-4 mb-2 text-center text-xs text-gray-400">
                                        No HTML content yet. Click the button below to open the editor.
                                    </div>
                                )}
                                <button
                                    type="button"
                                    onClick={() => setShowHtmlEditor(true)}
                                    className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold text-[#a81732] border border-[#a81732] rounded-md hover:bg-[#fdf2f4] transition-colors cursor-pointer"
                                >
                                    <Maximize2 className="w-3.5 h-3.5" />
                                    Open HTML Editor
                                </button>
                                {showHtmlEditor && (
                                    <HtmlEditorModal
                                        value={section.html?.value ?? ''}
                                        onChange={(val) => update('html', { enabled: true, value: val })}
                                        onClose={() => setShowHtmlEditor(false)}
                                    />
                                )}
                            </div>
                        </div>

                        {/* Text */}
                        <div>
                            <label className="flex items-center gap-2.5 cursor-pointer text-sm py-1">
                                <input type="checkbox" className="w-4 h-4 accent-[#a81732] cursor-pointer"
                                    checked={section.text?.enabled ?? false}
                                    onChange={e => update('text', { ...section.text, enabled: e.target.checked })}
                                />
                                <span className="text-gray-700 select-none">Plain Text</span>
                            </label>
                            <div className={`overflow-hidden transition-all duration-200 ${section.text?.enabled ? 'max-h-20 opacity-100 mt-1.5 ml-6.5' : 'max-h-0 opacity-0'}`}>
                                <input type="text" className={inputClass} placeholder="Enter text to stamp"
                                    value={section.text?.value ?? ''}
                                    onChange={e => update('text', { enabled: true, value: e.target.value })}
                                />
                            </div>
                        </div>

                        {/* Link */}
                        <div>
                            <label className="flex items-center gap-2.5 cursor-pointer text-sm py-1">
                                <input type="checkbox" className="w-4 h-4 accent-[#a81732] cursor-pointer"
                                    checked={section.link?.enabled ?? false}
                                    onChange={e => update('link', { ...section.link, enabled: e.target.checked })}
                                />
                                <span className="text-gray-700 select-none">Hyperlink</span>
                            </label>
                            <div className={`overflow-hidden transition-all duration-200 ${section.link?.enabled ? 'max-h-32 opacity-100 mt-1.5 ml-6.5' : 'max-h-0 opacity-0'}`}>
                                <div className="space-y-2">
                                    <input type="text" className={inputClass} placeholder="Enter link text to display"
                                        value={section.link?.text ?? ''}
                                        onChange={e => update('link', { ...section.link, enabled: true, text: e.target.value })}
                                    />
                                    <input type="url" className={inputClass} placeholder="Enter link URL (e.g., https://...)"
                                        value={section.link?.url ?? ''}
                                        onChange={e => update('link', { ...section.link, enabled: true, url: e.target.value })}
                                    />
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Article metadata */}
                <div>
                    <div className="text-xs font-bold text-gray-500 uppercase tracking-wider mb-3">Article Metadata</div>
                    <div className="bg-gray-50 border border-gray-200 rounded-md p-4 space-y-3">
                        {ARTICLE_FIELDS.map(f => (
                            <label key={f.key} className="flex items-center gap-2.5 cursor-pointer text-sm py-1">
                                <input type="checkbox" className="w-4 h-4 accent-[#a81732] cursor-pointer"
                                    checked={section[f.key] ?? false}
                                    onChange={e => update(f.key, e.target.checked)}
                                />
                                <span className="text-gray-700 select-none">{f.label}</span>
                            </label>
                        ))}
                    </div>
                </div>

                {/* Other options */}
                <div>
                    <div className="text-xs font-bold text-gray-500 uppercase tracking-wider mb-3">Other Options</div>
                    <div className="bg-gray-50 border border-gray-200 rounded-md p-4 space-y-3">
                        {OTHER_FIELDS.map(f => (
                            <label key={f.key} className="flex items-center gap-2.5 cursor-pointer text-sm py-1">
                                <input type="checkbox" className="w-4 h-4 accent-[#a81732] cursor-pointer"
                                    checked={section[f.key] ?? false}
                                    onChange={e => update(f.key, e.target.checked)}
                                />
                                <span className="text-gray-700 select-none">{f.label}</span>
                            </label>
                        ))}

                        {/* Ads Banner */}
                        <div>
                            <label className="flex items-center gap-2.5 cursor-pointer text-sm py-1">
                                <input type="checkbox" className="w-4 h-4 accent-[#a81732] cursor-pointer"
                                    checked={section.adsBanner?.enabled ?? false}
                                    onChange={e => update('adsBanner', { ...section.adsBanner, enabled: e.target.checked })}
                                />
                                <span className="text-gray-700 select-none">Ads Banner</span>
                            </label>
                            <div className={`overflow-hidden transition-all duration-200 ${section.adsBanner?.enabled ? 'max-h-40 opacity-100 mt-1.5 ml-6.5' : 'max-h-0 opacity-0'}`}>
                                <div className="space-y-2">
                                    <input type="text" className={inputClass} placeholder="Ad banner URL or content"
                                        value={section.adsBanner?.value ?? ''}
                                        onChange={e => update('adsBanner', { ...section.adsBanner, enabled: true, value: e.target.value })}
                                    />
                                    <input type="text" className={inputClass} placeholder="Legacy domain (e.g. https://legacy.example.com)"
                                        value={section.adsBanner?.legacyDomain ?? ''}
                                        onChange={e => update('adsBanner', { ...section.adsBanner, enabled: true, legacyDomain: e.target.value })}
                                    />
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default StampSectionPanel;

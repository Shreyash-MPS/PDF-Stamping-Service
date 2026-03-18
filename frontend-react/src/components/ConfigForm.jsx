import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useConfigContext, getDefaultConfig } from '../context/ConfigContext';
import { ArrowLeft, Save, Eye, X, FilePlus, PanelTop, PanelBottom, PanelLeft, PanelRight } from 'lucide-react';
import { TEMPLATES, getCurrentDate } from '../models/templates';
import stampingConfigData from '../data/stamping_config.json';
import StampSectionPanel from './StampSectionPanel';
import SearchableSelect from './SearchableSelect';

const TABS = [
    { key: 'newPage', label: 'Add New Page', icon: FilePlus },
    { key: 'header', label: 'Header', icon: PanelTop },
    { key: 'footer', label: 'Footer', icon: PanelBottom },
    { key: 'leftMargin', label: 'Left Side', icon: PanelLeft },
    { key: 'rightMargin', label: 'Right Side', icon: PanelRight },
];

const SECTION_LABELS = {
    newPage: 'New Page',
    header: 'Header',
    footer: 'Footer',
    leftMargin: 'Left Side',
    rightMargin: 'Right Side',
};

const getSectionSummary = (section) => {
    if (!section.enabled) return null;
    const items = [];
    if (section.logo?.enabled) items.push('Logo');
    if (section.html?.enabled) items.push('HTML');
    if (section.text?.enabled) items.push(`Text: "${section.text.value || '...'}"`);
    if (section.link?.enabled) items.push(`Link: "${section.link.text || '...'}"`);
    if (section.articleTitle) items.push('Title');
    if (section.articleAuthors) items.push('Authors');
    if (section.articleDoi) items.push('DOI');
    if (section.articleCopyright) items.push('Copyright');
    if (section.articleIssn) items.push('ISSN');
    if (section.articleId) items.push('Article ID');
    if (section.dateOfDownload) items.push('Date of Download');
    if (section.downloadBy) items.push('Downloaded By');
    if (section.adsBanner?.enabled) items.push('Ads Banner');
    return items;
};

/* Full PDF preview modal */
const PreviewModal = ({ sections, templateName, onTemplateChange, onClose }) => {
    const template = TEMPLATES[templateName];
    const date = getCurrentDate();

    const buildPreviewHtml = () => {
        const enabledSections = TABS.filter(t => sections[t.key].enabled);
        if (enabledSections.length === 0) {
            return `<div style="padding:60px;text-align:center;color:#999;font-family:sans-serif;">
                <p style="font-size:18px;">No sections enabled</p>
                <p style="font-size:14px;">Enable at least one section to see a preview.</p>
            </div>`;
        }

        const headerSection = sections.header;
        const footerSection = sections.footer;
        const leftSection = sections.leftMargin;
        const rightSection = sections.rightMargin;
        const newPageSection = sections.newPage;

        const renderItems = (sec) => {
            const parts = [];
            if (sec.logo?.enabled) parts.push('<div style="margin-bottom:8px;"><span style="background:#eee;padding:4px 12px;border-radius:4px;font-size:12px;color:#888;">[ Logo Image ]</span></div>');
            if (sec.html?.enabled && sec.html.value) parts.push(`<div style="margin-bottom:6px;font-size:13px;">${sec.html.value}</div>`);
            if (sec.text?.enabled && sec.text.value) parts.push(`<div style="margin-bottom:6px;font-size:13px;">${sec.text.value}</div>`);
            if (sec.link?.enabled && sec.link.url && sec.link.text) parts.push(`<div style="margin-bottom:6px;font-size:13px;"><a href="${sec.link.url}" style="color:blue;text-decoration:underline;" target="_blank">${sec.link.text}</a></div>`);
            if (sec.articleTitle) parts.push('<div style="font-size:14px;font-weight:bold;color:#999;font-style:italic;margin-bottom:4px;">[Article Title]</div>');
            if (sec.articleAuthors) parts.push('<div style="font-size:13px;color:#999;font-style:italic;margin-bottom:4px;">[Authors]</div>');
            if (sec.articleDoi) parts.push('<div style="font-size:12px;color:#999;margin-bottom:3px;">doi: [DOI]</div>');
            if (sec.articleCopyright) parts.push('<div style="font-size:11px;color:#999;margin-bottom:3px;">© [Copyright]</div>');
            if (sec.articleIssn) parts.push('<div style="font-size:11px;color:#999;margin-bottom:3px;">ISSN: [ISSN]</div>');
            if (sec.articleId) parts.push('<div style="font-size:11px;color:#999;margin-bottom:3px;">ID: [Article ID]</div>');
            if (sec.dateOfDownload) parts.push(`<div style="font-size:11px;color:#888;margin-bottom:3px;">Downloaded: ${date}</div>`);
            if (sec.downloadBy) parts.push('<div style="font-size:11px;color:#888;margin-bottom:3px;">By: [User]</div>');
            if (sec.adsBanner?.enabled) parts.push('<div style="margin-top:6px;background:#f0f0f0;padding:6px 10px;border-radius:4px;font-size:11px;color:#888;text-align:center;">[ Ad Banner ]</div>');
            return parts.join('');
        };

        let newPageHtml = '';
        if (newPageSection.enabled && template) {
            let html = template.htmlTemplate.replace(/\{\{DATE\}\}/g, date);
            html = html.replace(/\{\{LINK_URL\}\}/g, newPageSection.link?.url || '#');
            html = html.replace(/\{\{LINK_TEXT\}\}/g, newPageSection.link?.text || '[Link]');
            try {
                const parser = new DOMParser();
                const doc = parser.parseFromString(html, 'text/html');
                if (doc.body) {
                    if (!newPageSection.logo?.enabled) doc.querySelectorAll('.logo-wrapper').forEach(e => e.remove());
                    if (!newPageSection.link?.enabled) doc.querySelectorAll('.link-block').forEach(e => e.remove());
                    if (!newPageSection.articleTitle) doc.querySelectorAll('.article-title-block').forEach(e => e.remove());
                    if (!newPageSection.articleAuthors) doc.querySelectorAll('.authors-block').forEach(e => e.remove());
                    if (!newPageSection.articleDoi) doc.querySelectorAll('.doi-block').forEach(e => e.remove());
                    if (!newPageSection.adsBanner?.enabled) doc.querySelectorAll('.ad-banner-block').forEach(e => e.remove());
                    if (!newPageSection.dateOfDownload) doc.querySelectorAll('.date-block').forEach(e => e.remove());
                    
                    html = doc.body.innerHTML;
                }
            } catch (e) {
                console.error("Error parsing html for template preview", e);
            }
            newPageHtml = html;
        } else if (newPageSection.enabled) {
            newPageHtml = `<div style="padding:40px;font-family:sans-serif;">${renderItems(newPageSection)}</div>`;
        }

        const headerHtml = headerSection.enabled
            ? `<div class="stamp-header">${renderItems(headerSection) || '<span style="color:#bbb;">Header (enabled, no content)</span>'}</div>` : '';
        const footerHtml = footerSection.enabled
            ? `<div class="stamp-footer">${renderItems(footerSection) || '<span style="color:#bbb;">Footer (enabled, no content)</span>'}</div>` : '';
        const leftHtml = leftSection.enabled
            ? `<div class="stamp-margin stamp-left"><span>${renderItems(leftSection) || 'Left Side'}</span></div>` : '';
        const rightHtml = rightSection.enabled
            ? `<div class="stamp-margin stamp-right"><span>${renderItems(rightSection) || 'Right Side'}</span></div>` : '';

        const position = newPageSection.pagePosition ?? 'front';
        const pdfPlaceholder = `<div class="pdf-content"><div style="text-align:center;"><div style="font-size:40px;margin-bottom:8px;">📄</div><div>Original PDF Content</div><div style="font-size:12px;margin-top:4px;">Pages would appear here</div></div></div>`;

        return `<!DOCTYPE html><html><head><meta charset="UTF-8"/><style>
            * { margin:0; padding:0; box-sizing:border-box; }
            body { font-family: sans-serif; background: #d0d0d0; display:flex; flex-direction:column; align-items:center; padding:24px 0; }
            .a4-page { width: 595px; height: 842px; background: white; box-shadow: 0 2px 12px rgba(0,0,0,0.18); margin-bottom: 20px; display: flex; flex-direction: column; overflow: hidden; position: relative; }
            .stamp-header { border-bottom: 1px solid #ddd; padding: 10px 20px; font-size: 12px; background: #fafafa; flex-shrink: 0; }
            .stamp-footer { border-top: 1px solid #ddd; padding: 10px 20px; font-size: 12px; background: #fafafa; flex-shrink: 0; }
            .body-row { display: flex; flex: 1; min-height: 0; }
            .stamp-margin { flex-shrink: 0; width: 32px; background: #fafafa; display: flex; align-items: center; justify-content: center; font-size: 11px; color: #888; }
            .stamp-margin span { writing-mode: vertical-rl; white-space: nowrap; }
            .stamp-left { border-right: 1px solid #ddd; }
            .stamp-left span { transform: rotate(180deg); }
            .stamp-right { border-left: 1px solid #ddd; }
            .pdf-content { flex: 1; display: flex; align-items: center; justify-content: center; color: #bbb; font-size: 16px; background: repeating-linear-gradient(45deg, #fafafa, #fafafa 10px, #fff 10px, #fff 20px); min-width: 0; }
        </style></head><body>
            ${position === 'front' && newPageHtml ? `<div class="a4-page" style="padding:0;"><div style="flex:1;overflow:auto;">${newPageHtml}</div></div>` : ''}
            <div class="a4-page">${headerHtml}<div class="body-row">${leftHtml}${pdfPlaceholder}${rightHtml}</div>${footerHtml}</div>
            ${position === 'back' && newPageHtml ? `<div class="a4-page" style="padding:0;"><div style="flex:1;overflow:auto;">${newPageHtml}</div></div>` : ''}
        </body></html>`;
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40" onClick={onClose}>
            <div className="bg-white rounded-lg shadow-xl w-full max-w-4xl max-h-[90vh] flex flex-col" onClick={e => e.stopPropagation()}>
                <div className="flex items-center justify-between px-5 py-4 border-b border-gray-200">
                    <h2 className="text-lg font-bold text-gray-800">Stamp Preview</h2>
                    <button onClick={onClose} className="p-1.5 text-gray-400 hover:text-gray-700 rounded-md hover:bg-gray-100 cursor-pointer"><X className="w-5 h-5" /></button>
                </div>
                <div className="px-5 py-3 border-b border-gray-100 bg-[#fafafa]">
                    <label className="block text-xs font-bold text-gray-500 uppercase tracking-wider mb-1.5">Template</label>
                    <select className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm bg-white focus:outline-none focus:ring-2 focus:ring-[#a81732]/20 focus:border-[#a81732]"
                        value={templateName} onChange={e => onTemplateChange(e.target.value)}>
                        {stampingConfigData.map(t => (<option key={t.templateName} value={t.templateName}>{t.displayName}</option>))}
                    </select>
                </div>
                <div className="px-5 py-2.5 border-b border-gray-100 flex flex-wrap gap-2">
                    {TABS.map(t => {
                        const items = getSectionSummary(sections[t.key]);
                        if (!items) return null;
                        return (<span key={t.key} className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-medium bg-[#fdf2f4] text-[#a81732] border border-[#a81732]/20">
                            <span className="w-1.5 h-1.5 rounded-full bg-[#a81732]" />{SECTION_LABELS[t.key]}
                        </span>);
                    })}
                </div>
                <div className="flex-1 overflow-auto p-5 bg-[#d0d0d0]">
                    <iframe title="Stamp Preview" srcDoc={buildPreviewHtml()} className="w-full h-[700px] rounded-md border-0" sandbox="allow-same-origin" />
                </div>
            </div>
        </div>
    );
};

const ConfigForm = () => {
    const navigate = useNavigate();
    const { id } = useParams();
    const isEdit = id && id !== 'new';
    const {
        publishers, getConfigById, addConfig, updateConfig,
        addPublisher, addJournalToPublisher,
    } = useConfigContext();

    const defaults = getDefaultConfig();

    const [pubId, setPubId] = useState('');
    const [pubName, setPubName] = useState('');
    const [jcode, setJcode] = useState('');
    const [jName, setJName] = useState('');
    const [templateName, setTemplateName] = useState(defaults.templateName);
    const [sections, setSections] = useState({
        newPage: defaults.newPage,
        header: defaults.header,
        footer: defaults.footer,
        leftMargin: defaults.leftMargin,
        rightMargin: defaults.rightMargin,
    });
    const [activeTab, setActiveTab] = useState('newPage');
    const [isNewPublisher, setIsNewPublisher] = useState(false);
    const [isNewJournal, setIsNewJournal] = useState(false);
    const [toast, setToast] = useState(null);
    const [showPreview, setShowPreview] = useState(false);

    useEffect(() => {
        if (isEdit) {
            const cfg = getConfigById(Number(id));
            if (!cfg) { navigate('/'); return; }
            setPubId(cfg.pubId);
            setJcode(cfg.jcode);
            setTemplateName(cfg.templateName || defaults.templateName);
            setSections({
                newPage: { ...defaults.newPage, ...cfg.newPage },
                header: { ...defaults.header, ...cfg.header },
                footer: { ...defaults.footer, ...cfg.footer },
                leftMargin: { ...defaults.leftMargin, ...cfg.leftMargin },
                rightMargin: { ...defaults.rightMargin, ...cfg.rightMargin },
            });
        }
    }, [id, isEdit]);

    const selectedPublisher = publishers.find(p => p.pubId === pubId);
    const journals = selectedPublisher?.journals || [];

    const showToast = (msg, isError = false) => {
        setToast({ msg, isError });
        setTimeout(() => setToast(null), 3000);
    };

    const updateSection = (key, value) => {
        setSections(prev => ({ ...prev, [key]: value }));
    };

    const handleSave = async () => {
        if (!pubId || !jcode) {
            showToast('Publisher and Journal are required.', true);
            return;
        }
        if (isNewPublisher && pubName) {
            addPublisher({ pubId, pubName, journals: [] });
        }
        if (isNewJournal && jName) {
            addJournalToPublisher(pubId, { jcode, jName });
        }
        const configData = { pubId, jcode, templateName, ...sections };
        let result;
        if (isEdit) {
            result = await updateConfig(Number(id), configData);
        } else {
            result = await addConfig(configData);
        }

        if (result?.success) {
            showToast(isEdit ? 'Configuration updated and saved.' : 'Configuration created and saved.');
        } else {
            showToast('Configuration saved locally but server save failed.', true);
        }
        setTimeout(() => navigate('/'), 500);
    };

    const enabledCount = TABS.filter(t => sections[t.key].enabled).length;

    return (
        <div className="min-h-screen bg-[#f5f5f5]">
            <div className="max-w-4xl mx-auto px-6 py-8">
                <div className="flex items-center justify-between mb-6">
                    <div className="flex items-center gap-4">
                        <button onClick={() => navigate('/')} className="p-2 text-gray-500 hover:text-gray-800 hover:bg-gray-200 rounded-md transition-colors cursor-pointer">
                            <ArrowLeft className="w-5 h-5" />
                        </button>
                        <div>
                            <h1 className="text-2xl font-bold text-gray-800">{isEdit ? 'Edit Configuration' : 'Add Configuration'}</h1>
                            <p className="text-sm text-gray-500 mt-0.5">{enabledCount} section{enabledCount !== 1 ? 's' : ''} enabled</p>
                        </div>
                    </div>
                    <div className="flex items-center gap-3">
                        <button onClick={() => setShowPreview(true)} className="inline-flex items-center gap-2 px-4 py-2.5 text-sm font-semibold text-[#a81732] border border-[#a81732] rounded-md hover:bg-[#fdf2f4] transition-colors cursor-pointer">
                            <Eye className="w-4 h-4" />View Preview
                        </button>
                        <button onClick={handleSave} className="inline-flex items-center gap-2 px-5 py-2.5 text-sm font-semibold text-white bg-[#a81732] rounded-md hover:bg-[#851227] transition-colors cursor-pointer">
                            <Save className="w-4 h-4" />{isEdit ? 'Update' : 'Save'}
                        </button>
                    </div>
                </div>

                {toast && (
                    <div className={`mb-4 px-4 py-2.5 rounded-md text-sm font-medium ${toast.isError ? 'bg-red-50 text-red-700 border border-red-200' : 'bg-green-50 text-green-700 border border-green-200'}`}>{toast.msg}</div>
                )}

                <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-5 mb-5">
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        <div>
                            <label className="block text-sm font-semibold text-gray-700 mb-1">Publisher</label>
                            {!isNewPublisher ? (
                                <div className="flex gap-2">
                                    <SearchableSelect options={publishers.map(p => ({ value: p.pubId, label: p.pubName }))} value={pubId}
                                        onChange={v => { setPubId(v); setJcode(''); setIsNewJournal(false); }} placeholder="Select publisher..." disabled={isEdit} />
                                    {!isEdit && (<button onClick={() => { setIsNewPublisher(true); setPubId(''); setJcode(''); }}
                                        className="px-2 py-1.5 text-xs text-[#a81732] border border-[#a81732] rounded-md hover:bg-[#fdf2f4] transition-colors cursor-pointer whitespace-nowrap">+ New</button>)}
                                </div>
                            ) : (
                                <div className="space-y-1.5">
                                    <input type="text" placeholder="Publisher ID" className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-[#a81732]/20 focus:border-[#a81732]"
                                        value={pubId} onChange={e => setPubId(e.target.value)} />
                                    <input type="text" placeholder="Publisher Name" className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-[#a81732]/20 focus:border-[#a81732]"
                                        value={pubName} onChange={e => setPubName(e.target.value)} />
                                    <button onClick={() => { setIsNewPublisher(false); setPubId(''); }} className="text-xs text-gray-500 hover:text-gray-700 cursor-pointer">← Existing</button>
                                </div>
                            )}
                        </div>
                        <div>
                            <label className="block text-sm font-semibold text-gray-700 mb-1">Journal</label>
                            {!isNewJournal ? (
                                <div className="flex gap-2">
                                    <SearchableSelect options={journals.map(j => ({ value: j.jcode, label: j.jName }))} value={jcode}
                                        onChange={v => setJcode(v)} placeholder="Select journal..." disabled={isEdit || (!pubId && !isNewPublisher)} />
                                    {!isEdit && pubId && (<button onClick={() => { setIsNewJournal(true); setJcode(''); }}
                                        className="px-2 py-1.5 text-xs text-[#a81732] border border-[#a81732] rounded-md hover:bg-[#fdf2f4] transition-colors cursor-pointer whitespace-nowrap">+ New</button>)}
                                </div>
                            ) : (
                                <div className="space-y-1.5">
                                    <input type="text" placeholder="Journal Code" className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-[#a81732]/20 focus:border-[#a81732]"
                                        value={jcode} onChange={e => setJcode(e.target.value)} />
                                    <input type="text" placeholder="Journal Name" className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-[#a81732]/20 focus:border-[#a81732]"
                                        value={jName} onChange={e => setJName(e.target.value)} />
                                    <button onClick={() => { setIsNewJournal(false); setJcode(''); }} className="text-xs text-gray-500 hover:text-gray-700 cursor-pointer">← Existing</button>
                                </div>
                            )}
                        </div>
                    </div>
                </div>

                <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
                    <div className="flex border-b border-gray-200 bg-[#fafafa] overflow-x-auto">
                        {TABS.map(tab => {
                            const Icon = tab.icon;
                            const isActive = activeTab === tab.key;
                            const isEnabled = sections[tab.key].enabled;
                            return (
                                <button key={tab.key} onClick={() => setActiveTab(tab.key)}
                                    className={`flex items-center gap-2 px-5 py-3 text-sm font-semibold whitespace-nowrap border-b-2 transition-colors cursor-pointer ${isActive ? 'border-[#a81732] text-[#a81732] bg-white' : 'border-transparent text-gray-500 hover:text-gray-700 hover:bg-gray-50'}`}>
                                    <Icon className="w-4 h-4" />{tab.label}
                                    {isEnabled && <span className={`w-2 h-2 rounded-full ${isActive ? 'bg-[#a81732]' : 'bg-green-500'}`} />}
                                </button>
                            );
                        })}
                    </div>
                    <div className="p-6">
                        <StampSectionPanel
                            sectionKey={activeTab}
                            section={sections[activeTab]}
                            onChange={(val) => updateSection(activeTab, val)}
                            templateName={templateName}
                            onTemplateChange={setTemplateName}
                        />
                    </div>
                </div>

                <div className="flex gap-3 mt-5">
                    <button onClick={() => navigate('/')} className="flex-1 px-4 py-2.5 text-sm font-semibold text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 transition-colors cursor-pointer">Cancel</button>
                    <button onClick={() => setShowPreview(true)} className="flex-1 inline-flex items-center justify-center gap-2 px-4 py-2.5 text-sm font-semibold text-[#a81732] bg-white border border-[#a81732] rounded-md hover:bg-[#fdf2f4] transition-colors cursor-pointer">
                        <Eye className="w-4 h-4" />View Preview
                    </button>
                    <button onClick={handleSave} className="flex-1 inline-flex items-center justify-center gap-2 px-4 py-2.5 text-sm font-semibold text-white bg-[#a81732] rounded-md hover:bg-[#851227] transition-colors cursor-pointer">
                        <Save className="w-4 h-4" />{isEdit ? 'Update' : 'Save'} Configuration
                    </button>
                </div>
            </div>

            {showPreview && (
                <PreviewModal sections={sections} templateName={templateName} onTemplateChange={setTemplateName} onClose={() => setShowPreview(false)} />
            )}
        </div>
    );
};

export default ConfigForm;

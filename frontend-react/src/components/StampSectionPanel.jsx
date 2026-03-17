const inputClass = 'w-full px-3 py-1.5 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-[#a81732]/20 focus:border-[#a81732]';

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

const StampSectionPanel = ({ section, onChange, sectionKey }) => {
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
                        <div className="bg-gray-50 border border-gray-200 rounded-md p-4">
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
                            <div className={`overflow-hidden transition-all duration-200 ${section.html?.enabled ? 'max-h-40 opacity-100 mt-1.5 ml-6.5' : 'max-h-0 opacity-0'}`}>
                                <textarea rows="3" className={`${inputClass} resize-y min-h-[60px] font-mono`}
                                    placeholder='e.g. <b>Draft</b> or any HTML content'
                                    value={section.html?.value ?? ''}
                                    onChange={e => update('html', { enabled: true, value: e.target.value })}
                                />
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

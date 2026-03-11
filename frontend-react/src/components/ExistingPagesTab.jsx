import React from 'react';
import { useTemplateContext } from '../context/TemplateContext';

const PositionBlock = ({ positionLabel, positionKey }) => {
    const { overlayConfig, updateOverlayConfig } = useTemplateContext();
    const config = overlayConfig[positionKey];

    const handleToggle = (e) => updateOverlayConfig(positionKey, 'enabled', e.target.checked);
    const handleCheck = (field) => (e) => updateOverlayConfig(positionKey, field, e.target.checked);
    const handleChange = (field) => (e) => updateOverlayConfig(positionKey, field, e.target.value);

    const handleFileChange = (field) => (e) => {
        const file = e.target.files[0];
        if (!file) return;
        const reader = new FileReader();
        reader.onload = (ev) => {
            const base64 = ev.target.result.split(',')[1];
            updateOverlayConfig(positionKey, field, base64);
        };
        reader.readAsDataURL(file);
    };

    return (
        <div className={`bg-[#fafafa] border-2 rounded-md p-3 transition-all duration-200 ${config.enabled ? 'border-[#a81732] ring-1 ring-[#a81732]' : 'border-gray-200'}`}>
            <div className="flex items-center justify-between gap-4">
                <span className="font-semibold text-[0.95rem] text-gray-800">{positionLabel}</span>
                <label className="relative inline-flex items-center cursor-pointer shrink-0">
                    <input type="checkbox" className="sr-only peer" checked={config.enabled} onChange={handleToggle} />
                    <div className="w-9 h-5 bg-gray-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-4 after:w-4 after:transition-all peer-checked:bg-[#a81732]"></div>
                </label>
            </div>

            <div className={`overflow-hidden transition-all duration-300 ease-in-out ${config.enabled ? 'max-h-[2000px] opacity-100 mt-2' : 'max-h-0 opacity-0'}`}>
                <div className="bg-white border border-gray-200 rounded-md p-3">
                    <p className="text-xs text-gray-500 mb-4">Configure what to stamp on existing pages in this position.</p>

                    <label className="flex items-center gap-2 cursor-pointer text-sm mb-3">
                        <input type="checkbox" className="w-4 h-4 accent-[#a81732] cursor-pointer" checked={config.includeCurrentUser} onChange={handleCheck('includeCurrentUser')} />
                        <span className="text-gray-800 select-none">Include Current User Information</span>
                    </label>

                    <div className="space-y-3">
                        <div className="flex flex-col gap-2">
                            <label className="flex items-center gap-2 cursor-pointer text-sm">
                                <input type="checkbox" className="w-4 h-4 accent-[#a81732] cursor-pointer" checked={!!config.logo} onChange={e => {
                                    if (!e.target.checked) updateOverlayConfig(positionKey, 'logo', null);
                                    else updateOverlayConfig(positionKey, 'logo', 'pending');
                                }} />
                                <span className="text-gray-800 select-none">Add Logo Image</span>
                            </label>
                            <div className={`overflow-hidden transition-all duration-200 ${config.logo ? 'max-h-40 opacity-100' : 'max-h-0 opacity-0'}`}>
                                <input type="file" className="block w-full text-sm text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded-md file:border-0 file:text-sm file:font-semibold file:bg-[#fdf2f4] file:text-[#a81732] hover:file:bg-[#fbdde1] border border-gray-300 rounded-md bg-gray-50 p-1" accept="image/png, image/jpeg" onChange={handleFileChange('logo')} />
                            </div>
                        </div>

                        <div className="flex flex-col gap-2">
                            <label className="flex items-center gap-2 cursor-pointer text-sm">
                                <input type="checkbox" className="w-4 h-4 accent-[#a81732] cursor-pointer" checked={config.text !== '' && config.text !== undefined} onChange={e => {
                                    if (!e.target.checked) updateOverlayConfig(positionKey, 'text', '');
                                    else updateOverlayConfig(positionKey, 'text', ' ');
                                }} />
                                <span className="text-gray-800 select-none">Add Plain Text</span>
                            </label>
                            <div className={`overflow-hidden transition-all duration-200 ${config.text ? 'max-h-40 opacity-100' : 'max-h-0 opacity-0'}`}>
                                <input type="text" className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-[#a81732]/20 focus:border-[#a81732]" placeholder="Enter text to stamp" value={config.text} onChange={handleChange('text')} />
                            </div>
                        </div>

                        <div className="flex flex-col gap-2">
                            <label className="flex items-center gap-2 cursor-pointer text-sm">
                                <input type="checkbox" className="w-4 h-4 accent-[#a81732] cursor-pointer" checked={config.html !== '' && config.html !== undefined} onChange={e => {
                                    if (!e.target.checked) updateOverlayConfig(positionKey, 'html', '');
                                    else updateOverlayConfig(positionKey, 'html', ' ');
                                }} />
                                <span className="text-gray-800 select-none">Add HTML Overlay</span>
                            </label>
                            <div className={`overflow-hidden transition-all duration-200 ${config.html ? 'max-h-40 opacity-100' : 'max-h-0 opacity-0'}`}>
                                <textarea rows="3" className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-[#a81732]/20 focus:border-[#a81732] resize-y min-h-[60px]" placeholder="Enter HTML snippet (e.g. <b>Draft</b>)" value={config.html} onChange={handleChange('html')}></textarea>
                            </div>
                        </div>

                        <div className="flex flex-col gap-2">
                            <label className="flex items-center gap-2 cursor-pointer text-sm">
                                <input type="checkbox" className="w-4 h-4 accent-[#a81732] cursor-pointer" checked={config.doi !== '' && config.doi !== undefined} onChange={e => {
                                    if (!e.target.checked) updateOverlayConfig(positionKey, 'doi', '');
                                    else updateOverlayConfig(positionKey, 'doi', ' ');
                                }} />
                                <span className="text-gray-800 select-none">Add DOI</span>
                            </label>
                            <div className={`overflow-hidden transition-all duration-200 ${config.doi ? 'max-h-40 opacity-100' : 'max-h-0 opacity-0'}`}>
                                <input type="text" className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-[#a81732]/20 focus:border-[#a81732]" placeholder="e.g. 10.3174/ajnr.A8959" value={config.doi} onChange={handleChange('doi')} />
                            </div>
                        </div>

                        <div className="flex flex-col gap-2">
                            <label className="flex items-center gap-2 cursor-pointer text-sm">
                                <input type="checkbox" className="w-4 h-4 accent-[#a81732] cursor-pointer" checked={config.date} onChange={handleCheck('date')} />
                                <span className="text-gray-800 select-none">Add Current Date</span>
                            </label>
                        </div>

                        <div className="flex flex-col gap-2">
                            <label className="flex items-center gap-2 cursor-pointer text-sm">
                                <input type="checkbox" className="w-4 h-4 accent-[#a81732] cursor-pointer" checked={config.ad !== '' && config.ad !== undefined} onChange={e => {
                                    if (!e.target.checked) updateOverlayConfig(positionKey, 'ad', '');
                                    else updateOverlayConfig(positionKey, 'ad', ' ');
                                }} />
                                <span className="text-gray-800 select-none">Include Advertisement</span>
                            </label>
                            <div className={`overflow-hidden transition-all duration-200 ${config.ad ? 'max-h-40 opacity-100' : 'max-h-0 opacity-0'}`}>
                                <input type="text" className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-[#a81732]/20 focus:border-[#a81732]" placeholder="Target click URL for the ad" value={config.ad} onChange={handleChange('ad')} />
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

const ExistingPagesTab = () => {
    return (
        <div id="tab-editExisting" className="block animate-[fadeIn_0.3s_ease-out]">
            <div className="mb-5">
                <div className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-3">Overlay Configurations</div>
                <p className="text-sm text-[#5a5a5a] mb-6">
                    Select positions to stamp content onto the <strong>pre-existing pages</strong> of the document.
                </p>

                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
                    <PositionBlock positionLabel="Header (Top)" positionKey="header" />
                    <PositionBlock positionLabel="Footer (Bottom)" positionKey="footer" />
                    <PositionBlock positionLabel="Left Margin" positionKey="leftMargin" />
                    <PositionBlock positionLabel="Right Margin" positionKey="rightMargin" />
                </div>
            </div>
        </div>
    );
};

export default ExistingPagesTab;

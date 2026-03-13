import React, { useState, useMemo } from 'react';
import { useTemplateContext } from '../context/TemplateContext';

const POSITIONS = [
    { key: 'header', label: 'Header', shortLabel: 'H' },
    { key: 'footer', label: 'Footer', shortLabel: 'F' },
    { key: 'leftMargin', label: 'Left Margin', shortLabel: 'L' },
    { key: 'rightMargin', label: 'Right Margin', shortLabel: 'R' },
];

const PositionConfigPanel = ({ positionKey, positionLabel, onClose }) => {
    const { overlayConfig, updateOverlayConfig } = useTemplateContext();
    const config = overlayConfig[positionKey];

    const handleCheck = (field) => (e) => updateOverlayConfig(positionKey, field, e.target.checked);
    const handleChange = (field) => (e) => updateOverlayConfig(positionKey, field, e.target.value);
    const handleFileChange = (field) => (e) => {
        const file = e.target.files[0];
        if (!file) return;
        const reader = new FileReader();
        reader.onload = (ev) => updateOverlayConfig(positionKey, field, ev.target.result.split(',')[1]);
        reader.readAsDataURL(file);
    };

    return (
        <div className="animate-[fadeIn_0.2s_ease-out]">
            <div className="flex items-center justify-between mb-4">
                <h3 className="text-sm font-semibold text-gray-800 uppercase tracking-wide">{positionLabel} Configuration</h3>
                <button onClick={onClose} className="text-gray-400 hover:text-gray-600 transition-colors p-1" title="Close">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="w-4 h-4"><path d="M18 6L6 18M6 6l12 12"/></svg>
                </button>
            </div>

            <div className="space-y-3">
                <label className="flex items-center gap-2 cursor-pointer text-sm">
                    <input type="checkbox" className="w-4 h-4 accent-[#a81732]" checked={config.includeCurrentUser} onChange={handleCheck('includeCurrentUser')} />
                    <span className="text-gray-800 select-none">Include Current User</span>
                </label>

                {/* Logo */}
                <div className="flex flex-col gap-2">
                    <label className="flex items-center gap-2 cursor-pointer text-sm">
                        <input type="checkbox" className="w-4 h-4 accent-[#a81732]" checked={!!config.logo} onChange={e => updateOverlayConfig(positionKey, 'logo', e.target.checked ? 'pending' : null)} />
                        <span className="text-gray-800 select-none">Add Logo Image</span>
                    </label>
                    <div className={`overflow-hidden transition-all duration-200 ${config.logo ? 'max-h-40 opacity-100' : 'max-h-0 opacity-0'}`}>
                        <input type="file" className="block w-full text-sm text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded-md file:border-0 file:text-sm file:font-semibold file:bg-[#fdf2f4] file:text-[#a81732] hover:file:bg-[#fbdde1] border border-gray-300 rounded-md bg-gray-50 p-1" accept="image/png, image/jpeg" onChange={handleFileChange('logo')} />
                    </div>
                </div>

                {/* Text */}
                <div className="flex flex-col gap-2">
                    <label className="flex items-center gap-2 cursor-pointer text-sm">
                        <input type="checkbox" className="w-4 h-4 accent-[#a81732]" checked={config.text !== '' && config.text !== undefined} onChange={e => updateOverlayConfig(positionKey, 'text', e.target.checked ? ' ' : '')} />
                        <span className="text-gray-800 select-none">Add Plain Text</span>
                    </label>
                    <div className={`overflow-hidden transition-all duration-200 ${config.text ? 'max-h-40 opacity-100' : 'max-h-0 opacity-0'}`}>
                        <input type="text" className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-[#a81732]/20 focus:border-[#a81732]" placeholder="Enter text to stamp" value={config.text} onChange={handleChange('text')} />
                    </div>
                </div>

                {/* HTML */}
                <div className="flex flex-col gap-2">
                    <label className="flex items-center gap-2 cursor-pointer text-sm">
                        <input type="checkbox" className="w-4 h-4 accent-[#a81732]" checked={config.html !== '' && config.html !== undefined} onChange={e => updateOverlayConfig(positionKey, 'html', e.target.checked ? ' ' : '')} />
                        <span className="text-gray-800 select-none">Add HTML Overlay</span>
                    </label>
                    <div className={`overflow-hidden transition-all duration-200 ${config.html ? 'max-h-40 opacity-100' : 'max-h-0 opacity-0'}`}>
                        <textarea rows="3" className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-[#a81732]/20 focus:border-[#a81732] resize-y min-h-[60px]" placeholder='e.g. <b>Draft</b>' value={config.html} onChange={handleChange('html')} />
                    </div>
                </div>

                {/* DOI */}
                <div className="flex flex-col gap-2">
                    <label className="flex items-center gap-2 cursor-pointer text-sm">
                        <input type="checkbox" className="w-4 h-4 accent-[#a81732]" checked={config.doi !== '' && config.doi !== undefined} onChange={e => updateOverlayConfig(positionKey, 'doi', e.target.checked ? ' ' : '')} />
                        <span className="text-gray-800 select-none">Add DOI</span>
                    </label>
                    <div className={`overflow-hidden transition-all duration-200 ${config.doi ? 'max-h-40 opacity-100' : 'max-h-0 opacity-0'}`}>
                        <input type="text" className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-[#a81732]/20 focus:border-[#a81732]" placeholder="e.g. 10.3174/ajnr.A8959" value={config.doi} onChange={handleChange('doi')} />
                    </div>
                </div>

                {/* Date */}
                <label className="flex items-center gap-2 cursor-pointer text-sm">
                    <input type="checkbox" className="w-4 h-4 accent-[#a81732]" checked={config.date} onChange={handleCheck('date')} />
                    <span className="text-gray-800 select-none">Add Current Date</span>
                </label>

                {/* Ad */}
                <div className="flex flex-col gap-2">
                    <label className="flex items-center gap-2 cursor-pointer text-sm">
                        <input type="checkbox" className="w-4 h-4 accent-[#a81732]" checked={config.ad !== '' && config.ad !== undefined} onChange={e => updateOverlayConfig(positionKey, 'ad', e.target.checked ? ' ' : '')} />
                        <span className="text-gray-800 select-none">Include Advertisement</span>
                    </label>
                    <div className={`overflow-hidden transition-all duration-200 ${config.ad ? 'max-h-40 opacity-100' : 'max-h-0 opacity-0'}`}>
                        <input type="text" className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-[#a81732]/20 focus:border-[#a81732]" placeholder="Target click URL for the ad" value={config.ad} onChange={handleChange('ad')} />
                    </div>
                </div>
            </div>
        </div>
    );
};

/** Builds a short summary string for a position's config */
const buildSummary = (config) => {
    if (!config.enabled) return null;
    const parts = [];
    if (config.text && config.text.trim()) parts.push('Text');
    if (config.html && config.html.trim()) parts.push('HTML');
    if (config.logo) parts.push('Logo');
    if (config.doi && config.doi.trim()) parts.push('DOI');
    if (config.date) parts.push('Date');
    if (config.ad && config.ad.trim()) parts.push('Ad');
    if (config.includeCurrentUser) parts.push('User');
    return parts.length ? parts.join(' · ') : 'Enabled (empty)';
};

const formatDate = () => {
    const d = new Date();
    const months = ['January','February','March','April','May','June','July','August','September','October','November','December'];
    return `${months[d.getMonth()]} ${d.getDate()}, ${d.getFullYear()}`;
};

/** Collects the stamp content items for a position as renderable text pieces */
const getStampItems = (config) => {
    if (!config.enabled) return [];
    const items = [];
    if (config.logo && config.logo !== 'pending') items.push({ type: 'logo', value: config.logo });
    if (config.text && config.text.trim()) items.push({ type: 'text', value: config.text.trim() });
    if (config.doi && config.doi.trim()) items.push({ type: 'doi', value: `doi: ${config.doi.trim().startsWith('http') ? config.doi.trim() : 'https://doi.org/' + config.doi.trim()}` });
    if (config.date) items.push({ type: 'text', value: `Date Generated: ${formatDate()}` });
    if (config.html && config.html.trim()) items.push({ type: 'html', value: config.html.trim() });
    if (config.ad && config.ad.trim()) items.push({ type: 'ad', value: 'Advertisement' });
    if (config.includeCurrentUser) items.push({ type: 'text', value: 'Current User' });
    return items;
};

/**
 * Renders stamp content inside a zone, matching how the backend stamper places it:
 * - Header: horizontally centered at top, inside page with margin
 * - Footer: horizontally centered at bottom, inside page with margin
 * - Left/Right Margin: vertically centered, text rotated 90deg, inside page with margin
 */
const StampContent = ({ config, isVertical }) => {
    const items = getStampItems(config);
    if (!items.length) return null;

    // For vertical margins: use smaller font, allow the text to flow naturally in the rotated writing mode
    const fontSize = isVertical ? '7px' : '9px';

    return (
        <div className="flex flex-col items-center justify-center gap-0.5 font-semibold text-[#1a1918]"
             style={{ fontFamily: 'Times New Roman, Arial, sans-serif', fontSize, lineHeight: 1.2, maxWidth: '100%', maxHeight: '100%', overflow: 'hidden', padding: '2px' }}>
            {items.map((item, i) => {
                if (item.type === 'logo') {
                    return <img key={i} src={`data:image/png;base64,${item.value}`} alt="Logo"
                                style={{ maxHeight: isVertical ? 14 : 16, maxWidth: isVertical ? 14 : 50, objectFit: 'contain' }} />;
                }
                if (item.type === 'html') {
                    return <span key={i} dangerouslySetInnerHTML={{ __html: item.value }}
                                 style={{ fontSize: 'inherit', lineHeight: 'inherit' }} />;
                }
                if (item.type === 'ad') {
                    return <span key={i} className="bg-gray-200 text-gray-600 px-1 rounded" style={{ fontSize: '6px' }}>Ad</span>;
                }
                if (item.type === 'doi') {
                    return <span key={i} style={{ whiteSpace: isVertical ? 'normal' : 'nowrap', color: 'black', fontSize: isVertical ? '6px' : '8px' }}>{item.value}</span>;
                }
                return <span key={i} style={{ whiteSpace: isVertical ? 'normal' : 'nowrap' }}>{item.value}</span>;
            })}
        </div>
    );
};

const ExistingPagesTab = () => {
    const { overlayConfig, updateOverlayConfig } = useTemplateContext();
    const [selectedPosition, setSelectedPosition] = useState(null);

    const handleZoneClick = (posKey) => {
        if (selectedPosition === posKey) {
            setSelectedPosition(null);
            return;
        }
        setSelectedPosition(posKey);
        if (!overlayConfig[posKey].enabled) {
            updateOverlayConfig(posKey, 'enabled', true);
        }
    };

    const activeCount = useMemo(() =>
        POSITIONS.filter(p => overlayConfig[p.key].enabled).length
    , [overlayConfig]);

    // Margin constant matching backend's near-zero margin
    const MARGIN = '0.4%';
    // Zone thickness for header/footer
    const ZONE_H = '6%';
    // Zone thickness for left/right margin
    const ZONE_W = '7%';

    const zoneBaseClass = 'absolute flex items-center justify-center cursor-pointer transition-all duration-200 z-10 overflow-hidden';

    const getZoneStyle = (posKey) => {
        const isSelected = selectedPosition === posKey;
        const isEnabled = overlayConfig[posKey].enabled;
        if (isSelected) return 'ring-2 ring-[#a81732] ring-inset bg-[#a81732]/10';
        if (isEnabled) return 'bg-[#a81732]/5';
        return 'hover:bg-gray-50';
    };

    return (
        <div id="tab-editExisting" className="block animate-[fadeIn_0.3s_ease-out]">
            <div className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-2">Overlay Configurations</div>
            <p className="text-sm text-[#5a5a5a] mb-5">
                Click a zone on the preview to configure stamps for that position. {activeCount > 0 && <span className="text-[#a81732] font-medium">{activeCount} position{activeCount > 1 ? 's' : ''} active.</span>}
            </p>

            <div className="flex flex-col lg:flex-row gap-5 min-h-[550px]">

                {/* Left: Visual Preview */}
                <div className="flex-1 flex flex-col items-center">
                    <div className="flex items-center gap-1.5 px-4 py-2.5 bg-[#f0f0f0] border border-gray-300 border-b-0 rounded-t-md text-[0.75rem] font-bold text-gray-500 tracking-wider uppercase w-full max-w-[480px]">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="w-4 h-4"><rect x="2" y="3" width="20" height="14" rx="2" ry="2"/><line x1="8" y1="21" x2="16" y2="21"/><line x1="12" y1="17" x2="12" y2="21"/></svg>
                        Stamp Preview
                    </div>
                    <div className="bg-[#e0e0e0] border border-gray-300 border-t-0 rounded-b-md p-6 w-full max-w-[480px] flex justify-center">
                        {/* PDF Page mockup — all zones are INSIDE the page with margins */}
                        <div className="bg-white shadow-lg relative select-none" style={{ width: '100%', maxWidth: 360, aspectRatio: '1 / 1.414' }}>
                            {/* Faux page content lines */}
                            <div className="absolute flex flex-col justify-center items-center pointer-events-none opacity-20"
                                 style={{ top: '14%', bottom: '14%', left: '12%', right: '12%' }}>
                                {[...Array(12)].map((_, i) => (
                                    <div key={i} className="w-full h-[5px] bg-gray-400 rounded-sm mb-2.5" style={{ width: `${55 + Math.sin(i) * 30}%` }} />
                                ))}
                            </div>

                            {/* HEADER zone — inside page, at top with margin */}
                            <div
                                onClick={() => handleZoneClick('header')}
                                className={`${zoneBaseClass} ${getZoneStyle('header')}`}
                                style={{ top: MARGIN, left: MARGIN, right: MARGIN, height: ZONE_H }}
                                title="Click to configure Header stamp"
                            >
                                {overlayConfig.header.enabled
                                    ? <StampContent config={overlayConfig.header} isVertical={false} />
                                    : <span className="text-gray-300 text-[10px] tracking-wider uppercase">Header</span>
                                }
                            </div>

                            {/* FOOTER zone — inside page, at bottom with margin */}
                            <div
                                onClick={() => handleZoneClick('footer')}
                                className={`${zoneBaseClass} ${getZoneStyle('footer')}`}
                                style={{ bottom: MARGIN, left: MARGIN, right: MARGIN, height: ZONE_H }}
                                title="Click to configure Footer stamp"
                            >
                                {overlayConfig.footer.enabled
                                    ? <StampContent config={overlayConfig.footer} isVertical={false} />
                                    : <span className="text-gray-300 text-[10px] tracking-wider uppercase">Footer</span>
                                }
                            </div>

                            {/* LEFT MARGIN zone — inside page, vertically centered, text rotated */}
                            <div
                                onClick={() => handleZoneClick('leftMargin')}
                                className={`${zoneBaseClass} ${getZoneStyle('leftMargin')}`}
                                style={{ top: `calc(${MARGIN} + ${ZONE_H})`, bottom: `calc(${MARGIN} + ${ZONE_H})`, left: MARGIN, width: ZONE_W }}
                                title="Click to configure Left Margin stamp"
                            >
                                <div style={{ writingMode: 'vertical-rl', transform: 'rotate(180deg)' }}>
                                    {overlayConfig.leftMargin.enabled
                                        ? <StampContent config={overlayConfig.leftMargin} isVertical={true} />
                                        : <span className="text-gray-300 text-[10px] tracking-wider uppercase">Left</span>
                                    }
                                </div>
                            </div>

                            {/* RIGHT MARGIN zone — inside page, vertically centered, text rotated */}
                            <div
                                onClick={() => handleZoneClick('rightMargin')}
                                className={`${zoneBaseClass} ${getZoneStyle('rightMargin')}`}
                                style={{ top: `calc(${MARGIN} + ${ZONE_H})`, bottom: `calc(${MARGIN} + ${ZONE_H})`, right: MARGIN, width: ZONE_W }}
                                title="Click to configure Right Margin stamp"
                            >
                                <div style={{ writingMode: 'vertical-rl' }}>
                                    {overlayConfig.rightMargin.enabled
                                        ? <StampContent config={overlayConfig.rightMargin} isVertical={true} />
                                        : <span className="text-gray-300 text-[10px] tracking-wider uppercase">Right</span>
                                    }
                                </div>
                            </div>

                            {/* Dashed border guides when nothing is active */}
                            {activeCount === 0 && (
                                <div className="absolute pointer-events-none border border-dashed border-gray-200 rounded-sm"
                                     style={{ top: MARGIN, left: MARGIN, right: MARGIN, bottom: MARGIN }} />
                            )}
                        </div>
                    </div>

                    {/* Quick toggle pills below preview */}
                    <div className="flex gap-2 mt-4 w-full max-w-[480px] justify-center flex-wrap">
                        {POSITIONS.map(pos => {
                            const isActive = overlayConfig[pos.key].enabled;
                            const isSelected = selectedPosition === pos.key;
                            return (
                                <button
                                    key={pos.key}
                                    type="button"
                                    onClick={() => handleZoneClick(pos.key)}
                                    className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-semibold transition-all duration-200 border cursor-pointer
                                        ${isSelected
                                            ? 'bg-[#a81732] text-white border-[#a81732] shadow-sm'
                                            : isActive
                                                ? 'bg-[#fdf2f4] text-[#a81732] border-[#a81732]/40 hover:bg-[#fbdde1]'
                                                : 'bg-white text-gray-500 border-gray-300 hover:border-gray-400 hover:text-gray-700'
                                        }`}
                                >
                                    <span className={`w-2 h-2 rounded-full ${isActive ? 'bg-[#a81732]' : 'bg-gray-300'} ${isSelected ? '!bg-white' : ''}`} />
                                    {pos.label}
                                </button>
                            );
                        })}
                    </div>
                </div>

                {/* Right: Configuration Panel */}
                <div className="w-full lg:w-[340px] shrink-0">
                    <div className="border border-gray-300 rounded-md bg-white overflow-hidden sticky top-4">
                        <div className="flex items-center gap-1.5 px-4 py-2.5 bg-[#f0f0f0] border-b border-gray-300 text-[0.75rem] font-bold text-gray-500 tracking-wider uppercase">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="w-4 h-4"><path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/></svg>
                            Position Settings
                        </div>
                        <div className="p-4 min-h-[300px]">
                            {selectedPosition ? (
                                <PositionConfigPanel
                                    positionKey={selectedPosition}
                                    positionLabel={POSITIONS.find(p => p.key === selectedPosition)?.label}
                                    onClose={() => setSelectedPosition(null)}
                                />
                            ) : (
                                <div className="flex flex-col items-center justify-center h-[280px] text-center">
                                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" className="w-10 h-10 text-gray-300 mb-3">
                                        <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
                                        <line x1="3" y1="9" x2="21" y2="9"/>
                                        <line x1="9" y1="21" x2="9" y2="9"/>
                                    </svg>
                                    <p className="text-sm text-gray-400">Click a zone on the preview<br/>or a pill below to configure it.</p>
                                </div>
                            )}

                            {/* Enable/Disable toggle for selected position */}
                            {selectedPosition && (
                                <div className="mt-4 pt-3 border-t border-gray-200 flex items-center justify-between">
                                    <span className="text-sm text-gray-600">Enable this position</span>
                                    <label className="relative inline-flex items-center cursor-pointer shrink-0">
                                        <input type="checkbox" className="sr-only peer" checked={overlayConfig[selectedPosition].enabled} onChange={e => updateOverlayConfig(selectedPosition, 'enabled', e.target.checked)} />
                                        <div className="w-9 h-5 bg-gray-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-4 after:w-4 after:transition-all peer-checked:bg-[#a81732]"></div>
                                    </label>
                                </div>
                            )}
                        </div>
                    </div>

                    {/* Summary cards */}
                    {activeCount > 0 && (
                        <div className="mt-4 space-y-2">
                            <div className="text-xs font-semibold text-gray-500 uppercase tracking-wide">Active Stamps</div>
                            {POSITIONS.filter(p => overlayConfig[p.key].enabled).map(pos => (
                                <div
                                    key={pos.key}
                                    onClick={() => handleZoneClick(pos.key)}
                                    className={`flex items-center gap-3 px-3 py-2 rounded-md border cursor-pointer transition-all duration-150
                                        ${selectedPosition === pos.key ? 'border-[#a81732] bg-[#fdf2f4]' : 'border-gray-200 bg-white hover:border-gray-300'}`}
                                >
                                    <span className="w-2 h-2 rounded-full bg-[#a81732] shrink-0" />
                                    <div className="min-w-0 flex-1">
                                        <div className="text-xs font-semibold text-gray-700">{pos.label}</div>
                                        <div className="text-[10px] text-gray-400 truncate">{buildSummary(overlayConfig[pos.key])}</div>
                                    </div>
                                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="w-3.5 h-3.5 text-gray-400 shrink-0"><polyline points="9 18 15 12 9 6"/></svg>
                                </div>
                            ))}
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};

export default ExistingPagesTab;

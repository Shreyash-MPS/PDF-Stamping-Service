import React, { useState } from 'react';
import { useTemplateContext } from '../context/TemplateContext';
import TemplateTab from './TemplateTab';
import ExistingPagesTab from './ExistingPagesTab';
import axios from 'axios';

const MainPortal = () => {
    const {
        publisherId, setPublisherId, jcode, setJcode,
        currentTemplateKey, setCurrentTemplateKey, templatesState, getResolvedHtmlForSave, overlayConfig,
        updateShortcode, updateHtmlTemplate, updateOverlayConfig
    } = useTemplateContext();

    const [activeTab, setActiveTab] = useState('addNewPage');
    const [enableAddNewPage, setEnableAddNewPage] = useState(true);
    const [loading, setLoading] = useState(false);
    const [toast, setToast] = useState({ show: false, message: '', isError: false });

    const showToast = (message, isError = false) => {
        setToast({ show: true, message, isError });
        setTimeout(() => setToast({ show: false, message: '', isError: false }), 4000);
    };

    const handleLoadTemplate = async () => {
        if (!publisherId || !jcode) {
            showToast('Please fill in Publisher ID and Jcode first.', true);
            return;
        }

        try {
            // 1. Load Template Defaults
            const templatePromise = axios.get(
                `http://localhost:8080/api/v1/template/load?publisherId=${encodeURIComponent(publisherId)}&jcode=${encodeURIComponent(jcode)}`
            ).catch(e => null); // Catch if template doesn't exist but config does

            // 2. Load Overlay Configuration
            const configPromise = axios.get(
                `http://localhost:8080/api/v1/config/load?publisherId=${encodeURIComponent(publisherId)}&jcode=${encodeURIComponent(jcode)}`
            ).catch(e => null);

            const [templateRes, configRes] = await Promise.all([templatePromise, configPromise]);

            if (!templateRes?.data && !configRes?.data) {
                showToast('No saved configuration or template found for this Publisher/Jcode.', true);
                return;
            }

            // Apply Template State
            if (templateRes?.data) {
                const data = templateRes.data;
                if (data.templateName && templatesState[data.templateName]) {
                    setCurrentTemplateKey(data.templateName);
                }

                if (data.shortcodes) {
                    Object.keys(data.shortcodes).forEach(key => {
                        const templateKey = data.templateName || currentTemplateKey;
                        if (templatesState[templateKey]?.shortcodes[key]) {
                            updateShortcode(templateKey, key, data.shortcodes[key]);
                        }
                    });
                }

                if (data.htmlTemplate) {
                    // Clean up any historically saved contenteditable wrappers injected by accident
                    let cleanHtml = data.htmlTemplate.replace(/<span contenteditable="true" style="outline: none;">(\{\{[a-zA-Z_]+\}\})<\/span>/g, '$1');
                    updateHtmlTemplate(data.templateName || currentTemplateKey, cleanHtml);
                }
            }

            // Apply Configuration State
            if (configRes?.data && configRes.data.positions) {
                const positions = configRes.data.positions;

                // Rehydrate 'enableAddNewPage'
                if (positions['CUSTOM'] && positions['CUSTOM'].addNewPage) {
                    setEnableAddNewPage(true);
                } else {
                    setEnableAddNewPage(false);
                }

                // Reset overlay config before applying
                ['header', 'footer', 'leftMargin', 'rightMargin'].forEach(pos => {
                    updateOverlayConfig(pos, 'enabled', false);
                    updateOverlayConfig(pos, 'text', '');
                    updateOverlayConfig(pos, 'html', '');
                    updateOverlayConfig(pos, 'doi', '');
                    updateOverlayConfig(pos, 'ad', '');
                });

                // Mapping of backend keys to frontend context keys
                const posMap = {
                    'HEADER': 'header',
                    'FOOTER': 'footer',
                    'LEFT_MARGIN': 'leftMargin',
                    'RIGHT_MARGIN': 'rightMargin'
                };

                Object.keys(posMap).forEach(backendPosKey => {
                    if (positions[backendPosKey]) {
                        const frontendPosKey = posMap[backendPosKey];
                        const conf = positions[backendPosKey];

                        updateOverlayConfig(frontendPosKey, 'enabled', true);
                        updateOverlayConfig(frontendPosKey, 'includeCurrentUser', conf.includeCurrentUser || false);

                        if (conf.logo && conf.logo.base64) updateOverlayConfig(frontendPosKey, 'logo', conf.logo.base64);
                        if (conf.text && conf.text.content) updateOverlayConfig(frontendPosKey, 'text', conf.text.content);
                        if (conf.html && conf.html.content) updateOverlayConfig(frontendPosKey, 'html', conf.html.content);
                        if (conf.doi && conf.doi.value) updateOverlayConfig(frontendPosKey, 'doi', conf.doi.value);
                        if (conf.date && conf.date.enabled) updateOverlayConfig(frontendPosKey, 'date', true);
                        if (conf.ad && conf.ad.link) updateOverlayConfig(frontendPosKey, 'ad', conf.ad.link);
                    }
                });
            }

            showToast('Template and Configuration loaded successfully!');
        } catch (error) {
            console.error('Error loading configuration:', error);
            showToast('Failed to load configuration. Is the server running?', true);
        }
    };

    const handleSaveTemplate = async () => {
        if (!publisherId || !jcode) {
            showToast('Please fill in Publisher ID and Jcode first.', true);
            return;
        }

        const template = templatesState[currentTemplateKey];
        const shortcodesToSave = {};

        if (template.shortcodes) {
            Object.keys(template.shortcodes).forEach(key => {
                shortcodesToSave[key] = template.shortcodes[key].value !== undefined
                    ? template.shortcodes[key].value
                    : template.shortcodes[key].default;
            });
        }

        const payload = {
            publisherId: publisherId,
            jcode: jcode,
            templateName: currentTemplateKey,
            shortcodes: shortcodesToSave,
            htmlTemplate: template.htmlTemplate,
            resolvedHtml: getResolvedHtmlForSave()
        };

        try {
            const response = await axios.post('http://localhost:8080/api/v1/template/save', payload);
            if (response.data.success) {
                showToast('Template saved successfully!');
            } else {
                showToast('Failed to save template: ' + response.data.message, true);
            }
        } catch (error) {
            console.error('Error saving template:', error);
            showToast('Failed to save template. Is the server running?', true);
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!publisherId || !jcode) {
            showToast('Please fill in Publisher ID and Jcode.', true);
            return;
        }

        setLoading(true);
        try {
            const positions = {};

            if (enableAddNewPage) {
                positions['CUSTOM'] = {
                    alignment: 'CENTER',
                    addNewPage: true,
                    includeCurrentUser: false,
                    html: { content: getResolvedHtmlForSave() }
                };
            }

            // Map overlay config...
            ['header', 'footer', 'leftMargin', 'rightMargin'].forEach(pos => {
                const conf = overlayConfig[pos];
                if (conf.enabled) {
                    let positionKey = pos.toUpperCase();
                    if (pos === 'leftMargin') positionKey = 'LEFT_MARGIN';
                    if (pos === 'rightMargin') positionKey = 'RIGHT_MARGIN';

                    positions[positionKey] = {
                        alignment: 'CENTER',
                        addNewPage: false,
                        includeCurrentUser: conf.includeCurrentUser,
                        logo: conf.logo ? { base64: conf.logo, mimeType: 'image/png' } : null,
                        text: conf.text ? { content: conf.text } : null,
                        html: conf.html ? { content: conf.html } : null,
                        doi: conf.doi ? { value: conf.doi } : null,
                        date: conf.date ? { enabled: true } : null,
                        ad: conf.ad ? { link: conf.ad } : null
                    };
                }
            });

            const payload = {
                publisherId,
                jcode,
                positions
            };

            const response = await axios.post('http://localhost:8080/api/v1/config/save', payload);
            if (response.data.success) {
                showToast('Configuration saved successfully!');
            } else {
                showToast('Failed to save config: ' + response.data.message, true);
            }
        } catch (error) {
            console.error(error);
            showToast('An error occurred during save.', true);
        } finally {
            setLoading(false);
        }
    };

    return (
        <>
            <header className="bg-white shadow-[0_2px_4px_rgba(0,0,0,0.3)] relative z-10">
                <nav className="max-w-[1140px] mx-auto px-3 flex items-center justify-between">
                    <span className="flex items-center mr-[30px] whitespace-nowrap py-2 no-underline">
                        <a title="HighWire Press, Inc." href="/">
                            <svg className="flex w-[57px] h-[56px]" width="57" height="56" xmlns="http://www.w3.org/2000/svg">
                                <g fill="#931430" fillRule="evenodd">
                                    <path
                                        d="M37.584 5.132l1.132-3.288c.95.337 1.874.735 2.764 1.19l-1.537 3.112c-.765-.388-1.546-.728-2.36-1.014M33.603 4.077L34.156.64c.995.164 1.97.392 2.924.685l-.993 3.33c-.806-.248-1.636-.442-2.484-.578M35.274 11.58L36.4 8.293c.787.267 1.6.606 2.357.993l-1.547 3.113c-.622-.322-1.272-.58-1.936-.817M29.675 10.818l.003-3.5c.835-.014 1.707.033 2.546.147l-.452 3.47c-.69-.094-1.387-.117-2.097-.117M32.422 10.972l.54-3.453c.843.12 1.69.3 2.5.542l-.98 3.365c-.672-.2-1.364-.34-2.06-.455M29.664 14.884l.007-3.48c.706 0 1.41.048 2.097.137l-.45 3.454c-.542-.073-1.092-.11-1.653-.11M33.816 15.617l1.128-3.288c.666.232 1.313.514 1.937.832l-1.542 3.114c-.49-.253-.996-.476-1.522-.66M31.727 15.066l.55-3.433c.695.116 1.385.273 2.053.477l-.99 3.337c-.527-.164-1.067-.29-1.613-.38M45.518 10.093l2.287-2.6c.75.677 1.454 1.4 2.105 2.166l-2.607 2.276c-.553-.656-1.15-1.272-1.785-1.843M41.586 7.028l1.72-3.016c.865.513 1.7 1.075 2.49 1.69L43.71 8.466c-.674-.524-1.385-1-2.122-1.437M41.132 14.81l2.29-2.595c.633.575 1.232 1.19 1.785 1.844l-2.6 2.267c-.458-.534-.953-1.046-1.475-1.518M38.237 12.834l1.718-3.015c.74.433 1.445.914 2.12 1.44l-2.09 2.76c-.556-.432-1.137-.825-1.748-1.186M38.042 18.113l2.294-2.596c.52.474 1.017.982 1.47 1.52l-2.608 2.272c-.355-.422-.74-.82-1.156-1.197M35.98 16.6l1.72-3.02c.61.36 1.194.756 1.75 1.187l-2.1 2.762c-.43-.338-.888-.65-1.37-.93M53.07 31.26l3.4.512c-.144.995-.352 1.988-.633 2.97l-3.303-.954c.23-.833.41-1.677.537-2.528M51.37 18.888l3.185-1.31c.378.94.687 1.9.943 2.89l-3.33.874c-.213-.84-.482-1.66-.8-2.454M15.467 49.36l-1.838 2.934c-.85-.545-1.655-1.15-2.414-1.796l2.2-2.673c.645.552 1.33 1.064 2.05 1.534M8.513 9.91L5.815 7.753c.618-.79 1.29-1.54 2.008-2.248L10.218 8c-.61.602-1.18 1.24-1.705 1.91M33.526 52.256l.576 3.432c-1 .172-1.992.273-2.982.312l-.13-3.482c.84-.03 1.686-.118 2.536-.262M45.682 46.12c.642-.55 1.244-1.14 1.804-1.758l2.536 2.343c-.652.726-1.36 1.427-2.122 2.077-.015.008-.02.02-.037.03l-2.216-2.665c.012-.006.02-.02.035-.026M3.78 32.468l-3.365.704C.21 32.177.072 31.178 0 30.167l3.43-.253c.06.858.178 1.712.35 2.554M48.777 13.976l2.8-2.024c.583.828 1.103 1.69 1.575 2.588l-3.03 1.637c-.404-.763-.85-1.5-1.345-2.2M52.92 24.847l3.413-.447c.13 1.002.19 2.016.187 3.034l-3.437-.01c.005-.867-.05-1.726-.162-2.577M51.41 38.02l3.122 1.464c-.42.908-.903 1.795-1.446 2.663l-2.904-1.868c.458-.73.875-1.485 1.228-2.26M41.33 49.467l1.674 3.042c-.886.494-1.794.92-2.713 1.29l-1.265-3.23c.775-.313 1.55-.676 2.304-1.103M24.63 52.458l-.65 3.423c-.987-.192-1.955-.448-2.9-.775l1.083-3.294c.803.267 1.623.488 2.466.648M7.883 42.483l-2.808 2.01c-.578-.834-1.098-1.7-1.558-2.594l3.043-1.62c.393.773.833 1.5 1.323 2.203M3.59 21.067l-3.353-.773c.224-.982.514-1.954.865-2.902l3.22 1.21c-.293.805-.543 1.63-.732 2.465M18.014 45.423l-1.838 2.938c-.72-.46-1.404-.975-2.052-1.527l2.204-2.672c.535.45 1.096.877 1.686 1.263M8.017 31.693l-3.37.705c-.166-.844-.29-1.696-.347-2.558l3.433-.246c.046.703.146 1.404.284 2.1M11.59 12.43l-2.696-2.16c.525-.667 1.096-1.305 1.71-1.908l2.393 2.498c-.506.497-.974 1.02-1.408 1.57M45.752 21.024l3.188-1.307c.316.8.582 1.62.797 2.455l-3.33.872c-.175-.686-.394-1.362-.655-2.02M42.024 41.87c.533-.453 1.02-.94 1.48-1.445l2.54 2.346c-.562.616-1.156 1.207-1.807 1.762-.008.005-.02.02-.028.025L41.99 41.9c.01-.01.02-.025.034-.03M47.222 30.24l3.4.502c-.12.847-.3 1.697-.54 2.528l-3.305-.956c.195-.682.344-1.377.445-2.074M32.804 47.202l.58 3.43c-.845.146-1.692.236-2.537.266l-.124-3.48c.69-.023 1.383-.094 2.08-.216M43.713 17.584l2.8-2.023c.492.703.936 1.44 1.34 2.2l-3.036 1.634c-.33-.626-.697-1.23-1.104-1.81M47.01 25.368l3.412-.444c.108.85.162 1.71.157 2.577l-3.437-.006c.005-.715-.04-1.422-.132-2.126M46.214 35.343l3.12 1.46c-.354.775-.765 1.532-1.23 2.262L45.2 37.203c.38-.605.72-1.224 1.014-1.86M38.818 44.662l1.675 3.042c-.75.42-1.524.784-2.306 1.097l-1.267-3.23c.64-.266 1.282-.56 1.898-.908M25.656 47.637l-.645 3.413c-.845-.163-1.664-.375-2.465-.648l1.08-3.303c.663.223 1.343.398 2.03.537M11.563 39.994l-2.812 2.002c-.488-.707-.928-1.44-1.32-2.2l3.045-1.615c.32.622.684 1.233 1.085 1.814M7.657 22.08l-3.352-.777c.158-.685.36-1.364.607-2.027l3.222 1.206c-.19.522-.353 1.052-.474 1.594M19.896 19.66l-2.693-2.16c.43-.554.902-1.076 1.41-1.57l2.39 2.498c-.395.384-.763.8-1.107 1.232M17.433 30.12l-3.372.703c-.14-.695-.236-1.4-.282-2.098l3.427-.26c.04.56.114 1.108.228 1.654M37.874 37.257c.42-.356.8-.732 1.16-1.127l2.54 2.345c-.458.507-.946.993-1.48 1.448-.01.007-.02.014-.035.022l-2.215-2.663c.007-.007.02-.015.03-.025M41.32 22.805l3.184-1.302c.26.655.48 1.326.66 2.015l-3.33.88c-.135-.547-.31-1.08-.515-1.593M31.836 40.12l.58 3.43c-.7.117-1.398.192-2.088.225l-.13-3.48c.546-.024 1.09-.08 1.638-.174M42.14 29.423l3.404.507c-.106.697-.25 1.393-.447 2.078l-3.31-.95c.157-.542.273-1.088.353-1.635M22.997 38.312l-1.837 2.94c-.593-.38-1.155-.802-1.69-1.254l2.205-2.672c.416.357.857.688 1.322.986M39.876 20.202l2.796-2.024c.405.578.77 1.182 1.1 1.808l-3.03 1.64c-.26-.493-.55-.972-.866-1.424M42.153 25.94l3.405-.447c.094.705.143 1.413.137 2.126l-3.444-.01c.01-.564-.027-1.12-.097-1.67M41.126 32.93l3.112 1.462c-.29.64-.626 1.26-1.01 1.862l-2.9-1.862c.3-.472.563-.962.798-1.46M35.86 38.697l1.676 3.04c-.62.345-1.256.646-1.895.903l-1.27-3.233c.504-.205 1.002-.44 1.49-.71M27.36 40.068l-.65 3.416c-.688-.132-1.366-.308-2.026-.537l1.08-3.3c.52.177 1.052.315 1.596.42M19.454 34.875l-2.812 2.012c-.4-.586-.763-1.187-1.086-1.816l3.044-1.616c.254.495.54.966.854 1.42M17.493 24.717l-3.355-.773c.158-.685.36-1.364.607-2.027l3.222 1.206c-.19.522-.353 1.052-.474 1.594M29.815 3.735l.003-3.48c1.014 0 2.014.063 3 .198l-.457 3.45c-.834-.112-1.685-.166-2.545-.168" />
                                </g>
                            </svg>
                        </a>
                        <div className="flex flex-col ml-2 justify-center">
                            <img src="data:image/svg+xml,%3csvg%20width='98'%20height='20'%20viewBox='0%200%2098%2020'%20xmlns='http://www.w3.org/2000/svg'%3e%3ctitle%3eH%20i%20g%20h%20w%20i%20r%20e%3c/title%3e%3cg%20fill='%231A1918'%20fill-rule='evenodd'%3e%3cpath%20d='M12.574%208.382H2.422v7.022H.21V.488H2.42v5.974h10.152V.488h2.213v14.916h-2.213V8.382M17.67.363h2.09v2.295h-2.09V.363zm0%204.09h2.09v10.95h-2.09V4.454zM32.1%206.064h-.04c-.813-1.252-2.465-1.86-4.26-1.86-2.904%200-5.974%201.588-5.974%205.663%200%204.05%203.07%205.642%205.994%205.642%201.42%200%203.07-.337%204.24-2.03h.04v1.59c0%201.795-1.105%203.302-4.07%203.302-1.983%200-3.26-.355-3.636-1.778h-2.09C22.745%2019.622%2025.46%2020%2027.82%2020c4.492%200%206.37-1.59%206.37-5.515V4.455H32.1v1.61zm0%203.825c0%202.254-1.5%203.865-4.03%203.865-2.548%200-4.03-1.61-4.03-3.866%200-2.256%201.482-3.93%204.03-3.93%202.53%200%204.03%201.674%204.03%203.93zM36.614.363h2.09v5.744h.04c.983-1.528%202.715-2.027%204.283-2.027%202.633%200%204.594%201.128%204.594%203.866v7.458h-2.086V8.36c0-1.605-1.004-2.525-3.24-2.525-1.898%200-3.59%201.107-3.59%203.26v6.31h-2.09V.362M60.26%203.036h-.045l-4.113%2012.368H53.66L48.75.488h2.402l3.82%2012.118h.044L59.006.488h2.63l4.095%2012.118h.043L69.578.488h2.293l-5.054%2014.916h-2.422L60.26%203.036M72.997.363h2.09v2.295h-2.09V.363zm0%204.09h2.09v10.95h-2.09V4.454zM77.638%204.454h2.09v2.008h.04c.98-1.735%202.382-2.382%203.76-2.382.52%200%20.796.022%201.19.123v2.26c-.522-.127-.918-.21-1.485-.21-2.066%200-3.505%201.213-3.505%203.594v5.557h-2.09V4.454M97.94%2010.553v-.267c0-4.535-3.05-6.206-5.935-6.206-4.257%200-6.348%202.696-6.348%205.85%200%203.155%202.09%205.85%206.348%205.85%202.155%200%204.594-1.07%205.644-3.7H95.43c-.708%201.546-2.34%201.947-3.53%201.947-1.86%200-3.864-1.215-4.03-3.474H97.94zM87.934%208.928c.313-1.963%202.046-3.093%203.97-3.093%201.962%200%203.505%201.107%203.822%203.093h-7.792z'/%3e%3c/g%3e%3c/svg%3e" alt="HighWire Press, Inc." className="mt-[3px]" />
                            <div className="text-[#a81732] text-sm font-semibold leading-none tracking-[0.3px] mt-1.5">Stamping Service</div>
                        </div>
                    </span>
                </nav>
            </header>

            <main className="flex-1 w-full max-w-[1140px] mx-auto px-4 py-8 pb-12">
                <h1 className="text-[1.75rem] font-bold text-[#a81732] text-center mb-6">PDF Stamping Configuration</h1>
                <div className="flex justify-center">
                    <form id="stampForm" onSubmit={handleSubmit} className="w-full">
                        <div className="bg-white border border-gray-300 rounded-md shadow-[0_2px_8px_rgba(0,0,0,0.1)] w-full max-w-[1100px] mb-5">
                            <div className="p-7">
                                <h2 className="text-[1.1rem] font-semibold text-gray-800 mb-5 pb-3 border-b border-gray-200">Build Configuration</h2>
                                <div className="flex flex-col md:flex-row gap-3 mb-3">
                                    <div className="flex-1">
                                        <label htmlFor="publisherId" className="block text-[0.85rem] font-medium text-gray-600 mb-1.5">Publisher ID</label>
                                        <input type="text" id="publisherId" className="w-full px-3 py-2 border border-gray-300 rounded-md text-[0.9rem] text-gray-800 bg-white focus:outline-none focus:ring-2 focus:ring-[#a81732]/20 focus:border-[#a81732] transition-colors" placeholder="e.g. ajnr" required
                                            value={publisherId} onChange={e => setPublisherId(e.target.value)} />
                                    </div>
                                    <div className="flex-1">
                                        <label htmlFor="jcode" className="block text-[0.85rem] font-medium text-gray-600 mb-1.5">Jcode</label>
                                        <input type="text" id="jcode" className="w-full px-3 py-2 border border-gray-300 rounded-md text-[0.9rem] text-gray-800 bg-white focus:outline-none focus:ring-2 focus:ring-[#a81732]/20 focus:border-[#a81732] transition-colors" placeholder="e.g. neurorad" required
                                            value={jcode} onChange={e => setJcode(e.target.value)} />
                                    </div>
                                    <div className="flex items-end gap-2.5">
                                        <button type="button" className="inline-flex justify-center items-center px-5 py-2.5 bg-transparent text-[#a81732] border border-[#a81732] rounded-md text-[0.9rem] font-semibold uppercase tracking-wide cursor-pointer hover:bg-[#fdf2f4] transition duration-150" onClick={handleLoadTemplate}>Load Template</button>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <div className="bg-white border border-gray-300 rounded-b-md shadow-[0_2px_8px_rgba(0,0,0,0.1)] w-full max-w-[1100px]">
                            <div className="flex flex-col md:flex-row border border-gray-300 rounded-md overflow-hidden bg-[#f5f5f5] mb-5 mx-7 mt-7">
                                <div className={`flex flex-1 justify-center items-center gap-2 px-4 py-3 border-none text-[0.9rem] font-semibold tracking-wide cursor-pointer transition-all duration-200 relative md:border-r border-gray-300 ${activeTab === 'addNewPage' ? 'bg-[#a81732] text-white shadow-[0_2px_6px_rgba(168,23,50,0.25)]' : 'bg-transparent text-gray-500 hover:bg-[#ebebeb] hover:text-gray-800'}`} onClick={() => setActiveTab('addNewPage')}>
                                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="w-[18px] h-[18px] shrink-0">
                                        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                                        <line x1="12" y1="18" x2="12" y2="12" />
                                        <line x1="9" y1="15" x2="15" y2="15" />
                                    </svg>
                                    Add New Page
                                </div>
                                <div className={`flex flex-1 justify-center items-center gap-2 px-4 py-3 border-none text-[0.9rem] font-semibold tracking-wide cursor-pointer transition-all duration-200 relative ${activeTab === 'editExisting' ? 'bg-[#a81732] text-white shadow-[0_2px_6px_rgba(168,23,50,0.25)]' : 'bg-transparent text-gray-500 hover:bg-[#ebebeb] hover:text-gray-800'}`} onClick={() => setActiveTab('editExisting')}>
                                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="w-[18px] h-[18px] shrink-0">
                                        <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
                                        <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
                                    </svg>
                                    Edit Existing Pages
                                </div>
                            </div>

                            <div className="px-7 pb-7 pt-2">
                                {activeTab === 'addNewPage' && (
                                    <TemplateTab enableAddNewPage={enableAddNewPage} setEnableAddNewPage={setEnableAddNewPage} handleSaveTemplate={handleSaveTemplate} />
                                )}
                                {activeTab === 'editExisting' && (
                                    <ExistingPagesTab />
                                )}
                            </div>
                        </div>

                        <div className="flex gap-3 justify-end mt-5">
                            <button type="button" className="inline-flex justify-center items-center px-6 py-2.5 bg-transparent text-[#a81732] border border-[#a81732] rounded-md text-[0.9rem] font-semibold uppercase tracking-wide cursor-pointer hover:bg-[#fdf2f4] transition duration-150">Cancel</button>
                            <button type="submit" disabled={loading} className="inline-flex justify-center items-center gap-2 px-6 py-2.5 bg-[#a81732] text-white border border-transparent rounded-md text-[0.9rem] font-semibold uppercase tracking-wide cursor-pointer hover:bg-[#851227] transition duration-150 disabled:opacity-50 disabled:cursor-not-allowed group relative overflow-hidden w-[260px]">
                                <span className={`flex items-center gap-2 transition-opacity duration-200 ${loading ? 'opacity-0' : 'opacity-100'}`}>
                                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="w-4 h-4">
                                        <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z" />
                                        <polyline points="17 21 17 13 7 13 7 21" />
                                        <polyline points="7 3 7 8 15 8" />
                                    </svg>
                                    Save Configuration
                                </span>
                                <div className={`absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 w-[18px] h-[18px] border-2 border-white/30 border-t-white rounded-full animate-spin transition-opacity duration-200 ${loading ? 'opacity-100' : 'opacity-0'}`}></div>
                            </button>
                        </div>
                    </form>
                </div>
            </main>

            <div className={`fixed bottom-6 right-6 bg-white border border-gray-300 py-3.5 px-5 rounded-md flex items-center gap-2.5 shadow-[0_4px_16px_rgba(0,0,0,0.15)] text-gray-800 z-50 max-w-[420px] text-[0.88rem] transition-all duration-300 ease-in-out ${toast.show ? 'translate-y-0 opacity-100' : 'translate-y-[100px] opacity-0'} ${toast.isError ? 'text-[#c62828]' : 'text-gray-800'}`}>
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className={`w-5 h-5 shrink-0 ${toast.isError ? 'text-[#c62828]' : 'text-[#2e7d32]'}`}>
                    <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
                    <polyline points="22 4 12 14.01 9 11.01" />
                </svg>
                <span>{toast.message}</span>
            </div>
        </>
    );
};

export default MainPortal;

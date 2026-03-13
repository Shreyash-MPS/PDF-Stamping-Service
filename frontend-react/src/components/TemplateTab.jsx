import React, { useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTemplateContext } from '../context/TemplateContext';

const TemplateTab = ({ enableAddNewPage, setEnableAddNewPage, handleSaveTemplate }) => {
    const navigate = useNavigate();
    const {
        currentTemplateKey, setCurrentTemplateKey,
        templatesState, updateShortcode,
        templateConfig, setTemplateConfig
    } = useTemplateContext();

    const iframeRef = useRef(null);

    const handleEditHtml = () => {
        navigate('/editor');
    };

    useEffect(() => {
        const handleMessage = (event) => {
            if (event.data && event.data.type === 'SHORTCODE_UPDATE') {
                updateShortcode(currentTemplateKey, event.data.key, event.data.value);
            }
        };
        window.addEventListener('message', handleMessage);
        return () => window.removeEventListener('message', handleMessage);
    }, [currentTemplateKey, updateShortcode]);

    // Re-render iframe content when templatesState changes
    useEffect(() => {
        if (!iframeRef.current) return;

        const template = templatesState[currentTemplateKey];
        if (!template) return;

        let html = template.htmlTemplate;
        const codes = template.shortcodes || {};

        // 1. Resolve Logo
        if (templateConfig.logo && templateConfig.logo !== 'pending') {
            const imgHtml = `<img src="data:image/png;base64,${templateConfig.logo}" style="max-width: 100%; object-fit: contain;" alt="Logo" />`;
            html = html.replace(/<div[^>]*class="logo-wrapper"[^>]*>[\s\S]*?\{\{LOGO_TEXT\}\}[\s\S]*?<\/div>/g, imgHtml);
            html = html.replace(/\{\{LOGO_TEXT\}\}/g, imgHtml);
        } else {
            const span = `<span contenteditable="true" data-shortcode="LOGO_TEXT" class="editable-shortcode placeholder-true">Logo Image acts here...</span>`;
            html = html.replace(/\{\{LOGO_TEXT\}\}/g, span);
        }

        // 2. Resolve Date
        if (templateConfig.includeDate) {
            const months = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];
            const d = new Date();
            const dateStr = `${months[d.getMonth()]} ${d.getDate()}, ${d.getFullYear()}`;
            html = html.replace(/\{\{DATE\}\}/g, dateStr);
        } else {
            html = html.replace(/<[^>]*class="date-block"[^>]*>[\s\S]*?<\/[^>]+>/g, '');
            html = html.replace(/\{\{DATE\}\}/g, '');
        }

        // 3. Resolve DOI visibility
        if (!templateConfig.includeDoi) {
            html = html.replace(/<[^>]*class="doi-block"[^>]*>[\s\S]*?<\/[^>]+>/g, '');
        } else {
            html = html.replace(/(class="doi-block"[^>]*style="[^"]*)(color: #999; font-style: italic;)/g, '$1color: inherit; font-style: normal;');
        }

        // 3.5 Show/hide static placeholder blocks based on boolean flags
        if (!templateConfig.includeArticleTitle) {
            html = html.replace(/<[^>]*class="article-title-block"[^>]*>[\s\S]*?<\/[^>]+>/g, '');
        } else {
            html = html.replace(/(class="article-title-block"[^>]*style="[^"]*)(color: #999; font-style: italic;)/g, '$1color: inherit; font-style: normal;');
        }
        if (!templateConfig.includeAuthors) {
            html = html.replace(/<[^>]*class="authors-block"[^>]*>[\s\S]*?<\/[^>]+>/g, '');
        } else {
            html = html.replace(/(class="authors-block"[^>]*style="[^"]*)(color: #999; font-style: italic;)/g, '$1color: inherit; font-style: normal;');
        }

        // 4. Resolve all other editable shortcodes
        Object.keys(codes).forEach(key => {
            if (key === 'LOGO_TEXT' || key === 'DATE' || key === 'DOI') return;

            const sc = codes[key];
            const val = sc.value !== undefined ? sc.value : sc.default;
            const isEmpty = !val;
            const displayVal = val || sc.placeholder || 'Type here...';

            const span = `<span contenteditable="true" data-shortcode="${key}" data-placeholder="${sc.placeholder || 'Type here...'}" class="editable-shortcode ${isEmpty ? 'placeholder-true' : ''}">${displayVal}</span>`;
            const regex = new RegExp('\\\\{\\\\{' + key + '\\\\}\\\\}', 'g');
            html = html.replace(regex, span);
        });

        const editorScript = `
        <style>
          .editable-shortcode {
            outline: 2px dashed transparent;
            border-radius: 2px;
            transition: all 0.2s;
            cursor: text;
            padding: 0 4px;
            display: inline-block;
            min-width: 20px;
            color: inherit;
          }
          .editable-shortcode:hover {
            outline-color: #a81732;
            background: rgba(168, 23, 50, 0.05);
          }
          .editable-shortcode:focus {
            outline-color: #a81732;
            background: #fff;
          }
          .placeholder-true {
            color: #999;
            font-style: italic;
          }
        </style>
        <script>
          document.querySelectorAll('.editable-shortcode').forEach(el => {
            el.addEventListener('focus', function() {
               if(this.classList.contains('placeholder-true')) {
                   this.innerText = '';
                   this.classList.remove('placeholder-true');
                   this.style.fontStyle = 'normal';
               }
            });
            el.addEventListener('blur', function() {
               if(this.innerText.trim() === '') {
                   this.classList.add('placeholder-true');
                   this.innerText = this.dataset.placeholder || 'Type here...';
                   this.style.fontStyle = 'italic';
               }
            });
            el.addEventListener('input', function(e) {
              window.parent.postMessage({
                type: 'SHORTCODE_UPDATE',
                key: this.dataset.shortcode,
                value: this.innerText
              }, '*');
            });
          });
        </script>
        `;

        html = html.replace('</body>', editorScript + '</body>');
        iframeRef.current.srcdoc = html;

    }, [currentTemplateKey, templatesState, templateConfig]);

    return (
        <div id="tab-addNewPage" className="block animate-[fadeIn_0.3s_ease-out]">
            <div className="flex flex-col md:flex-row border border-gray-300 rounded-md overflow-hidden bg-[#f9f9f9] min-h-[550px] items-stretch">
                <div className="flex flex-col w-full md:w-1/2 md:border-r border-gray-300 min-w-0">
                    <div className="flex items-center gap-1.5 px-4 py-3 bg-[#f0f0f0] border-b border-gray-300 text-[0.75rem] font-bold text-gray-500 tracking-wider uppercase">
                        Template Settings
                    </div>
                    <div className="p-5">
                        <div className="mb-5">
                            <label className="flex items-center gap-2 font-semibold text-[1.1rem] mb-4 text-gray-800 cursor-pointer">
                                <input type="checkbox" className="w-4 h-4 accent-[#a81732]" checked={enableAddNewPage} onChange={e => setEnableAddNewPage(e.target.checked)} />
                                <span className="select-none">Enable 'Add New Page' functionality</span>
                            </label>
                            <p className="text-sm text-gray-500 -mt-2 mb-6 ml-7">
                                If enabled, the HTML template configured below will be prepended as a new first page to the PDF.
                            </p>
                        </div>

                        <div className={`mb-5 transition-opacity duration-200 ${enableAddNewPage ? 'opacity-100' : 'opacity-50 pointer-events-none'}`}>
                            <div className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-3">Template</div>
                            <div className="mb-3">
                                <label htmlFor="templateSelector" className="block text-sm font-medium text-gray-600 mb-1">Choose a pre-defined template</label>
                                <div className="relative">
                                    <select id="templateSelector" className="w-full text-sm pl-3 pr-8 py-2.5 border border-gray-300 rounded-md bg-white text-gray-800 focus:outline-none focus:ring-2 focus:ring-[#a81732]/20 focus:border-[#a81732] appearance-none" value={currentTemplateKey} onChange={e => setCurrentTemplateKey(e.target.value)}>
                                        <option value="journal_article">Journal Article Front Page</option>
                                        <option value="simple_header">Simple Header Stamp</option>
                                        <option value="custom_html">Custom HTML</option>
                                    </select>
                                    <div className="absolute inset-y-0 right-0 flex items-center px-2 pointer-events-none text-gray-500">
                                        <svg className="w-4 h-4 fill-current" viewBox="0 0 20 20"><path d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z" clipRule="evenodd" fillRule="evenodd"></path></svg>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <div className={`mb-5 transition-opacity duration-200 mt-6 ${enableAddNewPage ? 'opacity-100' : 'opacity-50 pointer-events-none'}`}>
                            <div className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-3">Template Overlays</div>

                            <div className="bg-[#fafafa] border border-gray-300 rounded-md p-4 space-y-3">
                                <div className="flex flex-col gap-2">
                                    <label className="flex items-center gap-2 cursor-pointer text-sm">
                                        <input type="checkbox" className="w-4 h-4 accent-[#a81732] cursor-pointer" checked={!!templateConfig.logo} onChange={e => {
                                            if (!e.target.checked) setTemplateConfig({ ...templateConfig, logo: null });
                                            else setTemplateConfig({ ...templateConfig, logo: 'pending' });
                                        }} />
                                        <span className="text-gray-800 select-none">Add Logo Image</span>
                                    </label>
                                    <div className={`overflow-hidden transition-all duration-200 ${templateConfig.logo ? 'max-h-40 opacity-100' : 'max-h-0 opacity-0'}`}>
                                        <input type="file" className="block w-full text-sm text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded-md file:border-0 file:text-sm file:font-semibold file:bg-[#fdf2f4] file:text-[#a81732] hover:file:bg-[#fbdde1] border border-gray-300 rounded-md bg-gray-50 p-1" accept="image/png, image/jpeg" onChange={e => {
                                            const file = e.target.files[0];
                                            if (file) {
                                                const reader = new FileReader();
                                                reader.onload = ev => setTemplateConfig({ ...templateConfig, logo: ev.target.result.split(',')[1] });
                                                reader.readAsDataURL(file);
                                            }
                                        }} />
                                    </div>
                                </div>

                                <div className="flex flex-col gap-2">
                                    <label className="flex items-center gap-2 cursor-pointer text-sm">
                                        <input type="checkbox" className="w-4 h-4 accent-[#a81732] cursor-pointer" checked={templateConfig.includeDate} onChange={e => setTemplateConfig({ ...templateConfig, includeDate: e.target.checked })} />
                                        <span className="text-gray-800 select-none">Include Date</span>
                                    </label>
                                </div>

                                <div className="flex flex-col gap-2">
                                    <label className="flex items-center gap-2 cursor-pointer text-sm">
                                        <input type="checkbox" className="w-4 h-4 accent-[#a81732] cursor-pointer" checked={templateConfig.includeArticleTitle} onChange={e => setTemplateConfig({ ...templateConfig, includeArticleTitle: e.target.checked })} />
                                        <span className="text-gray-800 select-none">Include Article Title</span>
                                    </label>
                                </div>

                                <div className="flex flex-col gap-2">
                                    <label className="flex items-center gap-2 cursor-pointer text-sm">
                                        <input type="checkbox" className="w-4 h-4 accent-[#a81732] cursor-pointer" checked={templateConfig.includeAuthors} onChange={e => setTemplateConfig({ ...templateConfig, includeAuthors: e.target.checked })} />
                                        <span className="text-gray-800 select-none">Include Authors</span>
                                    </label>
                                </div>

                                <div className="flex flex-col gap-2">
                                    <label className="flex items-center gap-2 cursor-pointer text-sm">
                                        <input type="checkbox" className="w-4 h-4 accent-[#a81732] cursor-pointer" checked={templateConfig.includeDoi} onChange={e => setTemplateConfig({ ...templateConfig, includeDoi: e.target.checked })} />
                                        <span className="text-gray-800 select-none">Add DOI</span>
                                    </label>
                                </div>
                            </div>
                        </div>

                        <div className={`flex gap-3 mt-5 transition-opacity duration-200 ${enableAddNewPage ? 'opacity-100' : 'opacity-50 pointer-events-none'}`}>
                            <button type="button" className="inline-flex justify-center items-center gap-2 w-full px-5 py-2.5 bg-[#f0f0f0] text-gray-800 border border-gray-300 rounded-md text-[0.9rem] font-semibold uppercase tracking-wide cursor-pointer hover:bg-[#e4e4e4] hover:border-gray-400 transition duration-150" onClick={handleSaveTemplate}>
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="w-4 h-4 shrink-0">
                                    <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z" />
                                    <polyline points="17 21 17 13 7 13 7 21" />
                                    <polyline points="7 3 7 8 15 8" />
                                </svg>
                                Save Template Defaults
                            </button>
                        </div>
                    </div>
                </div>

                <div className="w-full md:w-1/2 flex flex-col min-w-0">
                    <div className="flex justify-between items-center px-3 py-[0.45rem] bg-[#f0f0f0] border-b border-gray-300 font-bold">
                        <span className="text-[0.75rem] font-bold text-gray-500 tracking-wider uppercase">Template Preview</span>
                        <button type="button" className="inline-flex items-center gap-1 px-2 py-1 text-[0.75rem] font-semibold tracking-wide uppercase bg-transparent text-[#a81732] border border-[#a81732] rounded hover:bg-[#fdf2f4] transition duration-150 cursor-pointer" onClick={handleEditHtml}>
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="w-3 h-3 mr-1 shrink-0">
                                <polyline points="16 18 22 12 16 6" />
                                <polyline points="8 6 2 12 8 18" />
                            </svg>
                            Edit as HTML
                        </button>
                    </div>
                    <div className="flex-1 bg-[#e0e0e0] flex flex-col items-center justify-start p-6 overflow-y-auto min-h-0 relative">
                        <p className={`text-[0.8rem] text-gray-500 mb-4 transition-opacity duration-200 ${enableAddNewPage ? 'opacity-100' : 'opacity-50'}`}>Use the toggles on the left to control which metadata fields appear. Greyed-out placeholders show where Drupal data will be injected.</p>
                        <div className={`w-full max-w-[800px] bg-white shadow-lg transition-opacity duration-200 shrink-0 ${enableAddNewPage ? 'opacity-100 pointer-events-auto' : 'opacity-50 pointer-events-none'}`} style={{ aspectRatio: '1 / 1.414' }}>
                            <iframe ref={iframeRef} className="w-full h-full border-none" sandbox="allow-same-origin allow-scripts"></iframe>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default TemplateTab;

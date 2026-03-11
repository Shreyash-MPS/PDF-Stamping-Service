import React, { useRef, useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import CodeMirror, { EditorView } from '@uiw/react-codemirror';
import { html } from '@codemirror/lang-html';
import { EditorSelection } from '@codemirror/state';
import { useTemplateContext } from '../context/TemplateContext';

const HtmlEditorPage = () => {
    const navigate = useNavigate();
    const { currentTemplateKey, templatesState, updateHtmlTemplate, templateConfig } = useTemplateContext();

    const template = templatesState[currentTemplateKey];

    useEffect(() => {
        if (!template) {
            navigate('/');
        }
    }, [template, navigate]);

    if (!template) return null;

    const [htmlCode, setHtmlCode] = useState(template.htmlTemplate);
    const [highlightedLine, setHighlightedLine] = useState(null);

    const iframeRef = useRef(null);
    const editorViewRef = useRef(null);

    // Build the resolved preview HTML and inject the dblclick postMessage script
    const buildPreviewHtml = useCallback((rawHtml) => {
        let previewHtml = rawHtml;

        // Resolve Logo for preview
        if (templateConfig?.logo && templateConfig.logo !== 'pending') {
            const imgHtml = `<img src="data:image/png;base64,${templateConfig.logo}" style="max-width: 100%; object-fit: contain;" alt="Logo" />`;
            previewHtml = previewHtml.replace(/<div[^>]*class="logo-wrapper"[^>]*>[\s\S]*?\{\{LOGO_TEXT\}\}[\s\S]*?<\/div>/g, imgHtml);
            previewHtml = previewHtml.replace(/\{\{LOGO_TEXT\}\}/g, imgHtml);
        }

        // Resolve Date for preview
        if (templateConfig?.includeDate) {
            const months = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];
            const d = new Date();
            previewHtml = previewHtml.replace(/\{\{DATE\}\}/g, `${months[d.getMonth()]} ${d.getDate()}, ${d.getFullYear()}`);
        } else {
            previewHtml = previewHtml.replace(/<p[^>]*>[\s\S]*?\{\{DATE\}\}[\s\S]*?<\/p>/g, '');
            previewHtml = previewHtml.replace(/\{\{DATE\}\}/g, '');
        }

        // Resolve DOI for preview
        if (templateConfig?.includeDoi) {
            const doiVal = templateConfig.doiText || (template.shortcodes && template.shortcodes['DOI']?.value) || '10.xxxx/xxxx';
            previewHtml = previewHtml.replace(/https:\/\/doi\.org\/\{\{DOI\}\}/g, `https://doi.org/${doiVal}`);
            previewHtml = previewHtml.replace(/\{\{DOI\}\}/g, doiVal);
        } else {
            previewHtml = previewHtml.replace(/<p[^>]*>[\s\S]*?\{\{DOI\}\}[\s\S]*?<\/p>/g, '');
            previewHtml = previewHtml.replace(/\{\{DOI\}\}/g, '');
        }

        // Resolve other shortcodes
        if (template.shortcodes) {
            Object.keys(template.shortcodes).forEach(key => {
                const sc = template.shortcodes[key];
                const val = sc.value !== undefined ? sc.value : sc.default;
                if (key !== 'LOGO_TEXT' && key !== 'DATE' && key !== 'DOI') {
                    const regex = new RegExp('\\\\{\\\\{' + key + '\\\\}\\\\}', 'g');
                    previewHtml = previewHtml.replace(regex, val || '');
                }
            });
        }

        // Inject dblclick listener into <body> — sends postMessage with the clicked element's text
        const dblclickScript = `
<script>
(function() {
    document.addEventListener('dblclick', function(e) {
        var el = e.target;
        // Walk up to find a meaningful block element
        var block = el;
        var blockTags = ['H1','H2','H3','H4','H5','H6','P','DIV','LI','SPAN','A','I','B','STRONG','EM','TD','TH'];
        while (block && block !== document.body) {
            if (blockTags.indexOf(block.tagName) !== -1) break;
            block = block.parentElement;
        }
        var text = block ? block.textContent.trim() : el.textContent.trim();
        window.parent.postMessage({ type: 'PREVIEW_DBLCLICK', text: text }, '*');
        // Visual feedback: flash the element
        if (block) {
            var origBg = block.style.outline;
            block.style.outline = '2px solid #a81732';
            setTimeout(function() { block.style.outline = origBg; }, 600);
        }
    });
})();
</script>`;

        // Insert script just before </body>
        if (previewHtml.includes('</body>')) {
            previewHtml = previewHtml.replace('</body>', dblclickScript + '</body>');
        } else {
            previewHtml += dblclickScript;
        }

        return previewHtml;
    }, [templateConfig, template.shortcodes]);

    // Update iframe srcdoc whenever htmlCode or config changes
    useEffect(() => {
        if (iframeRef.current) {
            iframeRef.current.srcdoc = buildPreviewHtml(htmlCode);
        }
    }, [htmlCode, buildPreviewHtml]);

    // Listen for double-click postMessages from the iframe
    useEffect(() => {
        const handleMessage = (event) => {
            if (event.data?.type !== 'PREVIEW_DBLCLICK') return;
            const clickedText = event.data.text;
            if (!clickedText || !editorViewRef.current) return;

            const view = editorViewRef.current;
            const source = view.state.doc.toString();

            // Search for the clicked text literally in the raw HTML source
            const needle = clickedText.replace(/\s+/g, ' ').trim();
            if (!needle) return;

            // Try exact match first, then partial (first 30 chars)
            let idx = source.indexOf(needle);
            if (idx === -1 && needle.length > 15) {
                idx = source.indexOf(needle.substring(0, 30).trim());
            }

            if (idx === -1) return;

            const from = idx;
            const to = Math.min(idx + needle.length, source.length);

            // Dispatch selection highlight to CodeMirror
            view.dispatch({
                selection: EditorSelection.range(from, to),
            });
            view.focus();

            // Scroll the matched position into view via the CodeMirror DOM
            try {
                const coords = view.coordsAtPos(from);
                if (coords) {
                    const scrollEl = view.scrollDOM;
                    const editorRect = scrollEl.getBoundingClientRect();
                    const relativeTop = coords.top - editorRect.top + scrollEl.scrollTop;
                    scrollEl.scrollTop = relativeTop - scrollEl.clientHeight / 2;
                }
            } catch (err) {
                // fallback: no-op
            }

            setHighlightedLine({ from, to });
        };

        window.addEventListener('message', handleMessage);
        return () => window.removeEventListener('message', handleMessage);
    }, []);

    const handleSaveAndReturn = () => {
        updateHtmlTemplate(currentTemplateKey, htmlCode);
        navigate('/');
    };

    const handleCancel = () => {
        navigate('/');
    };

    // Resizer state
    const [leftWidth, setLeftWidth] = useState(50);
    const [isDragging, setIsDragging] = useState(false);
    const splitPaneRef = useRef(null);
    const isResizing = useRef(false);

    const handleMouseDown = (e) => {
        isResizing.current = true;
        setIsDragging(true);
        document.body.style.cursor = 'col-resize';
        e.preventDefault();
    };

    const handleMouseMove = (e) => {
        if (!isResizing.current) return;
        const rect = splitPaneRef.current.getBoundingClientRect();
        const offsetX = e.clientX - rect.left;
        let leftPercent = (offsetX / rect.width) * 100;
        leftPercent = Math.max(10, Math.min(90, leftPercent));
        setLeftWidth(leftPercent);
    };

    const handleMouseUp = () => {
        if (isResizing.current) {
            isResizing.current = false;
            setIsDragging(false);
            document.body.style.cursor = '';
        }
    };

    useEffect(() => {
        document.addEventListener('mousemove', handleMouseMove);
        document.addEventListener('mouseup', handleMouseUp);
        return () => {
            document.removeEventListener('mousemove', handleMouseMove);
            document.removeEventListener('mouseup', handleMouseUp);
        };
    }, []);

    return (
        <div className="flex flex-col h-screen p-5 box-border bg-[#f5f5f5]">
            <div className="flex justify-between items-center mb-3">
                <div>
                    <h1 className="text-2xl font-bold m-0 text-gray-800">Raw HTML Editor</h1>
                    <p className="text-xs text-gray-400 mt-0.5">💡 Double-click any text in the preview to jump to its source code</p>
                </div>
                <div className="flex gap-3">
                    <button type="button" className="inline-flex justify-center items-center px-4 py-2 bg-transparent text-[#a81732] border border-[#a81732] rounded-md text-sm font-semibold uppercase tracking-wide cursor-pointer hover:bg-[#fdf2f4] transition duration-150" onClick={handleCancel}>
                        Cancel
                    </button>
                    <button type="button" className="inline-flex justify-center items-center gap-2 px-4 py-2 bg-[#a81732] text-white border border-transparent rounded-md text-sm font-semibold uppercase tracking-wide cursor-pointer hover:bg-[#851227] transition duration-150" onClick={handleSaveAndReturn}>
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="w-4 h-4">
                            <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z" />
                            <polyline points="17 21 17 13 7 13 7 21" />
                            <polyline points="7 3 7 8 15 8" />
                        </svg>
                        Save &amp; Return
                    </button>
                </div>
            </div>

            <div className="flex flex-1 min-h-0 bg-white rounded-md overflow-hidden shadow-[0_4px_12px_rgba(0,0,0,0.08)]" ref={splitPaneRef}>
                {/* Left: Source Editor */}
                <div className="flex flex-col overflow-hidden" style={{ width: `calc(${leftWidth}% - 6px)` }}>
                    <div className="flex items-center gap-1.5 px-3 py-2 bg-[#f0f0f0] border-b border-gray-300 text-xs font-semibold text-gray-500 tracking-wider uppercase">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="w-4 h-4">
                            <polyline points="16 18 22 12 16 6" />
                            <polyline points="8 6 2 12 8 18" />
                        </svg>
                        HTML Source
                    </div>
                    <div className="flex-1 overflow-auto outline-none">
                        <CodeMirror
                            value={htmlCode}
                            height="100%"
                            theme="dark"
                            extensions={[html()]}
                            onChange={(value) => setHtmlCode(value)}
                            onCreateEditor={(view) => { editorViewRef.current = view; }}
                            style={{ height: '100%', fontSize: '0.9rem', flex: 1 }}
                        />
                    </div>
                </div>

                {/* Drag handle */}
                <div className="w-3 bg-gray-200 cursor-col-resize flex flex-col justify-center items-center shrink-0 border-x border-gray-300 hover:bg-gray-300 transition-colors group" onMouseDown={handleMouseDown}>
                    <div className="w-1 h-8 rounded-sm bg-gray-400 group-hover:bg-[#a81732] transition-colors"></div>
                </div>

                {/* Right: Live Preview */}
                <div className="flex flex-col bg-white" style={{ width: `calc(${100 - leftWidth}% - 6px)` }}>
                    <div className="flex items-center gap-1.5 px-3 py-2 bg-[#f0f0f0] border-b border-gray-300 text-xs font-semibold text-gray-500 tracking-wider uppercase">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="w-4 h-4">
                            <rect x="2" y="3" width="20" height="14" rx="2" ry="2" />
                            <line x1="8" y1="21" x2="16" y2="21" />
                            <line x1="12" y1="17" x2="12" y2="21" />
                        </svg>
                        Live Preview
                        <span className="ml-auto font-normal normal-case text-[0.65rem] text-gray-400 italic">dblclick to jump to source</span>
                    </div>
                    <iframe
                        ref={iframeRef}
                        className="flex-1 w-full border-none bg-white"
                        sandbox="allow-same-origin allow-scripts"
                        style={{ pointerEvents: isDragging ? 'none' : 'auto' }}
                    ></iframe>
                </div>
            </div>
        </div>
    );
};

export default HtmlEditorPage;

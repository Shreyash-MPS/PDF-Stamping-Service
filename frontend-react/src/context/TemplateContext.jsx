import { createContext, useContext, useState, useEffect } from 'react';
import { TEMPLATES } from '../models/templates';

const TemplateContext = createContext();

export const useTemplateContext = () => useContext(TemplateContext);

export const TemplateProvider = ({ children }) => {
    const [publisherId, setPublisherId] = useState('');
    const [jcode, setJcode] = useState('');
    const [currentTemplateKey, setCurrentTemplateKey] = useState('journal_article');

    const [templateConfig, setTemplateConfig] = useState({
        logo: null,
        includeDate: true,
        includeDoi: true,
        includeArticleTitle: true,
        includeAuthors: true,
        doiText: ''
    });

    // State to hold the current working state of all templates
    // This allows switching templates and retaining user edits.
    const [templatesState, setTemplatesState] = useState(() => {
        const saved = localStorage.getItem('stamping_templates_state');
        let parsed = saved ? JSON.parse(saved) : {};
        
        // Auto-heal missing or outdated default templates from templates.js
        const merged = { ...parsed };
        Object.keys(TEMPLATES).forEach(key => {
            if (!merged[key] || merged[key].htmlTemplate !== TEMPLATES[key].htmlTemplate) {
                // If it's a default template and HTML differs, overwrite it to pick up code changes
                merged[key] = {
                    ...TEMPLATES[key],
                    shortcodes: { ...(merged[key]?.shortcodes || {}), ...TEMPLATES[key].shortcodes },
                    htmlTemplate: TEMPLATES[key].htmlTemplate
                };
            }
        });
        return merged;
    });

    const currentTemplateData = templatesState[currentTemplateKey];

    const updateShortcode = (templateKey, shortcodeKey, value) => {
        setTemplatesState(prev => {
            const newState = { ...prev };
            if (newState[templateKey].shortcodes[shortcodeKey]) {
                newState[templateKey].shortcodes[shortcodeKey].value = value;
            }
            return newState;
        });
    };

    const updateHtmlTemplate = (templateKey, newHtml) => {
        setTemplatesState(prev => {
            const newState = { ...prev };
            newState[templateKey].htmlTemplate = newHtml;
            return newState;
        });
    };

    const resetToDefault = () => {
        setTemplatesState(JSON.parse(JSON.stringify(TEMPLATES)));
        setPublisherId('');
        setJcode('');
        setCurrentTemplateKey('journal_article');
        setTemplateConfig({ logo: null, includeDate: true, includeDoi: true, includeArticleTitle: true, includeAuthors: true, doiText: '' });
    };

    // Replace shortcodes into the HTML template to get the resolved raw HTML
    const getResolvedHtmlForSave = () => {
        const template = templatesState[currentTemplateKey];
        let html = template.htmlTemplate;
        const codes = template.shortcodes || {};

        Object.keys(codes).forEach(key => {
            if (key === 'LOGO_TEXT' && templateConfig.logo) {
                // Handled below
                return;
            }
            if (key === 'DOI' && templateConfig.includeDoi && templateConfig.doiText) {
                // Handled below but we'll fall back if not checked
                return;
            }
            if (key === 'ARTICLE_TITLE' && !templateConfig.includeArticleTitle) return;
            if (key === 'AUTHORS' && !templateConfig.includeAuthors) return;

            const sc = codes[key];
            const val = sc.value !== undefined ? sc.value : sc.default;
            const regex = new RegExp('\\\\{\\\\{' + key + '\\\\}\\\\}', 'g');
            html = html.replace(regex, val);
        });

        // Logo Replacement
        if (templateConfig.logo) {
            const imgHtml = `<img src="data:image/png;base64,${templateConfig.logo}" style="max-height: 60px; object-fit: contain;" alt="Logo" />`;
            html = html.replace(/<h1[^>]*>\{\{LOGO_TEXT\}\}<\/h1>/g, imgHtml); // Replace full h1 if present
            html = html.replace(/\{\{LOGO_TEXT\}\}/g, imgHtml); // Fallback
        }

        // Date replacement
        if (templateConfig.includeDate) {
            const months = ['January', 'February', 'March', 'April', 'May', 'June',
                'July', 'August', 'September', 'October', 'November', 'December'];
            const d = new Date();
            const currentDateStr = `${months[d.getMonth()]} ${d.getDate()}, ${d.getFullYear()}`;
            html = html.replace(/\{\{DATE\}\}/g, currentDateStr);
        } else {
            // Remove element containing Date
            html = html.replace(/<[^>]*>(?:(?!<\/[^>]+>)[\s\S])*\{\{DATE\}\}(?:(?!<\/[^>]+>)[\s\S])*<\/[^>]+>/g, '');
            html = html.replace(/\{\{DATE\}\}/g, '');
        }

        // DOI replacement
        if (templateConfig.includeDoi) {
            const doiVal = templateConfig.doiText || (codes['DOI'] && codes['DOI'].value) || '';
            html = html.replace(/\{\{DOI\}\}/g, doiVal);
        } else {
            // Remove block containing DOI
            html = html.replace(/<[^>]*>(?:(?!<\/[^>]+>)[\s\S])*\{\{DOI\}\}(?:(?!<\/[^>]+>)[\s\S])*<\/[^>]+>/g, '');
            html = html.replace(/\{\{DOI\}\}/g, '');
        }

        // Article Title replacement
        if (!templateConfig.includeArticleTitle) {
            html = html.replace(/<[^>]*>(?:(?!<\/[^>]+>)[\s\S])*\{\{ARTICLE_TITLE\}\}(?:(?!<\/[^>]+>)[\s\S])*<\/[^>]+>/g, '');
            html = html.replace(/\{\{ARTICLE_TITLE\}\}/g, '');
        }

        // Authors replacement
        if (!templateConfig.includeAuthors) {
            html = html.replace(/<[^>]*>(?:(?!<\/[^>]+>)[\s\S])*\{\{AUTHORS\}\}(?:(?!<\/[^>]+>)[\s\S])*<\/[^>]+>/g, '');
            html = html.replace(/\{\{AUTHORS\}\}/g, '');
        }

        return html;
    };

    // Shared state for the Existing Pages overlays
    const [overlayConfig, setOverlayConfig] = useState({
        header: { enabled: false, includeCurrentUser: false, logo: null, text: '', html: '', doi: '', date: false, ad: '' },
        footer: { enabled: false, includeCurrentUser: false, logo: null, text: '', html: '', doi: '', date: false, ad: '' },
        leftMargin: { enabled: false, includeCurrentUser: false, logo: null, text: '', html: '', doi: '', date: false, ad: '' },
        rightMargin: { enabled: false, includeCurrentUser: false, logo: null, text: '', html: '', doi: '', date: false, ad: '' }
    });

    const updateOverlayConfig = (position, field, value) => {
        setOverlayConfig(prev => ({
            ...prev,
            [position]: {
                ...prev[position],
                [field]: value
            }
        }));
    };

    return (
        <TemplateContext.Provider value={{
            publisherId, setPublisherId,
            jcode, setJcode,
            currentTemplateKey, setCurrentTemplateKey,
            templatesState, currentTemplateData, setTemplatesState,
            templateConfig, setTemplateConfig,
            updateShortcode, updateHtmlTemplate, resetToDefault,
            getResolvedHtmlForSave,
            overlayConfig, updateOverlayConfig
        }}>
            {children}
        </TemplateContext.Provider>
    );
};

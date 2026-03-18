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
        includeAuthors: true
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
        setTemplateConfig({ logo: null, includeDate: true, includeDoi: true, includeArticleTitle: true, includeAuthors: true });
    };


    // Shared state for the Existing Pages overlays
    const [overlayConfig, setOverlayConfig] = useState({
        header: { enabled: false, includeCurrentUser: false, logo: null, text: '', html: '', doi: '', date: false, ad: '', linkUrl: '', linkText: '' },
        footer: { enabled: false, includeCurrentUser: false, logo: null, text: '', html: '', doi: '', date: false, ad: '', linkUrl: '', linkText: '' },
        leftMargin: { enabled: false, includeCurrentUser: false, logo: null, text: '', html: '', doi: '', date: false, ad: '', linkUrl: '', linkText: '' },
        rightMargin: { enabled: false, includeCurrentUser: false, logo: null, text: '', html: '', doi: '', date: false, ad: '', linkUrl: '', linkText: '' }
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
            overlayConfig, updateOverlayConfig
        }}>
            {children}
        </TemplateContext.Provider>
    );
};

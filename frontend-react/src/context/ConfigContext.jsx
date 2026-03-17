import { createContext, useContext, useState, useCallback, useEffect } from 'react';
import publishersData from '../data/publishers.json';
import configVariablesData from '../data/config_variables.json';
import { TEMPLATES } from '../models/templates';

const ConfigContext = createContext();

export const useConfigContext = () => useContext(ConfigContext);

const STORAGE_KEY = 'stamping_configs';

const API_BASE = 'http://localhost:8080/api/v1';

const POSITION_MAP = {
    newPage: 'CUSTOM',
    header: 'HEADER',
    footer: 'FOOTER',
    leftMargin: 'LEFT_MARGIN',
    rightMargin: 'RIGHT_MARGIN',
};

/** Convert a frontend section to backend DynamicStampRequest.Configuration */
const convertSectionToBackend = (section, sectionKey) => ({
    addNewPage: sectionKey === 'newPage',
    alignment: sectionKey === 'newPage' ? 'LEFT' : 'CENTER',
    includeArticleTitle: section.articleTitle ?? false,
    includeAuthors: section.articleAuthors ?? false,
    includeDoi: section.articleDoi ?? false,
    includeDate: section.dateOfDownload ?? false,
    includeCurrentUser: section.downloadBy ?? false,
    logo: section.logo?.enabled && section.logo?.value
        ? { base64: section.logo.value, mimeType: 'image/png' } : null,
    html: section.html?.enabled && section.html?.value
        ? { content: section.html.value } : null,
    text: section.text?.enabled && section.text?.value
        ? { content: section.text.value } : null,
    doi: null,
    date: section.dateOfDownload ? { enabled: true } : null,
    ad: section.adsBanner?.enabled && section.adsBanner?.value
        ? { link: section.adsBanner.value } : null,
});

const loadConfigs = () => {
    try {
        const stored = localStorage.getItem(STORAGE_KEY);
        if (stored) return JSON.parse(stored);
    } catch { /* ignore */ }
    return configVariablesData;
};

const DEFAULT_SECTION = {
    enabled: false,
    logo: { enabled: false, value: null },
    html: { enabled: false, value: '' },
    text: { enabled: false, value: '' },
    articleTitle: false,
    articleAuthors: false,
    articleDoi: false,
    articleCopyright: false,
    articleIssn: false,
    articleId: false,
    dateOfDownload: false,
    downloadBy: false,
    adsBanner: { enabled: false, value: '', legacyDomain: '' },
};

export const getDefaultConfig = () => ({
    templateName: 'journal_article',
    newPage: {
        ...DEFAULT_SECTION,
        enabled: true,
        pagePosition: 'front',
        articleTitle: true,
        articleAuthors: true,
        articleDoi: true,
        dateOfDownload: true,
    },
    header: { ...DEFAULT_SECTION },
    footer: { ...DEFAULT_SECTION },
    leftMargin: { ...DEFAULT_SECTION },
    rightMargin: { ...DEFAULT_SECTION },
});

export const ConfigProvider = ({ children }) => {
    const [publishers, setPublishers] = useState(publishersData);
    const [configs, setConfigs] = useState(loadConfigs);
    const [nextId, setNextId] = useState(() =>
        Math.max(...loadConfigs().map(c => c.id), 0) + 1
    );

    useEffect(() => {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(configs));
    }, [configs]);

    const getTableRows = useCallback(() => {
        return configs.filter(cfg => !cfg.archived).map((cfg, idx) => {
            const pub = publishers.find(p => p.pubId === cfg.pubId);
            const journal = pub?.journals.find(j => j.jId === cfg.jId);
            return {
                serial: idx + 1,
                id: cfg.id,
                pubId: cfg.pubId,
                pubName: pub?.pubName || cfg.pubId,
                jId: cfg.jId,
                jName: journal?.jName || cfg.jId,
            };
        });
    }, [configs, publishers]);

    const addConfig = (config) => {
        const newConfig = { ...config, id: nextId };
        setConfigs(prev => [...prev, newConfig]);
        setNextId(prev => prev + 1);
        return newConfig;
    };

    const updateConfig = (id, updatedConfig) => {
        setConfigs(prev => prev.map(c => c.id === id ? { ...c, ...updatedConfig } : c));
    };

    const deleteConfig = (id) => {
        setConfigs(prev => prev.map(c => c.id === id ? { ...c, archived: true } : c));
    };

    const restoreConfig = (id) => {
        setConfigs(prev => prev.map(c => c.id === id ? { ...c, archived: false } : c));
    };

    const getArchivedRows = useCallback(() => {
        return configs.filter(cfg => cfg.archived).map((cfg, idx) => {
            const pub = publishers.find(p => p.pubId === cfg.pubId);
            const journal = pub?.journals.find(j => j.jId === cfg.jId);
            return {
                serial: idx + 1,
                id: cfg.id,
                pubId: cfg.pubId,
                pubName: pub?.pubName || cfg.pubId,
                jId: cfg.jId,
                jName: journal?.jName || cfg.jId,
            };
        });
    }, [configs, publishers]);

    const getConfigById = (id) => configs.find(c => c.id === id);

    const addPublisher = (pub) => {
        setPublishers(prev => {
            if (prev.find(p => p.pubId === pub.pubId)) return prev;
            return [...prev, pub];
        });
    };

    const addJournalToPublisher = (pubId, journal) => {
        setPublishers(prev => prev.map(p => {
            if (p.pubId !== pubId) return p;
            if (p.journals.find(j => j.jId === journal.jId)) return p;
            return { ...p, journals: [...p.journals, journal] };
        }));
    };

    /** Save config + template to backend configs/ folder */
    const saveConfigToBackend = async (pubId, jId, sections, templateName) => {
        const positions = {};
        const sectionKeys = ['newPage', 'header', 'footer', 'leftMargin', 'rightMargin'];
        for (const key of sectionKeys) {
            positions[POSITION_MAP[key]] = convertSectionToBackend(sections[key], key);
        }
        const payload = { publisherId: pubId, jcode: jId, positions };
        try {
            // 1. Save config
            const configRes = await fetch(`${API_BASE}/config/save`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload),
            });
            const configResult = await configRes.json();

            // 2. Save template (if newPage is enabled and a template is selected)
            if (sections.newPage?.enabled && templateName && TEMPLATES[templateName]) {
                const tpl = TEMPLATES[templateName];
                const shortcodeValues = {};
                if (tpl.shortcodes) {
                    for (const key of Object.keys(tpl.shortcodes)) {
                        shortcodeValues[key] = tpl.shortcodes[key].default || '';
                    }
                }
                const templatePayload = {
                    publisherId: pubId,
                    jcode: jId,
                    templateName,
                    shortcodes: shortcodeValues,
                    htmlTemplate: tpl.htmlTemplate,
                };
                await fetch(`${API_BASE}/template/save`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(templatePayload),
                });
            }

            return configResult;
        } catch {
            return { success: false, message: 'Failed to reach backend' };
        }
    };

    return (
        <ConfigContext.Provider value={{
            publishers, configs,
            getTableRows, addConfig, updateConfig, deleteConfig, getConfigById,
            addPublisher, addJournalToPublisher, saveConfigToBackend,
            restoreConfig, getArchivedRows,
        }}>
            {children}
        </ConfigContext.Provider>
    );
};

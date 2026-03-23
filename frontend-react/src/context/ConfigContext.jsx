import { createContext, useContext, useState, useCallback, useEffect } from 'react';
import publishersData from '../data/publishers.json';

const ConfigContext = createContext();

export const useConfigContext = () => useContext(ConfigContext);

const API_BASE = 'http://localhost:8080/api/v1';

const DEFAULT_SECTION = {
    enabled: false,
    logo: { enabled: false, value: null },
    html: { enabled: false, value: '' },
    text: { enabled: false, value: '' },
    link: { enabled: false, url: '', text: '' },
    articleTitle: false,
    articleAuthors: false,
    articleDoi: false,
    articleCopyright: false,
    articleIssn: false,
    articleId: false,
    dateOfDownload: false,
    downloadBy: false,
    adsBanner: { enabled: false, legacyDomain: '' },
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
    const [configs, setConfigs] = useState([]);
    const [loading, setLoading] = useState(true);

    // Load configs from backend on mount
    useEffect(() => {
        const fetchConfigs = async () => {
            try {
                const res = await fetch(`${API_BASE}/configs`);
                if (res.ok) {
                    const data = await res.json();
                    setConfigs(data);
                }
            } catch {
                console.error('Failed to load configs from backend');
            } finally {
                setLoading(false);
            }
        };
        fetchConfigs();
    }, []);

    const getTableRows = useCallback(() => {
        return configs.filter(cfg => !cfg.archived).map((cfg, idx) => {
            const pub = publishers.find(p => p.pubId === cfg.pubId);
            const journal = pub?.journals.find(j => j.jcode === cfg.jcode);
            return {
                serial: idx + 1,
                id: cfg.id,
                pubId: cfg.pubId,
                pubName: pub?.pubName || cfg.pubId,
                jcode: cfg.jcode,
                jName: journal?.jName || cfg.jcode,
            };
        });
    }, [configs, publishers]);

    const saveToBackend = async (config) => {
        try {
            const res = await fetch(`${API_BASE}/configs`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(config),
            });
            return await res.json();
        } catch {
            return { success: false, message: 'Failed to reach backend' };
        }
    };

    const addConfig = async (config) => {
        const maxId = configs.length > 0 ? Math.max(...configs.map(c => c.id || 0)) : 0;
        const newConfig = { ...config, id: maxId + 1 };
        setConfigs(prev => [...prev, newConfig]);
        return await saveToBackend(newConfig);
    };

    const updateConfig = async (id, updatedConfig) => {
        const merged = configs.map(c => c.id === id ? { ...c, ...updatedConfig } : c);
        setConfigs(merged);
        const target = merged.find(c => c.id === id);
        if (target) return await saveToBackend(target);
        return { success: false, message: 'Config not found' };
    };

    const deleteConfig = async (id) => {
        const cfg = configs.find(c => c.id === id);
        if (!cfg) return;
        setConfigs(prev => prev.map(c => c.id === id ? { ...c, archived: true } : c));
        try {
            await fetch(`${API_BASE}/configs/${cfg.pubId}/${cfg.jcode}`, { method: 'DELETE' });
        } catch { /* silent */ }
    };

    const restoreConfig = async (id) => {
        const cfg = configs.find(c => c.id === id);
        if (!cfg) return;
        setConfigs(prev => prev.map(c => c.id === id ? { ...c, archived: false } : c));
        try {
            await fetch(`${API_BASE}/configs/${cfg.pubId}/${cfg.jcode}/restore`, { method: 'PUT' });
        } catch { /* silent */ }
    };

    const getArchivedRows = useCallback(() => {
        return configs.filter(cfg => cfg.archived).map((cfg, idx) => {
            const pub = publishers.find(p => p.pubId === cfg.pubId);
            const journal = pub?.journals.find(j => j.jcode === cfg.jcode);
            return {
                serial: idx + 1,
                id: cfg.id,
                pubId: cfg.pubId,
                pubName: pub?.pubName || cfg.pubId,
                jcode: cfg.jcode,
                jName: journal?.jName || cfg.jcode,
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
            if (p.journals.find(j => j.jcode === journal.jcode)) return p;
            return { ...p, journals: [...p.journals, journal] };
        }));
    };

    return (
        <ConfigContext.Provider value={{
            publishers, configs, loading,
            getTableRows, addConfig, updateConfig, deleteConfig, getConfigById,
            addPublisher, addJournalToPublisher,
            restoreConfig, getArchivedRows,
        }}>
            {children}
        </ConfigContext.Provider>
    );
};
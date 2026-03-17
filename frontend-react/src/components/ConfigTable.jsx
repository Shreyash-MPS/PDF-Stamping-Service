import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { useConfigContext } from '../context/ConfigContext';
import { Pencil, Trash2, Plus, ChevronLeft, ChevronRight, Filter, Undo2, X } from 'lucide-react';

const PAGE_SIZE = 20;

const ConfigTable = () => {
    const navigate = useNavigate();
    const { publishers, getTableRows, deleteConfig, restoreConfig } = useConfigContext();
    const [deleteConfirm, setDeleteConfirm] = useState(null);
    const [currentPage, setCurrentPage] = useState(1);
    const [filterPubId, setFilterPubId] = useState('');
    const [toast, setToast] = useState(null);
    const toastTimer = useRef(null);

    const rows = (() => {
        const all = getTableRows();
        return filterPubId ? all.filter(r => r.pubId === filterPubId) : all;
    })();
    const totalPages = Math.max(1, Math.ceil(rows.length / PAGE_SIZE));
    const page = Math.min(currentPage, totalPages);
    const pagedRows = rows.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);

    const handleEdit = (id) => navigate(`/config/${id}`);

    const handleDelete = (id) => {
        if (deleteConfirm === id) {
            const row = rows.find(r => r.id === id);
            deleteConfig(id);
            setDeleteConfirm(null);
            showToast(id, row?.jName || 'Configuration');
        } else {
            setDeleteConfirm(id);
            setTimeout(() => setDeleteConfirm(null), 3000);
        }
    };

    const showToast = (id, name) => {
        if (toastTimer.current) clearTimeout(toastTimer.current);
        setToast({ id, name });
        toastTimer.current = setTimeout(() => setToast(null), 5000);
    };

    const handleUndo = () => {
        if (toast) {
            restoreConfig(toast.id);
            if (toastTimer.current) clearTimeout(toastTimer.current);
            setToast(null);
        }
    };

    const dismissToast = () => {
        if (toastTimer.current) clearTimeout(toastTimer.current);
        setToast(null);
    };

    useEffect(() => () => { if (toastTimer.current) clearTimeout(toastTimer.current); }, []);

    return (
        <div className="min-h-screen bg-[#f5f5f5]">
            <div className="max-w-5xl mx-auto px-6 py-10">
                {/* Header */}
                <div className="flex items-center justify-between mb-8">
                    <div>
                        <h1 className="text-2xl font-bold text-gray-800">Stamping Configurations</h1>
                        <p className="text-sm text-gray-500 mt-1">Manage publisher and journal stamping configurations</p>
                    </div>
                    <button
                        onClick={() => navigate('/config/new')}
                        className="inline-flex items-center gap-2 px-5 py-2.5 bg-[#a81732] text-white rounded-md text-sm font-semibold hover:bg-[#851227] transition-colors cursor-pointer"
                    >
                        <Plus className="w-4 h-4" />
                        Add Configuration
                    </button>
                </div>

                {/* Publisher filter */}
                <div className="flex items-center gap-2 mb-4">
                    <Filter className="w-4 h-4 text-gray-400" />
                    <select
                        className="px-3 py-2 border border-gray-300 rounded-md text-sm bg-white focus:outline-none focus:ring-2 focus:ring-[#a81732]/20 focus:border-[#a81732]"
                        value={filterPubId}
                        onChange={e => { setFilterPubId(e.target.value); setCurrentPage(1); }}
                    >
                        <option value="">All Publishers</option>
                        {publishers.map(p => (
                            <option key={p.pubId} value={p.pubId}>{p.pubName}</option>
                        ))}
                    </select>
                    {filterPubId && (
                        <span className="text-xs text-gray-400">{rows.length} result{rows.length !== 1 ? 's' : ''}</span>
                    )}
                </div>

                {/* Table */}
                {rows.length === 0 ? (
                    <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-12 text-center">
                        <p className="text-gray-400 text-lg mb-2">No configurations yet</p>
                        <p className="text-gray-400 text-sm">Click "Add Configuration" to get started.</p>
                    </div>
                ) : (
                    <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
                        <table className="w-full text-left">
                            <thead>
                                <tr className="bg-[#f0f0f0] border-b border-gray-200">
                                    <th className="px-5 py-3 text-xs font-bold text-gray-500 uppercase tracking-wider w-16">S.No</th>
                                    <th className="px-5 py-3 text-xs font-bold text-gray-500 uppercase tracking-wider">Publisher Name</th>
                                    <th className="px-5 py-3 text-xs font-bold text-gray-500 uppercase tracking-wider">Journal Name</th>
                                    <th className="px-5 py-3 text-xs font-bold text-gray-500 uppercase tracking-wider w-32 text-center">Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                {pagedRows.map((row) => (
                                    <tr key={row.id} className="border-b border-gray-100 hover:bg-gray-50 transition-colors">
                                        <td className="px-5 py-3.5 text-sm text-gray-600 font-medium">{row.serial}</td>
                                        <td className="px-5 py-3.5 text-sm text-gray-800">{row.pubName}</td>
                                        <td className="px-5 py-3.5 text-sm text-gray-800">{row.jName}</td>
                                        <td className="px-5 py-3.5 text-center">
                                            <div className="inline-flex items-center gap-2">
                                                <button
                                                    onClick={() => handleEdit(row.id)}
                                                    className="p-2 text-gray-500 hover:text-[#a81732] hover:bg-[#fdf2f4] rounded-md transition-colors cursor-pointer"
                                                    title="Edit configuration"
                                                >
                                                    <Pencil className="w-4 h-4" />
                                                </button>
                                                <button
                                                    onClick={() => handleDelete(row.id)}
                                                    className={`p-2 rounded-md transition-colors cursor-pointer ${
                                                        deleteConfirm === row.id
                                                            ? 'text-white bg-red-500 hover:bg-red-600'
                                                            : 'text-gray-500 hover:text-red-600 hover:bg-red-50'
                                                    }`}
                                                    title={deleteConfirm === row.id ? 'Click again to confirm' : 'Delete configuration'}
                                                >
                                                    <Trash2 className="w-4 h-4" />
                                                </button>
                                            </div>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}

                {/* Pagination */}
                <div className="mt-4 flex items-center justify-between">
                    <div className="text-xs text-gray-400">{rows.length} configuration{rows.length !== 1 ? 's' : ''} total</div>
                    {totalPages > 1 && (
                        <div className="flex items-center gap-1">
                            <button onClick={() => setCurrentPage(p => Math.max(1, p - 1))} disabled={page === 1}
                                className="p-1.5 rounded-md text-gray-500 hover:text-gray-800 hover:bg-gray-200 disabled:opacity-30 disabled:cursor-not-allowed cursor-pointer transition-colors">
                                <ChevronLeft className="w-4 h-4" />
                            </button>
                            {Array.from({ length: totalPages }, (_, i) => i + 1).map(n => (
                                <button key={n} onClick={() => setCurrentPage(n)}
                                    className={`min-w-[32px] h-8 rounded-md text-sm font-medium transition-colors cursor-pointer ${
                                        n === page ? 'bg-[#a81732] text-white' : 'text-gray-600 hover:bg-gray-200'
                                    }`}>{n}</button>
                            ))}
                            <button onClick={() => setCurrentPage(p => Math.min(totalPages, p + 1))} disabled={page === totalPages}
                                className="p-1.5 rounded-md text-gray-500 hover:text-gray-800 hover:bg-gray-200 disabled:opacity-30 disabled:cursor-not-allowed cursor-pointer transition-colors">
                                <ChevronRight className="w-4 h-4" />
                            </button>
                        </div>
                    )}
                </div>
            </div>

            {/* Undo toast */}
            {toast && (
                <div className="fixed bottom-6 left-1/2 -translate-x-1/2 z-50 flex items-center gap-3 bg-gray-800 text-white px-5 py-3 rounded-lg shadow-lg text-sm animate-[fadeIn_0.2s_ease]">
                    <span>"{toast.name}" deleted</span>
                    <button onClick={handleUndo} className="inline-flex items-center gap-1 text-amber-300 hover:text-amber-200 font-semibold cursor-pointer">
                        <Undo2 className="w-3.5 h-3.5" /> Undo
                    </button>
                    <button onClick={dismissToast} className="ml-1 text-gray-400 hover:text-white cursor-pointer">
                        <X className="w-3.5 h-3.5" />
                    </button>
                </div>
            )}
        </div>
    );
};

export default ConfigTable;
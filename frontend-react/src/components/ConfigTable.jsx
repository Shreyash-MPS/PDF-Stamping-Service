import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { useConfigContext } from '../context/ConfigContext';
import { API_BASE } from '../config/api';
import { Pencil, Trash2, Plus, ChevronLeft, ChevronRight, Filter, Undo2, X, Download, Loader2 } from 'lucide-react';

const PAGE_SIZE = 20;

/* ── Skeleton row ── */
const SkeletonRow = ({ index }) => (
    <tr className="border-b border-gray-100">
        <td className="px-5 py-3.5"><div className="h-4 w-6 bg-gray-200 rounded animate-pulse" /></td>
        <td className="px-5 py-3.5"><div className={`h-4 bg-gray-200 rounded animate-pulse ${index % 3 === 0 ? 'w-40' : index % 3 === 1 ? 'w-52' : 'w-36'}`} /></td>
        <td className="px-5 py-3.5"><div className={`h-4 bg-gray-200 rounded animate-pulse ${index % 3 === 0 ? 'w-56' : index % 3 === 1 ? 'w-44' : 'w-60'}`} /></td>
        <td className="px-5 py-3.5">
            <div className="inline-flex items-center gap-2 justify-center w-full">
                <div className="h-8 w-8 bg-gray-200 rounded-md animate-pulse" />
                <div className="h-8 w-8 bg-gray-200 rounded-md animate-pulse" />
                <div className="h-8 w-8 bg-gray-200 rounded-md animate-pulse" />
            </div>
        </td>
    </tr>
);

/* ── Empty state illustration ── */
const EmptyState = ({ isFiltered, onAdd }) => (
    <div className="bg-white rounded-lg shadow-sm border border-gray-200 py-16 px-8 flex flex-col items-center text-center">
        <svg width="120" height="100" viewBox="0 0 120 100" fill="none" xmlns="http://www.w3.org/2000/svg" className="mb-6 opacity-60">
            <rect x="18" y="10" width="60" height="78" rx="4" fill="#f0f0f0" stroke="#d1d5db" strokeWidth="1.5" />
            <rect x="28" y="24" width="40" height="4" rx="2" fill="#d1d5db" />
            <rect x="28" y="34" width="32" height="3" rx="1.5" fill="#e5e7eb" />
            <rect x="28" y="42" width="36" height="3" rx="1.5" fill="#e5e7eb" />
            <rect x="28" y="50" width="28" height="3" rx="1.5" fill="#e5e7eb" />
            <rect x="28" y="58" width="34" height="3" rx="1.5" fill="#e5e7eb" />
            <rect x="28" y="66" width="24" height="3" rx="1.5" fill="#e5e7eb" />
            <circle cx="88" cy="68" r="20" fill="#fdf2f4" stroke="#a81732" strokeWidth="1.5" strokeDasharray="4 2" />
            <line x1="88" y1="61" x2="88" y2="75" stroke="#a81732" strokeWidth="2" strokeLinecap="round" />
            <line x1="81" y1="68" x2="95" y2="68" stroke="#a81732" strokeWidth="2" strokeLinecap="round" />
        </svg>
        {isFiltered ? (
            <>
                <p className="text-gray-700 font-semibold text-base mb-1">No results for this publisher</p>
                <p className="text-gray-400 text-sm">Try selecting a different publisher or clear the filter.</p>
            </>
        ) : (
            <>
                <p className="text-gray-700 font-semibold text-base mb-1">No configurations yet</p>
                <p className="text-gray-400 text-sm mb-5">Add your first publisher and journal stamping configuration to get started.</p>
                <button
                    onClick={onAdd}
                    className="inline-flex items-center gap-2 px-4 py-2 bg-[#a81732] text-white rounded-md text-sm font-semibold hover:bg-[#851227] transition-colors cursor-pointer"
                >
                    <Plus className="w-4 h-4" />
                    Add Configuration
                </button>
            </>
        )}
    </div>
);

/* ── Delete confirmation dialog ── */
const DeleteDialog = ({ row, onConfirm, onCancel }) => (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40" onClick={onCancel}>
        <div
            className="bg-white rounded-lg shadow-xl w-full max-w-sm mx-4 p-6"
            onClick={e => e.stopPropagation()}
        >
            <div className="flex items-start gap-4 mb-5">
                <div className="flex-shrink-0 w-10 h-10 rounded-full bg-red-50 flex items-center justify-center">
                    <Trash2 className="w-5 h-5 text-red-500" />
                </div>
                <div>
                    <h3 className="text-base font-bold text-gray-800 mb-1">Delete configuration?</h3>
                    <p className="text-sm text-gray-500">
                        <span className="font-medium text-gray-700">{row.jName}</span>
                    </p>
                </div>
            </div>
            <div className="flex gap-3 justify-end">
                <button
                    onClick={onCancel}
                    className="px-4 py-2 text-sm font-semibold text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 transition-colors cursor-pointer"
                >
                    Cancel
                </button>
                <button
                    onClick={onConfirm}
                    className="px-4 py-2 text-sm font-semibold text-white bg-red-500 rounded-md hover:bg-red-600 transition-colors cursor-pointer"
                >
                    Delete
                </button>
            </div>
        </div>
    </div>
);

/* ── Tooltip wrapper ── */
const Tooltip = ({ label, children, align = 'center' }) => (
    <div className="relative group/tip">
        {children}
        <div className={`pointer-events-none absolute bottom-full mb-2 px-2 py-1 rounded bg-gray-800 text-white text-xs whitespace-nowrap opacity-0 group-hover/tip:opacity-100 transition-opacity z-20 ${
            align === 'right' ? 'right-0' : align === 'left' ? 'left-0' : 'left-1/2 -translate-x-1/2'
        }`}>
            {label}
            <div className={`absolute top-full border-4 border-transparent border-t-gray-800 ${
                align === 'right' ? 'right-2' : align === 'left' ? 'left-2' : 'left-1/2 -translate-x-1/2'
            }`} />
        </div>
    </div>
);

const ConfigTable = () => {
    const navigate = useNavigate();
    const { publishers, getTableRows, deleteConfig, restoreConfig, loading } = useConfigContext();
    const [pendingDelete, setPendingDelete] = useState(null); // row object
    const [currentPage, setCurrentPage] = useState(1);
    const [filterPubId, setFilterPubId] = useState('');
    const [toast, setToast] = useState(null);
    const [downloading, setDownloading] = useState(null);
    const toastTimer = useRef(null);

    const rows = (() => {
        const all = getTableRows();
        return filterPubId ? all.filter(r => r.pubId === filterPubId) : all;
    })();
    const totalPages = Math.max(1, Math.ceil(rows.length / PAGE_SIZE));
    const page = Math.min(currentPage, totalPages);
    const pagedRows = rows.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);

    const handleEdit = (id) => navigate(`/config/${id}`);

    const handleDelete = (row) => setPendingDelete(row);

    const confirmDelete = () => {
        if (!pendingDelete) return;
        deleteConfig(pendingDelete.id);
        showToast(pendingDelete.id, pendingDelete.jName || 'Configuration');
        setPendingDelete(null);
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

    const handleDownloadDemo = async (row) => {
        const id = row.id;
        setDownloading(id);
        try {
            const response = await fetch(`${API_BASE}/stamp/demo-pdf/${row.pubId}/${row.jcode}`);
            if (!response.ok) throw new Error('Failed to generate demo PDF');
            const blob = await response.blob();
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `demo_${row.pubId}_${row.jcode}_stamped.pdf`;
            document.body.appendChild(a);
            a.click();
            a.remove();
            window.URL.revokeObjectURL(url);
        } catch (err) {
            alert('Failed to download demo PDF: ' + err.message);
        } finally {
            setDownloading(null);
        }
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
                        aria-label="Filter by publisher"
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

                {/* Table / skeleton / empty */}
                {loading ? (
                    <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
                        <table className="w-full text-left">
                            <thead>
                                <tr className="bg-[#f0f0f0] border-b border-gray-200">
                                    <th className="px-5 py-3 text-xs font-bold text-gray-500 uppercase tracking-wider w-16">S.No</th>
                                    <th className="px-5 py-3 text-xs font-bold text-gray-500 uppercase tracking-wider">Publisher Name</th>
                                    <th className="px-5 py-3 text-xs font-bold text-gray-500 uppercase tracking-wider">Journal Name</th>
                                    <th className="px-5 py-3 text-xs font-bold text-gray-500 uppercase tracking-wider w-44 text-center">Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                {Array.from({ length: 6 }).map((_, i) => <SkeletonRow key={i} index={i} />)}
                            </tbody>
                        </table>
                    </div>
                ) : rows.length === 0 ? (
                    <EmptyState isFiltered={!!filterPubId} onAdd={() => navigate('/config/new')} />
                ) : (
                    <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
                        <table className="w-full text-left">
                            <thead>
                                <tr className="bg-[#f0f0f0] border-b border-gray-200">
                                    <th className="px-5 py-3 text-xs font-bold text-gray-500 uppercase tracking-wider w-16">S.No</th>
                                    <th className="px-5 py-3 text-xs font-bold text-gray-500 uppercase tracking-wider">Publisher Name</th>
                                    <th className="px-5 py-3 text-xs font-bold text-gray-500 uppercase tracking-wider">Journal Name</th>
                                    <th className="px-5 py-3 text-xs font-bold text-gray-500 uppercase tracking-wider w-44 text-center">Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                {pagedRows.map((row) => (
                                    <tr key={row.id} className="border-b border-gray-100 hover:bg-gray-50 transition-colors">
                                        <td className="px-5 py-3.5 text-sm text-gray-600 font-medium">{row.serial}</td>
                                        <td className="px-5 py-3.5 text-sm text-gray-800">{row.pubName}</td>
                                        <td className="px-5 py-3.5 text-sm text-gray-800">{row.jName}</td>
                                        <td className="px-5 py-3.5 text-center">
                                            <div className="inline-flex items-center gap-1">
                                                <Tooltip label="Download sample PDF">
                                                    <button
                                                        onClick={() => handleDownloadDemo(row)}
                                                        disabled={downloading === row.id}
                                                        aria-label="Download sample PDF"
                                                        className="p-2 text-gray-500 hover:text-blue-600 hover:bg-blue-50 rounded-md transition-colors cursor-pointer disabled:opacity-50 disabled:cursor-wait"
                                                    >
                                                        {downloading === row.id ? <Loader2 className="w-4 h-4 animate-spin" /> : <Download className="w-4 h-4" />}
                                                    </button>
                                                </Tooltip>
                                                <Tooltip label="Edit configuration">
                                                    <button
                                                        onClick={() => handleEdit(row.id)}
                                                        aria-label="Edit configuration"
                                                        className="p-2 text-gray-500 hover:text-[#a81732] hover:bg-[#fdf2f4] rounded-md transition-colors cursor-pointer"
                                                    >
                                                        <Pencil className="w-4 h-4" />
                                                    </button>
                                                </Tooltip>
                                                <Tooltip label="Delete configuration" align="right">
                                                    <button
                                                        onClick={() => handleDelete(row)}
                                                        aria-label="Delete configuration"
                                                        className="p-2 text-gray-500 hover:text-red-600 hover:bg-red-50 rounded-md transition-colors cursor-pointer"
                                                    >
                                                        <Trash2 className="w-4 h-4" />
                                                    </button>
                                                </Tooltip>
                                            </div>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}

                {/* Pagination */}
                {!loading && (
                    <div className="mt-4 flex items-center justify-between">
                        <div className="text-xs text-gray-400">{rows.length} configuration{rows.length !== 1 ? 's' : ''} total</div>
                        {totalPages > 1 && (
                            <div className="flex items-center gap-1">
                                <button onClick={() => setCurrentPage(p => Math.max(1, p - 1))} disabled={page === 1}
                                    aria-label="Previous page"
                                    className="p-1.5 rounded-md text-gray-500 hover:text-gray-800 hover:bg-gray-200 disabled:opacity-30 disabled:cursor-not-allowed cursor-pointer transition-colors">
                                    <ChevronLeft className="w-4 h-4" />
                                </button>
                                {Array.from({ length: totalPages }, (_, i) => i + 1).map(n => (
                                    <button key={n} onClick={() => setCurrentPage(n)}
                                        aria-label={`Page ${n}`}
                                        aria-current={n === page ? 'page' : undefined}
                                        className={`min-w-[32px] h-8 rounded-md text-sm font-medium transition-colors cursor-pointer ${
                                            n === page ? 'bg-[#a81732] text-white' : 'text-gray-600 hover:bg-gray-200'
                                        }`}>{n}</button>
                                ))}
                                <button onClick={() => setCurrentPage(p => Math.min(totalPages, p + 1))} disabled={page === totalPages}
                                    aria-label="Next page"
                                    className="p-1.5 rounded-md text-gray-500 hover:text-gray-800 hover:bg-gray-200 disabled:opacity-30 disabled:cursor-not-allowed cursor-pointer transition-colors">
                                    <ChevronRight className="w-4 h-4" />
                                </button>
                            </div>
                        )}
                    </div>
                )}
            </div>

            {/* Delete confirmation dialog */}
            {pendingDelete && (
                <DeleteDialog
                    row={pendingDelete}
                    onConfirm={confirmDelete}
                    onCancel={() => setPendingDelete(null)}
                />
            )}

            {/* Undo toast */}
            {toast && (
                <div className="fixed bottom-6 left-1/2 -translate-x-1/2 z-50 flex items-center gap-3 bg-gray-800 text-white px-5 py-3 rounded-lg shadow-lg text-sm">
                    <span>"{toast.name}" deleted</span>
                    <button onClick={handleUndo} className="inline-flex items-center gap-1 text-amber-300 hover:text-amber-200 font-semibold cursor-pointer">
                        <Undo2 className="w-3.5 h-3.5" /> Undo
                    </button>
                    <button onClick={dismissToast} aria-label="Dismiss" className="ml-1 text-gray-400 hover:text-white cursor-pointer">
                        <X className="w-3.5 h-3.5" />
                    </button>
                </div>
            )}
        </div>
    );
};

export default ConfigTable;

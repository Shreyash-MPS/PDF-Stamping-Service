import { useState, useRef, useEffect } from 'react';
import { ChevronDown, Search } from 'lucide-react';

const SearchableSelect = ({ options, value, onChange, placeholder = 'Select...', disabled = false }) => {
    const [open, setOpen] = useState(false);
    const [search, setSearch] = useState('');
    const ref = useRef(null);
    const inputRef = useRef(null);

    const selected = options.find(o => o.value === value);
    const filtered = options.filter(o =>
        o.label.toLowerCase().includes(search.toLowerCase())
    );

    useEffect(() => {
        const handler = (e) => {
            if (ref.current && !ref.current.contains(e.target)) setOpen(false);
        };
        document.addEventListener('mousedown', handler);
        return () => document.removeEventListener('mousedown', handler);
    }, []);

    useEffect(() => {
        if (open && inputRef.current) inputRef.current.focus();
    }, [open]);

    return (
        <div ref={ref} className="relative flex-1">
            <button
                type="button"
                disabled={disabled}
                onClick={() => !disabled && setOpen(v => !v)}
                className={`w-full flex items-center justify-between px-3 py-2 border border-gray-300 rounded-md text-sm bg-white text-left
                    focus:outline-none focus:ring-2 focus:ring-[#a81732]/20 focus:border-[#a81732]
                    ${disabled ? 'opacity-60 cursor-not-allowed bg-gray-50' : 'cursor-pointer'}`}
            >
                <span className={selected ? 'text-gray-800' : 'text-gray-400'}>
                    {selected ? selected.label : placeholder}
                </span>
                <ChevronDown className={`w-4 h-4 text-gray-400 transition-transform ${open ? 'rotate-180' : ''}`} />
            </button>

            {open && (
                <div className="absolute z-50 mt-1 w-full bg-white border border-gray-200 rounded-md shadow-lg">
                    <div className="flex items-center gap-2 px-3 py-2 border-b border-gray-100">
                        <Search className="w-4 h-4 text-gray-400 shrink-0" />
                        <input
                            ref={inputRef}
                            type="text"
                            className="w-full text-sm outline-none placeholder-gray-400"
                            placeholder="Search..."
                            value={search}
                            onChange={e => setSearch(e.target.value)}
                        />
                    </div>
                    <ul className="max-h-48 overflow-y-auto py-1">
                        {filtered.length === 0 ? (
                            <li className="px-3 py-2 text-sm text-gray-400">No results</li>
                        ) : (
                            filtered.map(o => (
                                <li key={o.value}>
                                    <button
                                        type="button"
                                        className={`w-full text-left px-3 py-2 text-sm cursor-pointer transition-colors
                                            ${o.value === value ? 'bg-[#fdf2f4] text-[#a81732] font-medium' : 'text-gray-700 hover:bg-gray-50'}`}
                                        onClick={() => { onChange(o.value); setOpen(false); setSearch(''); }}
                                    >
                                        {o.label}
                                    </button>
                                </li>
                            ))
                        )}
                    </ul>
                </div>
            )}
        </div>
    );
};

export default SearchableSelect;

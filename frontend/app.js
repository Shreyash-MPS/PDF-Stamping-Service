document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('stampForm');
    const submitBtn = document.getElementById('submitBtn');
    const toast = document.getElementById('toast');
    const toastMessage = document.getElementById('toastMessage');

    // ---- Position toggle: show/hide per-position config panels ----
    document.querySelectorAll('.position-toggle').forEach(toggle => {
        const position = toggle.dataset.position;
        const configPanel = document.getElementById('config-' + position);

        toggle.addEventListener('change', () => {
            const block = toggle.closest('.position-block');
            if (toggle.checked) {
                configPanel.classList.add('show');
                block.classList.add('active');
            } else {
                configPanel.classList.remove('show');
                block.classList.remove('active');
            }
        });
    });

    // ---- Per-panel expandable fields (checkbox toggles sub-field visibility) ----
    document.querySelectorAll('.position-config-inner').forEach(panel => {
        panel.querySelectorAll('[data-field]').forEach(checkbox => {
            if (checkbox.type !== 'checkbox') return;

            const fieldName = checkbox.dataset.field;
            const expandable = panel.querySelector(`[data-expand="${fieldName}"]`);
            if (!expandable) return;

            checkbox.addEventListener('change', () => {
                if (checkbox.checked) {
                    expandable.classList.add('show');
                } else {
                    expandable.classList.remove('show');
                }
            });
        });
    });

    /**
     * Read a File as base64.
     */
    function readFileAsBase64(file) {
        return new Promise((resolve, reject) => {
            const reader = new FileReader();
            reader.onload = () => {
                const dataUrl = reader.result;
                const mimeType = dataUrl.substring(dataUrl.indexOf(':') + 1, dataUrl.indexOf(';'));
                const base64 = dataUrl.substring(dataUrl.indexOf(',') + 1);
                resolve({ base64, mimeType });
            };
            reader.onerror = reject;
            reader.readAsDataURL(file);
        });
    }

    /**
     * Extract config from a single position panel.
     */
    async function extractPositionConfig(panel) {
        const get = (field) => {
            const el = panel.querySelector(`[data-field="${field}"]`);
            if (!el) return null;
            if (el.type === 'checkbox') return el.checked;
            if (el.type === 'file') return el.files && el.files.length > 0 ? el.files[0] : null;
            return el.value || null;
        };

        const config = {
            alignment: 'CENTER',
            addNewPage: get('addNewPage'),
            includeCurrentUser: get('includeCurrentUser'),

            logo: get('addLogo') ? { base64: null, mimeType: null } : null,
            text: get('addText') ? { content: get('textContent') } : null,
            html: get('addHtml') ? { content: get('htmlContent') } : null,
            doi: get('addDoi') ? { value: get('doiValue') || '' } : null,
            date: get('addDate') ? { enabled: true } : null,
            ad: get('isAd') ? { link: get('adLink') } : null
        };

        // Handle logo file -> base64
        // Note: The `position` variable is not directly available here.
        // We need to get the position from the panel's ID or a data attribute.
        // Assuming panel has an ID like 'config-TOP_LEFT'
        const positionId = panel.id; // e.g., 'config-TOP_LEFT'
        const position = positionId.replace('config-', ''); // e.g., 'TOP_LEFT'

        const fileInput = document.querySelector(`#config-${position} input[type="file"]`);
        const filePrm = new Promise(resolve => {
            if (get('addLogo') && fileInput && fileInput.files.length > 0) {
                const file = fileInput.files[0];
                const reader = new FileReader();
                reader.onload = function (e) {
                    const dataUrl = e.target.result;
                    const base64Data = dataUrl.split(',')[1];
                    config.logo = {
                        base64: base64Data,
                        mimeType: file.type || 'image/png'
                    };
                    resolve();
                };
                reader.readAsDataURL(file);
            } else {
                resolve();
            }
        });
        await filePrm; // Wait for the file to be read

        return config;
    }

    /**
     * Show toast notification.
     */
    function showToast(message, isError = false) {
        toastMessage.textContent = message;
        toast.classList.toggle('error', isError);
        toast.classList.add('show');
        setTimeout(() => toast.classList.remove('show'), 4000);
    }

    // ---- Form submission ----
    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        submitBtn.classList.add('loading');

        try {
            const formData = new FormData(form);

            // Collect selected positions and their configs
            const positions = {};
            const toggles = document.querySelectorAll('.position-toggle:checked');

            if (toggles.length === 0) {
                showToast('Please select at least one stamp position.', true);
                return;
            }

            for (const toggle of toggles) {
                const posName = toggle.dataset.position;
                const panel = document.querySelector(`#config-${posName} .position-config-inner`);
                positions[posName] = await extractPositionConfig(panel);
            }

            const configPayload = {
                publisherId: formData.get('publisherId'),
                jcode: formData.get('jcode'),
                positions: positions
            };

            console.group('📄 Saving Configuration');
            console.log('Payload:', configPayload);
            console.groupEnd();

            const response = await fetch('http://localhost:8080/api/v1/config/save', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(configPayload)
            });

            const result = await response.json();

            if (result.success) {
                showToast('Configuration saved successfully!');
            } else {
                showToast(`Failed: ${result.message}`, true);
            }

        } catch (error) {
            console.error('Error saving config:', error);
            showToast('Failed to save configuration. Is the server running?', true);
        } finally {
            submitBtn.classList.remove('loading');
        }
    });
});

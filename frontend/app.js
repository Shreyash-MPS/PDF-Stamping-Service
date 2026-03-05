document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('stampForm');
    const addDoiCheckbox = document.getElementById('addDoi');
    const doiFieldWrapper = document.getElementById('doiFieldWrapper');
    const isAdCheckbox = document.getElementById('isAd');
    const adLinkWrapper = document.getElementById('adLinkWrapper');

    const addLogoCheckbox = document.getElementById('addLogo');
    const logoUploadWrapper = document.getElementById('logoUploadWrapper');
    const addTextCheckbox = document.getElementById('addText');
    const textInputWrapper = document.getElementById('textInputWrapper');
    const addHtmlCheckbox = document.getElementById('addHtml');
    const htmlInputWrapper = document.getElementById('htmlInputWrapper');

    const submitBtn = document.getElementById('submitBtn');
    const toast = document.getElementById('toast');
    const toastMessage = document.getElementById('toastMessage');

    // Toggle DOI input visibility
    addDoiCheckbox.addEventListener('change', (e) => {
        if (e.target.checked) {
            doiFieldWrapper.classList.add('show');
            document.getElementById('doiValue').required = true;
        } else {
            doiFieldWrapper.classList.remove('show');
            document.getElementById('doiValue').required = false;
        }
    });

    // Toggle Ad Link visibility
    isAdCheckbox.addEventListener('change', (e) => {
        if (e.target.checked) {
            adLinkWrapper.classList.add('show');
            document.getElementById('adLink').required = true;
        } else {
            adLinkWrapper.classList.remove('show');
            document.getElementById('adLink').required = false;
        }
    });

    // Toggle Content Fields visibility
    addLogoCheckbox.addEventListener('change', (e) => {
        if (e.target.checked) {
            logoUploadWrapper.classList.add('show');
            document.getElementById('imageUpload').required = true;
        } else {
            logoUploadWrapper.classList.remove('show');
            document.getElementById('imageUpload').required = false;
        }
    });

    addTextCheckbox.addEventListener('change', (e) => {
        if (e.target.checked) {
            textInputWrapper.classList.add('show');
            document.getElementById('textContent').required = true;
        } else {
            textInputWrapper.classList.remove('show');
            document.getElementById('textContent').required = false;
        }
    });

    addHtmlCheckbox.addEventListener('change', (e) => {
        if (e.target.checked) {
            htmlInputWrapper.classList.add('show');
            document.getElementById('htmlContent').required = true;
        } else {
            htmlInputWrapper.classList.remove('show');
            document.getElementById('htmlContent').required = false;
        }
    });

    /**
     * Read a File as a base64 data URL.
     * Returns a promise that resolves to { base64, mimeType }.
     */
    function readFileAsBase64(file) {
        return new Promise((resolve, reject) => {
            const reader = new FileReader();
            reader.onload = () => {
                // result is like "data:image/png;base64,iVBOR..."
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
     * Show a toast notification.
     */
    function showToast(message, isError = false) {
        toastMessage.textContent = message;
        toast.classList.toggle('error', isError);
        toast.classList.add('show');
        setTimeout(() => {
            toast.classList.remove('show');
        }, 4000);
    }

    // Form Submission — build JSON, send to backend to save
    form.addEventListener('submit', async (e) => {
        e.preventDefault();

        // UI Loading State
        submitBtn.classList.add('loading');

        try {
            // Gather form data
            const formData = new FormData(form);
            const strategy = formData.get('strategy');

            // Build configuration object
            const sectionConfig = {
                position: formData.get('position'),
                alignment: 'CENTER',
                addLogo: formData.get('addLogo') === 'on',
                addText: formData.get('addText') === 'on',
                textContent: formData.get('addText') === 'on' ? formData.get('textContent') : null,
                addHtml: formData.get('addHtml') === 'on',
                htmlContent: formData.get('addHtml') === 'on' ? formData.get('htmlContent') : null,
                addDoi: formData.get('addDoi') === 'on',
                doiValue: formData.get('addDoi') === 'on' ? formData.get('doiValue') : null,
                addDate: formData.get('addDate') === 'on',
                isAd: formData.get('isAd') === 'on',
                adLink: formData.get('isAd') === 'on' ? (formData.get('adLink') || null) : null,
                optionalText: formData.get('optionalText') || null
            };

            // If logo is selected, convert to base64 and embed in config
            if (sectionConfig.addLogo && document.getElementById('imageUpload').files.length > 0) {
                const logoFile = document.getElementById('imageUpload').files[0];
                const { base64, mimeType } = await readFileAsBase64(logoFile);
                sectionConfig.logoBase64 = base64;
                sectionConfig.logoMimeType = mimeType;
            }

            // Construct final payload (matches DynamicStampRequest structure)
            const configPayload = {
                publisherId: formData.get('publisherId'),
                jcode: formData.get('jcode'),
                strategy: strategy,
                configuration: sectionConfig
            };

            console.group('📄 Saving Configuration');
            console.log('Payload:', configPayload);
            console.groupEnd();

            // Send JSON to backend to save
            const response = await fetch('http://localhost:8080/api/v1/config/save', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(configPayload)
            });

            const result = await response.json();

            if (result.success) {
                showToast(`Config saved: ${result.outputFilePath}`);
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

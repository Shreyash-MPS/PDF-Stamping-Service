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

    // Form Submission
    form.addEventListener('submit', async (e) => {
        e.preventDefault();

        // UI Loading State
        submitBtn.classList.add('loading');

        // Gather Data
        const formData = new FormData(form);
        const strategy = formData.get('strategy'); // 'new_page' vs 'update_existing'

        // Construct standard section configuration
        const sectionConfig = {
            position: formData.get('position'),
            alignment: 'CENTER',
            addLogo: formData.get('addLogo') === 'on',
            imageFile: formData.get('addLogo') === 'on' ? formData.get('imageUpload') : null,
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

        // Construct final Payload
        const configPayload = {
            publisherId: formData.get('publisherId'),
            jcode: formData.get('jcode'),
            strategy: strategy,
            configuration: sectionConfig
        };

        const postData = new FormData();
        postData.append('file', document.getElementById('sourcePdf').files[0]);
        postData.append('config', JSON.stringify(configPayload));

        if (sectionConfig.addLogo && document.getElementById('imageUpload').files.length > 0) {
            postData.append('imageFile', document.getElementById('imageUpload').files[0]);
        }

        try {
            console.group('🚀 Sending Stamping Request');
            console.log('Config:', configPayload);
            console.groupEnd();

            const response = await fetch('http://localhost:8080/api/v1/stamp/dynamic', {
                method: 'POST',
                body: postData
            });

            if (!response.ok) {
                const errText = await response.text();
                throw new Error(errText || response.statusText);
            }

            // Download the blob
            const blob = await response.blob();
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `${configPayload.jcode || 'stamped'}.pdf`;
            document.body.appendChild(a);
            a.click();
            a.remove();

            // Output success
            toast.classList.add('show');
            setTimeout(() => {
                toast.classList.remove('show');
            }, 3000);

        } catch (error) {
            console.error('Error stamping PDF:', error);
            alert('Failed to stamp PDF.\n' + error.message);
        } finally {
            // Reset UI Loading State
            submitBtn.classList.remove('loading');
        }
    });
});

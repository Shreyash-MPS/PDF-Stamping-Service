export const TEMPLATES = {
  journal_article: {
    name: 'Journal Article Front Page',
    shortcodes: {
      LOGO_TEXT: { label: 'Logo Image', placeholder: 'Image will render here...', default: '' }
    },
    htmlTemplate: `<!DOCTYPE html>
<html>
<head><meta charset="UTF-8"/></head>
<body style="margin: 50px; font-family: Verdana, Arial, Helvetica, sans-serif; color: #000;">
<div style="display: flex; gap: 40px; align-items: flex-start; flex-wrap: nowrap;">
  <div style="flex: 0 0 30%; max-width: 30%; min-width: 150px; padding-top: 15px;">
    <div style="margin-bottom: 25px;" class="logo-wrapper">{{LOGO_TEXT}}</div>
    <p class="date-block" style="font-size: 15px; margin: 0; line-height: 1.3;">This information is current as of {{DATE}}.</p>
  </div>
  <div style="flex: 1; min-width: 300px;">
    <h1 class="article-title-block" style="font-size: 22px; font-weight: bold; margin: 0 0 16px 0; line-height: 1.2; color: #999; font-style: italic;">[Article Title Will Appear Here]</h1>
    <p class="authors-block" style="font-size: 16px; line-height: 1.4; margin: 0 0 25px 0; color: #999; font-style: italic;">[Authors Will Appear Here]</p>
    <div style="font-size: 15px; line-height: 1.3;">
      <p class="doi-block" style="margin: 0 0 4px 0; color: #999; font-style: italic;">doi: [DOI Will Appear Here]</p>
    </div>
  </div>
</div>
</body>
</html>`
  },
  simple_header: {
    name: 'Simple Header Stamp',
    shortcodes: {
      TITLE: { label: 'Title', placeholder: 'e.g. Published Online', default: '' },
      SUBTITLE: { label: 'Subtitle', placeholder: 'e.g. Journal of Medicine', default: '' }
    },
    htmlTemplate: `<!DOCTYPE html>
<html>
<head><meta charset="UTF-8"/></head>
<body style="margin: 40px; font-family: Verdana, Arial, Helvetica, sans-serif; text-align: center;">
  <h1 style="font-size: 28px; color: #1a1918; margin-bottom: 8px;">{{TITLE}}</h1>
  <p style="font-size: 18px; color: #555; margin-top: 0;">{{SUBTITLE}}</p>
  <p style="font-size: 14px; color: #888; margin-top: 16px;">Date: {{DATE}}</p>
</body>
</html>`
  },
  custom_html: {
    name: 'Custom HTML',
    shortcodes: {},
    htmlTemplate: `<!DOCTYPE html>
<html>
<head><meta charset="UTF-8"/></head>
<body style="margin: 40px; font-family: Verdana, Arial, Helvetica, sans-serif;">
  <div>
    <!-- Write your custom HTML here -->
    <p>Custom content goes here</p>
  </div>
</body>
</html>`
  }
};

export const getCurrentDate = () => {
  const months = ['January', 'February', 'March', 'April', 'May', 'June',
    'July', 'August', 'September', 'October', 'November', 'December'];
  const d = new Date();
  return `${months[d.getMonth()]} ${d.getDate()}, ${d.getFullYear()}`;
};

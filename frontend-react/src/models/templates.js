export const TEMPLATES = {
  journal_article: {
    name: 'Journal Article Front Page',
    shortcodes: {
      LOGO_TEXT: { label: 'Logo Image', placeholder: 'Image will render here...', default: '' },
      ARTICLE_TITLE: { label: 'Article Title', placeholder: 'e.g. American Society of Neuroradiology...', default: '', type: 'textarea' },
      AUTHORS: { label: 'Authors', placeholder: 'e.g. Jana Ivanidze, Ana M. Franceschi...', default: '', type: 'textarea' },
      CITATION: { label: 'Citation Text', placeholder: 'e.g. AJNR Am J Neuroradiol 2026, 47 (2) 281-288', default: '' },
      DOI: { label: 'DOI', placeholder: 'e.g. 10.3174/ajnr.A8959', default: '' },
      LINK: { label: 'Additional Link', placeholder: 'e.g. http://www.ajnr.org/content/47/2/281', default: '' }
    },
    htmlTemplate: `<!DOCTYPE html>
<html>
<head><meta charset="UTF-8"/></head>
<body style="margin: 50px; font-family: 'Times New Roman', Times, serif; color: #000;">
<div style="display: flex; gap: 40px; align-items: flex-start; flex-wrap: nowrap;">
  <div style="flex: 0 0 30%; max-width: 30%; min-width: 150px; padding-top: 15px;">
    <div style="margin-bottom: 25px;" class="logo-wrapper">{{LOGO_TEXT}}</div>
    <p style="font-size: 15px; margin: 0; line-height: 1.3;">This information is current as of {{DATE}}.</p>
  </div>
  <div style="flex: 1; min-width: 300px;">
    <h1 style="font-size: 22px; font-weight: bold; margin: 0 0 16px 0; line-height: 1.2;">{{ARTICLE_TITLE}}</h1>
    <p style="font-size: 16px; line-height: 1.4; margin: 0 0 25px 0;">{{AUTHORS}}</p>
    <div style="font-size: 15px; line-height: 1.3;">
      <p style="margin: 0 0 4px 0;"><i>{{CITATION}}</i></p>
      <p style="margin: 0; color: black;">doi: <a href="https://doi.org/{{DOI}}" style="color: blue; text-decoration: none;">https://doi.org/{{DOI}}</a></p>
      <p style="margin: 0; color: blue;"><a href="{{LINK}}" style="color: blue; text-decoration: none;">{{LINK}}</a></p>
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
<body style="margin: 40px; font-family: Arial, sans-serif; text-align: center;">
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
<body style="margin: 40px; font-family: Arial, sans-serif;">
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

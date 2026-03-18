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
  <div style="flex: 0 0 30%; max-width: 30%; min-width: 150px; padding: 50px;">
    <div style="margin-bottom: 25px;" class="logo-wrapper">{{LOGO_TEXT}}</div>
    <p class="date-block" style="font-size: 15px; margin: 0; line-height: 1.3;">This information is current as of {{DATE}}.</p>
  </div>
  <div style="flex: 1; min-width: 300px; padding:50px;">
    <h1 class="article-title-block" style="font-size: 22px; font-weight: bold; margin: 0 0 16px 0; line-height: 1.2; color: #999;">{{ARTICLE_TITLE}}</h1>
    <p class="authors-block" style="font-size: 16px; line-height: 1.4; margin: 0 0 25px 0; color: #999;">{{AUTHORS}}</p>
    <div style="font-size: 15px; line-height: 1.3;">
      <p class="doi-block" style="margin: 0 0 4px 0; color: #999;">doi: {{DOI}}</p>
      <div class="link-block" style="margin: 0 0 4px 0;"><a href="{{LINK_URL}}" style="color: blue; text-decoration: none;">{{LINK_TEXT}}</a></div>
    </div>
  </div>
</div>
</body>
</html>`
  },
  genome_last_page: {
    name: 'Genome Research (Last Page)',
    shortcodes: {},
    pagePosition: 'back',
    defaultSection: {
      enabled: true,
      pagePosition: 'back',
      logo: { enabled: true, value: null },
      html: { enabled: false, value: '' },
      text: { enabled: false, value: '' },
      articleTitle: true,
      articleAuthors: true,
      articleDoi: true,
      articleCopyright: true,
      articleIssn: true,
      articleId: false,
      dateOfDownload: true,
      downloadBy: false,
      adsBanner: { enabled: true, value: '', legacyDomain: '' },
    },
    htmlTemplate: `<!DOCTYPE html>
<html>
<head><meta charset="UTF-8"/></head>
<body style="margin: 0; padding: 0; font-family: Verdana, Arial, Helvetica, sans-serif; color: #000; font-size: 13px;">
  <div style="padding: 12px 40px; color: #c00; font-size: 11px;">
    Downloaded from <a href="#" style="color: #c00;">genome.cshlp.org</a> on {{DATE}} - Published by <a href="#" style="color: #c00;">Cold Spring Harbor Laboratory Press</a>
  </div>
  <div style="padding: 20px 50px 10px 50px;">
    <div style="margin-bottom: 20px;" class="logo-wrapper">{{LOGO}}</div>
    <h1 class="article-title-block" style="font-size: 20px; font-weight: bold; margin: 0 0 10px 0; line-height: 1.3;">[Article Title]</h1>
    <p class="authors-block" style="font-size: 13px; color: #444; margin: 0 0 10px 0;">[Authors]</p>
    <p style="font-size: 12px; color: #444; margin: 0 0 3px 0;"><i>Genome Res.</i> published online {{DATE}}</p>
    <p class="doi-block" style="font-size: 12px; margin: 0 0 15px 0;">Access the most recent version at doi:<a href="#" style="color: blue; text-decoration: none;">[DOI]</a></p>
    <hr style="border: none; border-top: 1px solid #999; margin: 15px 0;" />
    <table style="width: 100%; font-size: 12px; border-collapse: collapse;">
      <tr><td style="padding: 8px 0; width: 130px; vertical-align: top; text-align: right; padding-right: 15px; font-weight: bold;">P&lt;P</td>
          <td style="padding: 8px 0; vertical-align: top;">Published online {{DATE}} in advance of the print journal.</td></tr>
      <tr><td style="padding: 8px 0; vertical-align: top; text-align: right; padding-right: 15px; font-weight: bold;">Accepted<br/>Manuscript</td>
          <td style="padding: 8px 0; vertical-align: top;">Peer-reviewed and accepted for publication but not copyedited or typeset; accepted manuscript is likely to differ from the final, published version.</td></tr>
      <tr><td style="padding: 8px 0; vertical-align: top; text-align: right; padding-right: 15px; font-weight: bold;">Open Access</td>
          <td style="padding: 8px 0; vertical-align: top;">Freely available online through the Genome Research Open Access option.</td></tr>
      <tr><td style="padding: 8px 0; vertical-align: top; text-align: right; padding-right: 15px; font-weight: bold;">Creative<br/>Commons<br/>License</td>
          <td style="padding: 8px 0; vertical-align: top;">This manuscript is Open Access. This article, published in <i>Genome Research</i>, is available under a Creative Commons License (Attribution-NonCommercial 4.0 International license), as described at <a href="http://creativecommons.org/licenses/by-nc/4.0/" style="color: blue;">http://creativecommons.org/licenses/by-nc/4.0/</a>.</td></tr>
      <tr><td style="padding: 8px 0; vertical-align: top; text-align: right; padding-right: 15px; font-weight: bold;">Email Alerting<br/>Service</td>
          <td style="padding: 8px 0; vertical-align: top;">Receive free email alerts when new articles cite this article - sign up in the box at the top right corner of the article or <a href="#" style="color: blue; font-weight: bold;">click here.</a></td></tr>
    </table>
    <hr style="border: none; border-top: 1px solid #999; margin: 15px 0;" />
  </div>
  <div style="padding: 0 50px; text-align: center; margin-top: 30px;">
    <div class="ad-banner-block" style="margin-bottom: 20px;">{{AD_BANNER}}</div>
  </div>
  <div style="padding: 0 50px; margin-top: 30px;">
    <hr style="border: none; border-top: 1px solid #999; margin: 15px 0;" />
    <p style="font-size: 12px; margin: 8px 0;">To subscribe to <i>Genome Research</i> go to:<br/>
      <a href="https://genome.cshlp.org/subscriptions" style="color: blue; font-weight: bold;">https://genome.cshlp.org/subscriptions</a>
    </p>
    <p style="font-size: 12px; margin: 20px 0 0 0;">Published by Cold Spring Harbor Laboratory Press</p>
  </div>
</body>
</html>`
  },
  default_metadata: {
    name: 'Default Metadata Page',
    shortcodes: {},
    pagePosition: 'front',
    defaultSection: {
      enabled: true,
      pagePosition: 'front',
      logo: { enabled: true, value: null },
      html: { enabled: false, value: '' },
      text: { enabled: false, value: '' },
      articleTitle: true,
      articleAuthors: true,
      articleDoi: true,
      articleCopyright: true,
      articleIssn: true,
      articleId: true,
      dateOfDownload: true,
      downloadBy: true,
      adsBanner: { enabled: false, value: '', legacyDomain: '' },
    },
    htmlTemplate: `<!DOCTYPE html>
<html>
<head><meta charset="UTF-8"/></head>
<body style="margin: 50px; font-family: Verdana, Arial, Helvetica, sans-serif; color: #000;">
  <div style="margin-bottom: 25px;" class="logo-wrapper">{{LOGO}}</div>
  <h1 class="article-title-block" style="font-size: 22px; font-weight: bold; margin: 0 0 16px 0; line-height: 1.2;">[Article Title]</h1>
  <p class="authors-block" style="font-size: 16px; line-height: 1.4; margin: 0 0 25px 0;">[Authors]</p>
  <p class="doi-block" style="margin: 0 0 4px 0; font-size: 15px;">doi: <a href="#" style="color: blue; text-decoration: none;">[DOI]</a></p>
  <div class="link-block" style="margin: 0 0 4px 0; font-size: 15px;"><a href="{{LINK_URL}}" style="color: blue; text-decoration: none;">{{LINK_TEXT}}</a></div>
  <p style="margin: 4px 0; font-size: 13px;">[Copyright]</p>
  <p style="margin: 4px 0; font-size: 13px;">ISSN: [ISSN]</p>
  <p style="margin: 4px 0; font-size: 13px;">Article ID: [Article ID]</p>
  <p style="margin: 8px 0; font-size: 13px; color: #555;">Date Generated: {{DATE}}</p>
  <p style="margin: 4px 0; font-size: 13px; color: #555;">Downloaded By: [User]</p>
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

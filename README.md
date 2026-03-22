# PDF Stamping Service

A Spring Boot + React application for stamping PDFs with HTML overlays and dynamically generated metadata pages. Built for publishers and journals to apply branded content, article metadata, and institutional overlays to PDF documents at scale.

---

## Table of Contents

- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
- [Deployment](#deployment)
- [API Reference](#api-reference)
- [Template System](#template-system)
- [Configuration Storage](#configuration-storage)
- [Frontend](#frontend)
- [Project Structure](#project-structure)
- [Dev Utilities](#dev-utilities)

---

## Architecture

```
┌──────────────────────────────────────────────────────────────┐
│  React Frontend  (Vite + Tailwind CSS)                       │
│                                                              │
│  ConfigTable ──► ConfigForm ──► StampSectionPanel            │
│                                      │                       │
│              ConfigContext (state + REST calls to /api/v1)   │
└──────────────────────────┬───────────────────────────────────┘
                           │  HTTP REST
┌──────────────────────────▼───────────────────────────────────┐
│  Spring Boot Backend                                         │
│                                                              │
│  StampController                                             │
│       │                                                      │
│  ┌────┴──────────────────────────────────────┐              │
│  │  StampService  TemplateService  AdServices │              │
│  │  MetadataFrontPageService  PdfFontExtractor│              │
│  └────┬──────────────────────────────────────┘              │
│       │                                                      │
│  ┌────▼──────────────────────────────────────┐              │
│  │  HtmlStamper (iText pdfHTML)              │              │
│  │  HTML → PDF → XObject overlay / merge     │              │
│  └───────────────────────────────────────────┘              │
└──────────────────────────────────────────────────────────────┘
```

**Data flow for journal stamping:**
1. External system (e.g. Drupal) sends a `POST /stamp/journal-metadata` with a file path + article metadata
2. Backend reads the saved config for that `publisherId`/`jcode` from `configs/`
3. `PdfFontExtractor` extracts the primary font from the input PDF for visual consistency
4. `TemplateService` renders the configured template with the metadata and extracted font
5. `MetadataFrontPageService` converts HTML to PDF and prepends/appends it
6. Overlay positions (header, footer, margins) are stamped via `HtmlStamper` onto existing pages
7. All fonts are embedded via `DefaultFontProvider` for PAC compliance
8. Stamped PDF is returned in the response (and optionally saved to `outputPath`)

---

## Tech Stack

**Backend**
- Java 17
- Spring Boot 3.2.5
- iText 7 / html2pdf 5.0.3 — PDF manipulation and HTML-to-PDF rendering
- Lombok 1.18.40

**Frontend**
- React 19.2 + React Router 7.13
- Vite 7.3
- Tailwind CSS 4.2
- CodeMirror 6 — HTML editor with syntax highlighting
- Lucide React — icons

---

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.6+
- Node.js 18+

### Backend

```bash
mvn clean install
mvn spring-boot:run
```

API available at `http://localhost:8080`.

### Frontend

```bash
cd frontend-react
npm install
npm run dev
```

Dev server at `http://localhost:5173`.

---

## Deployment

### Build

```bash
# Backend
mvn clean package -DskipTests

# Frontend
cd frontend-react
npm run build
```

### What to deploy

| Artifact | Description |
|----------|-------------|
| `target/pdf-stamping-service-1.0.0.jar` | Backend fat jar (includes embedded Tomcat) |
| `frontend-react/dist/` | Built frontend static files |
| `configs/` | Saved publisher stamping configurations |
| `templates/` | External template JSON definitions (if any) |

The `frontend-react/` source folder, `node_modules/`, `src/`, `pom.xml` are NOT needed in production. The `dist/` folder contains the entire compiled frontend.

`publishers.json` and other data files in `frontend-react/src/data/` are bundled into the JS at build time — no need to deploy them separately.

### Running on Linux

```bash
java -jar pdf-stamping-service-1.0.0.jar
```

Ensure:
- Java 17+ is installed
- The working directory contains the `configs/` folder (or it will be created on first save)
- The `dist/` folder is served by a reverse proxy (nginx) or copied into `src/main/resources/static/` before building the jar
- File paths in requests from Drupal use Linux paths (e.g. `/var/data/pdfs/article.pdf`)

### Reverse proxy setup (nginx)

```nginx
server {
    listen 80;

    # Frontend static files
    location / {
        root /path/to/dist;
        try_files $uri $uri/ /index.html;
    }

    # Backend API
    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

With a reverse proxy, both frontend and backend are served from the same origin — no CORS issues.

---

## API Reference

Base URL: `/api/v1`

All error responses follow this shape:

```json
{
  "success": false,
  "message": "Description of the error"
}
```

### POST /api/v1/stamp/journal-metadata

Server-side journal stamping endpoint. Designed for integration with external systems (e.g. Drupal) that trigger stamping at article delivery time.

**Content-Type:** `application/json`

**Request body:**

| Field | Type | Required | Description |
|-------|------|:--------:|-------------|
| `pdfFilePath` | string | yes | Absolute path to the source PDF on the server |
| `outputPath` | string | no | If provided, the stamped PDF is also saved here |
| `publisherId` | string | yes | Publisher identifier (matches saved config) |
| `jcode` | string | yes | Journal code (matches saved config) |
| `env` | string | no | Set to `demo` to auto-fill blank metadata fields with placeholders |
| `articleTitle` | string | no | Article title |
| `authors` | string | no | Author names |
| `doiValue` | string | no | DOI value e.g. `10.xxxx/xxxx` |
| `articleCopyright` | string | no | Copyright line |
| `articleIssn` | string | no | ISSN |
| `articleId` | string | no | Article identifier |
| `downloadedBy` | string | no | Username of the person downloading |
| `positions` | object | yes | Map of position key to configuration (see below) |

**Position keys:** `NEW_PAGE`, `HEADER`, `FOOTER`, `LEFT_MARGIN`, `RIGHT_MARGIN`

**Configuration object per position:**

| Field | Type | Description |
|-------|------|-------------|
| `templateName` | string | Template to render for `NEW_PAGE` (e.g. `default_metadata`, `genome_last_page`) |
| `pagePosition` | string | `front` (prepend) or `back` (append) — only for `NEW_PAGE` |
| `includeArticleTitle` | boolean | Render article title from request |
| `includeAuthors` | boolean | Render authors from request |
| `includeDoi` | boolean | Render DOI link from request |
| `includeDate` | boolean | Render current date |
| `includeCopyright` | boolean | Render copyright from request |
| `includeIssn` | boolean | Render ISSN from request |
| `includeArticleId` | boolean | Render article ID from request |
| `includeCurrentUser` | boolean | Render downloaded-by from request |
| `logo` | string | Base64-encoded logo image |
| `logoMimeType` | string | MIME type of logo e.g. `image/png` |
| `text` | string | Plain text to stamp |
| `html` | string | Raw HTML to stamp |
| `linkUrl` | string | Hyperlink URL |
| `linkText` | string | Hyperlink display text |
| `adsEnabled` | boolean | Fetch and inject ads from BAM |
| `legacyDomain` | string | Legacy domain for resolving relative ad image paths |

**Example request:**

```json
{
  "pdfFilePath": "/data/articles/input.pdf",
  "outputPath": "/data/articles/output_stamped.pdf",
  "publisherId": "demoPub",
  "jcode": "demoJcode",
  "articleTitle": "Selfish Routing Games with Priority Lanes",
  "authors": "Yang Li, Alexander Skopalik, Marc Uetz",
  "doiValue": "10.3174/ajnr.A8959",
  "positions": {
    "NEW_PAGE": {
      "templateName": "default_metadata",
      "includeArticleTitle": true,
      "includeAuthors": true,
      "includeDoi": true,
      "includeDate": true
    },
    "HEADER": {
      "text": "Downloaded from example.org",
      "includeDate": true
    }
  }
}
```

**Response:** `application/pdf` — the stamped PDF as a file attachment.

### GET /api/v1/stamp/demo-pdf/{pubId}/{jcode}

Generates a demo-stamped PDF using placeholder metadata for a saved configuration. Used by the frontend "Download Demo" button.

### GET /api/v1/configs

Returns all active (non-archived) stamping configurations.

### GET /api/v1/configs/{pubId}/{jcode}

Returns a single configuration by publisher ID and journal code.

### POST /api/v1/configs

Creates or updates a stamping configuration. Saved to `configs/config_{pubId}_{jcode}.json`.

### DELETE /api/v1/configs/{pubId}/{jcode}

Soft-deletes a configuration by moving it to `archive_configs/`.

### PUT /api/v1/configs/{pubId}/{jcode}/restore

Restores an archived configuration back to `configs/`.

---

## Template System

Templates define the HTML layout for generated pages (`NEW_PAGE` position). They are rendered by `TemplateService` with placeholder substitution before being converted to PDF.

### Built-in Templates

| Template Name | Description |
|--------------|-------------|
| `journal_article` | Two-column layout with logo, date, title, authors, DOI |
| `genome_last_page` | Genome Research last-page layout with P<P table, Creative Commons license, ad banner |
| `default_metadata` | Centered layout with all metadata fields |
| `simple_header` | Centered title + subtitle + date |
| `custom_html` | Blank canvas — renders whatever HTML is in the `html` field |

### Placeholder Reference

| Placeholder | Replaced with |
|------------|---------------|
| `{{DATE}}` | Current date (e.g. `March 22, 2026`) |
| `{{LOGO}}` | Logo as base64 `<img>` tag |
| `{{ARTICLE_TITLE}}` | Article title from request |
| `{{AUTHORS}}` | Author names |
| `{{DOI}}` | DOI as a hyperlink |
| `{{COPYRIGHT}}` | Copyright text |
| `{{ISSN}}` | ISSN number |
| `{{ARTICLE_ID}}` | Article identifier |
| `{{USER}}` | Downloaded-by username |
| `{{AD_BANNER}}` | Ad banner HTML (fetched from BAM) |
| `{{LINK_URL}}` / `{{LINK_TEXT}}` | Hyperlink URL and display text |

### Font Handling

The service extracts the primary font from the input PDF and injects it into the HTML template via CSS `@font-face`. This ensures the stamped content visually matches the original document.

- Subset fonts (6-letter prefix like `FZQQVU+CMR17`) are detected and excluded from `@font-face` injection — they only contain partial glyph sets
- All HTML-to-PDF rendering uses `DefaultFontProvider(true, true, true)` to embed fonts for PAC compliance
- Fallback font stack: `Verdana, Arial, Helvetica, sans-serif`

---

## Configuration Storage

| State | Location |
|-------|----------|
| Active | `configs/config_{pubId}_{jcode}.json` |
| Archived | `archive_configs/config_{pubId}_{jcode}.json` |

---

## Frontend

### Routes

| Route | Component | Description |
|-------|-----------|-------------|
| `/` | `ConfigTable` | Lists all configurations with archive/restore/demo actions |
| `/config/new` | `ConfigForm` | Create a new publisher/journal configuration |
| `/config/:id` | `ConfigForm` | Edit an existing configuration |
| `/editor` | `HtmlEditorPage` | Split-pane HTML editor with live preview |

### Configuration Form

The form is tabbed across five stamp positions:

- **New Page** — generates a prepended/appended page using a template
- **Header** — overlay at the top of each page
- **Footer** — overlay at the bottom of each page
- **Left Margin** — rotated content on the left edge
- **Right Margin** — rotated content on the right edge

Each position supports: logo upload, custom HTML, plain text, metadata toggles, date, downloaded-by, link, and ad banner.

---

## Project Structure

```
├── src/main/java/com/stamping/
│   ├── StampingApplication.java
│   ├── controller/
│   │   ├── StampController.java          # All production endpoints
│   │   └── DevController.java            # DEV ONLY
│   ├── service/
│   │   ├── StampService.java             # Delegates to HtmlStamper
│   │   ├── TemplateService.java          # HTML template rendering
│   │   ├── MetadataFrontPageService.java # HTML→PDF conversion and PDF merging
│   │   ├── PdfFontExtractor.java         # PDF font extraction for compliance
│   │   ├── AdFetchService.java           # External ad JSON fetching
│   │   ├── AdStampService.java           # Ad HTML processing
│   │   ├── DemoStampService.java         # Demo PDF generation
│   │   ├── DemoConfigGeneratorService.java  # DEV ONLY
│   │   └── stamper/
│   │       ├── Stamper.java              # Strategy interface
│   │       └── HtmlStamper.java          # HTML→PDF overlay stamper
│   ├── model/
│   │   ├── StampRequest.java
│   │   ├── StampResponse.java
│   │   ├── StampType.java                # HTML
│   │   ├── StampPosition.java            # 9 positions + NEW_PAGE
│   │   ├── DynamicStampRequest.java      # Position config model
│   │   ├── JournalMetadataRequest.java   # journal-metadata endpoint request
│   │   └── ad/                           # Ad response models
│   └── exception/
│       ├── StampingException.java
│       └── GlobalExceptionHandler.java
├── src/main/resources/
│   └── application.yml
├── frontend-react/                       # React source (not deployed)
│   └── src/
│       ├── components/
│       ├── context/
│       ├── models/templates.js
│       └── data/                         # publishers.json (bundled at build time)
├── configs/                              # Active configuration JSON files
├── templates/                            # Template JSON definitions
└── test_requests/                        # Sample API request payloads
```

---

## Dev Utilities

These exist for local development and testing.

### Auto-generated test configs

Every time a configuration is saved via `POST /api/v1/configs`, the backend generates a `JournalMetadataRequest` JSON file at `test_requests/journal_metadata_{pubId}_{jcode}.json` with dummy metadata.

### Manual trigger

```
POST /api/v1/dev/generate-test-config/{pubId}/{jcode}
```

### Sample requests

```bash
curl -X POST http://localhost:8080/api/v1/stamp/journal-metadata \
  -H "Content-Type: application/json" \
  -d @test_requests/journal_metadata_demoPub_demoJcode.json \
  --output stamped.pdf
```

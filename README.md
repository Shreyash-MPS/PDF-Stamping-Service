# PDF Stamping Service

A Spring Boot service for stamping PDFs with HTML overlays, metadata pages, and ad banners. Designed for publisher and journal workflows where article PDFs need branded content, institutional overlays, and article metadata applied at delivery time.

Integrates with external systems (e.g. Drupal) via a JSON REST API. Accepts PDFs by local file path or remote URL.

---

## Table of Contents

- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
- [Configuration Reference](#configuration-reference)
- [API Reference](#api-reference)
- [PDF Source Options](#pdf-source-options)
- [Template System](#template-system)
- [Stamp Positions](#stamp-positions)
- [Font Handling](#font-handling)
- [Configuration Storage](#configuration-storage)
- [Temp File Management](#temp-file-management)
- [Frontend](#frontend)
- [Project Structure](#project-structure)
- [Deployment](#deployment)
- [Dev Utilities](#dev-utilities)

---

## Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│  React Frontend  (Vite + Tailwind CSS)                           │
│                                                                  │
│  ConfigTable ──► ConfigForm ──► StampSectionPanel                │
│                                        │                         │
│               ConfigContext (state + REST calls to /api/v1)      │
└────────────────────────────┬─────────────────────────────────────┘
                             │  HTTP REST
┌────────────────────────────▼─────────────────────────────────────┐
│  Spring Boot Backend                                             │
│                                                                  │
│  StampController                                                 │
│       │                                                          │
│  StampOrchestrationService                                       │
│       │                                                          │
│  ┌────┴──────────────────────────────────────────────┐          │
│  │  PdfDownloadService   (URL → temp file)           │          │
│  │  PdfFontExtractor     (font extraction)           │          │
│  │  TemplateService      (HTML template rendering)   │          │
│  │  MetadataFrontPageService  (HTML→PDF + merge)     │          │
│  │  AdFetchService / AdStampService  (BAM ads)       │          │
│  └────┬──────────────────────────────────────────────┘          │
│       │                                                          │
│  ┌────▼──────────────────────────────────────────────┐          │
│  │  HtmlStamper  (iText 7 pdfHTML)                   │          │
│  │  HTML → PDF XObject → overlay onto source pages   │          │
│  └───────────────────────────────────────────────────┘          │
└──────────────────────────────────────────────────────────────────┘
```

### Request flow

1. External system (e.g. Drupal) sends `POST /api/v1/stamp/journal-metadata` with article metadata and either a `pdfUrl` or `pdfFilePath`
2. If `pdfUrl` is provided, `PdfDownloadService` downloads the PDF to `temp/` and sets it as the working file
3. `PdfFontExtractor` scans the first 3 pages to identify the primary embedded font
4. `TemplateService` renders the configured HTML template, injecting metadata and the extracted font
5. `MetadataFrontPageService` converts the HTML to a PDF page and prepends or appends it
6. Overlay positions (HEADER, FOOTER, LEFT_MARGIN, RIGHT_MARGIN) are stamped onto existing pages via `HtmlStamper`
7. Link annotations from the HTML are transferred to the output PDF with correct coordinates
8. The stamped PDF is returned as a binary response; the temp file is deleted immediately

---

## Tech Stack

**Backend**
| Dependency | Version | Purpose |
|---|---|---|
| Java | 17 | Runtime |
| Spring Boot | 3.2.5 | Web framework, DI, scheduling |
| iText 7 / html2pdf | 5.0.3 | PDF manipulation, HTML-to-PDF rendering |
| OWASP HTML Sanitizer | 20240325.1 | Sanitize user-provided HTML before stamping |
| Lombok | 1.18.40 | Boilerplate reduction |

**Frontend**
| Dependency | Version | Purpose |
|---|---|---|
| React | 19.2 | UI framework |
| React Router | 7.13 | Client-side routing |
| Vite | 7.3 | Build tool and dev server |
| Tailwind CSS | 4.2 | Utility-first styling |
| CodeMirror 6 | — | HTML editor with syntax highlighting |
| Lucide React | — | Icon library |

---

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.6+
- Node.js 18+ (frontend only)

### Run the backend

```bash
mvn clean install
mvn spring-boot:run
```

API available at `http://localhost:8080`.

### Run the frontend

```bash
cd frontend-react
npm install
npm run dev
```

Dev server at `http://localhost:5173`.

### Quick smoke test

```bash
curl -X POST http://localhost:8080/api/v1/stamp/journal-metadata \
  -H "Content-Type: application/json" \
  -d @test_requests/journal_metadata_demoPub_demoJcode.json \
  --output stamped.pdf
```

---

## Configuration Reference

All settings live under the `stamping` prefix in `application.yml`.

| Property | Default | Description |
|---|---|---|
| `temp-dir` | `temp` | Directory for downloaded and demo temp PDFs |
| `config-dir` | `configs` | Active publisher/journal stamping configs |
| `archive-dir` | `archive_configs` | Archived (soft-deleted) configs |
| `test-requests-dir` | `test_requests` | Auto-generated demo request JSON files (dev) |
| `allowed-pdf-base-path` | _(empty)_ | Restrict local PDF access to this directory. Leave blank to allow any path |

**Ads**

| Property | Default | Description |
|---|---|---|
| `ads.base-url` | `https://bam-ads-presenter.highwire.org/api/ads` | BAM ad presenter API base URL |
| `ads.section-path` | `xpdf` | Section path parameter for ad requests |
| `ads.connect-timeout` | `5000` | Connection timeout in ms |
| `ads.read-timeout` | `10000` | Read timeout in ms |

**PDF Download**

| Property | Default | Description |
|---|---|---|
| `pdf-download.connect-timeout` | `5000` | Connection timeout in ms for remote PDF downloads |
| `pdf-download.read-timeout` | `30000` | Read timeout in ms for remote PDF downloads |
| `pdf-download.max-file-size` | `52428800` | Max allowed PDF size in bytes (50 MB) |

**CORS**

| Property | Default | Description |
|---|---|---|
| `cors.allowed-origins` | `http://localhost:5173, http://localhost:3000` | Allowed frontend origins. Override via `STAMPING_CORS_ORIGINS` env var |

**Environment variables**

| Variable | Maps to |
|---|---|
| `SPRING_PROFILES_ACTIVE` | Spring profile (`dev` by default) |
| `STAMPING_ALLOWED_PDF_PATH` | `allowed-pdf-base-path` |
| `STAMPING_CORS_ORIGINS` | `cors.allowed-origins` (comma-separated) |

---

## API Reference

**Base URL:** `/api/v1`

**Error response shape:**

```json
{
  "timestamp": "2026-03-24T15:20:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Either pdfUrl or pdfFilePath is required"
}
```

---

### POST `/api/v1/stamp/journal-metadata`

Stamps a PDF with metadata pages and overlays. The primary integration endpoint for external systems like Drupal.

**Content-Type:** `application/json`  
**Response:** `application/pdf` — stamped PDF as a file attachment

#### Request fields

| Field | Type | Required | Description |
|---|---|:---:|---|
| `pdfUrl` | string | one of | Remote URL of the source PDF (`http` or `https`) |
| `pdfFilePath` | string | one of | Absolute local path to the source PDF |
| `outputPath` | string | no | If provided, the stamped PDF is also saved to this path |
| `publisherId` | string | yes | Publisher identifier — must match a saved config |
| `jcode` | string | yes | Journal code — must match a saved config |
| `env` | string | no | Set to `demo` to auto-fill any blank metadata fields with placeholders |
| `articleTitle` | string | no | Article title |
| `authors` | string | no | Author names |
| `doiValue` | string | no | DOI value, e.g. `10.3174/ajnr.A8959` |
| `articleCopyright` | string | no | Copyright line |
| `articleIssn` | string | no | ISSN |
| `articleId` | string | no | Article identifier |
| `downloadedBy` | string | no | Username of the downloading user |
| `positions` | object | yes | Map of position key → configuration object (see below) |

> Provide either `pdfUrl` or `pdfFilePath`. If both are present, `pdfUrl` takes precedence.

#### Position configuration object

| Field | Type | Description |
|---|---|---|
| `templateName` | string | Template for `NEW_PAGE` (e.g. `journal_article`, `default_metadata`) |
| `pagePosition` | string | `front` or `back` — where to insert the new page (NEW_PAGE only) |
| `includeArticleTitle` | boolean | Render article title |
| `includeAuthors` | boolean | Render authors |
| `includeDoi` | boolean | Render DOI as a hyperlink |
| `includeDate` | boolean | Render current date |
| `includeCopyright` | boolean | Render copyright |
| `includeIssn` | boolean | Render ISSN |
| `includeArticleId` | boolean | Render article ID |
| `includeCurrentUser` | boolean | Render downloaded-by username |
| `logo` | string | Base64-encoded logo image (raw base64, no data URI prefix) |
| `logoMimeType` | string | MIME type of logo, e.g. `image/png`, `image/svg+xml` |
| `text` | string | Plain text to stamp |
| `html` | string | Raw HTML to stamp (sanitized via OWASP before rendering) |
| `linkUrl` | string | Hyperlink URL |
| `linkText` | string | Hyperlink display text |
| `adsEnabled` | boolean | Fetch and inject ads from BAM at stamp time |
| `legacyDomain` | string | Domain for resolving relative ad image paths (e.g. `hwmaint.genome.cshlp.org`) |

#### Example — local file path

```json
{
  "pdfFilePath": "/var/data/articles/input.pdf",
  "publisherId": "ASNR",
  "jcode": "neuro",
  "articleTitle": "Imaging Biomarkers in Neurological Disease",
  "authors": "Smith J, Doe A, Lee K",
  "doiValue": "10.3174/ajnr.A8959",
  "articleCopyright": "© 2026 ASNR. All rights reserved.",
  "articleIssn": "1936-959X",
  "positions": {
    "NEW_PAGE": {
      "templateName": "journal_article",
      "pagePosition": "front",
      "includeArticleTitle": true,
      "includeAuthors": true,
      "includeDoi": true,
      "includeDate": true
    },
    "HEADER": {
      "text": "Downloaded from ajnr.org",
      "includeDate": true
    },
    "FOOTER": {
      "includeCopyright": true
    }
  }
}
```

#### Example — remote URL (Drupal integration)

```json
{
  "pdfUrl": "https://cdn.example.org/articles/2026/article-123.pdf",
  "publisherId": "ASNR",
  "jcode": "neuro",
  "articleTitle": "Imaging Biomarkers in Neurological Disease",
  "authors": "Smith J, Doe A, Lee K",
  "doiValue": "10.3174/ajnr.A8959",
  "positions": {
    "NEW_PAGE": {
      "templateName": "journal_article",
      "pagePosition": "front",
      "includeArticleTitle": true,
      "includeAuthors": true,
      "includeDoi": true,
      "includeDate": true
    }
  }
}
```

---

### GET `/api/v1/stamp/demo-pdf/{pubId}/{jcode}`

Generates a demo-stamped PDF using placeholder metadata for a saved configuration. Used by the frontend "Download Demo" button.

**Response:** `application/pdf`

---

### GET `/api/v1/configs`

Returns all active (non-archived) stamping configurations as a JSON array.

---

### GET `/api/v1/configs/{pubId}/{jcode}`

Returns a single configuration by publisher ID and journal code.

**404** if no config exists for the given identifiers.

---

### POST `/api/v1/configs`

Creates or updates a stamping configuration. Saved to `configs/config_{pubId}_{jcode}.json`.

**Content-Type:** `application/json`

---

### DELETE `/api/v1/configs/{pubId}/{jcode}`

Soft-deletes a configuration by moving it to `archive_configs/`. Recoverable via the restore endpoint.

---

### PUT `/api/v1/configs/{pubId}/{jcode}/restore`

Restores an archived configuration back to `configs/`.

---

## PDF Source Options

The service accepts the source PDF in two ways:

| Method | Field | When to use |
|---|---|---|
| Remote URL | `pdfUrl` | PDF is hosted on a web server or cloud storage (Drupal, S3, CDN) |
| Local path | `pdfFilePath` | PDF is accessible on the server's filesystem |

When `pdfUrl` is provided:
- The file is downloaded to `temp/` using JDK 17's `HttpClient`
- Streamed to disk (not buffered in memory) — safe for large files
- Deleted immediately after stamping completes (or on error)
- A scheduled cleanup job runs every 15 minutes to remove any files older than 30 minutes

Constraints on `pdfUrl`:
- Must use `http` or `https` scheme
- No path traversal (`..`) allowed
- Max file size enforced during streaming (default 50 MB, configurable)

---

## Template System

Templates define the HTML layout for `NEW_PAGE` positions. `TemplateService` renders them with placeholder substitution and font injection before converting to PDF.

### Built-in templates

| Name | Description |
|---|---|
| `journal_article` | Two-column layout — logo + date on left, title + authors + DOI on right |
| `genome_last_page` | Genome Research last-page layout with P&lt;P table, Creative Commons license, ad banner |
| `default_metadata` | Centered layout with all metadata fields |
| `simple_header` | Minimal centered layout — title, authors, DOI, date |
| `custom_html` | Blank canvas — renders whatever is in the `html` field |

### Placeholder reference

| Placeholder | Replaced with |
|---|---|
| `{{DATE}}` | Current date, e.g. `March 24, 2026` |
| `{{LOGO}}` | Logo as an `<img>` tag with base64 data URI |
| `{{ARTICLE_TITLE}}` | Article title from request |
| `{{AUTHORS}}` | Author names from request |
| `{{DOI}}` | DOI as a clickable hyperlink |
| `{{COPYRIGHT}}` | Copyright text from request |
| `{{ISSN}}` | ISSN from request |
| `{{ARTICLE_ID}}` | Article ID from request |
| `{{USER}}` | Downloaded-by username from request |
| `{{AD_BANNER}}` | Ad banner HTML fetched from BAM |
| `{{LINK_URL}}` | Hyperlink URL from config |
| `{{LINK_TEXT}}` | Hyperlink display text from config |

Unused placeholders are removed from the output. Elements with matching CSS class names (e.g. `article-title-block`, `doi-block`) are also stripped when their corresponding field is disabled.

---

## Stamp Positions

| Position key | Description |
|---|---|
| `NEW_PAGE` | Generates a full HTML page and prepends or appends it to the PDF |
| `HEADER` | Overlay at the top of every original page |
| `FOOTER` | Overlay at the bottom of every original page |
| `LEFT_MARGIN` | Rotated 90° overlay on the left edge |
| `RIGHT_MARGIN` | Rotated 270° overlay on the right edge |

Overlay positions (HEADER, FOOTER, LEFT_MARGIN, RIGHT_MARGIN) are stamped only onto the original pages — not onto any pages added by `NEW_PAGE`.

---

## Font Handling

The service extracts the primary font from the input PDF and injects it into HTML templates to maintain visual consistency with the original document.

**Extraction logic:**
- Scans the first 3 pages of the PDF
- Skips the 14 standard PDF fonts (Helvetica, Times-Roman, etc.)
- Prefers fully embedded fonts over subset fonts
- Falls back to font-family name only if the font is a subset

**Injection logic:**
- If the font is fully embedded (not a subset), it is injected via CSS `@font-face` using a base64 data URI
- Subset fonts (identified by a 6-letter uppercase prefix, e.g. `FZQQVU+CMR17`) are excluded from `@font-face` injection — they only contain glyphs used in the original document and cannot render arbitrary new text
- Fallback CSS font stack: `Verdana, Arial, Helvetica, sans-serif`

All HTML-to-PDF rendering uses `DefaultFontProvider(true, true, true)` to embed fonts in the output for PDF/UA and PAC compliance.

---

## Configuration Storage

Configurations are stored as JSON files on disk.

| State | Location | Naming |
|---|---|---|
| Active | `configs/` | `config_{pubId}_{jcode}.json` |
| Archived | `archive_configs/` | `config_{pubId}_{jcode}.json` |

Archiving (DELETE endpoint) moves the file rather than deleting it. Restore (PUT endpoint) moves it back.

---

## Temp File Management

Downloaded PDFs are written to the `temp/` directory at the project root.

**Cleanup layers:**

| Layer | When | Mechanism |
|---|---|---|
| Immediate | After each request | `finally` block in `StampOrchestrationService` |
| Scheduled | Every 15 minutes | `PdfDownloadService.cleanupStaleTempFiles()` — removes files older than 30 minutes |

The `temp/` directory is tracked in git via `.gitkeep`. All `*.pdf` files inside it are gitignored.

---

## Frontend

A React admin UI for managing publisher/journal stamping configurations.

### Routes

| Route | Component | Description |
|---|---|---|
| `/` | `ConfigTable` | Lists all configurations with archive, restore, and demo actions |
| `/config/new` | `ConfigForm` | Create a new publisher/journal configuration |
| `/config/:id` | `ConfigForm` | Edit an existing configuration |
| `/editor` | `HtmlEditorPage` | Split-pane HTML editor with live preview |

### Configuration form

The form is tabbed across five stamp positions: New Page, Header, Footer, Left Margin, Right Margin.

Each position supports: logo upload, custom HTML, plain text, metadata field toggles, date, downloaded-by, hyperlink, and ad banner.

---

## Project Structure

```
├── src/main/java/com/stamping/
│   ├── StampingApplication.java              # Entry point, enables scheduling
│   ├── config/
│   │   ├── StampingProperties.java           # Typed config properties (@ConfigurationProperties)
│   │   └── WebConfig.java                    # CORS configuration
│   ├── controller/
│   │   ├── StampController.java              # All production endpoints
│   │   └── DevController.java                # Dev-only endpoints (profile: dev)
│   ├── service/
│   │   ├── StampOrchestrationService.java    # Full stamping pipeline orchestration
│   │   ├── StampService.java                 # Delegates to HtmlStamper
│   │   ├── PdfDownloadService.java           # Remote PDF download + scheduled cleanup
│   │   ├── TemplateService.java              # HTML template rendering with placeholder substitution
│   │   ├── MetadataFrontPageService.java     # HTML→PDF conversion and PDF merge/prepend/append
│   │   ├── PdfFontExtractor.java             # Embedded font extraction from PDF
│   │   ├── InputSanitizer.java               # File path, URL, identifier, and HTML validation
│   │   ├── AdFetchService.java               # BAM ad API client
│   │   ├── AdStampService.java               # Ad HTML processing and URL rewriting
│   │   ├── DemoStampService.java             # Demo PDF generation (profile: dev)
│   │   ├── DemoConfigGeneratorService.java   # Auto-generates test request JSON (profile: dev)
│   │   └── stamper/
│   │       ├── Stamper.java                  # Strategy interface
│   │       └── HtmlStamper.java              # HTML→PDF XObject overlay with annotation transfer
│   ├── model/
│   │   ├── JournalMetadataRequest.java       # Primary stamping request model
│   │   ├── DynamicStampRequest.java          # Per-position configuration model
│   │   ├── StampRequest.java                 # Low-level stamp parameters (position, rotation, pages)
│   │   ├── StampResponse.java                # Generic API response model
│   │   ├── StampType.java                    # Enum: HTML
│   │   ├── StampPosition.java                # Enum: 9 positions + NEW_PAGE
│   │   └── ad/                               # BAM ad API response models
│   └── exception/
│       ├── StampingException.java            # Domain exception (maps to HTTP 400)
│       └── GlobalExceptionHandler.java       # @RestControllerAdvice error handler
├── src/main/resources/
│   └── application.yml                       # All runtime configuration
├── frontend-react/                           # React admin UI (not deployed as-is)
│   └── src/
│       ├── components/                       # UI components
│       ├── context/                          # React context (config state, template state)
│       ├── models/templates.js               # Frontend template definitions
│       └── data/                             # Static data bundled at build time
├── configs/                                  # Active stamping configuration JSON files
├── templates/                                # External template JSON definitions
├── temp/                                     # Temp directory for downloaded PDFs (gitignored)
└── test_requests/                            # Sample API request payloads for testing
```

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

### Production artifacts

| Artifact | Description |
|---|---|
| `target/pdf-stamping-service-1.0.0.jar` | Self-contained fat jar with embedded Tomcat |
| `frontend-react/dist/` | Compiled frontend static files |
| `configs/` | Publisher/journal stamping configurations |
| `templates/` | Template JSON definitions |
| `temp/` | Temp directory (must exist and be writable) |

Source folders (`src/`, `frontend-react/src/`, `node_modules/`, `pom.xml`) are not needed in production.

### Run on Linux

```bash
java -jar pdf-stamping-service-1.0.0.jar
```

Ensure:
- Java 17+ is installed
- The working directory is writable (for `configs/`, `temp/`, `archive_configs/`)
- `STAMPING_CORS_ORIGINS` is set to your frontend origin
- `SPRING_PROFILES_ACTIVE` is set to `prod` (disables dev-only endpoints and services)

### Nginx reverse proxy

```nginx
server {
    listen 80;

    # Serve compiled frontend
    location / {
        root /path/to/frontend-react/dist;
        try_files $uri $uri/ /index.html;
    }

    # Proxy API requests to Spring Boot
    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_read_timeout 120s;
    }
}
```

With a reverse proxy, frontend and backend share the same origin — no CORS configuration needed on the backend.

---

## Dev Utilities

These are only active when `spring.profiles.active=dev`.

### Auto-generated test request files

Every time a config is saved via `POST /api/v1/configs`, the backend writes a ready-to-use `JournalMetadataRequest` JSON to `test_requests/journal_metadata_{pubId}_{jcode}.json` with placeholder metadata.

### Manual trigger

```
POST /api/v1/dev/generate-test-config/{pubId}/{jcode}
```

Regenerates the test request file for an existing saved config.

### Health check

```
GET /actuator/health
```

### Sample curl requests

```bash
# Stamp using a local file path
curl -X POST http://localhost:8080/api/v1/stamp/journal-metadata \
  -H "Content-Type: application/json" \
  -d @test_requests/journal_metadata_demoPub_demoJcode.json \
  --output stamped.pdf

# Stamp using a remote URL
curl -X POST http://localhost:8080/api/v1/stamp/journal-metadata \
  -H "Content-Type: application/json" \
  -d '{
    "pdfUrl": "https://example.org/article.pdf",
    "publisherId": "demoPub",
    "jcode": "demoJcode",
    "env": "demo"
  }' \
  --output stamped.pdf

# Download a demo-stamped PDF
curl http://localhost:8080/api/v1/stamp/demo-pdf/demoPub/demoJcode --output demo.pdf

# List all configs
curl http://localhost:8080/api/v1/configs
```

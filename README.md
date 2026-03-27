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
│  │  DemoStampService     (demo/sample PDF generation)│          │
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
| `STAMPING_ALLOWED_PDF_PATH` | `allowed-pdf-base-path` |
| `STAMPING_CORS_ORIGINS` | `cors.allowed-origins` (comma-separated) |

---

## API Reference

**Base URL:** `/api/v1`

See [API_DOCUMENTATION.md](API_DOCUMENTATION.md) for full endpoint details, request/response schemas, and examples.

**Error response shape:**

```json
{
  "timestamp": "2026-03-27T15:20:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Either pdfUrl or pdfFilePath is required"
}
```

### Endpoints

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/stamp/journal-metadata` | Stamp a PDF with metadata, ads, cover pages |
| GET | `/api/v1/stamp/demo-pdf/{pubId}/{jcode}` | Download a demo-stamped PDF |
| GET | `/api/v1/configs` | List all saved configurations |
| GET | `/api/v1/configs/{pubId}/{jcode}` | Get a single configuration |
| POST | `/api/v1/configs` | Save (create/update) a configuration |
| DELETE | `/api/v1/configs/{pubId}/{jcode}` | Archive (soft-delete) a configuration |
| PUT | `/api/v1/configs/{pubId}/{jcode}/restore` | Restore an archived configuration |

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

---

## PDF Source Options

The service accepts the source PDF in two ways:

| Method | Field | When to use |
|---|---|---|
| Remote URL | `pdfUrl` | PDF is hosted on a web server or cloud storage (Drupal, S3, CDN) |
| Local path | `pdfFilePath` | PDF is accessible on the server's filesystem |

When `pdfUrl` is provided:
- The file is downloaded to `temp/` using JDK 17's `HttpClient`
- Streamed to disk in 8 KB chunks (not buffered in memory) — safe for large files
- Deleted immediately after stamping completes (or on error)
- A scheduled cleanup job runs every 15 minutes to remove any orphaned files older than 30 minutes

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
| `genome_last_page` | Genome Research last-page layout with P&P table, Creative Commons license, ad banner |
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

All HTML-to-PDF rendering uses `DefaultFontProvider(true, true, true)` to embed fonts in the output.

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
| `/` | `ConfigTable` | Lists all configurations with archive, restore, and demo download actions |
| `/config/new` | `ConfigForm` | Create a new publisher/journal configuration |
| `/config/:id` | `ConfigForm` | Edit an existing configuration |
| `/editor` | `HtmlEditorPage` | Split-pane HTML editor with live preview |

### Configuration form

The form is tabbed across five stamp positions: New Page, Header, Footer, Left Margin, Right Margin.

Each position supports: logo upload, custom HTML, plain text, metadata field toggles, date, downloaded-by, hyperlink, and ad banner.

### UI features

- Skeleton loading states on the config table
- Illustrated empty state with call-to-action
- Breadcrumb navigation on the config form
- Sticky bottom action bar (Cancel, Preview, Sample PDF, Save)
- Confirmation dialog for delete actions with undo toast
- Custom tooltips on action buttons
- Tab scroll fade indicator on narrow screens
- ARIA labels and roles for accessibility

---

## Project Structure

```
├── src/main/java/com/stamping/
│   ├── StampingApplication.java              # Entry point, enables scheduling
│   ├── config/
│   │   ├── StampingProperties.java           # Typed config properties (@ConfigurationProperties)
│   │   └── WebConfig.java                    # CORS configuration
│   ├── controller/
│   │   └── StampController.java              # All API endpoints
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
│   │   ├── DemoStampService.java             # Demo/sample PDF generation from saved configs
│   │   ├── DemoConfigGeneratorService.java   # Translates frontend config to stamping positions
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
├── frontend-react/                           # React admin UI
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

### Run

```bash
java -jar pdf-stamping-service-1.0.0.jar
```

Ensure:
- Java 17+ is installed
- The working directory is writable (for `configs/`, `temp/`, `archive_configs/`)
- `STAMPING_CORS_ORIGINS` is set to your frontend origin(s)

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

### Health check

```
GET /actuator/health
```

Returns service health status. Exposed via Spring Boot Actuator.

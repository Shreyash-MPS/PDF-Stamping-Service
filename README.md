# PDF Stamping Service

A Spring Boot application for stamping PDFs with Text, Images, and HTML content.

## Features

- **Text Stamping**: Add text stamps with customizable font size, color, opacity, and rotation. Supports automatic text wrapping.
- **Image Stamping**: Overlay images (e.g., logos, signatures) with scaling and opacity control.
- **HTML Stamping**: Render HTML/CSS content onto PDFs. Supports rotated link annotations.
- **Flexible Positioning**: predefined positions (TOP_LEFT, CENTER, etc.) or custom X/Y coordinates.
- **Page Selection**: Stamp all pages, specific pages, or page ranges.

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.6+

### Build

```bash
mvn clean install
```

### Run

```bash
mvn spring-boot:run
```

The service will start on port `8080`.

## API Usage

### Endpoint

`POST /api/v1/stamp`

### Request Body (JSON)

#### Text Stamp Example

```json
{
  "inputFilePath": "C:/path/to/input.pdf",
  "outputFilePath": "C:/path/to/output.pdf",
  "stampType": "TEXT",
  "text": "CONFIDENTIAL",
  "position": "TOP_RIGHT",
  "fontSize": 24,
  "fontColor": "#FF0000",
  "opacity": 0.5,
  "rotation": 45,
  "stampWidth": 200,
  "pages": "ALL"
}
```

#### HTML Stamp Example

```json
{
  "inputFilePath": "C:/path/to/input.pdf",
  "outputFilePath": "C:/path/to/output.pdf",
  "stampFilePath": "C:/path/to/stamp.html",
  "stampType": "HTML",
  "position": "BOTTOM_RIGHT",
  "scale": 0.8,
  "opacity": 1.0,
  "rotation": 90,
  "pages": "1-3"
}
```

## Configuration

See `src/main/resources/application.properties` for configuration options.

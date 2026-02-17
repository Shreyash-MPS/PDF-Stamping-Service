# Postman Testing Guide

## 1. JSON-based File Path Stamping (Preferred for Local Testing)

This method creates a request that tells the server to read files from the local disk.

- **URL**: `http://localhost:8080/api/v1/stamp/file-path`
- **Method**: `POST`
- **Header**: `Content-Type: application/json`
- **Body**: `raw` (JSON)

### Example Payload (PDF Stamp)

```json
{
    "inputFilePath": "C:/path/to/source.pdf",
    "outputFilePath": "C:/path/to/output.pdf",
    "stampFilePath": "C:/path/to/stamp.pdf",
    "stampType": "PDF",
    "position": "CENTER",
    "scale": 0.5,
    "opacity": 0.8,
    "rotation": 45,
    "pages": "ALL"
}
```

### Example Payload (Text Stamp)

```json
{
    "inputFilePath": "C:/path/to/source.pdf",
    "outputFilePath": "C:/path/to/output.pdf",
    "stampType": "TEXT",
    "text": "CONFIDENTIAL",
    "position": "TOP_RIGHT",
    "fontSize": 24,
    "fontColor": "#FF0000",
    "stampWidth": 200
}
```

## 2. Multipart File Upload

This method uploads the files directly to the server, useful if the client and server are on different machines.

- **URL**: `http://localhost:8080/api/v1/stamp`
- **Method**: `POST`
- **Body**: `form-data`

### Key-Value Pairs

| Key | Type | Value | Description |
| :--- | :--- | :--- | :--- |
| `file` | File | [Select PDF file] | The source PDF to stamp |
| `stamp` | File | [Select Stamp file] | The image, HTML, or PDF file to use as a stamp (Required for IMAGE, HTML, PDF types) |
| `stampType` | Text | `PDF` | `TEXT`, `IMAGE`, `HTML`, or `PDF` |
| `position` | Text | `CENTER` | `TOP_LEFT`, `BOTTOM_RIGHT`, `CUSTOM`, etc. |
| `rotation` | Text | `45` | Rotation in degrees |
| `opacity` | Text | `0.5` | Opacity (0.0 - 1.0) |
| `scale` | Text | `0.5` | Scale factor |

### Response
The response will be the stamped PDF file stream. You can save it as a file in Postman ("Save Response to file").

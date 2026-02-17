# Using Custom Position for Stamps

## How to Add Custom Location

Use the `CUSTOM` position and specify `x` and `y` coordinates:

### JSON API Example

```json
{
  "inputFilePath": "C:/path/to/input.pdf",
  "outputFilePath": "C:/path/to/output.pdf",
  "stampType": "TEXT",
  "text": "My Custom Stamp",
  "position": "CUSTOM",
  "x": 150,
  "y": 300,
  "fontSize": 20
}
```

### Multipart API Example (curl)

```bash
curl -X POST http://localhost:8080/api/v1/stamp \
  -F "file=@sample.pdf" \
  -F "stampType=TEXT" \
  -F "text=Custom Location" \
  -F "position=CUSTOM" \
  -F "x=150" \
  -F "y=300" \
  -o output.pdf
```

## Understanding Coordinates

PDF coordinates start from the **bottom-left corner**:

```
(0, 792) ─────────────────── (612, 792)  ← Top of page
    │                              │
    │                              │
    │         PDF Page             │
    │      (Letter size:           │
    │       612 x 792 pts)         │
    │                              │
(0, 0) ───────────────────── (612, 0)    ← Bottom of page
 └─ Origin (0,0)
```

### Common Page Sizes (in points)

- **Letter**: 612 x 792 pts
- **A4**: 595 x 842 pts
- **Legal**: 612 x 1008 pts

### Example Positions

```json
// Bottom-left corner (with 50pt margin)
{ "position": "CUSTOM", "x": 50, "y": 50 }

// Top-left corner (Letter size, 50pt margin)
{ "position": "CUSTOM", "x": 50, "y": 742 }

// Center of Letter page
{ "position": "CUSTOM", "x": 306, "y": 396 }

// Custom position
{ "position": "CUSTOM", "x": 200, "y": 400 }
```

## Test File

I've created a test file: [custom_position.json](file:///c:/Users/Shreyash.Dehury/Desktop/stamping/test_requests/custom_position.json)

**Test it:**
```bash
curl -X POST http://localhost:8080/api/v1/stamp/file-path \
  -H "Content-Type: application/json" \
  -d @test_requests/custom_position.json
```

## Tips

1. **Start from bottom-left**: Remember Y increases upward
2. **Use predefined positions first**: Try `TOP_LEFT`, `CENTER`, etc. to understand the layout
3. **Adjust incrementally**: Start with a position and adjust x/y by small amounts (10-20 points)
4. **Account for stamp size**: The x/y is the lower-left corner of your stamp

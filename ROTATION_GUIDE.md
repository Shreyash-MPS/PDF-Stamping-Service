# Rotating HTML in Right Margin

## Quick Example

```json
{
  "stampFilePath": "google_ad_simplified.html",
  "stampType": "HTML",
  "position": "CUSTOM",
  "x": 520,
  "y": 300,
  "scale": 0.3,
  "rotation": 90
}
```

## Understanding Rotation + Position

### Letter Size Page (612 x 792 pts)
- **Right margin starts**: around x = 520-550
- **Rotation**: Rotates around the center of the HTML content
- **Common angles**: 90° (vertical), -90° (vertical flipped), 45° (diagonal)

### Positioning Tips for Right Margin

```json
// Vertical stamp on right side (90° rotation)
{
  "position": "CUSTOM",
  "x": 520,        // Near right edge
  "y": 300,        // Middle height
  "rotation": 90,  // Rotate clockwise
  "scale": 0.3     // Make smaller to fit margin
}

// Vertical stamp, different height
{
  "position": "CUSTOM",
  "x": 530,
  "y": 100,        // Lower on page
  "rotation": 90,
  "scale": 0.25
}

// Counter-clockwise rotation
{
  "position": "CUSTOM",
  "x": 520,
  "y": 400,
  "rotation": -90,  // Rotate counter-clockwise
  "scale": 0.3
}
```

## Test File

Created: [rotated_right_margin.json](file:///c:/Users/Shreyash.Dehury/Desktop/stamping/test_requests/rotated_right_margin.json)

**Test it:**
```bash
curl -X POST http://localhost:8080/api/v1/stamp/file-path \
  -H "Content-Type: application/json" \
  -d @test_requests/rotated_right_margin.json
```

## Adjusting the Position

If the stamp doesn't appear where you want:

1. **Too far right?** Decrease `x` (try 500, 480, etc.)
2. **Too far left?** Increase `x` (try 540, 560, etc.)
3. **Too high/low?** Adjust `y` (396 = middle of Letter page)
4. **Too big?** Decrease `scale` (try 0.2, 0.25, etc.)
5. **Wrong rotation?** Try `-90` instead of `90`

## Common Margin Positions (Letter size)

```json
// Right margin, top
{ "x": 520, "y": 650, "rotation": 90 }

// Right margin, middle
{ "x": 520, "y": 396, "rotation": 90 }

// Right margin, bottom
{ "x": 520, "y": 150, "rotation": 90 }
```

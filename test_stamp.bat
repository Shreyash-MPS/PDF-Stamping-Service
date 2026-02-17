@echo off
REM Quick test script for PDF Stamping Service
REM Make sure the service is running on http://localhost:8080

echo ========================================
echo PDF Stamping Service - Quick Test
echo ========================================
echo.

REM Check if sample.pdf exists
if not exist "sample.pdf" (
    echo ERROR: sample.pdf not found!
    echo Please place a PDF file named 'sample.pdf' in this directory.
    echo.
    pause
    exit /b 1
)

echo Test 1: TEXT Stamp - Adding "CONFIDENTIAL" watermark
echo ----------------------------------------
curl -X POST http://localhost:8080/api/v1/stamp ^
  -F "file=@sample.pdf" ^
  -F "stampType=TEXT" ^
  -F "text=CONFIDENTIAL" ^
  -F "position=TOP_RIGHT" ^
  -F "fontSize=24" ^
  -F "fontColor=#FF0000" ^
  -F "opacity=0.5" ^
  -o output_text.pdf

if %ERRORLEVEL% EQU 0 (
    echo SUCCESS: output_text.pdf created!
) else (
    echo FAILED: Could not create text stamp
)
echo.

echo Test 2: TEXT Stamp - Diagonal "DRAFT" watermark
echo ----------------------------------------
curl -X POST http://localhost:8080/api/v1/stamp ^
  -F "file=@sample.pdf" ^
  -F "stampType=TEXT" ^
  -F "text=DRAFT" ^
  -F "position=CENTER" ^
  -F "fontSize=72" ^
  -F "fontColor=#CCCCCC" ^
  -F "opacity=0.3" ^
  -F "rotation=45" ^
  -o output_draft.pdf

if %ERRORLEVEL% EQU 0 (
    echo SUCCESS: output_draft.pdf created!
) else (
    echo FAILED: Could not create draft stamp
)
echo.

echo Test 3: HTML Stamp - Verification badge
echo ----------------------------------------
if exist "sample_stamp.html" (
    curl -X POST http://localhost:8080/api/v1/stamp ^
      -F "file=@sample.pdf" ^
      -F "stamp=@sample_stamp.html" ^
      -F "stampType=HTML" ^
      -F "position=BOTTOM_RIGHT" ^
      -F "scale=0.8" ^
      -o output_html.pdf
    
    if %ERRORLEVEL% EQU 0 (
        echo SUCCESS: output_html.pdf created!
    ) else (
        echo FAILED: Could not create HTML stamp
    )
) else (
    echo SKIPPED: sample_stamp.html not found
)
echo.

echo ========================================
echo Testing Complete!
echo ========================================
echo Check the following output files:
echo   - output_text.pdf (red CONFIDENTIAL stamp)
echo   - output_draft.pdf (diagonal DRAFT watermark)
if exist "output_html.pdf" echo   - output_html.pdf (HTML verification badge)
echo.
pause

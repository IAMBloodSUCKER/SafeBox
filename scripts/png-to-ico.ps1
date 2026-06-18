param(
    [string]$PngPath = "src/main/resources/images/icon.png",
    [string]$IcoPath = "src/main/resources/images/icon.ico"
)

Add-Type -AssemblyName System.Drawing

$sizes = @(16, 32, 48, 64, 128, 256)
$source = [System.Drawing.Image]::FromFile((Resolve-Path $PngPath))

$iconStream = New-Object System.IO.MemoryStream
$writer = New-Object System.IO.BinaryWriter($iconStream)

# ICO header
$writer.Write([UInt16]0)      # reserved
$writer.Write([UInt16]1)      # type: icon
$writer.Write([UInt16]$sizes.Count)

$imageDataList = New-Object System.Collections.Generic.List[byte[]]
$offset = 6 + (16 * $sizes.Count)

foreach ($size in $sizes) {
    $bitmap = New-Object System.Drawing.Bitmap($size, $size)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $graphics.DrawImage($source, 0, 0, $size, $size)
    $graphics.Dispose()

    $pngStream = New-Object System.IO.MemoryStream
    $bitmap.Save($pngStream, [System.Drawing.Imaging.ImageFormat]::Png)
    $pngBytes = $pngStream.ToArray()
    $pngStream.Dispose()
    $bitmap.Dispose()

    $imageDataList.Add($pngBytes)

    $widthByte = if ($size -eq 256) { [byte]0 } else { [byte]$size }
    $heightByte = if ($size -eq 256) { [byte]0 } else { [byte]$size }
    $writer.Write($widthByte)
    $writer.Write($heightByte)
    $writer.Write([byte]0)              # color count
    $writer.Write([byte]0)              # reserved
    $writer.Write([UInt16]1)            # planes
    $writer.Write([UInt16]32)           # bit count
    $writer.Write([UInt32]$pngBytes.Length)
    $writer.Write([UInt32]$offset)
    $offset += $pngBytes.Length
}

foreach ($data in $imageDataList) {
    $writer.Write($data)
}

$writer.Flush()
$outPath = Join-Path (Get-Location) $IcoPath
[System.IO.File]::WriteAllBytes($outPath, $iconStream.ToArray())

$writer.Dispose()
$iconStream.Dispose()
$source.Dispose()

Write-Host "Created $IcoPath"

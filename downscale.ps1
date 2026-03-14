Add-Type -AssemblyName System.Drawing

$brain_dir = "C:\Users\Etema\.gemini\antigravity\brain\857cad15-2a9e-4e18-8d7e-c12fb8f8f00e"
$dest_dir = "d:\Mods\RagnarMMO\src\main\resources\assets\ragnarmmo\textures\gui\skills"

$mapping = @{
    "hunter_beast_bane" = "hunter_beast_bane.png"
    "hunter_blitz_beat" = "hunter_blitz_beat.png"
    "hunter_double_strafe" = "hunter_double_strafe.png"
    "hunter_freezing_trap" = "hunter_freezing_trap.png"
    "knight_bowling_bash" = "knight_bowling_bash.png"
    "knight_brandish_spear" = "knight_brandish_spear.png"
    "knight_pierce" = "knight_pierce.png"
    "knight_spear_mastery" = "knight_spear_mastery.png"
    "mage_cold_bolt" = "mage_cold_bolt.png"
    "mage_fire_ball" = "mage_fire_ball.png"
    "mage_fire_bolt" = "mage_fire_bolt.png"
    "mage_lightning_bolt" = "mage_lightning_bolt.png"
    "priest_blessing" = "priest_blessing.png"
    "priest_heal" = "priest_heal.png"
    "priest_magnificat" = "priest_magnificat.png"
}

foreach ($key in $mapping.Keys) {
    $files = Get-ChildItem -Path $brain_dir -Filter "$key`_*.png"
    if ($files.Count -gt 0) {
        $src = $files[0].FullName
        $dest = Join-Path $dest_dir $mapping[$key]
        
        $img = [System.Drawing.Image]::FromFile($src)
        $bmp = New-Object System.Drawing.Bitmap 64, 64
        $graph = [System.Drawing.Graphics]::FromImage($bmp)
        $graph.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $graph.DrawImage($img, 0, 0, 64, 64)
        
        $bmp.Save($dest, [System.Drawing.Imaging.ImageFormat]::Png)
        
        $graph.Dispose()
        $bmp.Dispose()
        $img.Dispose()
        
        Write-Host "Resized and Moved: $key"
    }
}

param([Parameter(Mandatory=$true)][string]$Phase,[Parameter(Mandatory=$true)][string]$Name)
$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$frameRoot=Join-Path $projectRoot ("demo-gifs/"+$Phase+"-frames")
$result=Get-Content -LiteralPath (Join-Path $frameRoot 'result.json') -Raw | ConvertFrom-Json
if($result.status -ne 'passed'){throw 'Cannot render a successful demo from a failed acceptance run'}
$frameNumber=1
foreach($frame in $result.frames){
    $target=Join-Path $frameRoot ('normalized-'+$frameNumber.ToString('00')+'.png')
    ffmpeg -hide_banner -loglevel error -y -i $frame -vf 'scale=1100:860:force_original_aspect_ratio=decrease,pad=1100:860:(ow-iw)/2:(oh-ih)/2:color=white' $target
    if($LASTEXITCODE -ne 0){throw 'Frame normalization failed'}
    $frameNumber++
}
$output=Join-Path $projectRoot ("demo-gifs/"+$Name+".gif")
ffmpeg -hide_banner -loglevel error -y -framerate 1/2 -start_number 1 -i "$frameRoot/normalized-%02d.png" -frames:v $result.frames.Count -vf 'split[a][b];[a]palettegen[p];[b][p]paletteuse' -loop 0 $output
if($LASTEXITCODE -ne 0){throw 'GIF rendering failed'}
ffprobe -v error -select_streams v:0 -show_entries stream=nb_frames,duration -of json $output

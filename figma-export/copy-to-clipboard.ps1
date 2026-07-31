# Figma html.to.design 에 붙여넣기 쉽도록 파일 내용을 클립보드로 복사한다.
#
# 사용법 (PowerShell 에서 figma-export 폴더로 이동 후):
#   .\copy-to-clipboard.ps1            → 파일 목록 보기
#   .\copy-to-clipboard.ps1 9          → 09-map.html 을 클립보드로
#   .\copy-to-clipboard.ps1 next       → 다음 순번 파일을 클립보드로 (진행 상황 기억)
#
# 복사한 뒤 Figma 플러그인의 Paste code 칸에 Ctrl+V 하면 된다.

param([string]$Target)

$dir = $PSScriptRoot
$files = Get-ChildItem -Path $dir -Filter "*.html" | Sort-Object Name
$stateFile = Join-Path $dir ".copy-progress"

if (-not $files) {
    Write-Host "HTML 파일이 없습니다. _build 에서 'node export.mjs' 를 먼저 실행하세요." -ForegroundColor Yellow
    exit 1
}

function Show-List {
    $done = if (Test-Path $stateFile) { [int](Get-Content $stateFile) } else { 0 }
    Write-Host ""
    Write-Host "figma-export — $($files.Count)개 화면" -ForegroundColor Cyan
    Write-Host ""
    for ($i = 0; $i -lt $files.Count; $i++) {
        $n = $i + 1
        $mark = if ($n -le $done) { "[v]" } else { "[ ]" }
        $size = "{0,6:N0}자" -f (Get-Item $files[$i].FullName).Length
        Write-Host ("  {0} {1,2}. {2,-30} {3}" -f $mark, $n, $files[$i].Name, $size)
    }
    Write-Host ""
    Write-Host "  .\copy-to-clipboard.ps1 <번호>   특정 파일 복사"
    Write-Host "  .\copy-to-clipboard.ps1 next     다음 파일 복사 (현재 $done/$($files.Count) 완료)"
    Write-Host "  .\copy-to-clipboard.ps1 reset    진행 상황 초기화"
    Write-Host ""
}

if (-not $Target) { Show-List; exit 0 }

if ($Target -eq "reset") {
    Remove-Item $stateFile -ErrorAction SilentlyContinue
    Write-Host "진행 상황을 초기화했습니다." -ForegroundColor Green
    exit 0
}

if ($Target -eq "next") {
    $done = if (Test-Path $stateFile) { [int](Get-Content $stateFile) } else { 0 }
    if ($done -ge $files.Count) {
        Write-Host "모든 파일을 복사했습니다. 초기화하려면: .\copy-to-clipboard.ps1 reset" -ForegroundColor Green
        exit 0
    }
    $index = $done
    Set-Content $stateFile ($done + 1)
} else {
    $num = 0
    if (-not [int]::TryParse($Target, [ref]$num) -or $num -lt 1 -or $num -gt $files.Count) {
        Write-Host "1 ~ $($files.Count) 사이의 번호를 넣어주세요." -ForegroundColor Yellow
        exit 1
    }
    $index = $num - 1
}

$file = $files[$index]
Get-Content $file.FullName -Raw -Encoding UTF8 | Set-Clipboard

Write-Host ""
Write-Host "복사됨 → $($file.Name)" -ForegroundColor Green
Write-Host "Figma 의 html.to.design > Paste code 칸에 Ctrl+V 하세요."
Write-Host ""

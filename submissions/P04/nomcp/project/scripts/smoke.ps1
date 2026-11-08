param([string]$BaseUrl = 'http://localhost:18084')
$ErrorActionPreference = 'Stop'
function Get-Csrf([string]$Html) {
    $match = [regex]::Match($Html, 'name="_csrf"[^>]*value="([^"]+)"')
    if (!$match.Success) { throw 'CSRF token not found' }
    return $match.Groups[1].Value
}
$list = Invoke-WebRequest "$BaseUrl/egovBoardList.do" -SessionVariable boardSession
if ($list.StatusCode -ne 200) { throw 'List failed' }
$token = Get-Csrf $list.Content
$form = Invoke-WebRequest "$BaseUrl/addBoardView.do" -Method Post -WebSession $boardSession -Body @{ _csrf=$token }
$token = Get-Csrf $form.Content
$samplePath = Join-Path $PSScriptRoot '../target/smoke-note.txt'
Set-Content -LiteralPath $samplePath -Value 'real HTTP attachment' -Encoding utf8 -NoNewline
$added = Invoke-WebRequest "$BaseUrl/addBoard.do" -Method Post -WebSession $boardSession -Form @{ _csrf=$token; title='HTTP smoke'; content='multipart body'; writer='smoke'; file_1=Get-Item $samplePath }
if ($added.Content -notmatch 'HTTP smoke') { throw 'Multipart create failed' }
$id = [regex]::Match($added.Content, 'name="boardId"[^>]*value="([^"]+)"').Groups[1].Value
if (!$id) { throw 'Created board ID not found' }
$token = Get-Csrf $added.Content
$edit = Invoke-WebRequest "$BaseUrl/updateBoardView.do" -Method Post -WebSession $boardSession -Body @{ _csrf=$token; boardId=$id }
$token = Get-Csrf $edit.Content
$group = [regex]::Match($edit.Content, 'name="atchFileId"[^>]*value="([^"]+)"').Groups[1].Value
$fileId = [regex]::Match($edit.Content, 'downloadFile.do\?fileId=([a-f0-9-]+)').Groups[1].Value
$download = Invoke-WebRequest "$BaseUrl/downloadFile.do?fileId=$fileId" -WebSession $boardSession
if ($download.Headers['Content-Disposition'] -notlike 'attachment;*') { throw 'Download disposition failed' }
$secondPath = Join-Path $PSScriptRoot '../target/smoke-second.txt'
Set-Content -LiteralPath $secondPath -Value 'second attachment' -Encoding utf8 -NoNewline
$updated = Invoke-WebRequest "$BaseUrl/updateBoard.do" -Method Post -WebSession $boardSession -Form @{ _csrf=$token; boardId=$id; atchFileId=$group; title='HTTP updated'; content='updated body'; writer='smoke'; file_1=Get-Item $secondPath }
if ($updated.Content -notmatch 'HTTP updated') { throw 'Update failed' }
$edit2 = Invoke-WebRequest "$BaseUrl/updateBoardView.do" -Method Post -WebSession $boardSession -Body @{ _csrf=(Get-Csrf $updated.Content); boardId=$id }
if (([regex]::Matches($edit2.Content, 'class="download-link"')).Count -ne 2) { throw 'Existing attachment was not retained' }
if ($edit2.Content -notmatch [regex]::Escape($group)) { throw 'Attachment group changed' }
$deleted = Invoke-WebRequest "$BaseUrl/deleteBoard.do" -Method Post -WebSession $boardSession -Body @{ _csrf=(Get-Csrf $edit2.Content); boardId=$id }
if ($deleted.Content -match 'HTTP updated') { throw 'Delete failed' }
$missing = Invoke-WebRequest "$BaseUrl/downloadFile.do?fileId=$fileId" -SkipHttpErrorCheck
if ($missing.StatusCode -ne 404) { throw 'Deleted attachment still exists' }
$blocked = Invoke-WebRequest "$BaseUrl/addBoardView.do" -Method Post -WebSession $boardSession -SkipHttpErrorCheck
if ($blocked.StatusCode -ne 403) { throw 'CSRF enforcement failed' }
Write-Output 'PASS: real HTTP CRUD, multipart CSRF, download, attachment preservation, delete, CSRF rejection'

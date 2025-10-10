Param()

$ApiPort = if ($env:API_PORT) { $env:API_PORT } else { 5001 }
$base = "http://127.0.0.1:$ApiPort"

Write-Host "Using base URL: $base"

$rand = [int][double]::Parse((Get-Date -UFormat %s))
$dip = "dip_$rand@example.com"
$mgr = "mgr_$rand@example.com"

Write-Host "`n=== Register Dipendente ==="
$dipBody = @{
    email = $dip
    password = 'pwd'
    nome = 'Nome'
    cognome = 'Cognome'
    data_nascita = '1990-01-01'
    Dipartimento_id_dipartimento = 1
} | ConvertTo-Json
$dipResp = curl.exe -s -w "`n%{http_code}" -X POST "$base/api/register/dipendente" -H "Content-Type: application/json" -d $dipBody
Write-Host $dipResp

$dipCode = ($dipResp -split "`n")[-1]
$dipBody = ($dipResp -split "`n")[0..(($dipResp -split "`n").Length-2)] -join "`n"
if ($dipCode -eq '409') {
    Write-Host "User already registered, attempting login..."
    $login = curl.exe -s -w "`n%{http_code}" -X POST "$base/api/login" -H "Content-Type: application/json" -d (ConvertTo-Json @{ email = $dip; password = 'pwd' })
    $loginCode = ($login -split "`n")[-1]
    $loginBody = ($login -split "`n")[0..(($login -split "`n").Length-2)] -join "`n"
    if ($loginCode -ne '200') { Write-Host "Login failed: $loginCode"; exit 1 }
    $dipToken = (ConvertFrom-Json $loginBody).token
} else {
    $dipToken = (ConvertFrom-Json $dipBody).token
}
Write-Host "Dipendente token: $dipToken"

Write-Host "`n=== Login Dipendente with token ==="
curl.exe -s -X POST "$base/api/login" -H "Content-Type: application/json" -d (ConvertTo-Json @{ token = $dipToken }) | Write-Host

Write-Host "`n=== Login Dipendente with email/password ==="
curl.exe -s -X POST "$base/api/login" -H "Content-Type: application/json" -d (ConvertTo-Json @{ email = $dip; password = 'pwd' }) | Write-Host

Write-Host "`n=== Register Manager ==="
$mgrBody = @{
    email = $mgr
    password = 'pwd'
    nome = 'Mario'
    cognome = 'Rossi'
    data_nascita = '1980-01-01'
    anni_lavorativi = 5
    Dipartimento_id_dipartimento = 1
} | ConvertTo-Json
$mgrResp = curl.exe -s -w "`n%{http_code}" -X POST "$base/api/register/manager" -H "Content-Type: application/json" -d $mgrBody
Write-Host $mgrResp

$mgrCode = ($mgrResp -split "`n")[-1]
$mgrBodyOnly = ($mgrResp -split "`n")[0..(($mgrResp -split "`n").Length-2)] -join "`n"
if ($mgrCode -eq '409') {
    Write-Host "Manager already registered, attempting login..."
    $login = curl.exe -s -w "`n%{http_code}" -X POST "$base/api/login" -H "Content-Type: application/json" -d (ConvertTo-Json @{ email = $mgr; password = 'pwd' })
    $loginCode = ($login -split "`n")[-1]
    $loginBody = ($login -split "`n")[0..(($login -split "`n").Length-2)] -join "`n"
    if ($loginCode -ne '200') { Write-Host "Login failed: $loginCode"; exit 1 }
    $mgrToken = (ConvertFrom-Json $loginBody).token
} else {
    $mgrToken = (ConvertFrom-Json $mgrBodyOnly).token
}
Write-Host "Manager token: $mgrToken"

Write-Host "`n=== Login Manager with token ==="
curl.exe -s -X POST "$base/api/login" -H "Content-Type: application/json" -d (ConvertTo-Json @{ token = $mgrToken }) | Write-Host

Write-Host "`n=== Login Manager with email/password ==="
curl.exe -s -X POST "$base/api/login" -H "Content-Type: application/json" -d (ConvertTo-Json @{ email = $mgr; password = 'pwd' }) | Write-Host

Write-Host "`nAll done."

# ------------------------------------------------------------------------------------
# ESTENSIONE TEST: Progetti, Task, Endpoints di visibilità e Update/Delete
# Porting parziale dei casi presenti in scripts/curl_tests.sh (versione bash)
# NOTE: Questo script si concentra sugli scenari principali per Windows PowerShell.
# ------------------------------------------------------------------------------------

function Invoke-JsonPost {
    param(
        [Parameter(Mandatory)] [string] $Path,
        [Parameter(Mandatory)] $BodyObj
    )
    $json = if ($BodyObj -is [string]) { $BodyObj } else { $BodyObj | ConvertTo-Json -Depth 6 }
    $resp = curl.exe -s -w "`n%{http_code}" -X POST "$base$Path" -H "Content-Type: application/json" -d $json
    $code = ($resp -split "`n")[-1]
    $body = ($resp -split "`n")[0..(($resp -split "`n").Length-2)] -join "`n"
    return [pscustomobject]@{ Code = [int]$code; BodyRaw = $body; Body = (try { $body | ConvertFrom-Json } catch { $null }) }
}

function Assert-Status {
    param(
        [Parameter(Mandatory)] $Response,
        [Parameter(Mandatory)] [int[]] $Expected,
        [string] $Message = 'Unexpected status'
    )
    if ($Expected -notcontains $Response.Code) {
        Write-Host "[FAIL] $Message (got $($Response.Code) expected $(($Expected -join ',')))" -ForegroundColor Red
        Write-Host $Response.BodyRaw
        exit 1
    }
}

Write-Host "`n=== Crea Progetto (auto id) ==="
$projPayload = @{ token = $mgrToken; descrizione = 'Test project'; budget = 10000.50; nome = 'TestProj'; data_inizio='2025-01-01'; data_fine='2025-12-31'; id_dipartimento=1 }
$projResp = Invoke-JsonPost -Path '/api/add/Project' -BodyObj $projPayload
Assert-Status $projResp -Expected 201 -Message 'Project creation failed'
$projId = $projResp.Body.data.id_progetto
Write-Host "Project id: $projId"

Write-Host "`n=== Update Project ==="
$updProj = @{ token=$mgrToken; id_progetto=$projId; id_dipartimento=1; nome='TestProjUpdated'; descrizione='Descrizione aggiornata' }
$updProjResp = Invoke-JsonPost -Path '/api/update/Project' -BodyObj $updProj
Assert-Status $updProjResp -Expected 200 -Message 'Update project failed'

Write-Host "`n=== Projects in Progress (manager) ==="
$pip = Invoke-JsonPost -Path '/api/projects/in-progress' -BodyObj @{ token=$mgrToken }
Assert-Status $pip -Expected 200 -Message 'projects/in-progress failed'

Write-Host "`n=== Projects in Progress (dipendente should 403) ==="
$pipDip = Invoke-JsonPost -Path '/api/projects/in-progress' -BodyObj @{ token=$dipToken }
Assert-Status $pipDip -Expected 403 -Message 'projects/in-progress dipendente expected 403'

Write-Host "`n=== Projects by Dipendente ==="
$pbd = Invoke-JsonPost -Path '/api/projects/by-dipendente' -BodyObj @{ email_dipendente = $dip }
Assert-Status $pbd -Expected 200 -Message 'projects/by-dipendente failed'

Write-Host "`n=== Project Budget ==="
$budgetResp = Invoke-JsonPost -Path '/api/projects/budget' -BodyObj @{ id_progetto = $projId }
Assert-Status $budgetResp -Expected 200 -Message 'projects/budget failed'
Write-Host "Budget: $($budgetResp.Body.data.budget)"

Write-Host "`n=== Crea Task (auto id) ==="
$taskPayload = @{ token=$mgrToken; stato='Open'; descrizione='Task di test'; data_inizio='2025-03-01'; data_fine='2025-03-31'; id_progetto=$projId; id_dipartimento=1; email_dipendente=$dip; email_manager=$mgr }
$taskResp = Invoke-JsonPost -Path '/api/add/Task' -BodyObj $taskPayload
Assert-Status $taskResp -Expected 201 -Message 'Task creation failed'
$taskId = $taskResp.Body.data.id_task
Write-Host "Task id: $taskId"

Write-Host "`n=== Update Task ==="
$updTask = @{ token=$mgrToken; id=$taskId; id_progetto=$projId; id_dipartimento=1; stato='InProgress' }
$updTaskResp = Invoke-JsonPost -Path '/api/update/Task' -BodyObj $updTask
Assert-Status $updTaskResp -Expected 200 -Message 'Update task failed'

Write-Host "`n=== Delete Task ==="
$delTaskResp = Invoke-JsonPost -Path '/api/delete/Task' -BodyObj @{ token=$mgrToken; id=$taskId; id_progetto=$projId; id_dipartimento=1 }
Assert-Status $delTaskResp -Expected 200 -Message 'Delete task failed'

Write-Host "`n=== Delete Task (negative 404) ==="
$delTaskNeg = Invoke-JsonPost -Path '/api/delete/Task' -BodyObj @{ token=$mgrToken; id=$taskId; id_progetto=$projId; id_dipartimento=1 }
Assert-Status $delTaskNeg -Expected 404 -Message 'Delete task negative expected 404'

Write-Host "`n=== Delete Project (negative non esistente) ==="
$delProjNeg = Invoke-JsonPost -Path '/api/delete/Project' -BodyObj @{ token=$mgrToken; id_progetto=987654; id_dipartimento=1 }
Assert-Status $delProjNeg -Expected 404 -Message 'Delete project negative expected 404'

Write-Host "`n=== Delete Project (happy path) ==="
$delProj = Invoke-JsonPost -Path '/api/delete/Project' -BodyObj @{ token=$mgrToken; id_progetto=$projId; id_dipartimento=1 }
Assert-Status $delProj -Expected 200 -Message 'Delete project failed'

Write-Host "`n=== Tasks by Project (manager scope after deletion should be empty or 0) ==="
$tasksVis = Invoke-JsonPost -Path '/api/task/by-project' -BodyObj @{ token=$mgrToken; id_progetto=$projId; id_dipartimento=1 }
Assert-Status $tasksVis -Expected 200 -Message 'task/by-project visibility failed'
if ($tasksVis.Body.data.count -gt 0) { Write-Host "[WARN] Expected 0 tasks after deletion but found $($tasksVis.Body.data.count)" -ForegroundColor Yellow }

Write-Host "`n=== TEST CONCLUSI (PowerShell subset) ===" -ForegroundColor Green
Write-Host "Tutti gli step principali sono passati."

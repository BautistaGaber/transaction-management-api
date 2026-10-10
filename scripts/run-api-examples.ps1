param(
    [string]$BaseUrl = "http://localhost:8080"
)

$ErrorActionPreference = "Stop"
$base = "$($BaseUrl.TrimEnd('/'))/transactions"
$passed = 0
$failed = 0

function Invoke-Api {
    param(
        [string]$Method,
        [string]$Path,
        [object]$Body = $null,
        [int]$ExpectedStatus = 200
    )

    $params = @{
        Uri = "$base$Path"
        Method = $Method
        UseBasicParsing = $true
    }

    if ($null -ne $Body) {
        $params.ContentType = "application/json"
        $params.Body = ConvertTo-Json -InputObject $Body -Depth 5
    }

    $status = 0
    $content = ""

    try {
        $response = Invoke-WebRequest @params
        $status = [int]$response.StatusCode
        $content = [string]$response.Content
    }
    catch {
        if ($null -eq $_.Exception.Response) {
            throw
        }

        $status = [int]$_.Exception.Response.StatusCode
    }

    if ($status -ne $ExpectedStatus) {
        throw "Expected HTTP $ExpectedStatus, received $status for $Method $Path"
    }

    # Return a wrapper, rather than a naked JSON value. PowerShell otherwise
    # enumerates arrays from functions and can turn [] into $null or [10] into 10.
    return [pscustomobject]@{
        StatusCode = $status
        Content = $content
    }
}

function Assert-Equal {
    param($Expected, $Actual)

    if ($Expected -ne $Actual) {
        throw "Expected $Expected, received $Actual"
    }
}

function Test-Scenario {
    param(
        [string]$Name,
        [scriptblock]$Action
    )

    try {
        & $Action
        $script:passed++
        Write-Host "[PASS] $Name" -ForegroundColor Green
    }
    catch {
        $script:failed++
        Write-Host "[FAIL] $Name - $_" -ForegroundColor Red
    }
}

function Save-Transaction {
    param(
        [long]$Id,
        [double]$Amount,
        [string]$Type,
        [Nullable[long]]$ParentId = $null
    )

    $body = @{ amount = $Amount; type = $Type }
    if ($null -ne $ParentId) {
        $body.parent_id = $ParentId
    }

    $response = Invoke-Api -Method PUT -Path "/$Id" -Body $body
    $result = ConvertFrom-Json -InputObject $response.Content
    Assert-Equal "ok" $result.status
}

function Assert-Sum {
    param([long]$Id, [double]$Expected)

    $response = Invoke-Api -Method GET -Path "/sum/$Id"
    $result = ConvertFrom-Json -InputObject $response.Content
    Assert-Equal $Expected $result.sum
}

function Assert-Ids {
    param(
        [string]$Type,
        [long[]]$Expected
    )

    $response = Invoke-Api -Method GET -Path "/types/$Type"
    $json = $response.Content.Trim()

    if (-not ($json.StartsWith('[') -and $json.EndsWith(']'))) {
        throw "Expected a JSON array for type $Type, received: $json"
    }

    # ConvertFrom-Json may enumerate a single-element or empty array.
    # Filtering null preserves the correct counts for both cases.
    $parsed = ConvertFrom-Json -InputObject $json
    $actual = @($parsed | Where-Object { $null -ne $_ })

    if ($actual.Count -ne $Expected.Count) {
        throw "Expected $($Expected.Count) IDs, received $($actual.Count) for type $Type"
    }

    for ($i = 0; $i -lt $Expected.Count; $i++) {
        if ([long]$actual[$i] -ne $Expected[$i]) {
            throw "Expected ID $($Expected[$i]), received $($actual[$i])"
        }
    }
}

Write-Host "`nMENDEL - OFFICIAL EXAMPLES" -ForegroundColor Cyan

Test-Scenario "Official transactions and sums" {
    Save-Transaction 10 5000 "cars"
    Save-Transaction 11 10000 "shopping" 10
    Save-Transaction 12 5000 "shopping" 11

    Assert-Sum 10 20000
    Assert-Sum 11 15000
    Assert-Ids -Type "cars" -Expected @(10)
}

Write-Host "`nADDITIONAL SCENARIOS" -ForegroundColor Cyan

Test-Scenario "Multiple branches" {
    Save-Transaction 100 1000 "travel"
    Save-Transaction 101 500 "hotel" 100
    Save-Transaction 102 300 "food" 100
    Save-Transaction 103 200 "transport" 101

    Assert-Sum 100 2000
    Assert-Sum 101 700
    Assert-Ids -Type "hotel" -Expected @(101)
}

Test-Scenario "Update existing transaction" {
    Save-Transaction 200 500 "office"
    Assert-Sum 200 500
    Save-Transaction 200 900 "office"
    Assert-Sum 200 900
}

Test-Scenario "Circular relationship rejected" {
    Save-Transaction 300 100 "equipment"
    Save-Transaction 301 200 "equipment" 300
    Save-Transaction 302 300 "equipment" 301

    $body = @{ amount = 100; type = "equipment"; parent_id = 302 }
    $null = Invoke-Api -Method PUT -Path "/300" -Body $body -ExpectedStatus 400
    Assert-Sum 300 600
}

Test-Scenario "Missing parent rejected" {
    $body = @{ amount = 500; type = "shopping"; parent_id = 999999 }
    $null = Invoke-Api -Method PUT -Path "/400" -Body $body -ExpectedStatus 400
}

Test-Scenario "Blank type rejected" {
    $null = Invoke-Api -Method PUT -Path "/401" -Body @{ amount = 100; type = "" } -ExpectedStatus 400
}

Test-Scenario "Missing amount rejected" {
    $null = Invoke-Api -Method PUT -Path "/402" -Body @{ type = "shopping" } -ExpectedStatus 400
}

Test-Scenario "Unknown transaction returns 404" {
    $null = Invoke-Api -Method GET -Path "/sum/999999" -ExpectedStatus 404
}

Test-Scenario "Unknown type returns empty list" {
    Assert-Ids -Type "unknown-category" -Expected @()
}

Write-Host "`n------------------------------"
Write-Host "Passed: $passed"
Write-Host "Failed: $failed"
Write-Host "------------------------------"

if ($failed -gt 0) {
    exit 1
}

Write-Host "All scenarios passed!" -ForegroundColor Green
exit 0

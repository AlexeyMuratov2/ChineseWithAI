param(
    [string] $BaseUrl = "http://localhost:8080",
    [string] $Username = ("pipeline_tester_{0}" -f (Get-Date -Format "yyyyMMddHHmmss")),
    [string] $Password = "StrongPass123!",
    [string] $ModelKey = "fake-model"
)

$ErrorActionPreference = "Stop"

function Invoke-Json {
    param(
        [string] $Method,
        [string] $Uri,
        [hashtable] $Headers,
        [object] $Body
    )

    $request = @{
        Method = $Method
        Uri = $Uri
        ContentType = "application/json"
    }
    if ($Headers) {
        $request.Headers = $Headers
    }
    if ($null -ne $Body) {
        $request.Body = ($Body | ConvertTo-Json -Depth 20)
    }

    Invoke-RestMethod @request
}

$sourceText = @'
Small HSK 5 lesson description. Topic: making a responsible decision at work. The lesson should teach learners how to discuss opportunity, preparation, influence, attitude, responsibility, and choice. Target vocabulary in pinyin: jihui, zhunbei, yingxiang, taidu, zeren, xuanze. Suggested grammar focus in pinyin: suiran...danshi..., yuqi...buru..., zhiyou...cai.... The reading text should be short, practical, and suitable for an adult learner. Add one speaking task where the learner explains a decision they made and one memory game for the target words.
'@
$sourceText = $sourceText.Trim()

$registerBody = @{
    username = $Username
    password = $Password
    displayName = "HSK5 Pipeline Tester"
}
Invoke-Json -Method "POST" -Uri "$BaseUrl/api/v1/auth/register" -Body $registerBody | Out-Null

$login = Invoke-Json -Method "POST" -Uri "$BaseUrl/api/v1/auth/login" -Body @{
    username = $Username
    password = $Password
}
$headers = @{ Authorization = "Bearer $($login.accessToken)" }

$draft = Invoke-Json -Method "POST" -Uri "$BaseUrl/api/v1/lesson-drafts" -Headers $headers -Body @{
    title = "Manual HSK5 pipeline draft"
    description = "Draft for checking the hsk5-quality:v1 multi-step generation pipeline."
    userInstructions = "Create an HSK 5 lesson with new words, review words, grammar explanations, exercises, and a vocabulary game. Use Russian for explanations."
    explanationLanguage = "ru"
    translationLanguage = "ru"
}

Invoke-Json -Method "POST" -Uri "$BaseUrl/api/v1/lesson-drafts/$($draft.id)/sources" -Headers $headers -Body @{
    type = "TEXT_NOTE"
    textContent = $sourceText
} | Out-Null

$generateBody = @{
    draftId = $draft.id
    moduleKey = "hsk5_v1"
    modelKey = $ModelKey
} | ConvertTo-Json -Compress

Write-Host ""
Write-Host "Created test user: $Username"
Write-Host "Created draftId: $($draft.id)"
Write-Host ""
Write-Host "Run generation yourself with:"
Write-Host ""
Write-Host "`$headers = @{ Authorization = 'Bearer $($login.accessToken)' }"
Write-Host "`$body = '$generateBody'"
Write-Host "Invoke-RestMethod -Method POST -Uri '$BaseUrl/api/v1/lessons/generate' -Headers `$headers -ContentType 'application/json' -Body `$body"
Write-Host ""
Write-Host "Expected hsk5 pipeline stages: blueprint, grammar, vocabulary_practice, word_game, composer."

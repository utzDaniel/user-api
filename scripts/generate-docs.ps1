$specPath = "docs\openapi\spec.json"
$outputPath = "docs\openapi\spec.html"

if (-not (Test-Path $specPath)) {
    Write-Error "spec.json nao encontrado em $specPath"
    exit 1
}

$spec = Get-Content $specPath -Raw -Encoding UTF8

$html = @"
<!DOCTYPE html>
<html>
  <head>
    <title>API de Usuários</title>
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <style>body { margin: 0; padding: 0; }</style>
  </head>
  <body>
    <div id="redoc-container"></div>
    <script src="https://cdn.jsdelivr.net/npm/redoc/bundles/redoc.standalone.js"></script>
    <script>
      var spec = $spec;
      Redoc.init(spec, {}, document.getElementById('redoc-container'));
    </script>
  </body>
</html>
"@

$html | Out-File -FilePath $outputPath -Encoding UTF8
Write-Host "spec.html gerado com sucesso em $outputPath"

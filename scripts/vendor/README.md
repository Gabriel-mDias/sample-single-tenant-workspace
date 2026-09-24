# Toolkit GEMS fixado para o CI

`gems_ai-2.0.0-py3-none-any.whl` foi construído a partir do objeto Git
`9abe113f69edf8bf8e7c13fe4cd8cbdedc2a7e5b` de `Gabriel-mDias/gems-ai-sdk`.
O arquivo fonte foi extraído com `git archive` desse SHA e empacotado com
`uv build --wheel`. SHA-256 do wheel:

`17d2fa32fbc29fe86ccc1931fb203188bedeb6b0f6c521fd3885f37bbf879aab`

O workflow confere esse hash antes de instalar o wheel e executa `check --strict`
e `verify-angular`. O pacote fica no repositório porque o token de Packages do
Sample não pode ler o repositório privado do toolkit: o download por API retornou
HTTP 404 no CI do commit `cfa2e2c08d876a90986a5fabdd5b4e134b850292`.
Atualizar o commit do toolkit exige reconstruir o wheel, revisar o plano de `apply`,
atualizar `gems-ai.json`/lock e repetir os gates.

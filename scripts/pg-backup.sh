#!/usr/bin/env bash
#
# Backup dos dois bancos da aplicacao, enviado para fora da mÃ¡quina.
#
# POR QUE OS DOIS DUMPS SÃƒO UMA UNIDADE
# -------------------------------------
# `security.USUARIO.CD_KEYCLOAK_ID` Ã© chave estrangeira ENTRE DOIS BANCOS, e nenhum banco a garante.
# `pg_dump` Ã© por banco, entÃ£o dois dumps sÃ£o dois instantes distintos: um vÃ­nculo de serviÃ§o criado
# entre eles deixaria um USUARIO apontando para uma conta que nÃ£o estÃ¡ no dump do Keycloak, ou uma
# conta Ã³rfÃ£ no realm. Ã‰ o mesmo defeito que a compensaÃ§Ã£o transacional evita em runtime, voltando
# pela porta do backup.
#
# MitigaÃ§Ã£o: os dois dumps saem em sequÃªncia imediata e compartilham o MESMO carimbo de lote no nome
# do arquivo. Restaure sempre o par â€” nunca um sÃ³. Se um dia a janela entre eles incomodar, a saÃ­da
# Ã© backup fÃ­sico do cluster (`pg_basebackup` + WAL), atÃ´mico entre bancos.
#
# POR QUE AS SENHAS DOS USUARIOS DEPENDEM DISTO
# --------------------------------------------------
# As contas e senhas da equipe existem APENAS no keycloak-db. O `security.USUARIO`
# guarda login e identificador externo, nunca a senha. Perder o keycloak-db sem backup significa que
# ninguÃ©m mais entra no sistema, e nÃ£o hÃ¡ como recriar as credenciais.
#
# USO
#     ./scripts/pg-backup.sh
#
# Agendamento: systemd timer no host â€” ver environments/vps/05-backup-e-restore.md.

set -euo pipefail

# --- ConfiguraÃ§Ã£o -------------------------------------------------------------------------------
# Lidas do ambiente; o timer as fornece por EnvironmentFile.
: "${PGCONTAINER:=app-postgres}"
: "${POSTGRES_SUPERUSER:?defina POSTGRES_SUPERUSER}"
: "${APP_DB_NAME:=app-db}"
: "${KEYCLOAK_DB_NAME:=keycloak-db}"
: "${BACKUP_DIR:=/var/backups/app}"
: "${BACKUP_RETENTION_DAYS:=14}"
# Bucket e credencial SEPARADOS dos da aplicaÃ§Ã£o: um vazamento da credencial de upload nÃ£o pode
# alcanÃ§ar as cÃ³pias de seguranÃ§a.
: "${BACKUP_S3_BUCKET:?defina BACKUP_S3_BUCKET}"

LOTE="$(date -u +%Y%m%dT%H%M%SZ)"
mkdir -p "${BACKUP_DIR}"

dump() {
    local banco="$1"
    local destino="${BACKUP_DIR}/${LOTE}__${banco}.sql.gz"

    echo "==> Dump de '${banco}' ..."
    # -Fp comprimido por gzip: legÃ­vel com zcat e restaurÃ¡vel com psql, sem depender de pg_restore
    # da mesma versÃ£o. --clean para que o restore nÃ£o exija banco vazio.
    docker exec "${PGCONTAINER}" pg_dump \
        --username "${POSTGRES_SUPERUSER}" \
        --dbname "${banco}" \
        --clean --if-exists --no-owner \
        | gzip -9 > "${destino}"

    # Um dump que saiu vazio por erro de conexÃ£o Ã© pior que dump nenhum: dÃ¡ falsa sensaÃ§Ã£o de
    # cobertura. Falhar aqui Ã© o comportamento correto.
    if [ ! -s "${destino}" ]; then
        echo "ERRO: dump de '${banco}' saiu vazio." >&2
        exit 1
    fi

    echo "    ${destino} ($(du -h "${destino}" | cut -f1))"
}

# A ordem importa pouco; a proximidade no tempo Ã© que importa. Nada de tarefa demorada entre as duas.
dump "${APP_DB_NAME}"
dump "${KEYCLOAK_DB_NAME}"

echo "==> Enviando o lote ${LOTE} para s3://${BACKUP_S3_BUCKET}/ ..."
# Backup guardado na mesma mÃ¡quina que ele deveria proteger nÃ£o Ã© backup. O envio para fora Ã© parte
# da rotina, nÃ£o um extra.
aws s3 cp "${BACKUP_DIR}/${LOTE}__${APP_DB_NAME}.sql.gz"    "s3://${BACKUP_S3_BUCKET}/${LOTE}/"
aws s3 cp "${BACKUP_DIR}/${LOTE}__${KEYCLOAK_DB_NAME}.sql.gz" "s3://${BACKUP_S3_BUCKET}/${LOTE}/"

echo "==> Limpando cÃ³pias locais com mais de ${BACKUP_RETENTION_DAYS} dias ..."
find "${BACKUP_DIR}" -name '*.sql.gz' -mtime "+${BACKUP_RETENTION_DAYS}" -delete

echo "Lote ${LOTE} concluÃ­do: 2 bancos, enviados para fora da mÃ¡quina."
echo "Lembre-se: as duas cÃ³pias formam um PAR e devem ser restauradas em conjunto."

# syntax=docker/dockerfile:1
#
# Imagem do frontend App. O build de produção troca environment.ts por environment.prod.ts
# (fileReplacements em angular.json), mas os dois só definem DEFAULTS compilados: o valor real de
# runtime vem de env.js, gerado pelo docker-entrypoint.sh a partir de variáveis de ambiente do
# contêiner — é o que permite repontar a API sem rebuild.

# ---------------------------------------------------------------------------
# Stage 1 — build
# ---------------------------------------------------------------------------
FROM node:22 AS build

# Credenciais do registry privado GEMS (@gabriel-mdias/angular-gems-sdk).
#
# O pacote NÃO resolve sem credencial: npm.pkg.github.com responde 401 com token ausente.
# Na máquina do desenvolvedor ele resolve porque o ~/.npmrc global tem _authToken; num build limpo,
# não. O CI repassa GEMS_REGISTRY_USER/GEMS_REGISTRY_TOKEN como build args.
#
# As variáveis continuam com default vazio, e o .npmrc só é gerado quando as duas chegam
# preenchidas: um .npmrc com token vazio quebra a resolução em vez de ser inofensivo, e a mensagem
# de erro resultante é pior que o 401 puro.
ARG GEMS_REGISTRY_USER=""
ARG GEMS_REGISTRY_TOKEN=""

WORKDIR /workspace

COPY package.json package-lock.json ./

RUN if [ -n "$GEMS_REGISTRY_USER" ] && [ -n "$GEMS_REGISTRY_TOKEN" ]; then \
      printf '@gabriel-mdias:registry=https://npm.pkg.github.com\n//npm.pkg.github.com/:_authToken=%s\n' "$GEMS_REGISTRY_TOKEN" > .npmrc; \
    fi

# --legacy-peer-deps é OBRIGATÓRIO: angular-gems-sdk@1.1.0 declara peer @angular/common@^20 e o
# projeto está no Angular 22. Sem a flag, o install aborta com ERESOLVE (CLAUDE.md §2).
RUN npm ci --legacy-peer-deps

COPY . .

RUN npm run build -- --configuration production

# ---------------------------------------------------------------------------
# Stage 2 — runtime
# ---------------------------------------------------------------------------
FROM nginx:alpine

# O nome do projeto Angular é "app" (frontend/angular.json) — o builder @angular/build:application
# escreve o output do browser em dist/<projeto>/browser, não em dist/ direto.
COPY --from=build /workspace/dist/app/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY docker-entrypoint.sh /docker-entrypoint-app.sh

RUN chmod +x /docker-entrypoint-app.sh

EXPOSE 80

# O entrypoint gera env.js a partir das variáveis de ambiente do contêiner e só então inicia o
# nginx em foreground (o próprio script termina com `exec nginx -g 'daemon off;'`).
ENTRYPOINT ["/docker-entrypoint-app.sh"]

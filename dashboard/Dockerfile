# ── dev (docker-compose.yaml uses build.target: dev) ───────────────────────────
FROM node:26-alpine AS dev

WORKDIR /app

# Install dependencies first (cached unless package.json/package-lock.json changes)
COPY package.json package-lock.json ./
RUN npm ci

COPY . .

EXPOSE 5173

CMD ["npm", "run", "dev"]

# ── prod stage 1: build the static bundle ───────────────────────────────────────
FROM node:26-alpine AS build

WORKDIR /app

# VITE_* vars are baked in at build time, not read at container runtime.
ARG VITE_WALLETCONNECT_PROJECT_ID=

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# ── prod stage 2: serve dist/ with a non-root nginx ─────────────────────────────
FROM nginxinc/nginx-unprivileged:1.31.6-alpine AS prod

# Base image already defaults to this — explicit so static Dockerfile scanners (Trivy)
# that can't see an inherited base-image USER don't flag a false "runs as root".
USER nginx

COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.prod.conf /etc/nginx/conf.d/default.conf

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=5s --retries=5 \
    CMD wget -q --spider http://127.0.0.1:8080/ || exit 1

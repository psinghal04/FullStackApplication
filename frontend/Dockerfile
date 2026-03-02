FROM node:22-bookworm-slim AS build
WORKDIR /app

ENV CYPRESS_INSTALL_BINARY=0

RUN npm config set strict-ssl false

COPY package*.json ./
RUN npm install --no-audit --no-fund

COPY . .
RUN npm run build

FROM nginx:1.27-alpine AS runtime
WORKDIR /usr/share/nginx/html

COPY nginx/default.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/hr-frontend/browser ./

EXPOSE 80

HEALTHCHECK --interval=15s --timeout=5s --retries=10 --start-period=20s \
  CMD wget -qO- http://localhost/ >/dev/null 2>&1 || exit 1
# Build stage
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json .
RUN sed -i '/@next\/swc-win32/d' package-lock.json && npm install --legacy-peer-deps --ignore-scripts
COPY . .
RUN npm run build

# Runtime stage
FROM node:20-alpine
WORKDIR /app
COPY --from=builder /app/.next/standalone .
COPY --from=builder /app/.next/static ./.next/static
COPY --from=builder /app/public ./public
EXPOSE 3000
CMD ["node", "server.js"]

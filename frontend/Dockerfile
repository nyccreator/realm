# Frontend Dockerfile - Graph visualization optimized
FROM node:18-alpine as build

WORKDIR /app

# Copy package files for better layer caching
COPY package*.json ./

# Install dependencies including graph visualization libraries
RUN npm ci --only=production

# Copy source code
COPY . .

# Build the React application
RUN npm run build

# Production stage - serve with nginx
FROM nginx:alpine

# Copy built application
COPY --from=build /app/build /usr/share/nginx/html

# Copy custom nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:80 || exit 1

# Install curl for health checks
RUN apk add --no-cache curl

# Expose port 80
EXPOSE 80

# Start nginx
CMD ["nginx", "-g", "daemon off;"]
FROM node:18 AS build

WORKDIR /app
COPY package*.json ./
RUN npm install

# Copy all source files
COPY . .

# Create production build
RUN npm run build

# Production environment
FROM nginx:alpine

# Copy custom nginx config (if you have one)
# COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy built assets from builder stage
COPY --from=build /app/build /usr/share/nginx/html

# Add nginx configuration to handle React Router
RUN echo "server { \
    listen 80; \
    location / { \
        root /usr/share/nginx/html; \
        try_files \$uri \$uri/ /index.html; \
    } \
}" > /etc/nginx/conf.d/default.conf

EXPOSE 80

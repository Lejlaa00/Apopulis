FROM node:18 AS build

# Default to Docker network URL, can be overridden during build
ARG REACT_APP_API_URL=http://backend:5001/api
ENV REACT_APP_API_URL=$REACT_APP_API_URL

WORKDIR /app
COPY package*.json ./
RUN npm install

# Copy all source files
COPY . .

# Create production build
RUN npm run build

# Production environment
FROM nginx:alpine

# Create nginx directories and set permissions
RUN mkdir -p /usr/share/nginx/html && \
    chown -R nginx:nginx /usr/share/nginx/html

# Copy built assets from builder stage
COPY --from=build /app/build /usr/share/nginx/html/

# Add nginx configuration to handle React Router
RUN echo "server { \
    listen 80; \
    location / { \
        root /usr/share/nginx/html; \
        try_files \$uri \$uri/ /index.html; \
    } \
}" > /etc/nginx/conf.d/default.conf

EXPOSE 80

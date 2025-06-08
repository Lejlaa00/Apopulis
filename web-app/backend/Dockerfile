FROM node:18

# Create app directory
WORKDIR /app

# Install app dependencies
COPY package*.json ./
RUN npm install --production

# Bundle app source
COPY . .

# Create directories and set permissions
RUN mkdir -p /app/images /app/uploads && \
    chown -R node:node /app/images /app/uploads && \
    chmod 755 /app/images /app/uploads

# Set environment variables
# Make sure Node.js listens on all interfaces
ENV NODE_ENV=production \
    PORT=5001 \
    HOST=0.0.0.0

# Expose the port
EXPOSE 5001

# Start the server
CMD ["node", "app.js"]

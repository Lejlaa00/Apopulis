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
ENV NODE_ENV=production
ENV PORT=5001
ENV HOST=0.0.0.0  # Make sure Node.js listens on all interfaces

# Expose the port
EXPOSE 5001

# Start the server
CMD ["node", "app.js"]

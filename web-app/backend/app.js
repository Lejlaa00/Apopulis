const express = require('express');
const cors = require('cors');
const mongoose = require('mongoose');
const app = express();
const authMiddleware = require('./middleware/authMiddleware');
require('dotenv').config();

//Trending news
const cron = require('node-cron');
const recalculateAndCachePopularity = require('./cron/popularityCron');

// Import routes
const userRoutes = require('./routes/userRoutes');
const newsRoutes = require('./routes/newsRoutes');
const commentRoutes = require('./routes/commentRoutes');
const voteRoutes = require('./routes/voteRoutes');
const categoryRoutes = require('./routes/categoryRoutes');
const locationRoutes = require('./routes/locationRoutes');
const sourceRoutes = require('./routes/sourceRoutes');
const provinceRoutes = require('./routes/provinceRoutes');
const statsRoutes = require('./routes/statsRoutes');
const healthRoutes = require('./routes/healthRoutes');

mongoose.connect(process.env.MONGO_URI)
    .then(() => console.log("MongoDB connected"))
    .catch(err => console.error("MongoDB connection error:", err));

// CORS configuration
const allowedOrigins = [
    'http://localhost:3000',
    process.env.FRONTEND_URL,
    'http://backend:5001',
    'http://localhost:5001'
].filter(Boolean);

app.use(cors({
    origin: function(origin, callback) {
        // allow requests with no origin (like mobile apps or curl requests)
        if (!origin) return callback(null, true);
        
        if (allowedOrigins.indexOf(origin) === -1) {
            return callback(new Error('CORS policy violation'), false);
        }
        return callback(null, true);
    },
    methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
    allowedHeaders: ['Content-Type', 'Authorization'],
    credentials: true
}));
app.use(express.json());


const path = require('path');
app.use('/images', express.static(path.join(__dirname, 'images')));


// Request logging middleware
app.use((req, res, next) => {
    console.log(`[${new Date().toISOString()}] ${req.method} ${req.url}`);
    console.log('Request Headers:', req.headers);
    console.log('Request Body:', req.body);
    next();
});

// Test routes
app.get('/', (req, res) => {
    res.send('MERN Backend Running');
});



// API routes
app.use('/api/users', userRoutes);
app.use('/api/news', newsRoutes);
app.use('/api/comments', commentRoutes);
app.use('/api/votes', voteRoutes);
app.use('/api/categories', categoryRoutes);
app.use('/api/locations', locationRoutes);
app.use('/api/sources', sourceRoutes);
app.use('/api/provinces', provinceRoutes);
app.use('/api/stats', statsRoutes);
app.use('/api', healthRoutes);

// Protected test route
app.get('/api/me', authMiddleware, (req, res) => {
    res.json({ msg: `Hello, user ${req.user.id}` });
});

// Start Server
const PORT = process.env.PORT || 5001;
app.listen(PORT, () => {
    console.log(`Server running on http://localhost:${PORT}`);
});

cron.schedule('0 * * * *', async () => {   //testing every one minute->'* * * * *'
    await recalculateAndCachePopularity(); //every hour ->'0 * * * *'
});
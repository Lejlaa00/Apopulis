const express = require('express');
const cors = require('cors');
const mongoose = require('mongoose');
const app = express();
const authMiddleware = require('./middleware/authMiddleware');
require('dotenv').config();

//Trending news
const cron = require('node-cron');
const recalculateAndCachePopularity = require('./cron/popularityCron');


mongoose.connect('mongodb+srv://ivanaailic:malodete167@cluster0.iemfweq.mongodb.net/Apopulis')
    .then(() => console.log("MongoDB connected"))
    .catch(err => console.error("MongoDB connection error:", err));

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

// Middleware
app.use(cors({
    origin: 'http://localhost:3000',
    methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
    allowedHeaders: ['Content-Type', 'Authorization'],
    credentials: true
}));
app.use(express.json());

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
//app.use('/api/comments', authMiddleware, commentRoutes); // Comments require authentication
//app.use('/api/votes', authMiddleware, voteRoutes); // Votes require authentication
app.use('/api/comments', commentRoutes);
app.use('/api/votes', voteRoutes);
app.use('/api/categories', categoryRoutes);
app.use('/api/locations', locationRoutes);
app.use('/api/sources', sourceRoutes);
app.use('/api/provinces', provinceRoutes);
app.use('/api/stats', statsRoutes);

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
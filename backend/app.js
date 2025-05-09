const express = require('express');
const cors = require('cors');
const mongoose = require('mongoose');
const app = express();
require('dotenv').config(); //Loads .env file
const authMiddleware = require('./authMiddleware');

mongoose.connect(process.env.MONGODB_URI, {
    useNewUrlParser: true,
    useUnifiedTopology: true,
})
    .then(() => console.log("MongoDB connected"))
    .catch(err => console.error("MongoDB connection error:", err));


// Import routes
const userRoutes = require('./routes/userRoutes');

// Middleware
app.use(cors());
app.use(express.json());


app.get('/api/me', authMiddleware, (req, res) => {
    res.json({ msg: `Hello, user ${req.user.id}` });
});

// Test route
app.get('/', (req, res) => {
    res.send('MERN Backend Running');
});

// User auth routes
app.use('/users', userRoutes);

// Start Server
const PORT = 5000;
app.listen(PORT, () => {
    console.log(`Server running on http://localhost:${PORT}`);
});
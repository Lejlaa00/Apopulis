const express = require('express');
const router = express.Router();
const authMiddleware = require('../middleware/authMiddleware');
const User = require('../models/userModel');
const { register, login, logout, getBookmarks, addBookmark, removeBookmark, updateProfile } = require('../controllers/userController');

// Bookmarked news for logged-in user
router.get('/bookmarks', authMiddleware, async (req, res) => {
    try {
        const user = await User.findById(req.user.id).populate('bookmarks');
        res.json({ news: user.bookmarks });
    } catch (err) {
        console.error('Error fetching bookmarks:', err);
        res.status(500).json({ msg: 'Failed to load bookmarks' });
    }
});

router.post('/register', register);
router.post('/login', login);
router.post('/logout', logout); 

router.get('/bookmarks', authMiddleware, getBookmarks);
router.post('/bookmarks/:newsId', authMiddleware, addBookmark);
router.delete('/bookmarks/:newsId', authMiddleware, removeBookmark);

router.put('/profile', authMiddleware, updateProfile);

module.exports = router;

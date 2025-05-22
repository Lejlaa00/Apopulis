const express = require('express');
const router = express.Router();
const authMiddleware = require('../middleware/authMiddleware');
const { register, login, logout, getBookmarks, addBookmark, removeBookmark } = require('../controllers/userController');

router.post('/register', register);
router.post('/login', login);
router.post('/logout', logout); 

router.get('/bookmarks', authMiddleware, getBookmarks);
router.post('/bookmarks/:newsId', authMiddleware, addBookmark);
router.delete('/bookmarks/:newsId', authMiddleware, removeBookmark);

module.exports = router;

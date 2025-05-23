const express = require('express');
const router = express.Router();
const { 
    getComments, 
    createComment, 
    updateComment, 
    deleteComment 
} = require('../controllers/commentController');
const authMiddleware = require('../middleware/authMiddleware');

// Get all comments for a news item
router.get('/news/:newsItemId', getComments);

// Create a new comment for a news item
router.post('/news/:newsItemId', authMiddleware, createComment);

// Update a comment
router.put('/:id',authMiddleware, updateComment);

// Delete a comment
router.delete('/:id',authMiddleware, deleteComment);

module.exports = router;

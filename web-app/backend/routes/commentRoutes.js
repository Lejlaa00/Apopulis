const express = require('express');
const router = express.Router();
const { 
    getComments, 
    createComment, 
    updateComment, 
    deleteComment 
} = require('../controllers/commentController');

// Get all comments for a news item
router.get('/news/:newsItemId', getComments);

// Create a new comment for a news item
router.post('/news/:newsItemId', createComment);

// Update a comment
router.put('/:id', updateComment);

// Delete a comment
router.delete('/:id', deleteComment);

module.exports = router;

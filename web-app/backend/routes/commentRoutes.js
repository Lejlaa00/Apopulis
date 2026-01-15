const express = require('express');
const router = express.Router();
const { 
    getComments, 
    createComment, 
    updateComment, 
    deleteComment,
    updateCommentMap,
    deleteCommentMap
} = require('../controllers/commentController');
const authMiddleware = require('../middleware/authMiddleware');
const optionalAuthMiddleware = require('../middleware/optionalAuthMiddleware');

// Get all comments for a news item
router.get('/news/:newsItemId', getComments);

// Create a new comment for a news item
router.post('/news/:newsItemId', optionalAuthMiddleware, createComment);

// MAP (guest/simulated) update/delete without auth, based on ownerKey
router.put('/map/:id', updateCommentMap);
router.delete('/map/:id', deleteCommentMap);

// Update a comment
router.put('/:id',authMiddleware, updateComment);

// Delete a comment
router.delete('/:id',authMiddleware, deleteComment);

module.exports = router;

const express = require('express');
const router = express.Router();
const { 
    getVotes, 
    getUserVote, 
    vote 
} = require('../controllers/voteController');

// Get all votes for a news item
router.get('/news/:newsItemId', getVotes);

// Get user's vote for a news item
router.get('/news/:newsItemId/user', getUserVote);

// Vote on a news item
router.post('/news/:newsItemId', vote);

module.exports = router;

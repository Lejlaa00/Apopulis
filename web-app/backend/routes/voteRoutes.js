const express = require('express');
const router = express.Router();
const { 
    getVotes, 
    getUserVote, 
    vote 
} = require('../controllers/voteController');

const authMiddleware = require('../middleware/authMiddleware');

// All users can see a number of votes
router.get('/news/:newsItemId', getVotes);

// Only logged users can vote
router.get('/news/:newsItemId/user', authMiddleware, getUserVote);
router.post('/news/:newsItemId', authMiddleware, vote);

module.exports = router;
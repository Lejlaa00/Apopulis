const Vote = require('../models/voteModel');

// Get votes for a news item
exports.getVotes = async (req, res) => {
    try {
        const { newsItemId } = req.params;

        const upvotes = await Vote.countDocuments({ newsItemId, type: 'UP' });
        const downvotes = await Vote.countDocuments({ newsItemId, type: 'DOWN' });

        res.json({
            upvotes,
            downvotes,
            total: upvotes - downvotes
        });
    } catch (err) {
        console.error('Error fetching votes:', err);
        res.status(500).json({ msg: 'Error fetching votes', error: err.message });
    }
};

// Get user's vote for a news item
exports.getUserVote = async (req, res) => {
    try {
        const { newsItemId } = req.params;
        const userId = req.user.id; // Assuming user info is added by auth middleware

        const vote = await Vote.findOne({ newsItemId, userId });

        res.json({ vote: vote ? vote.type : null });
    } catch (err) {
        console.error('Error fetching user vote:', err);
        res.status(500).json({ msg: 'Error fetching user vote', error: err.message });
    }
};

// Vote on a news item
exports.vote = async (req, res) => {
    try {
        const { newsItemId } = req.params;
        const { type } = req.body; // 'UP' or 'DOWN'
        const userId = req.user.id; // Assuming user info is added by auth middleware

        if (!['UP', 'DOWN'].includes(type)) {
            return res.status(400).json({ msg: 'Invalid vote type' });
        }

        // Find existing vote
        let vote = await Vote.findOne({ newsItemId, userId });

        if (vote) {
            if (vote.type === type) {
                // Remove vote if same type (toggle off)
                await vote.remove();
                res.json({ msg: 'Vote removed', type: null });
            } else {
                // Update vote type if different
                vote.type = type;
                await vote.save();
                res.json({ msg: 'Vote updated', type });
            }
        } else {
            // Create new vote
            vote = new Vote({
                userId,
                newsItemId,
                type
            });
            await vote.save();
            res.json({ msg: 'Vote recorded', type });
        }
    } catch (err) {
        console.error('Error processing vote:', err);
        res.status(500).json({ msg: 'Error processing vote', error: err.message });
    }
};

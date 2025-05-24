const Vote = require('../models/voteModel');
const { updateNewsMetrics } = require('./newsController');
const NewsItem = require('../models/newsItemModel');


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
        const { type } = req.body;

        if (!req.user) {
            return res.status(401).json({ msg: 'Only logged-in users can vote' });
        }

        const userId = req.user.id;

        if (!['UP', 'DOWN'].includes(type)) {
            return res.status(400).json({ msg: 'Invalid vote type' });
        }

        let vote = await Vote.findOne({ newsItemId, userId });
        const newsItem = await NewsItem.findById(newsItemId);

        if (!newsItem) {
            return res.status(404).json({ msg: 'News item not found' });
        }

        if (vote) {
            if (vote.type === type) {
                // Toggle off: remove vote
                await Vote.deleteOne({ _id: vote._id });

                // Ako je bio UP, ukloni korisnika iz likedBy
                if (type === 'UP') {
                    newsItem.likedBy = newsItem.likedBy.filter(id => id.toString() !== userId);
                    await newsItem.save();
                }

                await updateNewsMetrics(newsItemId);
                return res.json({ msg: 'Vote removed', type: null });
            } else {
                // Change vote type
                const oldType = vote.type;
                vote.type = type;
                await vote.save();

                // Ako prelazimo sa DOWN na UP, dodaj korisnika u likedBy
                if (type === 'UP' && !newsItem.likedBy.includes(userId)) {
                    newsItem.likedBy.push(userId);
                    await newsItem.save();
                }

                // Ako prelazimo sa UP na DOWN, ukloni korisnika iz likedBy
                if (oldType === 'UP') {
                    newsItem.likedBy = newsItem.likedBy.filter(id => id.toString() !== userId);
                    await newsItem.save();
                }

                await updateNewsMetrics(newsItemId);
                return res.json({ msg: 'Vote updated', type });
            }
        } else {
            // New vote
            vote = new Vote({ userId, newsItemId, type });
            await vote.save();

            if (type === 'UP' && !newsItem.likedBy.includes(userId)) {
                newsItem.likedBy.push(userId);
                await newsItem.save();
            }

            await updateNewsMetrics(newsItemId);
            return res.json({ msg: 'Vote recorded', type });
        }
    } catch (err) {
        console.error('Error processing vote:', err);
        res.status(500).json({ msg: 'Error processing vote', error: err.message });
    }
};
  

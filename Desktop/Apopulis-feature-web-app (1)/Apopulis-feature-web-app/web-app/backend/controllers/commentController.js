const Comment = require('../models/commentModel');

// Get all comments for a news item
exports.getComments = async (req, res) => {
    try {
        const { newsItemId } = req.params;
        const { page = 1, limit = 10 } = req.query;
        const skip = (page - 1) * limit;

        const comments = await Comment.find({ newsItemId })
            .populate('userId', 'username')
            .sort({ createdAt: -1 })
            .skip(skip)
            .limit(parseInt(limit));

        const total = await Comment.countDocuments({ newsItemId });

        res.json({
            comments,
            currentPage: parseInt(page),
            totalPages: Math.ceil(total / limit),
            totalItems: total
        });
    } catch (err) {
        console.error('Error fetching comments:', err);
        res.status(500).json({ msg: 'Error fetching comments', error: err.message });
    }
};

// Create a new comment
exports.createComment = async (req, res) => {
    try {
        const { content } = req.body;
        const { newsItemId } = req.params;
        const userId = req.user.id; // Assuming user info is added by auth middleware

        const comment = new Comment({
            userId,
            newsItemId,
            content
        });

        await comment.save();

        const populatedComment = await Comment.findById(comment._id)
            .populate('userId', 'username');

        res.status(201).json(populatedComment);
    } catch (err) {
        console.error('Error creating comment:', err);
        res.status(500).json({ msg: 'Error creating comment', error: err.message });
    }
};

// Update a comment
exports.updateComment = async (req, res) => {
    try {
        const { content } = req.body;
        const { id } = req.params;
        const userId = req.user.id; // Assuming user info is added by auth middleware

        const comment = await Comment.findOne({ _id: id, userId });

        if (!comment) {
            return res.status(404).json({ msg: 'Comment not found or unauthorized' });
        }

        comment.content = content;
        await comment.save();

        const populatedComment = await Comment.findById(comment._id)
            .populate('userId', 'username');

        res.json(populatedComment);
    } catch (err) {
        console.error('Error updating comment:', err);
        res.status(500).json({ msg: 'Error updating comment', error: err.message });
    }
};

// Delete a comment
exports.deleteComment = async (req, res) => {
    try {
        const { id } = req.params;
        const userId = req.user.id; // Assuming user info is added by auth middleware

        const comment = await Comment.findOneAndDelete({ _id: id, userId });

        if (!comment) {
            return res.status(404).json({ msg: 'Comment not found or unauthorized' });
        }

        res.json({ msg: 'Comment deleted successfully' });
    } catch (err) {
        console.error('Error deleting comment:', err);
        res.status(500).json({ msg: 'Error deleting comment', error: err.message });
    }
};

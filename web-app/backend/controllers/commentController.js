const Comment = require('../models/commentModel');
const { updateNewsMetrics } = require('./newsController');
const NewsItem = require('../models/newsItemModel');

// Get all comments for a news item
exports.getComments = async (req, res) => {
    try {
        const { newsItemId } = req.params;

        // Fetch all comments for the news item
        const allComments = await Comment.find({ newsItemId })
            .populate('userId', 'username')
            .sort({ createdAt: -1 }); // newest first

        // Create a map: commentId -> comment
        const commentMap = {};
        allComments.forEach(comment => {
            comment = comment.toObject(); // convert to plain object to allow mutation
            comment.replies = []; // initialize replies array
            commentMap[comment._id] = comment;
        });

        // Build the hierarchy
        const rootComments = [];
        allComments.forEach(comment => {
            if (comment.parentCommentId) {
                const parent = commentMap[comment.parentCommentId];
                if (parent) {
                    parent.replies.push(commentMap[comment._id]);
                }
            } else {
                rootComments.push(commentMap[comment._id]);
            }
        });

        res.json({ comments: rootComments });

    } catch (err) {
        console.error('Error fetching comments:', err);
        res.status(500).json({ msg: 'Error fetching comments', error: err.message });
    }
};


// Create a new comment
exports.createComment = async (req, res) => {
    try {
        const { content, parentCommentId } = req.body;
        const { newsItemId } = req.params;
        const userId = req.user.id;

        const comment = new Comment({
            userId,
            newsItemId,
            content,
            parentCommentId: parentCommentId || null
        });

        await comment.save();

        const newsItem = await NewsItem.findById(newsItemId);
        if (newsItem && !newsItem.commentedBy.includes(userId)) {
            newsItem.commentedBy.push(userId);
            await newsItem.save();
        }
        
        await updateNewsMetrics(newsItemId);

        console.log("Kreira se komentar:", {
            content,
            parentCommentId,
            newsItemId,
            userId
        });
          
        const populatedComment = await Comment.findById(comment._id).populate('userId', 'username');

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
        const userId = req.user.id;

        const comment = await Comment.findOne({ _id: id, userId });

        if (!comment) {
            return res.status(404).json({ msg: 'Comment not found or unauthorized' });
        }

        await Comment.deleteOne({ _id: id });
        await updateNewsMetrics(comment.newsItemId);

        const remainingComments = await Comment.countDocuments({
            newsItemId: comment.newsItemId,
            userId,
        });

        if (remainingComments === 0) {
            const newsItem = await NewsItem.findById(comment.newsItemId);
            if (newsItem) {
                newsItem.commentedBy = newsItem.commentedBy.filter(
                    (id) => id.toString() !== userId
                );
                await newsItem.save();
            }
        }

        res.json({ msg: 'Comment deleted successfully' });
    } catch (err) {
        console.error('Error deleting comment:', err);
        res.status(500).json({ msg: 'Error deleting comment', error: err.message });
    }
};
  

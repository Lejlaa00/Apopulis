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
      .sort({ createdAt: 1 }); // najstariji prvi (može i -1)

    // Pretvori sve komentare u plain objecte i mapiraj po ID-u
    const commentMap = {};
    const plainComments = allComments.map(c => {
      const obj = c.toObject();
      obj.replies = [];
      commentMap[obj._id.toString()] = obj;
      return obj;
    });

    // Složi hijerarhiju
    const rootComments = [];
    plainComments.forEach(comment => {
      if (comment.parentCommentId) {
        const parent = commentMap[comment.parentCommentId.toString()];
        if (parent) {
          parent.replies.push(comment);
          // Sortiraj replies unutar svakog roditelja po datumu
          parent.replies.sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt));
        } else {
          // fallback ako roditelj ne postoji
          rootComments.push(comment);
        }
      } else {
        rootComments.push(comment);
      }
    });

    // Sortiraj root komentare po datumu rasta (najstariji prvi)
    rootComments.sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt));

    res.json({ comments: rootComments });

  } catch (err) {
    console.error('Error fetching comments:', err);
    res.status(500).json({ msg: 'Error fetching comments', error: err.message });
  }
};

exports.createComment = async (req, res) => {
  try {
    const { content, parentCommentId, isSimulated, simulationId } = req.body;
    const { newsItemId } = req.params;

    const hasUser = !!req.user;
    const userId = hasUser ? req.user.id : null;

    if (!content || !content.trim()) {
      return res.status(400).json({ msg: 'Content is required' });
    }

    // Ako nije ulogovan: dozvoli samo simulated top-level komentar
    if (!hasUser) {
      if (!isSimulated) {
        return res.status(401).json({ msg: 'Authentication required for non-simulated comments.' });
      }
      if (parentCommentId) {
        return res.status(400).json({ msg: 'Simulated comments cannot be replies.' });
      }
    }

    // (ovo može ostati, i dalje štiti web app)
    if (isSimulated && parentCommentId) {
      return res.status(400).json({ msg: 'Simulated comments cannot be replies.' });
    }

    // Validacija parent-a ostaje, ali će se aktivirati samo za web app (ulogovani)
    if (parentCommentId) {
      const mongoose = require('mongoose');
      if (!mongoose.Types.ObjectId.isValid(parentCommentId)) {
        return res.status(400).json({ msg: 'Invalid parent comment ID' });
      }

      const parent = await Comment.findById(parentCommentId);
      if (!parent || parent.parentCommentId) {
        return res.status(400).json({ msg: 'Replies can only be made to top-level comments.' });
      }
    }

    const comment = new Comment({
      userId,
      newsItemId,
      content: content.trim(),
      parentCommentId: parentCommentId || null,
      isSimulated: !!isSimulated,
      simulationId: simulationId || (hasUser ? null : 'MAP')
    });

    await comment.save();

    // samo za prave korisnike vodi metrics u commentedBy
    if (hasUser) {
      const newsItem = await NewsItem.findById(newsItemId);
      if (newsItem && !newsItem.commentedBy.includes(userId)) {
        newsItem.commentedBy.push(userId);
        await newsItem.save();
      }
    }

    await updateNewsMetrics(newsItemId);

    const populatedComment = await Comment.findById(comment._id).populate('userId', 'username');
    return res.status(201).json(populatedComment);

  } catch (err) {
    console.error(' Error creating comment:', err);
    return res.status(500).json({ msg: 'Error creating comment', error: err.message });
  }
};



// Update a comment
exports.updateComment = async (req, res) => {
    try {
        const { content } = req.body;
        const { id } = req.params;
        const userId = req.user.id; 

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
  

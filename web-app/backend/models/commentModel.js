const mongoose = require('mongoose');
const { Schema } = mongoose;

const commentSchema = new Schema({
    userId: { type: Schema.Types.ObjectId,  ref: 'User' },
    newsItemId: { type: Schema.Types.ObjectId, ref: 'NewsItem' },
    content: { type: String, required: true },
    parentCommentId: { type: Schema.Types.ObjectId, ref: 'Comment', default: null },
    isSimulated: { type: Boolean, default: false },
    simulationId: { type: String, default: null },
    ownerKey: { type: String, default: null }
}, { timestamps: true }); // default createdAt i updatedAt

module.exports = mongoose.model('Comment', commentSchema);

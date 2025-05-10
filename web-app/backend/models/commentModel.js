const mongoose = require('mongoose');
const { Schema } = mongoose;

const commentSchema = new Schema({
    userId: { type: Schema.Types.ObjectId,  ref: 'User' },
    newsItemId: { type: Schema.Types.ObjectId, ref: 'NewsItem' },
    content: String,
   
}, { timestamps: true }); // default createdAt i updatedAt

module.exports = mongoose.model('Comment', commentSchema);

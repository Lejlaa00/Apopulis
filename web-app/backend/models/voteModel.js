const mongoose = require('mongoose');
const { Schema } = mongoose;

const voteSchema = new Schema({
    userId: { type: Schema.Types.ObjectId, ref: 'User' },
    newsItemId: { type: Schema.Types.ObjectId, ref: 'NewsItem' },
    type: { type: String, enum: ['UP', 'DOWN'] },
}, { timestamps: true }); //votedAt iz .docs --> default mongoose-createdAt

module.exports = mongoose.model('Vote', voteSchema);

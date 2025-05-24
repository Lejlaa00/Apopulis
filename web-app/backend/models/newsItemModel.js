const mongoose = require('mongoose');
const {Schema} =mongoose;

const newsItemSchema = new Schema({
    title: String,
    summary: String,
    content: String,
    author: String,
    publishedAt: Date,
    sourceId: { type: Schema.Types.ObjectId, ref: 'Source' },
    locationId: { type: Schema.Types.ObjectId, ref: 'Location' },
    categoryId: { type: Schema.Types.ObjectId, ref: 'Category' },
    url: String,
    imageUrl: String,
    views: { type: Number, default: 0 },
    likes: { type: Number, default: 0 },
    dislikes: { type: Number, default: 0 },
    commentsCount: { type: Number, default: 0 },
    bookmarks: { type: Number, default: 0 },    
    cachedPopularityScore: { type: Number, default: null },
    keywords: [String],
}, { timestamps: true }); // default createdAt i updatedAt

module.exports = mongoose.model('NewsItem', newsItemSchema);
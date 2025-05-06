const mongoose = require('mongoose');
const {Schema} =mongoose;

const newsItemSchema = new Schema({
    title: String,
    summary: String,
    content: String,
    publishedAt: Date,
    sourceId: { type: Schema.Types.ObjectId, ref: 'Source' },
    locationId: { type: Schema.Types.ObjectId, ref: 'Location' },
    categoryId: { type: Schema.Types.ObjectId, ref: 'Category' },
    url: String
}, { timestamps: true }); // default createdAt i updatedAt

module.exports = mongoose.model('NewsItem', newsItemSchema);
const cron = require('node-cron');
const NewsItem = require('../models/newsItemModel');
const { calculatePopularity } = require('../utils/popularity');

// Helper to recalculate and cache popularity scores
async function recalculateAndCachePopularity() {
    console.log('Starting popularity score calculation...');
    try {
        // Get max values from all news
        const maxValues = {
            views: await NewsItem.find().sort({ views: -1 }).limit(1).then(d => d[0]?.views || 1),
            likes: await NewsItem.find().sort({ likes: -1 }).limit(1).then(d => d[0]?.likes || 1),
            comments: await NewsItem.find().sort({ commentsCount: -1 }).limit(1).then(d => d[0]?.commentsCount || 1),
            bookmarks: await NewsItem.find().sort({ bookmarks: -1 }).limit(1).then(d => d[0]?.bookmarks || 1),
        };

        const articles = await NewsItem.find({
            publishedAt: { $gte: new Date(Date.now() - 2 * 24 * 60 * 60 * 1000) }
        });
      
        console.log("Found articles:", articles.map(a => ({ id: a._id, publishedAt: a.publishedAt })));

        for (const article of articles) {
            const score = calculatePopularity(article, maxValues);
            await NewsItem.findByIdAndUpdate(article._id, { cachedPopularityScore: score });
        }

        console.log(`Updated popularity scores for ${articles.length} articles.`);
    } catch (err) {
        console.error('Error updating popularity scores:', err);
    }
}

// Schedule it to run every hour
cron.schedule('0 * * * *', () => {
    recalculateAndCachePopularity();
});

module.exports = recalculateAndCachePopularity;

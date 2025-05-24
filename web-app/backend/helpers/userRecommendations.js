const NewsItem = require('../models/newsItemModel');
const { getUserTopCategories } = require('./userStats');

async function getUserTopInterests(userId) {
    const maxPerType = 20;

    const liked = await NewsItem.find({ likedBy: userId }).limit(maxPerType);
    const viewed = await NewsItem.find({ viewedBy: userId }).limit(maxPerType);
    const commented = await NewsItem.find({ commentedBy: userId }).limit(maxPerType);

    const all = [...liked, ...viewed, ...commented];

    const keywordCounts = {};

    for (const item of all) {
        (item.keywords || []).forEach(k => {
            keywordCounts[k] = (keywordCounts[k] || 0) + 1;
        });
    }

    const topKeywords = Object.entries(keywordCounts)
        .sort((a, b) => b[1] - a[1])
        .slice(0, 5)
        .map(([keyword]) => keyword);

    const topCategories = await getUserTopCategories(userId);

    return { topKeywords, topCategories };
}

module.exports = { getUserTopInterests };


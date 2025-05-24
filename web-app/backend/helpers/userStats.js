// helpers/userStats.js
const NewsItem = require('../models/newsItemModel');

async function getUserTopCategories(userId) {
    // Pronađi novosti koje je user lajkovao, komentarisao ili gledao
    const liked = await NewsItem.find({ likedBy: userId });
    const viewed = await NewsItem.find({ viewedBy: userId });
    const commented = await NewsItem.find({ commentedBy: userId });

    const all = [...liked, ...viewed, ...commented];

    const counts = {};

    for (const item of all) {
        const cat = item.categoryId?.toString();
        if (!cat) continue;
        counts[cat] = (counts[cat] || 0) + 1;
    }

    const sorted = Object.entries(counts)
        .sort((a, b) => b[1] - a[1])
        .slice(0, 3)
        .map(([catId]) => catId);

    return sorted;
}
module.exports = { getUserTopCategories };

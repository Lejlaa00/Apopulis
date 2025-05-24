function calculateRecencyScore(publishedAt) {
    const daysOld = Math.floor((Date.now() - new Date(publishedAt)) / (1000 * 60 * 60 * 24));
    return 1 / (1 + daysOld); 
}

function normalize(value, min, max) {
    if (max === min) return 0; 
    return (value - min) / (max - min);
}

function calculatePopularity(article, maxValues) {
    const allZero =
        article.views === 0 &&
        article.likes === 0 &&
        article.dislikes === 0 &&
        article.commentsCount === 0 &&
        article.bookmarks === 0;

    if (allZero) return 0;

    const viewsNorm = normalize(article.views, 0, maxValues.views);
    const likesNorm = normalize(article.likes - article.dislikes, -maxValues.likes, maxValues.likes);
    const commentsNorm = normalize(article.commentsCount, 0, maxValues.comments);
    const bookmarksNorm = normalize(article.bookmarks, 0, maxValues.bookmarks);
    const recencyScore = calculateRecencyScore(article.publishedAt);

    const score = (
        0.3 * viewsNorm +
        0.25 * likesNorm +
        0.2 * commentsNorm +
        0.1 * bookmarksNorm +
        0.15 * recencyScore
    );

    return Number(score.toFixed(4));
}


module.exports = { calculatePopularity };
  
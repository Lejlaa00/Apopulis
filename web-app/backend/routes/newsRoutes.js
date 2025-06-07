const express = require('express');
const router = express.Router();
const { 
    getNews, 
    getNewsById, 
    createNews, 
    updateNews, 
    deleteNews,
    getNewsByLocation,
    trackView,
    getPopularityScore
} = require('../controllers/newsController');
const NewsItem = require('../models/newsItemModel'); 
const { getRecommendedNews } = require('../controllers/newsController');
const authMiddleware = require('../middleware/authMiddleware');



router.get('/recommended', authMiddleware, getRecommendedNews);

//Trending route
router.get('/trending', async (req, res) => {
    try {
        const { category } = req.query;
        const filter = {
            publishedAt: { $gte: new Date(Date.now() - 2 * 24 * 60 * 60 * 1000) }
        };
        if (category) filter.category = category;

        const trendingNews = await NewsItem.find(filter)
            .sort({ cachedPopularityScore: -1 })
            .limit(20)
            .populate('sourceId')
            .exec();

        res.json({ news: trendingNews });
    } catch (err) {
        console.error('Error fetching trending news:', err);
        res.status(500).json({ msg: 'Failed to load trending news' });
    }
});

//Popularity score route for one newsItem
router.get('/:id/popularity', getPopularityScore);

// Create a view
router.post('/:id/view', trackView);

// Get all news with optional filtering
router.get('/', getNews);

// Get news by location
router.get('/location/:locationId', getNewsByLocation);

// Get a single news item
router.get('/:id', getNewsById);

// Create a new news item
router.post('/', createNews);

// Update a news item
router.put('/:id', updateNews);

// Delete a news item
router.delete('/:id', deleteNews);


module.exports = router;

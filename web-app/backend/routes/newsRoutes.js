const express = require('express');
const router = express.Router();
const { 
    getNews, 
    getNewsById, 
    createNews, 
    updateNews, 
    deleteNews,
    getNewsByLocation,
    trackView
} = require('../controllers/newsController');
router.post('/:id/view', trackView);


// Get all news with optional filtering
router.get('/', getNews);

// Get news by location
router.get('/location/:locationId', getNewsByLocation);

// Get a single news item
router.get('/:id', getNewsById);

// Create a view
router.post('/:id/view', trackView);

// Create a new news item
router.post('/', createNews);

// Update a news item
router.put('/:id', updateNews);

// Delete a news item
router.delete('/:id', deleteNews);

module.exports = router;

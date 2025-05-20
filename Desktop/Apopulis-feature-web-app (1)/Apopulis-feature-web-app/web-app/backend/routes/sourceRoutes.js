const express = require('express');
const router = express.Router();
const { 
    getSources,
    getSourcesByType,
    getSourceById,
    createSource,
    updateSource,
    deleteSource
} = require('../controllers/sourceController');

// Get all sources
router.get('/', getSources);

// Get sources by scraper type
router.get('/type/:scraperType', getSourcesByType);

// Get a single source
router.get('/:id', getSourceById);

// Create a new source
router.post('/', createSource);

// Update a source
router.put('/:id', updateSource);

// Delete a source
router.delete('/:id', deleteSource);

module.exports = router;

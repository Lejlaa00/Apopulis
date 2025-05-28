const express = require('express');
const router = express.Router();
const statsController = require('../controllers/statsController');

// Handle all stats types through a single route with type parameter
router.post('/:type', statsController.getStats);

module.exports = router;

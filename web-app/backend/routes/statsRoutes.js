const express = require('express');
const router = express.Router();
const statsController = require('../controllers/statsController');
const authMiddleware = require('../middleware/authMiddleware');
const { getStats, getCombinedUserCategoryData } = require('../controllers/statsController');



// Pie chart - user
router.post('/user-interest-compass-pie', authMiddleware, async (req, res) => {
    try {
        const { startDate, endDate } = req.body;
        const data = await getCombinedUserCategoryData(req.user.id, startDate, endDate);
        res.json({ pieData: data.pieData });
    } catch (err) {
        console.error('Error in /user-interest-compass-pie:', err);
        res.status(500).json({ error: 'Failed to get pie data' });
    }
});

// Radar chart - user
router.post('/user-interest-compass-radar', authMiddleware, async (req, res) => {
    try {
        const { startDate, endDate } = req.body;
        const data = await getCombinedUserCategoryData(req.user.id, startDate, endDate);
        res.json({ radarData: data.radarData });
    } catch (err) {
        console.error('Error in /user-interest-compass-radar:', err);
        res.status(500).json({ error: 'Failed to get radar data' });
    }
});

// Global charts
router.post('/:type', getStats);

module.exports = router;

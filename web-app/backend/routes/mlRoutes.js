const express = require('express');
const router = express.Router();
const multer = require('multer');
const mlController = require('../controllers/mlController');

// Configure multer for memory storage (buffer)
const upload = multer({
    storage: multer.memoryStorage(),
    limits: {
        fileSize: 10 * 1024 * 1024, // 10MB limit
    },
    fileFilter: (req, file, cb) => {
        // Accept images only
        if (!file.mimetype.startsWith('image/')) {
            return cb(new Error('Only image files are allowed'), false);
        }
        cb(null, true);
    }
});

// POST /api/ml/predict - Upload image and get fake/real prediction
router.post('/predict', upload.single('image'), mlController.predictImage);

module.exports = router;


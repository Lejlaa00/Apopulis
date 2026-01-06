const axios = require('axios');
const FormData = require('form-data');

// ML Service URL
const ML_SERVICE_URL = process.env.ML_SERVICE_URL || 'http://127.0.0.1:8000';

/**
 * Predict if an image is real or fake using the ML service
 * Expects multipart/form-data with an image file
 */
const predictImage = async (req, res) => {
    try {
        // Check if file was uploaded
        if (!req.file) {
            return res.status(400).json({ 
                error: 'No image file provided',
                message: 'Please upload an image file' 
            });
        }

        // Create form data to send to ML service
        const formData = new FormData();
        formData.append('file', req.file.buffer, {
            filename: req.file.originalname,
            contentType: req.file.mimetype
        });

        // Forward request to ML service
        const mlResponse = await axios.post(`${ML_SERVICE_URL}/predict`, formData, {
            headers: {
                ...formData.getHeaders(),
            },
            timeout: 30000, // 30 second timeout
        });

        // Return ML service response
        res.json({
            success: true,
            data: mlResponse.data
        });

    } catch (error) {
        console.error('ML prediction error:', error.message);
        
        // Handle different error types
        if (error.response) {
            // ML service returned an error
            return res.status(error.response.status).json({
                error: 'ML service error',
                message: error.response.data.detail || 'Prediction failed',
                details: error.response.data
            });
        } else if (error.code === 'ECONNREFUSED') {
            // ML service is not running
            return res.status(503).json({
                error: 'ML service unavailable',
                message: 'The ML service is not running. Please start it first.'
            });
        } else if (error.code === 'ETIMEDOUT') {
            // Request timed out
            return res.status(504).json({
                error: 'Request timeout',
                message: 'The prediction request took too long'
            });
        } else {
            // Other errors
            return res.status(500).json({
                error: 'Internal server error',
                message: error.message
            });
        }
    }
};

module.exports = {
    predictImage
};


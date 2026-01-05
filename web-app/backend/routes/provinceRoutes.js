const express = require('express');
const router = express.Router();
const provinceController = require('../controllers/provinceController');

// Get province news statistics
router.get('/stats/news', provinceController.getProvinceNewsStats);

// Get all provinces
router.get('/', provinceController.getAllProvinces);

// Get a single province by ID
router.get('/:id', provinceController.getProvince);

// Create a new province
router.post('/', provinceController.createProvince);

// Update a province
router.put('/:id', provinceController.updateProvince);

// Delete a province
router.delete('/:id', provinceController.deleteProvince);

module.exports = router; 
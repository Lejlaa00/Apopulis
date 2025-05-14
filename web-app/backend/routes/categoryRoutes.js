const express = require('express');
const router = express.Router();
const { 
    getCategories, 
    getCategoryById, 
    createCategory, 
    updateCategory, 
    deleteCategory 
} = require('../controllers/categoryController');

// Get all categories
router.get('/', getCategories);

// Get a single category
router.get('/:id', getCategoryById);

// Create a new category
router.post('/', createCategory);

// Update a category
router.put('/:id', updateCategory);

// Delete a category
router.delete('/:id', deleteCategory);

module.exports = router;

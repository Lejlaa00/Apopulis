const Category = require('../models/categoryModel');

// Get all categories
exports.getCategories = async (req, res) => {
    try {
        const categories = await Category.find().sort({ name: 1 });
        res.json(categories);
    } catch (err) {
        console.error('Error fetching categories:', err);
        res.status(500).json({ msg: 'Error fetching categories', error: err.message });
    }
};

// Get a single category
exports.getCategoryById = async (req, res) => {
    try {
        const category = await Category.findById(req.params.id);
        
        if (!category) {
            return res.status(404).json({ msg: 'Category not found' });
        }

        res.json(category);
    } catch (err) {
        console.error('Error fetching category:', err);
        res.status(500).json({ msg: 'Error fetching category', error: err.message });
    }
};

// Create a new category
exports.createCategory = async (req, res) => {
    try {
        const { name } = req.body;

        // Check if category already exists
        const existingCategory = await Category.findOne({ name });
        if (existingCategory) {
            return res.status(400).json({ msg: 'Category already exists' });
        }

        const category = new Category({ name });
        await category.save();

        res.status(201).json(category);
    } catch (err) {
        console.error('Error creating category:', err);
        res.status(500).json({ msg: 'Error creating category', error: err.message });
    }
};

// Update a category
exports.updateCategory = async (req, res) => {
    try {
        const { name } = req.body;

        // Check if new name already exists for another category
        const existingCategory = await Category.findOne({ 
            name, 
            _id: { $ne: req.params.id } 
        });
        if (existingCategory) {
            return res.status(400).json({ msg: 'Category name already exists' });
        }

        const category = await Category.findByIdAndUpdate(
            req.params.id,
            { name },
            { new: true }
        );

        if (!category) {
            return res.status(404).json({ msg: 'Category not found' });
        }

        res.json(category);
    } catch (err) {
        console.error('Error updating category:', err);
        res.status(500).json({ msg: 'Error updating category', error: err.message });
    }
};

// Delete a category
exports.deleteCategory = async (req, res) => {
    try {
        const category = await Category.findByIdAndDelete(req.params.id);

        if (!category) {
            return res.status(404).json({ msg: 'Category not found' });
        }

        res.json({ msg: 'Category deleted successfully' });
    } catch (err) {
        console.error('Error deleting category:', err);
        res.status(500).json({ msg: 'Error deleting category', error: err.message });
    }
};

const Province = require('../models/provinceModel');

// Get all provinces
exports.getAllProvinces = async (req, res) => {
    try {
        const provinces = await Province.find();
        res.status(200).json(provinces);
    } catch (error) {
        res.status(500).json({ message: error.message });
    }
};

// Get a single province
exports.getProvince = async (req, res) => {
    try {
        const province = await Province.findById(req.params.id);
        if (!province) {
            return res.status(404).json({ message: 'Province not found' });
        }
        res.status(200).json(province);
    } catch (error) {
        res.status(500).json({ message: error.message });
    }
};

// Create a new province
exports.createProvince = async (req, res) => {
    try {
        const province = new Province(req.body);
        const newProvince = await province.save();
        res.status(201).json(newProvince);
    } catch (error) {
        res.status(400).json({ message: error.message });
    }
};

// Update a province
exports.updateProvince = async (req, res) => {
    try {
        const province = await Province.findByIdAndUpdate(
            req.params.id,
            req.body,
            { new: true, runValidators: true }
        );
        if (!province) {
            return res.status(404).json({ message: 'Province not found' });
        }
        res.status(200).json(province);
    } catch (error) {
        res.status(400).json({ message: error.message });
    }
};

// Delete a province
exports.deleteProvince = async (req, res) => {
    try {
        const province = await Province.findByIdAndDelete(req.params.id);
        if (!province) {
            return res.status(404).json({ message: 'Province not found' });
        }
        res.status(200).json({ message: 'Province deleted successfully' });
    } catch (error) {
        res.status(500).json({ message: error.message });
    }
}; 
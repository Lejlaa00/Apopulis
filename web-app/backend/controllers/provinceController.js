const Province = require('../models/provinceModel');
const Location = require('../models/locationModel');
const NewsItem = require('../models/newsItemModel');

// Get all provinces
exports.getAllProvinces = async (req, res) => {
    try {
        const provinces = await Province.find();
        res.status(200).json(provinces);
    } catch (error) {
        res.status(500).json({ message: error.message });
    }
};

// Get news count by province with center coordinates
exports.getProvinceNewsStats = async (req, res) => {
    try {
        const { hours = 24 } = req.query;
        const timeAgo = new Date(Date.now() - hours * 60 * 60 * 1000);

        const provinces = await Province.find();
        const provinceStats = [];

        for (const province of provinces) {
            // Get all locations in this province
            const locations = await Location.find({ province: province._id });
            
            if (locations.length === 0) {
                continue;
            }

            // Calculate center point as average of all locations
            const centerLat = locations.reduce((sum, loc) => sum + loc.latitude, 0) / locations.length;
            const centerLon = locations.reduce((sum, loc) => sum + loc.longitude, 0) / locations.length;

            // Count news items for this province
            const locationIds = locations.map(loc => loc._id);
            const newsCount = await NewsItem.countDocuments({
                locationId: { $in: locationIds },
                publishedAt: { $gte: timeAgo }
            });

            if (newsCount > 0) {
                provinceStats.push({
                    provinceId: province._id,
                    provinceName: province.name,
                    provinceCode: province.code,
                    centerLatitude: centerLat,
                    centerLongitude: centerLon,
                    newsCount: newsCount
                });
            }
        }

        res.status(200).json(provinceStats);
    } catch (error) {
        console.error('Error fetching province news stats:', error);
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
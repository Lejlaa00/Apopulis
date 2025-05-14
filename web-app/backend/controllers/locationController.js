const Location = require('../models/locationModel');

// Get all locations
exports.getLocations = async (req, res) => {
    try {
        const locations = await Location.find().sort({ name: 1 });
        res.json(locations);
    } catch (err) {
        console.error('Error fetching locations:', err);
        res.status(500).json({ msg: 'Error fetching locations', error: err.message });
    }
};

// Get locations by region
exports.getLocationsByRegion = async (req, res) => {
    try {
        const { region } = req.params;
        const locations = await Location.find({ region }).sort({ name: 1 });
        res.json(locations);
    } catch (err) {
        console.error('Error fetching locations by region:', err);
        res.status(500).json({ msg: 'Error fetching locations by region', error: err.message });
    }
};

// Get a single location
exports.getLocationById = async (req, res) => {
    try {
        const location = await Location.findById(req.params.id);
        
        if (!location) {
            return res.status(404).json({ msg: 'Location not found' });
        }

        res.json(location);
    } catch (err) {
        console.error('Error fetching location:', err);
        res.status(500).json({ msg: 'Error fetching location', error: err.message });
    }
};

// Create a new location
exports.createLocation = async (req, res) => {
    try {
        const { name, latitude, longitude, region } = req.body;

        // Check if location already exists
        const existingLocation = await Location.findOne({ name, region });
        if (existingLocation) {
            return res.status(400).json({ msg: 'Location already exists in this region' });
        }

        const location = new Location({ 
            name, 
            latitude, 
            longitude, 
            region 
        });
        await location.save();

        res.status(201).json(location);
    } catch (err) {
        console.error('Error creating location:', err);
        res.status(500).json({ msg: 'Error creating location', error: err.message });
    }
};

// Update a location
exports.updateLocation = async (req, res) => {
    try {
        const { name, latitude, longitude, region } = req.body;

        // Check if new name already exists for another location in the same region
        const existingLocation = await Location.findOne({ 
            name, 
            region,
            _id: { $ne: req.params.id } 
        });
        if (existingLocation) {
            return res.status(400).json({ msg: 'Location name already exists in this region' });
        }

        const location = await Location.findByIdAndUpdate(
            req.params.id,
            { name, latitude, longitude, region },
            { new: true }
        );

        if (!location) {
            return res.status(404).json({ msg: 'Location not found' });
        }

        res.json(location);
    } catch (err) {
        console.error('Error updating location:', err);
        res.status(500).json({ msg: 'Error updating location', error: err.message });
    }
};

// Delete a location
exports.deleteLocation = async (req, res) => {
    try {
        const location = await Location.findByIdAndDelete(req.params.id);

        if (!location) {
            return res.status(404).json({ msg: 'Location not found' });
        }

        res.json({ msg: 'Location deleted successfully' });
    } catch (err) {
        console.error('Error deleting location:', err);
        res.status(500).json({ msg: 'Error deleting location', error: err.message });
    }
};

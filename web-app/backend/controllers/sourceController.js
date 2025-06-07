const Source = require('../models/sourceModel');

// Get all sources
exports.getSources = async (req, res) => {
    try {
        const sources = await Source.find().sort({ name: 1 });
        res.json(sources);
    } catch (err) {
        console.error('Error fetching sources:', err);
        res.status(500).json({ msg: 'Error fetching sources', error: err.message });
    }
};

// Get sources by scraper type
exports.getSourcesByType = async (req, res) => {
    try {
        const { scraperType } = req.params;
        const sources = await Source.find({ scraperType }).sort({ name: 1 });
        res.json(sources);
    } catch (err) {
        console.error('Error fetching sources by type:', err);
        res.status(500).json({ msg: 'Error fetching sources by type', error: err.message });
    }
};

// Get a single source
exports.getSourceById = async (req, res) => {
    try {
        const source = await Source.findById(req.params.id);
        
        if (!source) {
            return res.status(404).json({ msg: 'Source not found' });
        }

        res.json(source);
    } catch (err) {
        console.error('Error fetching source:', err);
        res.status(500).json({ msg: 'Error fetching source', error: err.message });
    }
};

// Create a new source
exports.createSource = async (req, res) => {
    try {
        const { name, url, scraperType } = req.body;

        // Check if source already exists
        const existingSource = await Source.findOne({ 
            $or: [{ name }, { url }] 
        });
        if (existingSource) {
            return res.status(400).json({ 
                msg: existingSource.name === name 
                    ? 'Source name already exists' 
                    : 'Source URL already exists' 
            });
        }

        const source = new Source({ 
            name, 
            url, 
            scraperType 
        });
        await source.save();

        res.status(201).json(source);
    } catch (err) {
        console.error('Error creating source:', err);
        res.status(500).json({ msg: 'Error creating source', error: err.message });
    }
};

// Update a source
exports.updateSource = async (req, res) => {
    try {
        const { name, url, scraperType } = req.body;

        // Check if new name or url already exists for another source
        const existingSource = await Source.findOne({ 
            $or: [
                { name, _id: { $ne: req.params.id } },
                { url, _id: { $ne: req.params.id } }
            ]
        });
        if (existingSource) {
            return res.status(400).json({ 
                msg: existingSource.name === name 
                    ? 'Source name already exists' 
                    : 'Source URL already exists' 
            });
        }

        const source = await Source.findByIdAndUpdate(
            req.params.id,
            { name, url, scraperType },
            { new: true }
        );

        if (!source) {
            return res.status(404).json({ msg: 'Source not found' });
        }

        res.json(source);
    } catch (err) {
        console.error('Error updating source:', err);
        res.status(500).json({ msg: 'Error updating source', error: err.message });
    }
};

// Delete a source
exports.deleteSource = async (req, res) => {
    try {
        const source = await Source.findByIdAndDelete(req.params.id);

        if (!source) {
            return res.status(404).json({ msg: 'Source not found' });
        }

        res.json({ msg: 'Source deleted successfully' });
    } catch (err) {
        console.error('Error deleting source:', err);
        res.status(500).json({ msg: 'Error deleting source', error: err.message });
    }
};

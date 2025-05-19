const NewsItem = require('../models/newsItemModel');

// Get all news items with optional filtering
exports.getNews = async (req, res) => {
    try {
        const { category, location, source, page = 1, limit = 10 } = req.query;
        const query = {};

        if (category) query.categoryId = category;
        if (location) query.locationId = location;
        if (source) query.sourceId = source;

        const skip = (page - 1) * limit;

        const news = await NewsItem.find(query)
            .populate('sourceId', 'name url')
            .populate('locationId', 'name')
            .populate('categoryId', 'name')
            .sort({ publishedAt: -1 })
            .skip(skip)
            .limit(parseInt(limit));

        const total = await NewsItem.countDocuments(query);

        res.json({
            news,
            currentPage: parseInt(page),
            totalPages: Math.ceil(total / limit),
            totalItems: total
        });
    } catch (err) {
        console.error('Error fetching news:', err);
        res.status(500).json({ msg: 'Error fetching news', error: err.message });
    }
};

// Get a single news item by ID
exports.getNewsById = async (req, res) => {
    try {
        const newsItem = await NewsItem.findById(req.params.id)
            .populate('sourceId', 'name url')
            .populate('locationId', 'name')
            .populate('categoryId', 'name');

        if (!newsItem) {
            return res.status(404).json({ msg: 'News item not found' });
        }

        res.json(newsItem);
    } catch (err) {
        console.error('Error fetching news item:', err);
        res.status(500).json({ msg: 'Error fetching news item', error: err.message });
    }
};

// Create a new news item
exports.createNews = async (req, res) => {
    try {
        const { title, summary, content, publishedAt, sourceId, locationId, categoryId, url } = req.body;

        const newsItem = new NewsItem({
            title,
            summary,
            content,
            publishedAt: publishedAt || new Date(),
            sourceId,
            locationId,
            categoryId,
            url
        });

        await newsItem.save();
        res.status(201).json(newsItem);
    } catch (err) {
        console.error('Error creating news item:', err);
        res.status(500).json({ msg: 'Error creating news item', error: err.message });
    }
};

// Update a news item
exports.updateNews = async (req, res) => {
    try {
        const { title, summary, content, publishedAt, sourceId, locationId, categoryId, url } = req.body;

        const newsItem = await NewsItem.findByIdAndUpdate(
            req.params.id,
            {
                title,
                summary,
                content,
                publishedAt,
                sourceId,
                locationId,
                categoryId,
                url
            },
            { new: true }
        );

        if (!newsItem) {
            return res.status(404).json({ msg: 'News item not found' });
        }

        res.json(newsItem);
    } catch (err) {
        console.error('Error updating news item:', err);
        res.status(500).json({ msg: 'Error updating news item', error: err.message });
    }
};

// Delete a news item
exports.deleteNews = async (req, res) => {
    try {
        const newsItem = await NewsItem.findByIdAndDelete(req.params.id);

        if (!newsItem) {
            return res.status(404).json({ msg: 'News item not found' });
        }

        res.json({ msg: 'News item deleted successfully' });
    } catch (err) {
        console.error('Error deleting news item:', err);
        res.status(500).json({ msg: 'Error deleting news item', error: err.message });
    }
};

// Get news items by location ID
exports.getNewsByLocation = async (req, res) => {
    try {
        const { locationId } = req.params;
        const { page = 1, limit = 10 } = req.query;
        const skip = (page - 1) * limit;

        const news = await NewsItem.find({ locationId })
            .populate('sourceId', 'name url')
            .populate('locationId', 'name')
            .populate('categoryId', 'name')
            .sort({ publishedAt: -1 })
            .skip(skip)
            .limit(parseInt(limit));

        const total = await NewsItem.countDocuments({ locationId });

        res.json({
            news,
            currentPage: parseInt(page),
            totalPages: Math.ceil(total / limit),
            totalItems: total
        });
    } catch (err) {
        console.error('Error fetching news by location:', err);
        res.status(500).json({ msg: 'Error fetching news by location', error: err.message });
    }
};

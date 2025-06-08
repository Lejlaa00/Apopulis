const NewsItem = require('../models/newsItemModel');
const Comment = require('../models/commentModel');
const Vote = require('../models/voteModel');
const { getUserTopInterests } = require('../helpers/userRecommendations');
const { calculatePopularity } = require('../utils/popularity');
const jwt = require('jsonwebtoken');
const cron = require('node-cron');

// Cache for news summary
let summaryCache = {
    summary: null,
    timestamp: null,
    newsCount: 0
};

// Function to generate summary
async function generateNewsSummary() {
    try {
        const todaysNews = await NewsItem.find()
            .sort({ publishedAt: -1 })
            .select('title content')
            .limit(10);

        if (todaysNews.length === 0) {
            summaryCache = {
                summary: 'No news items found for today.',
                timestamp: new Date(),
                newsCount: 0
            };
            return;
        }

        const newsContent = todaysNews.map(news => 
            `Title: ${news.title}\nContent: ${news.content || 'No content available'}\n`
        ).join('\n');

        const OpenAI = require('openai');
        const client = new OpenAI({
            apiKey: process.env.OPENAI_API_KEY || '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'
        });

        const completion = await client.chat.completions.create({
            model: 'gpt-3.5-turbo',
            messages: [
                {
                    role: 'system',
                    content: 'You are a professional news summarizer. Create a comprehensive summary of today\'s news articles. Focus on key details and maintain context while ensuring readability, write it in slovene language.'
                },
                {
                    role: 'user',
                    content: `Please create a detailed summary of these news articles. The summary should be around 500-600 words, divided into well-structured paragraphs. Cover the main points of each article while maintaining a coherent narrative:\n${newsContent}`
                }
            ],
            max_tokens: 1000
        });

        summaryCache = {
            summary: completion.choices[0].message.content,
            timestamp: new Date(),
            newsCount: todaysNews.length
        };

        console.log('News summary generated at:', new Date().toLocaleTimeString());
    } catch (err) {
        console.error('Error generating news summary:', err);
    }
}

// Generate summary every 50 minutes
cron.schedule('*/50 * * * *', generateNewsSummary);

// Generate initial summary when server starts
generateNewsSummary();

const ACCESS_TOKEN_SECRET = process.env.ACCESS_TOKEN_SECRET || 'default_access_secret';

async function updateNewsMetrics(newsItemId) {
    const likes = await Vote.countDocuments({ newsItemId, type: 'UP' });
    const dislikes = await Vote.countDocuments({ newsItemId, type: 'DOWN' });
    const commentsCount = await Comment.countDocuments({ newsItemId });

    await NewsItem.findByIdAndUpdate(newsItemId, {
        likes,
        dislikes,
        commentsCount
    });
}

// Get all news items with optional filtering
exports.getNews = async (req, res) => {
    try {
        const { category, location, source, page = 1, limit = 10, search } = req.query;
        const query = {};

        if (category) query.categoryId = category;
        if (location) query.locationId = location;
        if (source) query.sourceId = source;

        if (search) {
        query.$or = [
            { title: { $regex: search, $options: 'i' } },
            { summary: { $regex: search, $options: 'i' } },
            { content: { $regex: search, $options: 'i' } },
            { keywords: { $regex: search, $options: 'i' } }
        ];
    }

        const skip = (page - 1) * limit;

        const news = await NewsItem.find(query)
            .populate('sourceId', 'name url')
            .populate('locationId', 'name')
            .populate('categoryId', 'name')
            .sort({ publishedAt: -1 })
            .skip(skip)

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
        const { title, summary, content, publishedAt, sourceId, locationId, category, url } = req.body;

        const categoryDoc = await Category.findOne({ name: category });

        if (!categoryDoc) {
            return res.status(400).json({ msg: `Category '${category}' does not exist.` });
        }

        const newsItem = new NewsItem({
            title,
            summary,
            content,
            publishedAt: publishedAt || new Date(),
            sourceId,
            locationId,
            categoryId: categoryDoc._id,
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

// Track a view for a specific news item
exports.trackView = async (req, res) => {
    try {
        const newsItem = await NewsItem.findById(req.params.id);
        if (!newsItem) {
            return res.status(404).json({ msg: 'News item not found' });
        }

        const authHeader = req.headers.authorization;
        console.log("authHeader =", authHeader);
        let userId = null;

        if (authHeader && authHeader.startsWith('Bearer ')) {
            const token = authHeader.split(' ')[1];
            try {
                const decoded = jwt.verify(token, ACCESS_TOKEN_SECRET);
                userId = decoded.id;

                console.log("Tracking view, userId =", userId);
            } catch (err) {
                console.warn('Invalid token in view (non-fatal):', err.message);
            }
        }

        newsItem.views = (newsItem.views || 0) + 1;

        if (userId && !newsItem.viewedBy.includes(userId)) {
            newsItem.viewedBy.push(userId);
        }

        await newsItem.save();
        await updateNewsMetrics(newsItem._id);

        res.json({ views: newsItem.views });
    } catch (err) {
        console.error('Error tracking view:', err);
        res.status(500).json({ msg: 'Error tracking view', error: err.message });
    }
  };

exports.getPopularityScore = async (req, res) => {
    try {
        const article = await NewsItem.findById(req.params.id);
        if (!article) return res.status(404).json({ msg: 'News not found' });

        // NewsItem old 2 days
        const twoDaysAgo = new Date(Date.now() - 2 * 24 * 60 * 60 * 1000);
        if (article.publishedAt < twoDaysAgo) {
            return res.json({ popularityScore: null, msg: 'Article is older than 2 days' });
        }

        // Max vlaue from all news
        const maxValues = {
            views: await NewsItem.find().sort({ views: -1 }).limit(1).then(d => d[0]?.views || 1),
            likes: await NewsItem.find().sort({ likes: -1 }).limit(1).then(d => d[0]?.likes || 1),
            comments: await NewsItem.find().sort({ commentsCount: -1 }).limit(1).then(d => d[0]?.commentsCount || 1),
            bookmarks: await NewsItem.find().sort({ bookmarks: -1 }).limit(1).then(d => d[0]?.bookmarks || 1),
        };

        const score = calculatePopularity(article, maxValues);
        res.json({ popularityScore: score });

    } catch (err) {
        console.error('Error calculating popularity:', err);
        res.status(500).json({ msg: 'Internal server error' });
    }
};


exports.getRecommendedNews = async (req, res) => {
    try {
        const userId = req.user.id;
        const { topKeywords, topCategories } = await getUserTopInterests(userId);

        if (topKeywords.length === 0 && topCategories.length === 0) {
            return res.json({ news: [] });
        }

        const query = {
            $or: [
                { keywords: { $in: topKeywords } },
                { categoryId: { $in: topCategories } }
            ]
        };

        const recommended = await NewsItem.find(query)
            .sort({ cachedPopularityScore: -1, publishedAt: -1 })
            .limit(5)
            .populate('sourceId');

        res.json({ news: recommended });
    } catch (err) {
        console.error('Error fetching recommended news:', err);
        res.status(500).json({ msg: 'Error fetching recommended news' });
    }
  };

// Get summary of today's news using OpenAI
exports.getSummary = async (req, res) => {
    try {
        // If no cache exists yet, generate one
        if (!summaryCache.summary) {
            await generateNewsSummary();
        }

        res.json({
            ...summaryCache,
            fromCache: true
        });
    } catch (err) {
        console.error('Error getting news summary:', err);
        res.status(500).json({ 
            msg: 'Error getting news summary', 
            error: err.message 
        });
    }
};

// Get news counts by location for the last 24 hours
exports.getLocationNewsStats = async (req, res) => {
    try {
        const oneDayAgo = new Date(Date.now() - 24 * 60 * 60 * 1000);
          const locationStats = await NewsItem.aggregate([
            {
                $match: {
                    publishedAt: { $gte: oneDayAgo }
                }
            },
            {
                $group: {
                    _id: "$locationId",
                    newsCount: { $sum: 1 }
                }
            },
            {
                $lookup: {
                    from: "locations",
                    localField: "_id",
                    foreignField: "_id",
                    as: "locationInfo"
                }
            },
            {
                $unwind: "$locationInfo"
            },
            {
                $project: {
                    _id: "$_id",
                    name: "$locationInfo.name",
                    latitude: "$locationInfo.latitude",
                    longitude: "$locationInfo.longitude",
                    newsCount: 1
                }
            },
            {
                $sort: { newsCount: -1 }
            }
        ]);

        res.json(locationStats);
    } catch (err) {
        console.error('Error fetching location news stats:', err);
        res.status(500).json({ msg: 'Error fetching location statistics', error: err.message });
    }
};

exports.updateNewsMetrics = updateNewsMetrics;
const NewsItem = require('../models/newsItemModel');
const Category = require('../models/categoryModel');
const Source = require('../models/sourceModel');
const Vote = require('../models/voteModel');
const Comment = require('../models/commentModel');

exports.getStats = async (req, res) => {
    const { type } = req.params;
    const { startDate, endDate } = req.body;

    try {
        let data;
        
        switch (type) {
            case 'popularity-trend':
                data = await getPopularityTrend(startDate, endDate);
                break;
            case 'category-distribution':
                data = await getCategoryDistribution();
                break;
            case 'engagement-by-source':
                data = await getEngagementBySource();
                break;

            default:
                return res.status(400).json({ error: 'Invalid stats type' });
        }


        res.json(data);
    } catch (error) {
        console.error('Stats error:', error);
        res.status(500).json({ error: error.message || 'Error generating statistics' });
    }
};

async function getPopularityTrend(startDate, endDate) {
    try {
        const dateFilter = {};
        if (startDate) dateFilter.createdAt = { $gte: new Date(startDate) };
        if (endDate) dateFilter.createdAt = { ...dateFilter.createdAt, $lte: new Date(endDate) };

        const newsItems = await NewsItem.aggregate([
            { $match: dateFilter },
            {
                $lookup: {
                    from: 'votes',
                    localField: '_id',
                    foreignField: 'newsItemId',
                    as: 'votes'
                }
            },
            {
                $lookup: {
                    from: 'comments',
                    localField: '_id',
                    foreignField: 'newsItemId',
                    as: 'comments'
                }
            },
            {
                $group: {
                    _id: { $dateToString: { format: '%Y-%m-%d', date: '$createdAt' } },
                    popularityScore: {
                        $avg: {
                            $add: [
                                { $size: '$votes' },
                                { $size: '$comments' }
                            ]
                        }
                    }
                }
            },
            { $sort: { '_id': 1 } },
            { $limit: 30 } // Last 30 days
        ]);

        // If no data found, return empty arrays
        if (newsItems.length === 0) {
            return {
                labels: [],
                popularityScores: []
            };
        }

        return {
            labels: newsItems.map(item => item._id),
            popularityScores: newsItems.map(item => item.popularityScore)
        };
    } catch (error) {
        console.error('Error in getPopularityTrend:', error);
        // Return empty arrays on error
        return {
            labels: [],
            popularityScores: []
        };
    }
}

async function getCategoryDistribution() {
    try {
        // First, get all categories to ensure we have their names
        const allCategories = await Category.find();
        const categoryMap = {};
        allCategories.forEach(cat => {
            categoryMap[cat._id.toString()] = cat.name;
        });
        
        // Now aggregate news items by categoryId
        const newsItemsByCategory = await NewsItem.aggregate([
            {
                $group: {
                    _id: '$categoryId',  // Group by categoryId instead of category
                    count: { $sum: 1 }
                }
            },
            { $sort: { count: -1 } }
        ]);

        // Map the results using our category map
        const categoryCounts = newsItemsByCategory.map(item => ({
            categoryId: item._id,
            categoryName: categoryMap[item._id] || 'Uncategorized',
            count: item.count
        }));

        // If no categories found, return empty arrays
        if (categoryCounts.length === 0) {
            return {
                labels: [],
                counts: []
            };
        }

        console.log('Category distribution data:', categoryCounts);

        return {
            labels: categoryCounts.map(cat => cat.categoryName),
            counts: categoryCounts.map(cat => cat.count)
        };
    } catch (error) {
        console.error('Error in getCategoryDistribution:', error);
        // Return empty arrays on error
        return {
            labels: [],
            counts: []
        };
    }
}

async function getEngagementBySource() {
    try {
        // First, get all sources
        const allSources = await Source.find();
        
        // Create a map of existing engagement data
        const sourceEngagement = await NewsItem.aggregate([
            {
                $lookup: {
                    from: 'votes',
                    localField: '_id',
                    foreignField: 'newsItemId',
                    as: 'votes'
                }
            },
            {
                $lookup: {
                    from: 'comments',
                    localField: '_id',
                    foreignField: 'newsItemId',
                    as: 'comments'
                }
            },
            {
                $group: {
                    _id: '$sourceId',
                    totalEngagement: {
                        $sum: {
                            $add: [
                                { $size: '$votes' },
                                { $size: '$comments' },
                                { $ifNull: ['$views', 0] }
                            ]
                        }
                    },
                    count: { $sum: 1 }
                }
            }
        ]);

        // Create a map of source ID to engagement
        const engagementMap = {};
        sourceEngagement.forEach(item => {
            engagementMap[item._id.toString()] = item.totalEngagement;
        });

        // Map all sources, including those with no engagement
        const mappedSourceEngagement = allSources.map(source => ({
            sourceId: source._id,
            sourceName: source.name,
            totalEngagement: engagementMap[source._id.toString()] || 0
        }));

        // Sort by engagement (high to low)
        mappedSourceEngagement.sort((a, b) => b.totalEngagement - a.totalEngagement);

        console.log('Source engagement data:', mappedSourceEngagement);

        return {
            labels: mappedSourceEngagement.map(source => source.sourceName),
            engagement: mappedSourceEngagement.map(source => source.totalEngagement)
        };
    } catch (error) {
        console.error('Error in getEngagementBySource:', error);
        return {
            labels: [],
            engagement: []
        };
    }
}



const mongoose = require('mongoose');

// Importing all models
const Category = require('../models/categoryModel');
const Comment = require('../models/commentModel');
const Location = require('../models/locationModel');
const NewsItem = require('../models/newsitemModel');
const Source = require('../models/sourceModel');
const User = require('../models/userModel');
const Vote = require('../models/voteModel');

// Connecting to MongoDB
mongoose.connect(process.env.MONGODB_URI || 'mongodb://localhost:27017/apopulis', {
});


async function populateDatabase() {
    try {
        await mongoose.connection.dropDatabase();
        console.log('Cleared existing database');

        // Creating test sources
        const source1 = await Source.create({
            name: 'Reuters',
            url: 'https://reuters.com',
            scraperType: 'rss'
        });

        const source2 = await Source.create({
            name: 'AP News',
            url: 'https://apnews.com',
            scraperType: 'api'
        });

        // Creating test locations
        const location1 = await Location.create({
            name: 'New York',
            latitude: 40.7128,
            longitude: -74.0060,
            region: 'Northeast'
        });

        const location2 = await Location.create({
            name: 'London',
            latitude: 51.5074,
            longitude: -0.1278,
            region: 'Europe'
        });

        // Creating test categories
        const category1 = await Category.create({ name: 'Politics' });
        const category2 = await Category.create({ name: 'Technology' });

        // Creating test users
        const user1 = await User.create({
            username: 'john_doe',
            email: 'john@example.com',
            isActive: true
        });

        const user2 = await User.create({
            username: 'jane_smith',
            email: 'jane@example.com',
            isActive: true
        });

        // Creating test news items
        const news1 = await NewsItem.create({
            title: 'Global Summit Concludes',
            summary: 'World leaders reach agreement on climate change',
            content: 'Full story content here...',
            publishedAt: new Date(),
            sourceId: source1._id,  
            locationId: location1._id,
            categoryId: category1._id,
            url: 'https://example.com/news/1'
        });

        const news2 = await NewsItem.create({
            title: 'New AI Breakthrough',
            summary: 'Researchers develop new neural network architecture',
            content: 'Full technical details...',
            publishedAt: new Date(),
            sourceId: source2._id,
            locationId: location2._id,
            categoryId: category2._id,
            url: 'https://example.com/news/2'
        });

        // Creatint test comments
        await Comment.create({
            user: user1._id,
            newsItem: news1._id,
            content: 'This is an important development!'
        });

        await Comment.create({
            user: user2._id,
            newsItem: news2._id,
            content: 'Fascinating research!'
        });

        // Creating test votes
        await Vote.create({
            user: user1._id,
            newsItem: news1._id,
            type: 'UP'
        });

        await Vote.create({
            user: user2._id,
            newsItem: news2._id,
            type: 'DOWN'
        });

        console.log('Database populated successfully!');
        console.log(`
    Created:
    - ${await Source.countDocuments()} sources
    - ${await Location.countDocuments()} locations
    - ${await Category.countDocuments()} categories
    - ${await User.countDocuments()} users
    - ${await NewsItem.countDocuments()} news items
    - ${await Comment.countDocuments()} comments
    - ${await Vote.countDocuments()} votes
    `);

        // Sample query to verify relationships
        const populatedNews = await NewsItem.findById(news1._id)
            .populate('sourceId')
            .populate('locationId')
            .populate('categoryId');

        console.log('🔍 Sample populated news item:', {
            title: populatedNews.title,
            source: populatedNews.sourceId.name,
            location: populatedNews.locationId.name,
            category: populatedNews.categoryId.name
        });

    } catch (error) {
        console.error('Error populating database:', error);
    } finally {
        mongoose.connection.close();
        process.exit(0);
    }
}

populateDatabase();
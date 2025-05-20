const axios = require('axios');
const mongoose = require('mongoose');

const API = 'http://localhost:5000/api';

async function populateDatabase() {
    try {
        // Connect to MongoDB just to clear the database
        await mongoose.connect(process.env.MONGODB_URI || 'mongodb://localhost:27017/apopulis');
        await mongoose.connection.dropDatabase();
        console.log('Cleared existing database');
        await mongoose.connection.close();

        // 1. Create Provinces
        console.log('Creating provinces...');
        const provinces = await Promise.all([
            axios.post(`${API}/provinces`, { name: 'Province A', code: 'PA' }),
            axios.post(`${API}/provinces`, { name: 'Province B', code: 'PB' }),
            axios.post(`${API}/provinces`, { name: 'Province C', code: 'PC' }),
            axios.post(`${API}/provinces`, { name: 'Province D', code: 'PD' }),
            axios.post(`${API}/provinces`, { name: 'Province E', code: 'PE' }),
        ]);
        const provinceIds = provinces.map(r => r.data._id);
        console.log('Provinces created successfully');

        // 2. Create Categories
        console.log('Creating categories...');
        const categories = await Promise.all([
            axios.post(`${API}/categories`, { name: 'Politics' }),
            axios.post(`${API}/categories`, { name: 'Technology' }),
            axios.post(`${API}/categories`, { name: 'Sports' }),
            axios.post(`${API}/categories`, { name: 'Health' }),
            axios.post(`${API}/categories`, { name: 'Business' }),
        ]);
        const categoryIds = categories.map(r => r.data._id);
        console.log('Categories created successfully');

        // 3. Create and login user
        console.log('Creating test user...');
        const userRes = await axios.post(`${API}/users/register`, {
            username: 'testuser',
            email: 'test@example.com',
            password: 'Password1!'
        });
        console.log('User created successfully');

        console.log('Logging in...');
        const loginRes = await axios.post(`${API}/users/login`, {
            username: 'testuser',
            password: 'Password1!'
        });
        const token = loginRes.data.token;
        const authHeader = { headers: { Authorization: `Bearer ${token}` } };
        console.log('Login successful');

        // 4. Create Sources
        console.log('Creating sources...');
        const sources = await Promise.all([
            axios.post(`${API}/sources`, { name: 'Reuters', url: 'https://reuters.com', scraperType: 'rss' }),
            axios.post(`${API}/sources`, { name: 'AP News', url: 'https://apnews.com', scraperType: 'api' }),
            axios.post(`${API}/sources`, { name: 'BBC', url: 'https://bbc.com', scraperType: 'rss' }),
            axios.post(`${API}/sources`, { name: 'CNN', url: 'https://cnn.com', scraperType: 'api' }),
            axios.post(`${API}/sources`, { name: 'Al Jazeera', url: 'https://aljazeera.com', scraperType: 'rss' }),
        ]);
        const sourceIds = sources.map(r => r.data._id);
        console.log('Sources created successfully');

        // 5. Create Locations
        console.log('Creating locations...');
        const locations = await Promise.all([
            axios.post(`${API}/locations`, { name: 'City 1', latitude: 10.1, longitude: 20.1, province: provinceIds[0] }),
            axios.post(`${API}/locations`, { name: 'City 2', latitude: 11.2, longitude: 21.2, province: provinceIds[1] }),
            axios.post(`${API}/locations`, { name: 'City 3', latitude: 12.3, longitude: 22.3, province: provinceIds[2] }),
            axios.post(`${API}/locations`, { name: 'City 4', latitude: 13.4, longitude: 23.4, province: provinceIds[3] }),
            axios.post(`${API}/locations`, { name: 'City 5', latitude: 14.5, longitude: 24.5, province: provinceIds[4] }),
        ]);
        const locationIds = locations.map(r => r.data._id);
        console.log('Locations created successfully');

        // 6. Create News Items
        console.log('Creating news items...');
        const newsItems = await Promise.all([
            axios.post(`${API}/news`, {
                title: 'News 1',
                summary: 'Summary 1',
                content: 'Content 1',
                publishedAt: new Date(),
                sourceId: sourceIds[0],
                locationId: locationIds[0],
                categoryId: categoryIds[0],
                url: 'https://example.com/1'
            }),
            axios.post(`${API}/news`, {
                title: 'News 2',
                summary: 'Summary 2',
                content: 'Content 2',
                publishedAt: new Date(),
                sourceId: sourceIds[1],
                locationId: locationIds[1],
                categoryId: categoryIds[1],
                url: 'https://example.com/2'
            }),
            axios.post(`${API}/news`, {
                title: 'News 3',
                summary: 'Summary 3',
                content: 'Content 3',
                publishedAt: new Date(),
                sourceId: sourceIds[2],
                locationId: locationIds[2],
                categoryId: categoryIds[2],
                url: 'https://example.com/3'
            }),
            axios.post(`${API}/news`, {
                title: 'News 4',
                summary: 'Summary 4',
                content: 'Content 4',
                publishedAt: new Date(),
                sourceId: sourceIds[3],
                locationId: locationIds[3],
                categoryId: categoryIds[3],
                url: 'https://example.com/4'
            }),
            axios.post(`${API}/news`, {
                title: 'News 5',
                summary: 'Summary 5',
                content: 'Content 5',
                publishedAt: new Date(),
                sourceId: sourceIds[4],
                locationId: locationIds[4],
                categoryId: categoryIds[4],
                url: 'https://example.com/5'
            }),
        ]);
        const newsItemIds = newsItems.map(r => r.data._id);
        console.log('News items created successfully');

        // 7. Create Comments
        console.log('Creating comments...');
        await Promise.all([
            axios.post(`${API}/comments/news/${newsItemIds[0]}`, { content: 'Great news!' }, authHeader),
            axios.post(`${API}/comments/news/${newsItemIds[1]}`, { content: 'Interesting.' }, authHeader),
            axios.post(`${API}/comments/news/${newsItemIds[2]}`, { content: 'Wow!' }, authHeader),
            axios.post(`${API}/comments/news/${newsItemIds[3]}`, { content: 'Nice.' }, authHeader),
            axios.post(`${API}/comments/news/${newsItemIds[4]}`, { content: 'Cool.' }, authHeader),
        ]);
        console.log('Comments created successfully');

        // 8. Create Votes
        console.log('Creating votes...');
        await Promise.all([
            axios.post(`${API}/votes/news/${newsItemIds[0]}`, { type: 'UP' }, authHeader),
            axios.post(`${API}/votes/news/${newsItemIds[1]}`, { type: 'DOWN' }, authHeader),
            axios.post(`${API}/votes/news/${newsItemIds[2]}`, { type: 'UP' }, authHeader),
            axios.post(`${API}/votes/news/${newsItemIds[3]}`, { type: 'DOWN' }, authHeader),
            axios.post(`${API}/votes/news/${newsItemIds[4]}`, { type: 'UP' }, authHeader),
        ]);
        console.log('Votes created successfully');

        console.log('All demo data inserted successfully!');
        process.exit(0);
    } catch (error) {
        console.error('Error:', error.response?.data || error.message);
        process.exit(1);
    }
}

populateDatabase();
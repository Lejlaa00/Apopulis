const mongoose = require('mongoose');
const dotenv = require('dotenv');
dotenv.config();

const { getUserTopCategories } = require('./userStats');
const NewsItem = require('../models/newsItemModel');

mongoose.connect(process.env.MONGO_URI || 'mongodb://localhost:27017/Apopulis', {
    useNewUrlParser: true,
    useUnifiedTopology: true,
}).then(async () => {
    console.log('Connected to DB');

    const userId = '682b9a3785fcdd14f75ffa2f'; // ← zameni sa stvarnim ObjectId iz baze

    const categories = await getUserTopCategories(userId);

    console.log('Top kategorije za korisnika:', categories);

    mongoose.disconnect();
}).catch(err => {
    console.error('Error connecting to DB:', err);
});

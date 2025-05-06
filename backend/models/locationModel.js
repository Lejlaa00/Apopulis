const mongoose = require('mongoose');
const { Schema } = mongoose;

const locationSchema = new Schema({
    name: String,
    latitude: Number,
    longitude: Number,
    region: String
});

module.exports = mongoose.model('Location', locationSchema);

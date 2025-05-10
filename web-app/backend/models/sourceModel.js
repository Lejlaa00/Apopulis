const mongoose = require('mongoose');
const { Schema } = mongoose;

const sourceSchema = new Schema({
    name: String,
    url: String,
    scraperType: String
});

module.exports = mongoose.model('Source', sourceSchema);

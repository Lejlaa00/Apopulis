const mongoose = require('mongoose');
const { Schema } = mongoose;

const categorySchema = new Schema({ //_id default from mongoose for every id(.docs)
    name: String
});

module.exports = mongoose.model('Category', categorySchema);

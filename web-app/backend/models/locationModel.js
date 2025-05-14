const mongoose = require('mongoose');
const { Schema } = mongoose;

const locationSchema = new Schema({
    name: {
        type: String,
        required: true
    },
    latitude: {
        type: Number,
        required: true
    },
    longitude: {
        type: Number,
        required: true
    },
    region: String,
    province: {
        type: Schema.Types.ObjectId,
        ref: 'Province',
        required: true
    }
}, {
    timestamps: true
});

module.exports = mongoose.model('Location', locationSchema);

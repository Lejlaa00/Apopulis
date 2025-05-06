const mongoose = require('mongoose');
const {Schema} = mongoose;

const userSchema = new Schema ({
    usrename: String,
    email: String,
    isActive: Boolean
}, { timestamps: true }); // defaultno createdAt i updatedAt

module.exports = mongoose.model('User', userSchema);
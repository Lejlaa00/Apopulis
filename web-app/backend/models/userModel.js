const mongoose = require('mongoose');
const {Schema} = mongoose;

const userSchema = new Schema ({
    username: { type: String, required: true },
    email: { type: String, required: true, unique: true },
    password: { type: String, required: true },
    isActive: { type: Boolean, default: true },
    verificationToken: { type: String },  // Token za verifikaciju JWT
    verificationTokenExpires: { type: Date }
}, { timestamps: true }); // defaultno createdAt i updatedAt

module.exports = mongoose.model('User', userSchema);
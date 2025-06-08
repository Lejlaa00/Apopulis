const bcrypt = require('bcryptjs');
const User = require('../models/userModel');
const validator = require('validator');
const { generateToken } = require('../utils/auth');

const PASSWORD_MIN_LENGTH = 8;

// Helper function for email validation
const validateEmail = (email) => {
    return validator.isEmail(email) &&
        validator.normalizeEmail(email);
};

// Helper function for password validation
const validatePassword = (password) => {
    return password.length >= PASSWORD_MIN_LENGTH &&
        /[A-Z]/.test(password) &&
        /[0-9]/.test(password) &&
        /[!@#$%^&*]/.test(password);
};

exports.refreshToken = async (req, res) => {
    try {
        const refreshToken = req.cookies.refreshToken;
        if (!refreshToken) {
            return res.status(401).json({ msg: 'No refresh token provided' });
        }

        const decoded = jwt.verify(refreshToken, REFRESH_TOKEN_SECRET);
        const user = await User.findById(decoded.id);
        if (!user) {
            return res.status(404).json({ msg: 'User not found' });
        }

        const newToken = jwt.sign(
            {
                id: user._id,
                isVerified: user.isVerified,
                role: user.role || 'user'
            },
            ACCESS_TOKEN_SECRET,
            { expiresIn: JWT_EXPIRES_IN }
        );

        res.json({ token: newToken });
    } catch (err) {
        console.error('Refresh token error:', err);
        res.status(401).json({ msg: 'Invalid refresh token' });
    }
};


// Register
exports.register = async (req, res) => {
    try {
        const { username, email, password } = req.body;

        if (!username || !email || !password) {
            return res.status(400).json({ msg: 'Please fill all fields' });
        }

        //Validation of username and password
        const normalizedEmail = validateEmail(email);
        if (!normalizedEmail) {
            return res.status(400).json({ msg: 'Invalid email format' });
        }

        if (!validatePassword(password)) {
            return res.status(400).json({
                msg: `Password must be at least ${PASSWORD_MIN_LENGTH} characters long and contain at least one uppercase letter, one number, and one special character`
            });
        }

        // If user alredy exists
        const existingUser = await User.findOne({ $or: [{ email: normalizedEmail }, { username }] });
        if (existingUser) {
            return res.status(400).json({
                msg: existingUser.email === normalizedEmail
                    ? 'Email already in use'
                    : 'Username already taken'
            });
        }

        // Password hash
        const hashedPassword = await bcrypt.hash(password, 12);

        const verificationToken = uuidv4();
        const user = new User({
            username,
            email: normalizedEmail,
            password: hashedPassword,
            verificationToken,
            isVerified: true // TEMPORARY: Set to true for development
        });

        console.log('Saving user...');
        await user.save();
        console.log('User saved!');

        // sendVerificationEmail(user.email, verificationToken);

        // Generate JWT token
        const token = jwt.sign(
            {
                id: user._id,
                isVerified: true, // TEMPORARY: Set to true for development
                role: 'user'
            },
            ACCESS_TOKEN_SECRET,
            { expiresIn: JWT_EXPIRES_IN }
        );

        res.status(201).json({
            msg: 'User registered successfully.',
            token,
            user: {
                id: user._id,
                username: user.username,
                email: user.email,
                isVerified: true // TEMPORARY: Set to true for development
            }
        });

    } catch (err) {
        console.error('Registration error:', err);
        res.status(500).json({ msg: 'Error registering user', error: err.message });
    }
};

// Login
exports.login = async (req, res) => {
    try {
        const { username, password } = req.body;

        if (!username || !password) {
            return res.status(400).json({
                success: false,
                message: 'Username and password are required'
            });
        }

        const user = await User.findOne({ username });
        if (!user || !(await bcrypt.compare(password, user.password))) {
            return res.status(401).json({
                success: false,
                message: 'Invalid credentials'
            });
        }

        const token = generateToken(user);
          res.json({
            success: true,
            token,
            user: {
                id: user._id,
                username: user.username,
                email: user.email,
                avatarColor: user.avatarColor,
                role: user.role || 'user'
            }
        });        // For development, we're not using refresh tokens
        // but in production you might want to implement them
        console.log('User logged in successfully:', user.username);

    } catch (err) {
        console.error('Login error:', err);
        res.status(500).json({ msg: 'Error logging in', error: err.message });
    }
};

//Logout
exports.logout = (req, res) => {
    res.clearCookie('refreshToken');
    res.json({ msg: 'Logout successful' });
};

//Bookmarking newsItem
exports.addBookmark = async (req, res) => {
    try {
        const user = await User.findById(req.user.id);
        const { newsId } = req.params;

        if (!user.bookmarks.includes(newsId)) {
            user.bookmarks.push(newsId);
            await user.save();
        }

        res.json({ msg: 'News bookmarked' });
    } catch (err) {
        console.error('Error bookmarking news:', err);
        res.status(500).json({ msg: 'Server error' });
    }
};

//Removing bookmar
exports.removeBookmark = async (req, res) => {
    try {
        const user = await User.findById(req.user.id);
        const { newsId } = req.params;

        user.bookmarks = user.bookmarks.filter(id => id.toString() !== newsId);
        await user.save();

        res.json({ msg: 'Bookmark removed' });
    } catch (err) {
        console.error('Error removing bookmark:', err);
        res.status(500).json({ msg: 'Server error' });
    }
};

//Ger all bookmarks for user
exports.getBookmarks = async (req, res) => {
    try {
        const user = await User.findById(req.user.id).populate('bookmarks');
        res.json({ bookmarks: user.bookmarks });
    } catch (err) {
        console.error('Error getting bookmarks:', err);
        res.status(500).json({ msg: 'Server error' });
    }
};

exports.updateProfile = async (req, res) => {
    try {
        const user = await User.findById(req.user.id);
        const { username, email, avatarColor } = req.body;

        if (username) user.username = username;
        if (email) user.email = email;
        if (avatarColor) user.avatarColor = avatarColor;

        await user.save();

        res.json({
            msg: 'Profile updated successfully',
            user: {
                id: user._id,
                username: user.username,
                email: user.email,
                avatarColor: user.avatarColor
            }
        });
    } catch (err) {
        console.error('Update profile error:', err);
        res.status(500).json({ msg: 'Failed to update profile' });
    }
};

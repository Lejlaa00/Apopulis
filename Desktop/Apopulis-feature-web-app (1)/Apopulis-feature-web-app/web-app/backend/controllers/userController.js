const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const User = require('../models/userModel');
const validator = require('validator');
const { v4: uuidv4 } = require('uuid');


const ACCESS_TOKEN_SECRET = process.env.ACCESS_TOKEN_SECRET || 'default_access_secret';
const REFRESH_TOKEN_SECRET = process.env.REFRESH_TOKEN_SECRET || 'default_refresh_secret';
const JWT_EXPIRES_IN = process.env.JWT_EXPIRES_IN || '1h';
const REFRESH_TOKEN_EXPIRES_IN = process.env.REFRESH_TOKEN_EXPIRES_IN || '7d';
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
            return res.status(400).json({ msg: 'Please provide both username and password' });
        }

        const user = await User.findOne({ username });

        if (!user) {
            return res.status(401).json({ msg: 'Invalid credentials' });
        }

        // Password check
        const isMatch = await bcrypt.compare(password, user.password);
        if (!isMatch) {
            return res.status(401).json({ msg: 'Invalid credentials' });
        }

       
        /*if (!user.isVerified) {
            return res.status(403).json({
                msg: 'Account not verified. Please check your email for verification instructions.',
                needsVerification: true
            });
        }*/

        // Generate JWT token
        const token = jwt.sign(
            {
                id: user._id,
                isVerified: true, // TEMPORARY: Set to true for development
                role: user.role || 'user'
            },
            ACCESS_TOKEN_SECRET,
            { expiresIn: JWT_EXPIRES_IN }
        );

        // Refresh tokens as HTTP-only cookie
        const refreshToken = jwt.sign(
            { id: user._id },
            REFRESH_TOKEN_SECRET,
            { expiresIn: REFRESH_TOKEN_EXPIRES_IN }
        );

        res.cookie('refreshToken', refreshToken, {
            httpOnly: true,
            secure: process.env.NODE_ENV === 'production',
            sameSite: 'strict',
            maxAge: 7 * 24 * 60 * 60 * 1000 // 7 dana
        });

        res.json({
            token,
            user: {
                id: user._id,
                username: user.username,
                email: user.email,
                isVerified: user.isVerified,
                role: user.role || 'user'
            }
        });

    } catch (err) {
        console.error('Login error:', err);
        res.status(500).json({ msg: 'Error logging in', error: err.message });
    }
};


exports.logout = (req, res) => {
    res.clearCookie('refreshToken');
    res.json({ msg: 'Logout successful' });
};

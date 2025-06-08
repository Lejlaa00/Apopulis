import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import AuthService from '../services/authService';
import '../css/login.css';

function Register() {
    const [username, setUsername] = useState('');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const navigate = useNavigate();     const handleRegister = async (e) => {
        e.preventDefault();
        try {
            await AuthService.register(username, email, password);
            toast.success('Registration successful! Please log in.');
            navigate('/login');
        } catch (err) {
            toast.error(err.message || 'Registration failed');
            console.error('Registration error:', err);
        }
    };

    return (
        <div className="login-container">
            <h2 className="login-title">Create Account</h2>
            <form onSubmit={handleRegister} className="login-form">
                <label>Username:</label>
                <input
                    type="text"
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    className="login-input"
                    placeholder="Enter your username"
                />

                <label>Email:</label>
                <input
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    className="login-input"
                    placeholder="Enter your email"
                />

                <label>Password:</label>
                <input
                    type="password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    className="login-input"
                    placeholder="Enter your password"
                />

                <button type="submit" className="login-button">Register</button>
            </form>
        </div>
    );
}

export default Register;

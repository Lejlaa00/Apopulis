import React, { useState, useContext } from 'react';
import { useNavigate } from 'react-router-dom';
import { UserContext } from "../userContext";
import { toast } from 'react-toastify';
import AuthService from '../services/authService';
import '../css/login.css';

function Login() {
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const { setUserContext } = useContext(UserContext);
    const navigate = useNavigate();    const handleLogin = async (e) => {
        e.preventDefault();
        try {
            const data = await AuthService.login(username, password);
            toast.success('Login successful!');
            setUserContext(data.user);
            navigate('/');
        } catch (err) {
            toast.error(err.message || 'Login failed');
            console.error('Login error:', err);
        }
    };

    return (
        <div className="login-container">
            <h2 className="login-title">Log In</h2>
            <form onSubmit={handleLogin} className="login-form">
                <label>Username:</label>
                <input
                    type="text"
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    className="login-input"
                    placeholder="Enter your username"
                />

                <label>Password:</label>
                <input
                    type="password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    className="login-input"
                    placeholder="Enter your password"
                />

                <button type="submit" className="login-button">Log In</button>
            </form>
        </div>
    );
}

export default Login;

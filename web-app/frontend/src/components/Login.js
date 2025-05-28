import React, { useState, useContext } from 'react';
import { useNavigate } from 'react-router-dom';
import { UserContext } from "../userContext";
import '../css/login.css';

const API_URL = process.env.REACT_APP_API_URL || 'http://localhost:5001/api';

function Login() {
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const { setUserContext } = useContext(UserContext);
    const navigate = useNavigate();

    const handleLogin = async (e) => {
        e.preventDefault();
        try {
            const res = await fetch(`${API_URL}/users/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, password })
            });

            const data = await res.json();
            if (res.ok) {
                alert('Login successful!');
                localStorage.setItem("token", data.token);
                setUserContext(data.user); 
                navigate('/');
            } else {
                alert(data.msg || 'Login failed');
            }
        } catch (err) {
            console.error('Error:', err);
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

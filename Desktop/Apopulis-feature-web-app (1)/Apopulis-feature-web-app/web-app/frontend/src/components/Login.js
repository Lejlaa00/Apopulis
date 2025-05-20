import React, { useState, useContext } from 'react';
import { useNavigate } from 'react-router-dom';
import { UserContext } from "../userContext";
import { authFetch } from './authFetch';


function Login() {
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const { setUserContext } = useContext(UserContext);
    const navigate = useNavigate();


    const handleLogin = async (e) => {
        e.preventDefault();
        try {
            const res = await fetch('http://localhost:5000/api/users/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, password })
            });

            const data = await res.json();
            if (res.ok) {
                alert('Login sucsessful!');
                localStorage.setItem("token", data.token);
                setUserContext(data.user); 
                navigate('/');
            } else {
                alert(data.msg || 'Error wiht login');
            }
        } catch (err) {
            console.error('Error:', err);
        }
    };
    
    return (
        <div>
            <h2>Login</h2>
            <form onSubmit={handleLogin}>
                <label>Username:</label>
                <input type="text" value={username} onChange={(e) => setUsername(e.target.value)} /><br />
                <label>Lozinka:</label>
                <input type="password" value={password} onChange={(e)=> setPassword(e.target.value)} /><br />
                <button type="submit">Prijava</button>
            </form>
        </div>
    );
}

export default Login;
  
import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';

function Register() {
    const [username, setUsername] = useState('');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const navigate = useNavigate(); 

    const handleRegister = async (e) => {
        e.preventDefault();
        try {
            const res = await fetch('http://localhost:5000/users/register', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, email, password })
            });

            const data = await res.json();
            if (res.ok) {
                alert('Registration successful');
                console.log('Token:', data.token);
                navigate('/login'); 
            } else {
                alert(data.msg || 'Error with registration.');
            }
        } catch (err) {
            console.error('Error:', err);
        }
    };

    return (
        <div>
            <h2>Register</h2>
            <form onSubmit={handleRegister}>
                <label>Ime:</label>
                <input
                    type="text"
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                /><br />
                <label>Email:</label>
                <input
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                /><br />
                <label>Lozinka:</label>
                <input
                    type="password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                /><br />
                <button type="submit">Registracija</button>
            </form>
        </div>
    );
}

export default Register;

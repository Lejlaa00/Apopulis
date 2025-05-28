import React, { useContext, useState, useEffect } from 'react';
import { UserContext } from '../userContext';
import { authFetch } from './authFetch';
import '../css/profile.css';

const API_URL = process.env.REACT_APP_API_URL || 'http://localhost:5001/api';

const AVATAR_COLORS = [
  '#e74c3c', '#e67e22', '#ebe231', '#2ecc71',
  '#3498db', '#ecf0f1', '#95a5a6', '#a696ec',
  '#1abdd6', '#ff5b99', '#00cc99', '#8e44ad',
  '#da82c8'
];

function Profile() {
  const { user, setUserContext } = useContext(UserContext);
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [avatarColor, setAvatarColor] = useState('');
  const [message, setMessage] = useState('');

  useEffect(() => {
    if (user) {
      setUsername(user.username || '');
      setEmail(user.email || '');
      setAvatarColor(user.avatarColor || '#ffffff');
    }
  }, [user]);

  const handleSave = async () => {
    try {
      const res = await authFetch(`${API_URL}/users/profile`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, email, avatarColor })
      });

      const data = await res.json();
      if (res.ok) {
        setUserContext(data.user);
        setMessage('Profile updated successfully.');
      } else {
        setMessage(data.msg || 'Update failed.');
      }
    } catch (err) {
      console.error('Update error:', err);
      setMessage('Server error.');
    }
  };

  return (
    <div className="profile-page">
      <h2>Edit Profile</h2>

      <label>Username</label>
      <input
        type="text"
        value={username}
        onChange={(e) => setUsername(e.target.value)}
        className="profile-input"
        placeholder="Enter your username"
      />

      <label>Email</label>
      <input
        type="email"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        className="profile-input"
        placeholder="Enter your email"
      />

      <label>Avatar Color</label>
      <div className="color-picker single-row">
        {AVATAR_COLORS.map(color => (
          <div
            key={color}
            className={`color-circle ${avatarColor === color ? 'selected' : ''}`}
            style={{ backgroundColor: color }}
            onClick={() => setAvatarColor(color)}
          />
        ))}
      </div>

      <button className="profile-save-button" onClick={handleSave}>
        Save Changes
      </button>

      {message && <p className="profile-message">{message}</p>}
    </div>
  );
}

export default Profile;

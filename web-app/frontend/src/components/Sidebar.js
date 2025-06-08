import React, { useContext, useState, useRef, useEffect } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { UserContext } from '../userContext';
import { FaHome, FaUserCircle } from 'react-icons/fa';
import '../css/sidebar.css';

export default function Sidebar() {
  const { user, setUserContext } = useContext(UserContext);
  const navigate = useNavigate();

  const [hovered, setHovered] = useState(false);
  const dropdownRef = useRef(null);

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setUserContext(null);
    navigate('/login');
  };

  const hoverTimeout = useRef(null);

  useEffect(() => {
  function handleClickOutside(event) {
    if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
      setHovered(false);
    }
  }

  document.addEventListener('mousedown', handleClickOutside);
  return () => {
    document.removeEventListener('mousedown', handleClickOutside);
    clearTimeout(hoverTimeout.current);
  };
}, []);

  return (
    <aside className="sidebar">
      <div className="sidebar-header">
        <img src="/logo.png" alt="Apopulis Logo" className="sidebar-logo" />
      </div>
      <hr className="sidebar-divider" />

      <ul className="nav-list">
        {/* HOME */}
        <li>
          <NavLink
            to="/"
            className={({ isActive }) => (isActive ? 'active-link' : 'nav-link')}
            title="Home"
          >
            <FaHome size={20} />
            <span className="nav-text">Home</span>
          </NavLink>
        </li>

        {/* PROFILE */}
        <li className="profile-container" ref={dropdownRef}>
          <div
            className="profile-hover-area"
            onMouseEnter={() => {
              clearTimeout(hoverTimeout.current);
              setHovered(true);
            }}
            onMouseLeave={() => {
              hoverTimeout.current = setTimeout(() => {
                setHovered(false);
              }, 150); 
            }}
          >
            <NavLink to="/profile" className={({ isActive }) => (isActive ? 'active-link' : 'nav-link')} onClick={() => setHovered(false)}>
              <div className="profile-dropdown-item">
                <FaUserCircle size={20} style={{ color: user?.avatarColor || 'var(--color-text-white)' }} />
                <span className="nav-text">Profile</span>
              </div>
            </NavLink>

            {hovered && (
              <div className="profile-dropdown">
                {!user ? (
                  <>
                    <NavLink to="/login" className="dropdown-item" onClick={() => setHovered(false)}>Login</NavLink>
                    <NavLink to="/register" className="dropdown-item" onClick={() => setHovered(false)}>Register</NavLink>
                  </>
                ) : (
                  <>
                    <button className="dropdown-item" onClick={handleLogout}>Logout</button>
                  </>
                )}
              </div>
            )}
          </div>
        </li>
      </ul>
    </aside>
  );
}

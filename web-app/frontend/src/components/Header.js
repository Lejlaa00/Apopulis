import React, { useContext } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { UserContext } from '../userContext';

export default function Header() {
  const { user, setUserContext } = useContext(UserContext);
  const navigate = useNavigate();
  const handleLogout = () => {
    AuthService.logout();
    setUserContext(null);
    navigate('/login');
    toast.info('Logged out successfully');
  };

  return (
    <header className="header">
      <div className="header-content">
        <h1 className="logo"><Link to="/" style={{ color: 'inherit', textDecoration: 'none' }}>Apopulis</Link></h1>
        <nav className="nav-links">
          {user ? (
            <>
              <span>Welcome, {user.username}</span>
              <button onClick={handleLogout} className="btn-logout">Logout</button>
            </>
          ) : (
            <>
              <Link to="/login">Login</Link>
              <Link to="/register" style={{ marginLeft: '1rem' }}>Register</Link>
            </>
          )}
        </nav>
      </div>
    </header>
  );
}

import React, { useState } from "react";
import { NavLink, useNavigate } from "react-router-dom";
import { Home, LogIn, UserPlus, LogOut, User, Mail, Info, Menu } from "lucide-react";
import "../css/sidebar.css";

const Sidebar = () => {
  const navigate = useNavigate();
  const [isOpen, setIsOpen] = useState(false);
  const isAuthenticated = !!localStorage.getItem("token");

  const handleLogout = () => {
    localStorage.removeItem("token");
    navigate("/login");
    setIsOpen(false);
  };

  const closeSidebar = () => setIsOpen(false);

  return (
    <>
      {/* Hamburger button for mobile */}
      <button className="hamburger-btn" onClick={() => setIsOpen(!isOpen)}>
        <Menu size={28} />
      </button>


      {/* Overlay */}
      {isOpen && <div className="sidebar-overlay" onClick={closeSidebar}></div>}

      <div className={`sidebar ${isOpen ? "open" : ""}`}>
        <div className="sidebar-header">
          <img src="/logo.png" alt="Apopulis Logo" className="sidebar-logo" />
        </div>
        <hr className="sidebar-divider" />
        <ul className="nav-list">
          <li>
            <NavLink to="/" onClick={closeSidebar} className={({ isActive }) => isActive ? "active-link" : ""}>
              <Home size={18} />
              Home
            </NavLink>
          </li>
          <li>
            <a href="mailto:lejlagutic2019@gmail.com" onClick={closeSidebar} className="profile-nav">
              <Mail size={18} />
              Contact Us
            </a>
          </li>
          <li>
            <NavLink to="/about" onClick={closeSidebar} className={({ isActive }) => isActive ? "active-link" : ""}>
              <Info size={18} />
              About Us
            </NavLink>
          </li>
          <hr className="sidebar-divider" />
          <div className="auth-section">
            {!isAuthenticated ? (
              <>
                <li>
                  <NavLink to="/login" onClick={closeSidebar} className={({ isActive }) => isActive ? "active-link" : ""}>
                    <LogIn size={18} />
                    Login
                  </NavLink>
                </li>
                <li>
                  <NavLink to="/register" onClick={closeSidebar} className={({ isActive }) => isActive ? "active-link" : ""}>
                    <UserPlus size={18} />
                    Register
                  </NavLink>
                </li>
              </>
            ) : (
              <>
                <li>
                  <NavLink to="/profile" onClick={closeSidebar} className={({ isActive }) => isActive ? "active-link" : ""}>
                    <User size={18} />
                    Profile
                  </NavLink>
                </li>
                <li>
                  <button onClick={handleLogout} className="profile-nav">
                    <LogOut size={18} />
                    Logout
                  </button>
                </li>
              </>
            )}
          </div>
        </ul>
      </div>
    </>
  );
};

export default Sidebar;

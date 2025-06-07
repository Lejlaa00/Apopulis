import React from 'react';
import Sidebar from './Sidebar';
import Footer from './Footer';
import '../css/layout.css';

export default function Layout({ children }) {
  return (
    <div className="layout">
      <div className="content-area">
        <Sidebar />
        <main className="main-content">{children}</main>
      </div>
      <Footer /> 
    </div>
  );
}


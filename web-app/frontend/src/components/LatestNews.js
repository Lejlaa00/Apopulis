import React, { useEffect, useState } from 'react';
import '../css/latestNews.css';

const API_URL = process.env.REACT_APP_API_URL || 'http://localhost:5001/api';

export default function LatestNews({ onSelect }) {
  return (
    <div className="latest-news">
        <div className="news-summary">
          <h3>Today's News Summary</h3>
        </div>
    </div>
  );
}
import React, { useEffect, useState } from 'react';
import '../css/latestNews.css';

const API_URL = process.env.REACT_APP_API_URL || 'http://localhost:5001/api';

export default function LatestNews({ onSelect }) {
  const [news, setNews] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function fetchNews() {
      try {
        const res = await fetch(`${API_URL}/news`);
        const data = await res.json();
        if (res.ok || res.status === 200) {
          setNews(data.news);
        } else {
          alert(data.msg || 'Failed to load news');
        }
      } catch (err) {
        alert('Error loading news');
        console.error(err);
      } finally {
        setLoading(false);
      }
    }
    fetchNews();
  }, []);

  function timeAgo(dateString) {
    const diff = Math.floor((new Date() - new Date(dateString)) / 1000);
    if (diff < 60) return `${diff}s ago`;
    if (diff < 3600) return `${Math.floor(diff / 60)}m ago`;
    if (diff < 86400) return `${Math.floor(diff / 3600)}h ago`;
    return `${Math.floor(diff / 86400)}d ago`;
  }

  return (
  <div className="latest-news">
    {loading && <div className="news-loading">Loading news...</div>}

    {!loading && news.length === 0 && (
      <div className="news-empty">No news items to display.</div>
    )}

    {news.map((item) => (
      <div key={item._id} className="news-card" onClick={() => onSelect(item._id)}>
        <span className="news-time">{timeAgo(item.publishedAt)}</span>
        <span className="news-title">{item.title}</span>
        {item.url && item.sourceId?.name && (
          <div className="news-source-wrapper">
            <a
              href={item.url}
              className="news-source"
              target="_blank"
              rel="noopener noreferrer"
              onClick={(e) => e.stopPropagation()}
            >
              <span className="icon">🔗</span> {item.sourceId.name}
            </a>
          </div>
        )}
      </div>
    ))}
  </div>
);
}

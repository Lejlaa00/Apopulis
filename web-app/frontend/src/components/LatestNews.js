import React, { useEffect, useState } from 'react';
import '../css/latestNews.css';

const API_URL = process.env.REACT_APP_API_URL || 'http://localhost:5001/api';

export default function LatestNews({ onSelect }) {
  const [summary, setSummary] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [lastUpdated, setLastUpdated] = useState(null);

  const fetchSummary = async () => {
    try {
      setLoading(true);
      const response = await fetch(`${API_URL}/news/summary`);
      if (!response.ok) throw new Error('Failed to fetch summary');
      const data = await response.json();

      setSummary(data.summary);
      setLastUpdated(new Date(data.timestamp));
    } catch (err) {
      setError(err.message);
      console.error('Error fetching summary:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchSummary();

    // Poll every 20 minutes to match backend update interval
    const pollInterval = setInterval(() => {
      fetchSummary();
    }, 1200000); // 20 minutes

    // Cleanup interval on unmount
    return () => clearInterval(pollInterval);
  }, []);

  return (
    <div className="latest-news">
      <div className="news-summary">
        <div className="summary-header">
          <h3>Today's News Summary</h3>
          {lastUpdated && (
            <div className="summary-timestamp">
              Last updated: {new Date(lastUpdated).toLocaleTimeString()}
            </div>
          )}
        </div>
        {loading ? (
          <div className="summary-loading">Loading summary...</div>
        ) : error ? (
          <div className="summary-error">{error}</div>
        ) : (
          <div className="summary-content">
            {summary.split('\n\n').map((paragraph, index) => (
              <p key={index}>{paragraph}</p>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
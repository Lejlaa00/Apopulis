import React, { useState, useEffect } from 'react';
import SortedNewsHeader from './SortedNewsHeader';
import '../css/sortedNews.css'; // koristi isti stil

const API_URL = process.env.REACT_APP_API_URL || 'http://localhost:5001/api';

export default function SortedNews() {
  const [categories, setCategories] = useState([]);
  const [filter, setFilter] = useState('trending');
  const [selectedCategory, setSelectedCategory] = useState('all');
  const [searchTerm, setSearchTerm] = useState('');
  const [news, setNews] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
  async function fetchCategories() {
    const res = await fetch(`${API_URL}/categories`);
    const data = await res.json();
    setCategories(data); // NEMA .categories jer već dobivaš niz
  }
  fetchCategories();
}, []);

  useEffect(() => {
    async function fetchNews() {
      setLoading(true);
      try {
        let url = '';
        const params = [];

        if (filter === 'trending') {
          url = `${API_URL}/news/trending`;
          console.log("Fetching trending news from:", url);

          if (selectedCategory !== 'all') {
            const categoryObj = categories.find(c => c._id === selectedCategory || c.name === selectedCategory);
            if (categoryObj) {
              params.push(`category=${categoryObj._id}`);
            }
          }

          if (params.length > 0) {
            url += '?' + params.join('&');
          }
        } else {
          url = `${API_URL}/news?limit=0`;

          if (selectedCategory !== 'all') {
            const categoryObj = categories.find(c => c._id === selectedCategory || c.name === selectedCategory);
            if (categoryObj) {
              params.push(`category=${categoryObj._id}`);
            }
          }

          if (searchTerm.trim() !== '') {
            params.push(`search=${searchTerm}`);
          }

          if (params.length > 0) {
            url += '&' + params.join('&');
          }
        }

        const res = await fetch(url);
        const data = await res.json();
        if (res.ok) {
          setNews(data.news || data);
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
  }, [selectedCategory, searchTerm, filter, categories]); // ← DODAJ `filter`


  function timeAgo(dateString) {
    const diff = Math.floor((new Date() - new Date(dateString)) / 1000);
    if (diff < 60) return `${diff}s ago`;
    if (diff < 3600) return `${Math.floor(diff / 60)}m ago`;
    if (diff < 86400) return `${Math.floor(diff / 3600)}h ago`;
    return `${Math.floor(diff / 86400)}d ago`;
  }

 return (
  <div className="sorted-news-wrapper">
    <SortedNewsHeader
      categories={categories}
      onFilterChange={setFilter}
      onCategoryChange={setSelectedCategory}
      onSearch={setSearchTerm}
    />

    <div className="sorted-news-content">
      {loading && <div className="news-loading">Loading...</div>}
      {!loading && news.length === 0 && (
        <div className="news-empty">No news items found.</div>
      )}
      {!loading && news.length > 0 && (
        news.map((item) => (
          <div
            key={item._id}
            className="news-card"
            onClick={() => window.location.href = `/news/${item._id}`}
            role="link"
            tabIndex={0}
          >
            <span className="news-time">{timeAgo(item.publishedAt)}</span>
            <span className="news-title">{item.title}</span>
            {item.url && item.sourceId?.name && (
              <a
                href={item.url}
                className="news-source"
                target="_blank"
                rel="noopener noreferrer"
                onClick={(e) => e.stopPropagation()}
              >
                <span className="icon">🔗</span> {item.sourceId.name}
              </a>
            )}
          </div>
        ))
      )}
    </div>
  </div>
);

}

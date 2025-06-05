import React, { useState, useEffect } from 'react';
import SortedNewsHeader from './SortedNewsHeader';
import { authFetch } from './authFetch';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import '../css/sortedNews.css'; 

const API_URL = process.env.REACT_APP_API_URL || 'http://localhost:5001/api';

export default function SortedNews({ onSelect }) {
  const [categories, setCategories] = useState([]);
  const [filter, setFilter] = useState('trending');
  const [selectedCategory, setSelectedCategory] = useState('all');
  const [searchTerm, setSearchTerm] = useState('');
  const [news, setNews] = useState([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();


  useEffect(() => {
  async function fetchCategories() {
    const res = await fetch(`${API_URL}/categories`);
    const data = await res.json();
    setCategories(data); 
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
        } else if (filter === 'bookmark') {
          url = `${API_URL}/users/bookmarks`;
        } else if (filter === 'category') {
          url = `${API_URL}/news?limit=0`;
          if (selectedCategory !== 'all') {
            const categoryObj = categories.find(c => c._id === selectedCategory || c.name === selectedCategory);
            if (categoryObj) {
              params.push(`category=${categoryObj._id}`);
            }
          }
        }
        else if (filter === 'latest') {
          url = `${API_URL}/news?limit=0`;
        }

        // Allow search in all except bookmark
        if (filter !== 'bookmark' && searchTerm.trim() !== '') {
          params.push(`search=${searchTerm}`);
        }

        if (params.length > 0) {
          url += (url.includes('?') ? '&' : '?') + params.join('&');
        }

        console.log("Fetching news from:", url);

        const res = await authFetch(url, { method: 'GET' });
        const data = await res.json();

        if (res.ok) {
          setNews(data.news || data);
        } else {
          toast.error(data.msg || 'Failed to load news');
        }
      } catch (err) {
        toast.error('Error loading news');
        console.error(err);
      } finally {
        setLoading(false);
      }
    }

    fetchNews();
  }, [selectedCategory, searchTerm, filter, categories]);


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
      isAuthenticated={!!localStorage.getItem('token')}
    />

    <div className="sorted-news-content">
      {loading && <div className="news-loading"> </div>}
      {!loading && news.length === 0 && (
        <div className="news-empty">No news items found.</div>
      )}
      {!loading && news.length > 0 && (
        news.map((item) => (
          <div
            key={item._id}
            className="news-card"
            onClick={() => onSelect(item._id)}
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

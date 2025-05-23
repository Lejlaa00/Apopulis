import React, { useEffect, useState, useContext } from 'react';
import { UserContext } from '../userContext';
import { authFetch } from './authFetch';
import { Link } from 'react-router-dom';

import MapSection from './MapSection';
import LatestNews from './LatestNews';
import SortedNews from './SortedNews';
import Graphs from './Graphs';
import '../css/dashboard.css';
import '../css/mapSection.css';
import '../css/latestNews.css';
import '../css/sortedNews.css';
import '../css/graphs.css';

const API_URL = process.env.REACT_APP_API_URL || 'http://localhost:5001/api';

export default function Home() {
  const [newsList, setNewsList] = useState([]);
  const [loading, setLoading] = useState(true);
  const { user } = useContext(UserContext);

  const [userVotes, setUserVotes] = useState({});
  const [commentsMap, setCommentsMap] = useState({});
  const [newComments, setNewComments] = useState({});
  const [voteCounts, setVoteCounts] = useState({});
  const [bookmarkedIds, setBookmarkedIds] = useState(new Set());

  useEffect(() => {
    async function fetchNews() {
      try {
        const res = await fetch(`${API_URL}/news`);
        const data = await res.json();
        if (res.ok || res.status === 200) {
          setNewsList(data.news);
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

  useEffect(() => {
    async function fetchUserVotes() {
      if (!user) {
        setUserVotes({});
        return;
      }
      try {
        const votesMap = {};
        for (const news of newsList) {
          const res = await authFetch(`${API_URL}/votes/${news._id}/user`);
          const data = await res.json();
          votesMap[news._id] = data.vote;
        }
        setUserVotes(votesMap);
      } catch (err) {
        console.error('Error fetching user votes', err);
      }
    }
    fetchUserVotes();
  }, [user, newsList]);

  useEffect(() => {
    async function fetchVoteCounts() {
      try {
        const map = {};
        for (const news of newsList) {
          const res = user
            ? await authFetch(`${API_URL}/votes/news/${news._id}`)
            : await fetch(`${API_URL}/votes/news/${news._id}`);

          if (res.ok) {
            const data = await res.json();
            map[news._id] = {
              up: data.upvotes,
              down: data.downvotes,
            };
          } else {
            map[news._id] = { up: 0, down: 0 };
          }
        }
        setVoteCounts(map);
      } catch (err) {
        console.error('Error loading vote counts', err);
      }
    }
    if (newsList.length > 0) fetchVoteCounts();
  }, [newsList, user]);

  useEffect(() => {
    async function fetchComments() {
      try {
        const map = {};
        for (const news of newsList) {
          const res = await fetch(`${API_URL}/comments/${news._id}?limit=3&page=1`);
          if (res.ok) {
            const data = await res.json();
            map[news._id] = data.comments;
          } else {
            map[news._id] = [];
          }
        }
        setCommentsMap(map);
      } catch (err) {
        console.error('Error loading comments', err);
      }
    }
    fetchComments();
  }, [newsList]);

  useEffect(() => {
    async function fetchBookmarks() {
      if (!user) return;
      try {
        const res = await authFetch(`${API_URL}/users/bookmarks`);
        if (res.ok) {
          const data = await res.json();
          const ids = data.bookmarks.map(b => b._id);
          setBookmarkedIds(new Set(ids));
        }
      } catch (err) {
        console.error('Error fetching bookmarks', err);
      }
    }
    fetchBookmarks();
  }, [user]);

  useEffect(() => {
    if (!user) setBookmarkedIds(new Set());
  }, [user]);

  const handleVote = async (newsId, voteType) => {
    if (!user) return alert('You must be logged in to vote.');
    try {
      const res = await authFetch(`${API_URL}/votes/news/${newsId}`, {
        method: 'POST',
        body: JSON.stringify({ type: voteType }),
      });
      if (res.ok) {
        const data = await res.json();
        setUserVotes(prev => ({ ...prev, [newsId]: data.type }));

        const countRes = await authFetch(`${API_URL}/votes/news/${newsId}`);
        if (countRes.ok) {
          const countData = await countRes.json();
          setVoteCounts(prev => ({
            ...prev,
            [newsId]: {
              up: countData.upvotes,
              down: countData.downvotes,
            }
          }));
        }
      } else {
        const data = await res.json();
        alert(data.msg || 'Vote failed');
      }
    } catch (err) {
      alert('Error processing vote');
      console.error(err);
    }
  };

  const handleToggleBookmark = async (newsId) => {
    if (!user) return alert('You must be logged in to bookmark.');
    const isBookmarked = bookmarkedIds.has(newsId);
    try {
      const res = await authFetch(`${API_URL}/users/bookmarks/${newsId}`, {
        method: isBookmarked ? 'DELETE' : 'POST',
      });
      if (res.ok) {
        setBookmarkedIds(prev => {
          const newSet = new Set(prev);
          isBookmarked ? newSet.delete(newsId) : newSet.add(newsId);
          return newSet;
        });
      } else {
        const data = await res.json();
        alert(data.msg || 'Failed to update bookmark');
      }
    } catch (err) {
      alert('Error updating bookmark');
      console.error(err);
    }
  };

  const handleNewCommentChange = (newsId, value) => {
    setNewComments(prev => ({ ...prev, [newsId]: value }));
  };

  const handleSubmitComment = async (newsId) => {
    if (!user) return alert('You must be logged in to comment.');
    const content = newComments[newsId];
    if (!content || content.trim() === '') return alert('Comment cannot be empty.');
    try {
      const res = await authFetch(`${API_URL}/comments/${newsId}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ content }),
      });
      if (res.ok) {
        const newComment = await res.json();
        setCommentsMap(prev => ({
          ...prev,
          [newsId]: [newComment, ...(prev[newsId] || [])].slice(0, 3),
        }));
        setNewComments(prev => ({ ...prev, [newsId]: '' }));
      } else {
        const data = await res.json();
        alert(data.msg || 'Failed to add comment');
      }
    } catch (err) {
      alert('Error submitting comment');
      console.error(err);
    }
  };

  if (loading) return <p>Loading news...</p>;

  return (
    <div className="dashboard-grid">
      <div className="map-area">
        <MapSection />
      </div>
      <div className="latest-news">
        <LatestNews />
      </div>
      <div className="sorted-news">
        <SortedNews />
      </div>
      <div className="graphs-area">
        <Graphs />
      </div>
    </div>
  );
}
import React, { useEffect, useState, useContext } from 'react';
import { UserContext } from '../userContext';
import { authFetch } from './authFetch';

const API_URL = process.env.REACT_APP_API_URL || 'http://localhost:5001/api';

function Home() {
  const [newsList, setNewsList] = useState([]);
  const [loading, setLoading] = useState(true);
  const { user } = useContext(UserContext);

  const [userVotes, setUserVotes] = useState({});
  const [commentsMap, setCommentsMap] = useState({});
  const [newComments, setNewComments] = useState({});

  // Store bookmarked news IDs in a Set for easy lookup
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

  // Fetch user votes
  useEffect(() => {
    async function fetchUserVotes() {
      if (!user) return;
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

  // Fetch comments
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

  // Fetch bookmarked news IDs
  useEffect(() => {
    async function fetchBookmarks() {
      if (!user) return;
      try {
        const res = await authFetch(`${API_URL}/bookmarks`);
        if (res.ok) {
          const data = await res.json(); // assuming { bookmarks: [newsId, ...] }
          setBookmarkedIds(new Set(data.bookmarks));
        }
      } catch (err) {
        console.error('Error fetching bookmarks', err);
      }
    }
    fetchBookmarks();
  }, [user]);

  // Vote handler (same as before)
  const handleVote = async (newsId, voteType) => {
    if (!user) {
      alert('You must be logged in to vote.');
      return;
    }
    try {
      const res = await authFetch(`${API_URL}/votes/${newsId}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ type: voteType }),
      });
      if (res.ok) {
        const data = await res.json();
        setUserVotes(prev => ({ ...prev, [newsId]: data.type }));
      } else {
        const data = await res.json();
        alert(data.msg || 'Vote failed');
      }
    } catch (err) {
      alert('Error processing vote');
      console.error(err);
    }
  };

  // Bookmark toggle handler
  const handleToggleBookmark = async (newsId) => {
    if (!user) {
      alert('You must be logged in to bookmark.');
      return;
    }
    const isBookmarked = bookmarkedIds.has(newsId);
    try {
      const res = await authFetch(`${API_URL}/bookmarks/${newsId}`, {
        method: isBookmarked ? 'DELETE' : 'POST',
      });
      if (res.ok) {
        setBookmarkedIds(prev => {
          const newSet = new Set(prev);
          if (isBookmarked) {
            newSet.delete(newsId);
          } else {
            newSet.add(newsId);
          }
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

  // New comment input change handler
  const handleNewCommentChange = (newsId, value) => {
    setNewComments(prev => ({ ...prev, [newsId]: value }));
  };

  // Submit new comment (same as before)
  const handleSubmitComment = async (newsId) => {
    if (!user) {
      alert('You must be logged in to comment.');
      return;
    }
    const content = newComments[newsId];
    if (!content || content.trim() === '') {
      alert('Comment cannot be empty.');
      return;
    }
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
    <div>
      <h2>Latest News</h2>
      {newsList.length === 0 && <p>No news available.</p>}
      <ul style={{ listStyle: 'none', padding: 0 }}>
        {newsList.map(news => {
          const isBookmarked = bookmarkedIds.has(news._id);
          return (
            <li key={news._id} style={{ borderBottom: '1px solid #ddd', padding: '10px 0' }}>
              <a
                href={news.url || '#'}
                target="_blank"
                rel="noopener noreferrer"
                style={{ fontWeight: 'bold', fontSize: '18px' }}
              >
                {news.title}
              </a>
              <p style={{ fontSize: '12px', color: '#666' }}>
                Category: {news.categoryId?.name || 'Unknown'} | Published: {new Date(news.publishedAt).toLocaleString()}
              </p>
              <p>{news.summary || (news.content?.substring(0, 150) + '...')}</p>

              {/* Voting buttons */}
              <div>
                <button
                  onClick={() => handleVote(news._id, 'UP')}
                  style={{
                    backgroundColor: userVotes[news._id] === 'UP' ? 'green' : 'lightgray',
                    color: userVotes[news._id] === 'UP' ? 'white' : 'black',
                    marginRight: '8px',
                  }}
                >
                  👍
                </button>
                <button
                  onClick={() => handleVote(news._id, 'DOWN')}
                  style={{
                    backgroundColor: userVotes[news._id] === 'DOWN' ? 'red' : 'lightgray',
                    color: userVotes[news._id] === 'DOWN' ? 'white' : 'black',
                  }}
                >
                  👎
                </button>

                {/* Bookmark toggle */}
                <button
                  onClick={() => handleToggleBookmark(news._id)}
                  style={{
                    marginLeft: '16px',
                    backgroundColor: isBookmarked ? '#007bff' : 'lightgray',
                    color: isBookmarked ? 'white' : 'black',
                    borderRadius: '4px',
                    padding: '4px 10px',
                    cursor: 'pointer',
                  }}
                  title={isBookmarked ? 'Remove bookmark' : 'Add bookmark'}
                >
                  {isBookmarked ? '🔖 Bookmarked' : '🔖 Bookmark'}
                </button>
              </div>

              {/* Comments Section */}
              <div style={{ marginTop: '15px' }}>
                <h4>Comments</h4>
                {(commentsMap[news._id] && commentsMap[news._id].length > 0) ? (
                  <ul style={{ listStyle: 'none', paddingLeft: 0 }}>
                    {commentsMap[news._id].map(comment => (
                      <li key={comment._id} style={{ marginBottom: '10px', borderBottom: '1px solid #eee', paddingBottom: '5px' }}>
                        <strong>{comment.userId?.username || 'Anonymous'}</strong>: {comment.content}
                        <br />
                        <small style={{ color: '#999' }}>{new Date(comment.createdAt).toLocaleString()}</small>
                      </li>
                    ))}
                  </ul>
                ) : (
                  <p>No comments yet.</p>
                )}

                {user ? (
                  <div>
                    <textarea
                      placeholder="Write a comment..."
                      value={newComments[news._id] || ''}
                      onChange={(e) => handleNewCommentChange(news._id, e.target.value)}
                      rows={3}
                      style={{ width: '100%', marginBottom: '5px' }}
                    />
                    <button onClick={() => handleSubmitComment(news._id)}>Submit Comment</button>
                  </div>
                ) : (
                  <p><em>Login to comment</em></p>
                )}
              </div>
            </li>
          );
        })}
      </ul>
    </div>
  );
}

export default Home;

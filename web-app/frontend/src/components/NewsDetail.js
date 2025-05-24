// abbreviated top
import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { useContext } from 'react';
import { UserContext } from '../userContext';
import { authFetch } from './authFetch';
import CommentItem from './CommentItem';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faThumbsUp, faThumbsDown, faStar, faLocationDot } from '@fortawesome/free-solid-svg-icons';
import { faComment } from '@fortawesome/free-solid-svg-icons';

import '../css/newsDetail.css';

const API_URL = process.env.REACT_APP_API_URL || 'http://localhost:5001/api';


function NewsDetail({ id: propId, embedded = false }) {
  const { id: routeId } = useParams();
  const id = propId || routeId;
  const { user } = useContext(UserContext);
  const [news, setNews] = useState(null);
  const [loading, setLoading] = useState(true);
  const [userVote, setUserVote] = useState(null);
  const [voteCount, setVoteCount] = useState({ up: 0, down: 0 });
  const [comments, setComments] = useState([]);
  const [newComment, setNewComment] = useState('');
  const [isBookmarked, setIsBookmarked] = useState(false);
  const [showComments, setShowComments] = useState(false);

  useEffect(() => {
    async function fetchNewsItem() {
      const res = await fetch(`${API_URL}/news/${id}`);
      const data = await res.json();
      if (res.ok) {
        setNews(data);
        const voteRes = await authFetch(`${API_URL}/votes/news/${id}`);
        if (voteRes.ok) {
          const voteData = await voteRes.json();
          setVoteCount({ up: voteData.upvotes, down: voteData.downvotes });
        }
        if (user) {
          const userVoteRes = await authFetch(`${API_URL}/votes/news/${id}/user`);
          if (userVoteRes.ok) {
            const userVoteData = await userVoteRes.json();
            setUserVote(userVoteData.vote);
          }
          const bookmarksRes = await authFetch(`${API_URL}/users/bookmarks`);
          if (bookmarksRes.ok) {
            const bookmarksData = await bookmarksRes.json();
            const ids = bookmarksData.bookmarks.map(b => b._id);
            setIsBookmarked(ids.includes(id));
          }
        }
      }
      setLoading(false);
    }
    fetchNewsItem();
  }, [id]);

  useEffect(() => {
    fetch(`${API_URL}/news/${id}/view`, { method: 'POST' });
  }, [id]);

  useEffect(() => {
    fetchComments();
  }, [id, user]);

  const fetchComments = async () => {
  const url = `${API_URL}/comments/news/${id}?limit=100&page=1`;
  let res = await fetch(url);
  if (res.status === 401 && user) {
    res = await authFetch(url);
  }
  if (res.ok) {
    const data = await res.json();
    setComments(data.comments); // koristi direktno ono što backend šalje
  }
};

  const handleVote = async (type) => {
    if (!user) return alert('Login required');
    const res = await authFetch(`${API_URL}/votes/news/${id}`, {
      method: 'POST',
      body: JSON.stringify({ type }),
    });
    if (res.ok) {
      const data = await res.json();
      setUserVote(data.type);
      const countRes = await authFetch(`${API_URL}/votes/news/${id}`);
      if (countRes.ok) {
        const countData = await countRes.json();
        setVoteCount({ up: countData.upvotes, down: countData.downvotes });
      }
    }
  };

  const handleReply = async (parentCommentId, content) => {
  if (!user) return alert('Login required');
  if (!content.trim()) return alert('Reply cannot be empty');

  const isReplyToReply = !comments.some(c => c._id === parentCommentId);
  if (isReplyToReply) {
    return alert('You can only reply to top-level comments.');
  }

  const res = await authFetch(`${API_URL}/comments/news/${id}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ content, parentCommentId }),
  });

  if (res.ok) {
    const newReply = await res.json();

    // Dodaj novi odgovor unutar odgovarajućeg roditeljskog komentara
    setComments(prevComments => {
      const addReplyRecursively = (comments) =>
        comments.map(comment => {
          if (comment._id === parentCommentId) {
            return {
              ...comment,
              replies: [...(comment.replies || []), newReply],
            };
          } else if (comment.replies?.length > 0) {
            return {
              ...comment,
              replies: addReplyRecursively(comment.replies),
            };
          } else {
            return comment;
          }
        });

      return addReplyRecursively(prevComments);
    });
  } else {
    alert('Failed to submit reply');
  }
};

  const handleToggleBookmark = async () => {
    if (!user) return alert('Login required');
    const res = await authFetch(`${API_URL}/users/bookmarks/${id}`, {
      method: isBookmarked ? 'DELETE' : 'POST',
    });
    if (res.ok) setIsBookmarked(prev => !prev);
  };

  const handleSubmitComment = async () => {
    if (!user) return alert('Login required');
    if (!newComment.trim()) return alert('Comment cannot be empty');
    const res = await authFetch(`${API_URL}/comments/news/${id}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ content: newComment }),
    });
    if (res.ok) {
      await fetchComments();
      setNewComment('');
    }
  };

  if (loading) return <p>Loading...</p>;
  if (!news) return <p>Not found</p>;

  return (
    <div className="news-detail-grid">
      <div className="news-content">
        <h2>{news.title}</h2>
        <p className="news-meta-published">
          Published: {new Date(news.publishedAt).toLocaleString()}
        </p>
        <hr className="news-meta-divider" />
        <div className="news-meta-row">
          <div className="news-meta-item">
            <div className="news-meta-icon">👤</div>
            <span><strong>{news.author || "N/A"}</strong></span>
          </div>
          <div className="news-meta-divider-vertical"></div>
          <div className="news-meta-item">
            <div className="news-meta-icon">
              <FontAwesomeIcon icon={faLocationDot} />
            </div>
            <span><strong>{news.locationId?.name || "N/A"}</strong></span>
          </div>
        </div>
        <hr className="news-meta-divider" />
        {news.imageUrl && (
          <div className="news-image-wrapper">
            <img src={news.imageUrl} alt="News" />
          </div>
        )}
        <div className="news-content-text">
          {news.content || news.summary}
        </div>
        {!embedded && news.url && (
          <p><a href={news.url} target="_blank" rel="noreferrer">View original article ↗</a></p>
        )}
      </div>

      <div className="side-panel">
        <div className="news-action-bar">
          <div className={`news-action-item ${userVote === 'UP' ? 'active-vote' : ''}`} onClick={() => handleVote('UP')}>
            <FontAwesomeIcon icon={faThumbsUp} className="news-action-icon" />
            <span className="news-action-label">{voteCount.up}</span>
          </div>
          <div className={`news-action-item ${userVote === 'DOWN' ? 'active-vote' : ''}`} onClick={() => handleVote('DOWN')}>
            <FontAwesomeIcon icon={faThumbsDown} className="news-action-icon" />
            <span className="news-action-label">{voteCount.down}</span>
          </div>
          <div className="news-action-item" onClick={handleToggleBookmark}>
            <FontAwesomeIcon icon={faStar} className={`news-action-icon ${isBookmarked ? 'active-bookmark' : ''}`} />
            <span className="news-action-label">{isBookmarked ? 'Saved' : 'Save'}</span>
          </div>
        </div>

        {user && (
          <>
            <textarea
              rows={2}
              className="comment-input"
              placeholder="Write a comment..."
              value={newComment}
              onChange={(e) => setNewComment(e.target.value)}
            />
            <button onClick={handleSubmitComment} className="comment-submit-btn">Submit</button>
          </>
        )}

        <div className="comments-summary" onClick={() => setShowComments(prev => !prev)}>
            <div className="comments-summary-left">
                <FontAwesomeIcon icon={faComment} className="comments-summary-icon" />
                {comments.length} comments
            </div>
            <div className="comments-summary-arrow">
                {showComments ? '▴' : '▾'}
            </div>
            </div>


        {showComments && (
        <ul className="comments-list">
           {comments.map(comment => (
            <li key={comment._id} className="comment-item">
                <CommentItem
                comment={comment}
                user={user}
                onReply={handleReply}
                onEdit={() => {}}
                onDelete={() => {}}
                />
            </li>
            ))}
        </ul>
        )}

      </div>
    </div>
  );
}

export default NewsDetail;
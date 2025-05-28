// abbreviated top
import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { useContext } from 'react';
import { UserContext } from '../userContext';
import { authFetch } from './authFetch';
import CommentItem from './CommentItem';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faThumbsUp, faThumbsDown, faStar, faLocationDot, faVolumeHigh, faVolumeXmark } from '@fortawesome/free-solid-svg-icons';
import { faComment } from '@fortawesome/free-solid-svg-icons';
import { useNavigate } from 'react-router-dom';

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
    const [editingCommentId, setEditingCommentId] = useState(null);
    const [editedContent, setEditedContent] = useState('');
    const [recommendedNews, setRecommendedNews] = useState([]);
    const [isSpeaking, setIsSpeaking] = useState(false);
    const navigate = useNavigate();

    const handleTextToSpeech = () => {
        if (isSpeaking) {
            window.speechSynthesis.cancel();
            setIsSpeaking(false);
            return;
        }

        const text = news?.content || news?.summary;
        if (!text) return;

        // Cancel any ongoing speech
        window.speechSynthesis.cancel();

        const utterance = new SpeechSynthesisUtterance(text);
        
        // Optimize for Slovenian
        utterance.lang = 'sl-SI';
        utterance.rate = 0.9; // Slightly slower for better pronunciation
        utterance.pitch = 1.0;
        
        // Get Slovenian voice if available
        const voices = window.speechSynthesis.getVoices();
        const slovenianVoice = voices.find(voice => voice.lang.includes('sl'));
        if (slovenianVoice) {
            utterance.voice = slovenianVoice;
        }
        utterance.lang = 'sl-SI';
        utterance.onend = () => setIsSpeaking(false);
        utterance.onerror = () => setIsSpeaking(false);
        
        window.speechSynthesis.speak(utterance);
        setIsSpeaking(true);
    };

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
                        const ids = Array.isArray(bookmarksData.bookmarks)
                            ? bookmarksData.bookmarks.map(b => b._id)
                            : [];
                        setIsBookmarked(ids.includes(id));
                    }

                }
            }
            setLoading(false);
        }
        fetchNewsItem();
    }, [id]);

    // Track view — sa tokenom ako postoji
    useEffect(() => {
        const token = localStorage.getItem('token'); 
        fetch(`${API_URL}/news/${id}/view`, {
            method: 'POST',
            headers: token ? { Authorization: `Bearer ${token}` } : {},
        }).catch(err => {
            console.warn('Failed to track view:', err);
        });
    }, [id]);

    useEffect(() => {
        fetchComments();
    }, [id, user]);

    useEffect(() => {
        fetchRecommendedNews();
    }, []);

    useEffect(() => {
        fetch(`${API_URL}/news/${id}/view`, { method: 'POST' });
    }, [id]);

    const fetchComments = async () => {
        const url = `${API_URL}/comments/news/${id}?limit=100&page=1`;
        let res = await fetch(url);
        if (res.status === 401 && user) {
            res = await authFetch(url);
        }
        if (res.ok) {
            const data = await res.json();
            setComments(data.comments); 
        }
    };


    //Upvoting or downvoting newsItem
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
    const fetchRecommendedNews = async () => {
        try {
            const res = await authFetch(`${API_URL}/news/recommended`);
            const data = await res.json();
            if (res.ok) {
                const filtered = (data.news || []).filter(n => n._id !== id);
                setRecommendedNews(filtered);
            } else {
                console.error('Failed to fetch recommendations:', data.msg);
            }
        } catch (err) {
            console.error('Error fetching recommended news:', err);
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

    const handleEditStart = (comment) => {
        if (user && comment.userId?._id === user.id) {
            setEditedContent(comment.content);
            setEditingCommentId(comment._id);
        } else {
            alert("You can only edit your own comments.");
        }
    };
      

    const handleEditCancel = () => {
        setEditingCommentId(null);
        setEditedContent('');
    };

    //Editing comment
    const handleEditComment = async (commentId) => {
        if (!editedContent.trim()) {
            alert('Comment cannot be empty.');
            return;
        }

        try {
            const res = await authFetch(`${API_URL}/comments/${commentId}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ content: editedContent }),
            });

            if (res.ok) {
                await fetchComments(); 
                setEditingCommentId(null);
                setEditedContent('');
            } else {
                const data = await res.json();
                alert(data.msg || 'Failed to update comment');
            }
        } catch (err) {
            console.error('Error updating comment:', err);
        }
      };

    //Adding comment
    const handleSubmitComment = async () => {
        if (!user) {
            alert('You must be logged in to comment.');
            return;
        }
        if (!newComment.trim()) {
            alert('Comment cannot be empty.');
            return;
        }
        const res = await authFetch(`${API_URL}/comments/news/${id}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ content: newComment }),
        });

        console.log("Submitting comment for news ID:", id);
        console.log("Comment content:", newComment);
        if (res.ok) {
            const newCommentData = await res.json();
            setComments(prev => [newCommentData, ...prev]); 
        }
    };
    

    //Deleting comment
    const handleDeleteComment = async (commentId) => {
        try {
            const res = await authFetch(`${API_URL}/comments/${commentId}`, {
                method: 'DELETE'
            });
            if (res.ok) {
                const commentsRes = user
                    ? await authFetch(`${API_URL}/comments/news/${id}?limit=3&page=1`)
                    : await fetch(`${API_URL}/comments/news/${id}?limit=3&page=1`);
                if (commentsRes.ok) {
                    const data = await commentsRes.json();
                    setComments(data.comments);
                }
            } else {
                const data = await res.json();
                alert(data.msg || 'Failed to delete comment');
            }
        } catch (err) {
            console.error('Error deleting comment:', err);
            alert('Error deleting comment');
        }
    };

    const handleToggleBookmark = async () => {
        if (!user) return alert('Login required');
        const res = await authFetch(`${API_URL}/users/bookmarks/${id}`, {
            method: isBookmarked ? 'DELETE' : 'POST',
        });
        if (res.ok) setIsBookmarked(prev => !prev);
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

                {recommendedNews.length > 0 && (
                    <div className="recommended-news-section">
                        <h3 className="recommended-title">You might also like</h3>
                        <hr className="news-meta-divider" />
                        <div className="recommended-news-grid">
                        {recommendedNews.slice(0, 6).map(item => (
                            <div
                                key={item._id}
                                className="recommended-news-card"
                                onClick={() => navigate(`/news/${item._id}`)}
                            >
                                {item.imageUrl && (
                                <div className="recommended-news-image-wrapper">
                                    <img src={item.imageUrl} alt="thumbnail" className="recommended-news-image" />
                                </div>
                                )}
                                <div className="recommended-news-title">{item.title}</div>
                                <div className="recommended-news-date">
                                {new Date(item.publishedAt).toLocaleDateString()}
                                </div>
                                <div className="recommended-news-source">
                                {item.sourceId?.name || 'Unknown Source'}
                                </div>
                            </div>
                        ))}
                        </div>
                    </div>
                )}
            </div>

            <div className="side-panel">
                <div className="text-to-speech-container" onClick={handleTextToSpeech}>
                    <FontAwesomeIcon 
                        icon={isSpeaking ? faVolumeXmark : faVolumeHigh} 
                        className={`text-to-speech-icon ${isSpeaking ? 'speaking' : ''}`} 
                    />
                    <span className="text-to-speech-label">
                        {isSpeaking ? 'Stop Reading' : 'Read Article'}
                    </span>
                </div>

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
                                    onEdit={handleEditComment}
                                    onEditStart={handleEditStart}
                                    onEditCancel={handleEditCancel}
                                    isEditing={editingCommentId === comment._id}
                                    editedContent={editedContent}
                                    setEditedContent={setEditedContent}
                                      onDelete={handleDeleteComment}
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
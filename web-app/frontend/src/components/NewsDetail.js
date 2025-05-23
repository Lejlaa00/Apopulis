import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { useContext } from 'react';
import { UserContext } from '../userContext';
import { authFetch } from './authFetch';
import CommentItem from './CommentItem';

const API_URL = process.env.REACT_APP_API_URL || 'http://localhost:5001/api';

function NewsDetail() {
    const { id } = useParams();
    const [news, setNews] = useState(null);
    const [loading, setLoading] = useState(true);
    const { user } = useContext(UserContext);
    const [userVote, setUserVote] = useState(null);
    const [voteCount, setVoteCount] = useState({ up: 0, down: 0 });
    const [comments, setComments] = useState([]);
    const [newComment, setNewComment] = useState('');
    const [isBookmarked, setIsBookmarked] = useState(false);
    const [editingCommentId, setEditingCommentId] = useState(null);
    const [editedContent, setEditedContent] = useState('');


    useEffect(() => {
        async function fetchNewsItem() {
            try {
                const res = await fetch(`${API_URL}/news/${id}`);
                const data = await res.json();
                if (res.ok) {
                    setNews(data);
                    // Fetch vote count
                    const voteRes = await authFetch(`${API_URL}/votes/news/${id}`);
                    if (voteRes.ok) {
                        const voteData = await voteRes.json();
                        setVoteCount({ up: voteData.upvotes, down: voteData.downvotes });
                    }

                    // Fetch user's vote
                    if (user) {
                        const userVoteRes = await authFetch(`${API_URL}/votes/news/${id}/user`);
                        if (userVoteRes.ok) {
                            const userVoteData = await userVoteRes.json();
                            setUserVote(userVoteData.vote);
                        }
                    }
                    // Fetch bookmarks for this user and check if this news is bookmarked
                    if (user) {
                        const bookmarksRes = await authFetch(`${API_URL}/users/bookmarks`);
                        if (bookmarksRes.ok) {
                            const bookmarksData = await bookmarksRes.json();
                            const ids = bookmarksData.bookmarks.map(b => b._id);
                            setIsBookmarked(ids.includes(id));
                        }
                    }
  

                } else {
                    console.error('Failed to fetch news item:', data.msg);
                }
            } catch (err) {
                console.error('Error fetching news item:', err);
            } finally {
                setLoading(false);
            }
        }

        fetchNewsItem();
    }, [id]);

    useEffect(() => {
        if (!user) {
            setIsBookmarked(false);
        }
    }, [user]); 

    //View
    useEffect(() => {
        fetch(`${API_URL}/news/${id}/view`, { method: 'POST' });
    }, [id]);
    
    useEffect(() => {
        fetchComments();
    }, [id, user]);

    const fetchComments = async () => {
        try {
            const url = `${API_URL}/comments/news/${id}?limit=100&page=1`;
            let commentsRes = await fetch(url);
            if (commentsRes.status === 401 && user) {
                commentsRes = await authFetch(url);
            }

            if (commentsRes.ok) {
                const data = await commentsRes.json();
                const allComments = data.comments;

                const commentMap = {};
                allComments.forEach(comment => {
                    comment.replies = [];
                    commentMap[comment._id.toString()] = comment;
                });

                const rootComments = [];
                allComments.forEach(comment => {
                    if (comment.parentCommentId && commentMap[comment.parentCommentId]) {
                        commentMap[comment.parentCommentId].replies.push(comment);
                    } else {
                        rootComments.push(comment);
                    }
                });

                setComments(rootComments); 
            } else {
                console.warn('Could not load comments:', commentsRes.status);
            }
        } catch (err) {
            console.error('Error loading comments:', err);
        }
    };
    
      
    //Upvoting or downvoting newsItem
    const handleVote = async (type) => {
        if (!user) {
            alert('You must be logged in to vote.');
            return;
        }
        const res = await authFetch(`${API_URL}/votes/news/${id}`, {
            method: 'POST',
            body: JSON.stringify({ type }),
        });
        if (res.ok) {
            const data = await res.json();
            setUserVote(data.type);

            // refresh count
            const countRes = await authFetch(`${API_URL}/votes/news/${id}`);
            if (countRes.ok) {
                const countData = await countRes.json();
                setVoteCount({ up: countData.upvotes, down: countData.downvotes });
            }
        }
    };

    //Bookmarking newsItem
    const handleToggleBookmark = async () => {
        if (!user) {
            alert('You must be logged in to bookmark.');
            return;
        }

        try {
            const res = await authFetch(`${API_URL}/users/bookmarks/${id}`, {
                method: isBookmarked ? 'DELETE' : 'POST',
            });
            if (res.ok) {
                setIsBookmarked(prev => !prev);
            } else {
                const data = await res.json();
                alert(data.msg || 'Failed to update bookmark');
            }
        } catch (err) {
            console.error('Error updating bookmark', err);
            alert('Error updating bookmark');
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
        if (res.ok) {
            await fetchComments();
            setNewComment('');
        }
    };

    //Deleting comment
    const handleDeleteComment = async (commentId) => {
        if (!window.confirm("Are you sure you want to delete this comment?")) return;
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

    //Replying to comment
    const handleReply = async (parentId, replyContent) => {
        if (!user) return alert('Login required');
        if (!replyContent.trim()) return alert('Comment cannot be empty');

        const res = await authFetch(`${API_URL}/comments/news/${id}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ content: replyContent, parentCommentId: parentId })
        });

        if (res.ok) {
            fetchComments(); 
        } else {
            const data = await res.json();
            alert(data.msg || 'Failed to post reply');
        }
    };
      
    
          
    if (loading) return <p>Loading...</p>;
    if (!news) return <p>News not found</p>;

    return (
        <div style={{ padding: '20px' }}>
            <h2>{news.title}</h2>
            <p style={{ color: '#555' }}>
                <strong>Published:</strong> {new Date(news.publishedAt).toLocaleString()}
            </p>
            <p><strong>Category:</strong> {news.categoryId?.name || 'N/A'}</p>
            <p><strong>Location:</strong> {news.locationId?.name || 'N/A'}</p>
            <p><strong>Source:</strong> <a href={news.sourceId?.url} target="_blank" rel="noreferrer">{news.sourceId?.name}</a></p>
            <p style={{ marginTop: '20px' }}>{news.content || news.summary}</p>
            <p>
                <a href={news.url} target="_blank" rel="noreferrer">
                    View original article ↗
                </a>
            </p>

            {/* Vote buttons */}
            <div style={{ marginTop: '20px' }}>
                <button
                    onClick={() => handleVote('UP')}
                    style={{
                        backgroundColor: userVote === 'UP' ? 'green' : 'lightgray',
                        color: userVote === 'UP' ? 'white' : 'black',
                        marginRight: '8px',
                    }}
                >
                    👍 {voteCount.up}
                </button>
                <button
                    onClick={() => handleVote('DOWN')}
                    style={{
                        backgroundColor: userVote === 'DOWN' ? 'red' : 'lightgray',
                        color: userVote === 'DOWN' ? 'white' : 'black',
                    }}
                >
                    👎 {voteCount.down}
                </button>

                <button
                    onClick={handleToggleBookmark}
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

            {/* Comments */}
            <div style={{ marginTop: '30px' }}>
                <h4>Comments</h4>

                {comments.length === 0 ? (
                    <p>No comments yet.</p>
                ) : (
                    <ul style={{ listStyle: 'none', paddingLeft: 0 }}>
                        {comments
                            .filter(comment => !comment.parentCommentId)
                            .map(parent => (
                                <li key={parent._id} style={{ marginBottom: '10px' }}>
                                    <CommentItem
                                        comment={parent}
                                        user={user}
                                        onReply={handleReply}
                                        onDelete={handleDeleteComment}
                                        onEdit={handleEditComment}
                                    />

                                    {/* Comment replays */}
                                    <ul style={{ listStyle: 'none', paddingLeft: '20px', marginTop: '5px' }}>
                                        {comments
                                            .filter(reply => reply.parentCommentId === parent._id)
                                            .map(reply => (
                                                <li key={reply._id}>
                                                    <CommentItem
                                                        comment={reply}
                                                        user={user}
                                                        onReply={handleReply}
                                                        onDelete={handleDeleteComment}
                                                        onEdit={handleEditComment}
                                                    />
                                                </li>
                                            ))}
                                    </ul>
                                </li>
                            ))}
                    </ul>
                )}

                {user ? (
                    <div style={{ marginTop: '10px' }}>
                        <textarea
                            rows={3}
                            style={{ width: '100%', padding: '8px' }}
                            placeholder="Write a comment..."
                            value={newComment}
                            onChange={(e) => setNewComment(e.target.value)}
                        />
                        <button onClick={handleSubmitComment} style={{ marginTop: '5px' }}>
                            Submit Comment
                        </button>
                    </div>
                ) : (
                    <p><em>Login to comment.</em></p>
                )}
            </div>

        </div>   
     );
}

export default NewsDetail;

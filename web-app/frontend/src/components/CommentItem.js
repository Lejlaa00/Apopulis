import React, { useState } from 'react';

function CommentItem({ comment, user, onReply, onDelete, onEdit, childComments = [] }) {
    const [showReply, setShowReply] = useState(false);
    const [replyContent, setReplyContent] = useState('');

    const handleSubmitReply = () => {
        if (!replyContent.trim()) {
            alert("Comment cannot be empty.");
            return;
        }
        onReply(comment._id, replyContent);
        setReplyContent('');
        setShowReply(false);
    };

    return (
        <div
            style={{
                marginLeft: comment.parentCommentId ? '20px' : '0',
                marginBottom: '10px',
                borderLeft: comment.parentCommentId ? '2px solid #eee' : 'none',
                paddingLeft: comment.parentCommentId ? '10px' : '0',
                fontSize: comment.parentCommentId ? '0.9em' : '1em',
            }}
        >
            <strong>{comment.userId?.username || 'Anonymous'}</strong>: {comment.content}
            <br />
            <small style={{ color: '#777' }}>{new Date(comment.createdAt).toLocaleString()}</small>

            {user && user.id === comment.userId?._id && (
                <>
                    <button onClick={() => onEdit(comment)} style={{ marginLeft: 10 }}>✏️</button>
                    <button onClick={() => onDelete(comment._id)} style={{ color: 'red', marginLeft: 5 }}>🗑️</button>
                </>
            )}

            {user && !comment.parentCommentId && (
                <button onClick={() => setShowReply(!showReply)} style={{ marginLeft: 10 }}>
                    💬 Reply
                </button>
            )}

            {showReply && (
                <div style={{ marginTop: 5 }}>
                    <textarea
                        rows={2}
                        value={replyContent}
                        onChange={(e) => setReplyContent(e.target.value)}
                        style={{ width: '100%' }}
                        placeholder="Write a reply..."
                    />
                    <button onClick={handleSubmitReply} style={{ marginTop: 5 }}>Submit Reply</button>
                </div>
            )}

            {childComments.length > 0 && (
                <div
                    style={{
                        marginTop: '8px',
                        paddingLeft: '15px',
                        borderLeft: '2px solid #ccc',
                        fontSize: '0.9em',
                    }}
                >
                    {childComments.map(reply => (
                        <CommentItem
                            key={reply._id}
                            comment={reply}
                            user={user}
                            onReply={onReply}
                            onDelete={onDelete}
                            onEdit={onEdit}
                            childComments={[]} // No recursion allowed
                        />
                    ))}
                </div>
            )}
        </div>
    );
}

export default CommentItem;

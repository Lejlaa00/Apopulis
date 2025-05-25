import React, { useState } from 'react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faReply, faPen, faTrash } from '@fortawesome/free-solid-svg-icons';

function CommentItem({
    comment,
    user,
    onReply,
    onDelete,
    onEdit,
    onEditStart,
    onEditCancel,
    isEditing,
    editedContent,
    setEditedContent,
  }) {
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
                marginLeft: comment.parentCommentId ? '18px' : '0',
                marginBottom: '10px',
                borderLeft: comment.parentCommentId ? '2px solid #eee' : 'none',
                paddingLeft: comment.parentCommentId ? '10px' : '0',
                fontSize: comment.parentCommentId ? '0.9em' : '1em',
            }}
        >
            <div className="comment-text">
                <span className="comment-author">{comment.userId?.username || 'Anonymous'}</span>
                {isEditing ? (
                    <>
                        <textarea
                            value={editedContent}
                            onChange={(e) => setEditedContent(e.target.value)}
                            rows={2}
                            style={{ width: '100%', marginTop: '5px' }}
                        />
                        <button onClick={() => onEdit(comment._id)}>Save</button>
                        <button onClick={onEditCancel}>Cancel</button>
                    </>
                ) : (
                    <span>: {comment.content}</span>
                )}
                <div className="comment-date">{new Date(comment.createdAt).toLocaleString()}</div>
            </div>


            <div className="comment-actions">
            <div className="comment-actions-left">
                {user && !comment.parentCommentId && (
                <button onClick={() => setShowReply(!showReply)}>
                    <FontAwesomeIcon icon={faReply} className="comment-action-icon" /> Reply
                </button>
                )}
            </div>
            <div className="comment-actions-right">
                {user && user.id === comment.userId?._id && (
                <>
                    <button onClick={() => onEditStart(comment)}>
                    <FontAwesomeIcon icon={faPen} className="comment-action-icon" />
                    </button>
                    <button onClick={() => onDelete(comment._id)}>
                    <FontAwesomeIcon icon={faTrash} className="comment-action-icon" />
                    </button>
                </>
                )}
            </div>
            </div>

           {showReply && (
            <div className="reply-box">
                <textarea
                className="reply-textarea"
                rows={2}
                value={replyContent}
                onChange={(e) => setReplyContent(e.target.value)}
                placeholder="Write a reply..."
                />
                <button className="reply-button" onClick={handleSubmitReply}>
                Submit Reply
                </button>
            </div>
            )}

            {comment.replies?.length > 0 && (
            <div style={{ marginTop: '8px', paddingLeft: '1px', fontSize: '0.9em' }}>
                {comment.replies.map(reply => (
                <CommentItem
                    key={reply._id}
                    comment={reply}
                    user={user}
                    onReply={onReply}
                    onDelete={onDelete}
                    onEdit={onEdit}
                />
                ))}
            </div>
            )}
        </div>
    );
}

export default CommentItem;
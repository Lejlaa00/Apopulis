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
    const [showDeleteModal, setShowDeleteModal] = useState(false);


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
                            className="reply-textarea"
                            value={editedContent}
                            onChange={(e) => setEditedContent(e.target.value)}
                            rows={2}
                        />
                        <div style={{ display: 'flex', gap: '8px', marginTop: '6px' }}>
                            <button className="reply-button" onClick={() => onEdit(comment._id, editedContent)}>Save</button>
                            <button className="reply-button" onClick={onEditCancel}>Cancel</button>
                        </div>
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
                    <button onClick={() => setShowDeleteModal(true)}>
                        <FontAwesomeIcon icon={faTrash} className="comment-action-icon" />
                    </button> 
                </>
                )}
            </div>
            {showDeleteModal && (
                <div className="news-popup-overlay">
                    <div className="delete-confirm-modal">
                    <p>Are you sure you want to delete this comment?</p>
                    <div className="delete-confirm-buttons">
                        <button
                        className="reply-button"
                        onClick={() => {
                            onDelete(comment._id);
                            setShowDeleteModal(false);
                        }}
                        >
                        Yes
                        </button>
                        <button className="reply-button" onClick={() => setShowDeleteModal(false)}>
                        No
                        </button>
                    </div>
                    </div>
                </div>
            )}
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
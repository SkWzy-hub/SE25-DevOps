import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAppContext } from '../App';
import { getItemMessages, addMessage, deleteMessage } from '../service/message';
import './MessageList.css';

const MessageList = ({ itemId }) => {
  const navigate = useNavigate();
  const { currentUser } = useAppContext();
  
  const [messages, setMessages] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [newMessage, setNewMessage] = useState('');
  const [replyTo, setReplyTo] = useState(null);
  const [replyContent, setReplyContent] = useState('');

  // 检查是否可以删除留言
  const canDeleteMessage = (userId) => {
    return currentUser && (
      currentUser.id === userId || // 是留言的作者
      currentUser.role === 'admin' // 或者是管理员
    );
  };

  // 加载留言
  useEffect(() => {
    const fetchMessages = async () => {
      try {
        setLoading(true);
        const data = await getItemMessages(itemId);
        setMessages(data);
      } catch (err) {
        setError('加载留言失败');
      } finally {
        setLoading(false);
      }
    };

    fetchMessages();
  }, [itemId]);

  // 发布留言
  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!currentUser) {
      navigate('/login');
      return;
    }

    if (!newMessage.trim()) {
      return;
    }

    try {
      const message = await addMessage(itemId, newMessage);
      if (message) {
        setMessages(prev => [message, ...prev]);
        setNewMessage('');
      }
    } catch (err) {
      console.error('发布留言失败:', err);
    }
  };

  // 发布回复
  const handleReply = async (messageId, parentId = null) => {
    if (!currentUser) {
      navigate('/login');
      return;
    }

    if (!replyContent.trim()) {
      return;
    }

    try {
      const reply = await addMessage(itemId, replyContent, parentId || messageId);
      if (reply) {
        setMessages(prev => prev.map(msg => {
          if (msg.messageId === (parentId || messageId)) {
            return {
              ...msg,
              replies: [...(msg.replies || []), reply]
            };
          }
          return msg;
        }));
        setReplyTo(null);
        setReplyContent('');
      }
    } catch (err) {
      console.error('发布回复失败:', err);
    }
  };

  // 删除留言或回复
  const handleDelete = async (messageId, parentId = null) => {
    if (!currentUser) {
      return;
    }

    if (window.confirm('确定要删除这条留言吗？')) {
      try {
        const success = await deleteMessage(messageId);
        if (success) {
          if (parentId) {
            // 删除回复
            setMessages(prev => prev.map(msg => {
              if (msg.messageId === parentId) {
                return {
                  ...msg,
                  replies: msg.replies.filter(reply => reply.messageId !== messageId)
                };
              }
              return msg;
            }));
          } else {
            // 删除主留言
            setMessages(prev => prev.filter(msg => msg.messageId !== messageId));
          }
        }
      } catch (err) {
        console.error('删除留言失败:', err);
      }
    }
  };

  if (loading) {
    return (
      <div className="message-list loading">
        <i className="fas fa-spinner fa-spin"></i>
        <p>加载留言中...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="message-list error">
        <i className="fas fa-exclamation-circle"></i>
        <p>{error}</p>
        <button onClick={() => window.location.reload()}>重试</button>
      </div>
    );
  }

  return (
    <div className="message-list">
      {/* 发布留言表单 */}
      <form className="message-form" onSubmit={handleSubmit}>
        <textarea
          value={newMessage}
          onChange={(e) => setNewMessage(e.target.value)}
          placeholder="写下你的留言..."
          rows="3"
        />
        <button type="submit" disabled={!newMessage.trim()}>
          发布留言
        </button>
      </form>

      {/* 留言列表 */}
      <div className="messages">
        {messages.map(message => (
          <div key={message.messageId} className="message">
            <div className="message-header">
              <span className="username">{message.username}</span>
              <span className="time">
                {new Date(message.replyTime).toLocaleString()}
              </span>
            </div>
            <div className="message-content">{message.content}</div>
            <div className="message-actions">
              <button onClick={() => setReplyTo(message.messageId)}>
                回复
              </button>
              {canDeleteMessage(message.userId) && (
                <button 
                  className="delete-btn"
                  onClick={() => handleDelete(message.messageId)}
                >
                  删除
                </button>
              )}
            </div>

            {/* 回复表单 */}
            {replyTo === message.messageId && (
              <div className="reply-form">
                <textarea
                  value={replyContent}
                  onChange={(e) => setReplyContent(e.target.value)}
                  placeholder="写下你的回复..."
                  rows="2"
                />
                <div className="reply-actions">
                  <button
                    onClick={() => handleReply(message.messageId)}
                    disabled={!replyContent.trim()}
                  >
                    发布回复
                  </button>
                  <button onClick={() => setReplyTo(null)}>取消</button>
                </div>
              </div>
            )}

            {/* 回复列表 */}
            {message.replies && message.replies.length > 0 && (
              <div className="replies">
                {message.replies.map(reply => (
                  <div key={reply.messageId} className="reply">
                    <div className="reply-header">
                      <span className="username">{reply.username}</span>
                      <span className="time">
                        {new Date(reply.replyTime).toLocaleString()}
                      </span>
                    </div>
                    <div className="reply-content">{reply.content}</div>
                    <div className="reply-actions">
                      <button onClick={() => setReplyTo(reply.messageId)}>
                        回复
                      </button>
                      {canDeleteMessage(reply.userId) && (
                        <button 
                          className="delete-btn"
                          onClick={() => handleDelete(reply.messageId, message.messageId)}
                        >
                          删除
                        </button>
                      )}
                    </div>
                    {/* 对回复的回复表单 */}
                    {replyTo === reply.messageId && (
                      <div className="reply-form">
                        <textarea
                          value={replyContent}
                          onChange={(e) => setReplyContent(e.target.value)}
                          placeholder="写下你的回复..."
                          rows="2"
                        />
                        <div className="reply-actions">
                          <button
                            onClick={() => handleReply(reply.messageId, message.messageId)}
                            disabled={!replyContent.trim()}
                          >
                            发布回复
                          </button>
                          <button onClick={() => setReplyTo(null)}>取消</button>
                        </div>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        ))}
      </div>

      {messages.length === 0 && (
        <div className="no-messages">
          <i className="far fa-comments"></i>
          <p>暂无留言</p>
        </div>
      )}
    </div>
  );
};

export default MessageList; 
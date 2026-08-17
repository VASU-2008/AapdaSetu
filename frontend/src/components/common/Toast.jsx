import React from 'react';
import { CheckCircle, AlertCircle, Info, X } from 'lucide-react';
import './Toast.css';

const Toast = ({ toast, onClose }) => {
  if (!toast) return null;

  const { message, type } = toast;

  const getIcon = () => {
    switch (type) {
      case 'success':
        return <CheckCircle size={18} className="toast-icon success" />;
      case 'error':
        return <AlertCircle size={18} className="toast-icon error" />;
      default:
        return <Info size={18} className="toast-icon info" />;
    }
  };

  return (
    <div className={`toast-notification toast-${type}`}>
      <div className="toast-body">
        {getIcon()}
        <span className="toast-text">{message}</span>
      </div>
      {onClose && (
        <button type="button" className="toast-close" onClick={onClose}>
          <X size={14} />
        </button>
      )}
    </div>
  );
};

export default Toast;

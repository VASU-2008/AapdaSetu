import React from 'react';
import { ArrowRight, Loader2 } from 'lucide-react';
import './Button.css';

const Button = ({
  children,
  onClick,
  type = 'button',
  variant = 'primary',
  loading = false,
  disabled = false,
  showArrow = true,
  className = ''
}) => {
  return (
    <button
      type={type}
      onClick={onClick}
      disabled={disabled || loading}
      className={`custom-btn btn-${variant} ${loading ? 'is-loading' : ''} ${className}`}
    >
      {loading ? (
        <span className="btn-loading-content">
          <Loader2 className="btn-spinner" size={18} />
          <span>Processing...</span>
        </span>
      ) : (
        <span className="btn-content">
          <span className="btn-text">{children}</span>
          {showArrow && <ArrowRight size={18} strokeWidth={2.2} className="btn-arrow" />}
        </span>
      )}
    </button>
  );
};

export default Button;

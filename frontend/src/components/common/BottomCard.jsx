import React from 'react';
import { ArrowRight } from 'lucide-react';
import './BottomCard.css';

const BottomCard = ({ prompt, actionText, onAction }) => {
  return (
    <div className="bottom-account-card">
      <div className="bottom-card-text">
        <span className="bottom-card-prompt">{prompt}</span>
        <button type="button" className="bottom-card-link" onClick={onAction}>
          {actionText}
        </button>
      </div>

      <button
        type="button"
        className="bottom-card-arrow-btn"
        onClick={onAction}
        aria-label={actionText}
      >
        <ArrowRight size={17} strokeWidth={2.2} />
      </button>
    </div>
  );
};

export default BottomCard;

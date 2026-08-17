import React from 'react';
import './HeaderBadge.css';

const HeaderBadge = ({ type = 'lock' }) => {
  return (
    <div className="header-badge-wrapper">
      {/* Decorative floating blur sphere */}
      <div className="badge-floating-sphere" />

      {/* Glossy Neumorphic 3D Card Base */}
      <div className="badge-card-base">
        {type === 'lock' ? (
          <div className="badge-icon-3d lock-3d">
            <svg viewBox="0 0 100 100" className="badge-svg">
              <defs>
                <linearGradient id="lockBody" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" stopColor="#8176ff" />
                  <stop offset="50%" stopColor="#6355fa" />
                  <stop offset="100%" stopColor="#4335da" />
                </linearGradient>
                <linearGradient id="lockShackle" x1="0%" y1="0%" x2="0%" y2="100%">
                  <stop offset="0%" stopColor="#c5beff" />
                  <stop offset="100%" stopColor="#867aff" />
                </linearGradient>
                <filter id="badgeShadow" x="-20%" y="-20%" width="140%" height="140%">
                  <feDropShadow dx="0" dy="6" stdDeviation="6" floodColor="#4335da" floodOpacity="0.35" />
                </filter>
              </defs>
              {/* Shackle */}
              <path
                d="M34 45 V 30 A 16 16 0 0 1 66 30 V 45"
                fill="none"
                stroke="url(#lockShackle)"
                strokeWidth="10"
                strokeLinecap="round"
              />
              {/* Body */}
              <rect
                x="22"
                y="40"
                width="56"
                height="48"
                rx="16"
                fill="url(#lockBody)"
                filter="url(#badgeShadow)"
              />
              {/* Keyhole */}
              <circle cx="50" cy="60" r="5" fill="#24197a" />
              <polygon points="47.5,60 52.5,60 54,72 46,72" fill="#24197a" />
              {/* Highlight Glint */}
              <ellipse cx="36" cy="48" rx="8" ry="4" fill="rgba(255,255,255,0.4)" transform="rotate(-15 36 48)" />
            </svg>
          </div>
        ) : (
          <div className="badge-icon-3d user-3d">
            <svg viewBox="0 0 100 100" className="badge-svg">
              <defs>
                <linearGradient id="userBody" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" stopColor="#8b80ff" />
                  <stop offset="60%" stopColor="#6355fa" />
                  <stop offset="100%" stopColor="#4538d6" />
                </linearGradient>
                <linearGradient id="plusBadge" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" stopColor="#5544ea" />
                  <stop offset="100%" stopColor="#3524cc" />
                </linearGradient>
                <filter id="userShadow" x="-20%" y="-20%" width="140%" height="140%">
                  <feDropShadow dx="0" dy="6" stdDeviation="6" floodColor="#4335da" floodOpacity="0.35" />
                </filter>
              </defs>
              {/* Head */}
              <circle cx="46" cy="30" r="16" fill="url(#userBody)" filter="url(#userShadow)" />
              {/* Shoulders */}
              <path
                d="M20 74 C20 54, 30 50, 46 50 C62 50, 72 54, 72 74"
                fill="url(#userBody)"
                filter="url(#userShadow)"
              />
              {/* Plus Badge */}
              <circle cx="68" cy="68" r="14" fill="url(#plusBadge)" />
              <path
                d="M68 61 V 75 M61 68 H 75"
                stroke="#ffffff"
                strokeWidth="3.5"
                strokeLinecap="round"
              />
              {/* Highlight */}
              <ellipse cx="40" cy="24" rx="6" ry="3" fill="rgba(255,255,255,0.4)" transform="rotate(-20 40 24)" />
            </svg>
          </div>
        )}
      </div>
    </div>
  );
};

export default HeaderBadge;

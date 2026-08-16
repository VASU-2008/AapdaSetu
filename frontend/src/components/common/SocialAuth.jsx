import React from 'react';
import { Phone } from 'lucide-react';
import './SocialAuth.css';

const SocialAuth = ({ onSocialSelect }) => {
  return (
    <div className="social-auth-section">
      <div className="social-divider">
        <span className="divider-line" />
        <span className="divider-text">or continue with</span>
        <span className="divider-line" />
      </div>

      <div className="social-buttons-grid">
        {/* Google Card */}
        <button
          type="button"
          className="social-btn-card"
          onClick={() => onSocialSelect && onSocialSelect('Google')}
          title="Sign in with Google"
        >
          <div className="social-icon-wrapper">
            <svg viewBox="0 0 24 24" className="social-svg-icon" width="22" height="22">
              <path
                d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
                fill="#4285F4"
              />
              <path
                d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
                fill="#34A853"
              />
              <path
                d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z"
                fill="#FBBC05"
              />
              <path
                d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z"
                fill="#EA4335"
              />
            </svg>
          </div>
          <span className="social-btn-label">Google</span>
        </button>

        {/* Apple Card */}
        <button
          type="button"
          className="social-btn-card"
          onClick={() => onSocialSelect && onSocialSelect('Apple')}
          title="Sign in with Apple"
        >
          <div className="social-icon-wrapper">
            <svg viewBox="0 0 170 170" className="social-svg-icon apple-icon" width="20" height="20">
              <path
                fill="#000000"
                d="M150.37 130.25c-2.45 5.66-5.35 10.87-8.71 15.66-4.58 6.53-8.33 11.05-11.22 13.56-4.48 4.12-9.28 6.23-14.42 6.35-3.69 0-8.14-1.05-13.32-3.18-5.19-2.12-9.97-3.17-14.34-3.17-4.58 0-9.49 1.05-14.75 3.17-5.26 2.13-9.5 3.24-12.74 3.35-4.35.13-9.16-1.9-14.42-6.08-3.69-3.04-7.67-7.81-11.96-14.34-6.3-9.59-11.17-20.73-14.61-33.4-3.44-12.67-5.16-24.32-5.16-34.94 0-14.34 3.75-26.06 11.25-35.17 7.5-9.11 16.89-13.78 28.18-14.01 4.79 0 10.22 1.25 16.29 3.75 6.07 2.5 10.13 3.75 12.18 3.75 1.58 0 5.86-1.36 12.83-4.07 6.97-2.72 12.82-3.83 17.55-3.34 13.06 1.07 23.44 6.32 31.14 15.75-11.5 6.94-17.06 16.5-16.68 28.69.38 9.56 4.13 17.65 11.25 24.26 7.12 6.61 15.72 10.37 25.8 11.28-2.22 6.95-4.99 14.15-8.31 21.61zM119.22 33.72c0-7.39 2.65-14.4 7.94-21.03 5.3-6.64 11.83-11.02 19.6-13.15.52 1.34.78 2.67.78 3.99 0 7.39-2.73 14.51-8.19 21.36-5.46 6.85-12.02 11.18-19.68 13-0.29-1.44-.45-2.83-.45-4.17z"
              />
            </svg>
          </div>
          <span className="social-btn-label">Apple</span>
        </button>

        {/* Phone Card */}
        <button
          type="button"
          className="social-btn-card"
          onClick={() => onSocialSelect && onSocialSelect('Phone')}
          title="Sign in with Phone"
        >
          <div className="social-icon-wrapper phone-icon-wrapper">
            <Phone size={18} strokeWidth={2.4} className="phone-icon-svg" />
          </div>
          <span className="social-btn-label">Phone</span>
        </button>
      </div>
    </div>
  );
};

export default SocialAuth;

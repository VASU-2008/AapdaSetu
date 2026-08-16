import React, { useState } from 'react';
import { Mail, Lock, Check } from 'lucide-react';
import StatusBar from '../common/StatusBar';
import HeaderBadge from '../common/HeaderBadge';
import InputField from '../common/InputField';
import Button from '../common/Button';
import SocialAuth from '../common/SocialAuth';
import BottomCard from '../common/BottomCard';
import { useAuth } from '../../context/AuthContext';
import './AuthViews.css';

const LoginView = ({ onSwitchToRegister }) => {
  const { login, loading, showToast } = useAuth();
  
  const [formData, setFormData] = useState({
    email: '',
    password: '',
    rememberMe: true,
  });

  const [errors, setErrors] = useState({});

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value,
    }));
    if (errors[name]) {
      setErrors((prev) => ({ ...prev, [name]: null }));
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const newErrors = {};
    if (!formData.email) newErrors.email = 'Email is required';
    if (!formData.password) newErrors.password = 'Password is required';

    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }

    await login(formData.email, formData.password, formData.rememberMe);
  };

  const handleSocial = (provider) => {
    showToast(`Signing in with ${provider}...`, 'info');
  };

  const handleForgotPassword = () => {
    showToast('Password reset instructions sent to your email.', 'info');
  };

  return (
    <div className="auth-phone-card">
      <StatusBar time="9:41" />

      <div className="auth-card-content">
        {/* Header with Title and 3D Badge */}
        <div className="auth-header-row">
          <div className="auth-title-block">
            <h1 className="auth-main-heading">
              Welcome back <span className="auth-emoji">👋</span>
            </h1>
            <p className="auth-sub-heading">Login to continue your journey</p>
          </div>
          <HeaderBadge type="lock" />
        </div>

        {/* Form Fields */}
        <form onSubmit={handleSubmit} className="auth-form-body">
          <InputField
            label="Email"
            name="email"
            type="email"
            placeholder="you@example.com"
            icon={Mail}
            value={formData.email}
            onChange={handleChange}
            error={errors.email}
            required
            autoComplete="email"
          />

          <InputField
            label="Password"
            name="password"
            type="password"
            placeholder="Enter your password"
            icon={Lock}
            value={formData.password}
            onChange={handleChange}
            error={errors.password}
            required
            autoComplete="current-password"
          />

          {/* Remember me & Forgot password */}
          <div className="auth-options-row">
            <label className="custom-checkbox-label">
              <input
                type="checkbox"
                name="rememberMe"
                checked={formData.rememberMe}
                onChange={handleChange}
                className="hidden-checkbox"
              />
              <span className={`custom-checkbox-box ${formData.rememberMe ? 'is-checked' : ''}`}>
                {formData.rememberMe && <Check size={12} strokeWidth={3.5} />}
              </span>
              <span className="checkbox-text">Remember me</span>
            </label>

            <button
              type="button"
              className="forgot-password-link"
              onClick={handleForgotPassword}
            >
              Forgot password?
            </button>
          </div>

          {/* Submit Button */}
          <Button
            type="submit"
            variant="primary"
            loading={loading}
          >
            Login
          </Button>

          {/* Social Auth */}
          <SocialAuth onSocialSelect={handleSocial} />

          {/* Bottom Card Navigation */}
          <BottomCard
            prompt="Don't have an account?"
            actionText="Create an account"
            onAction={onSwitchToRegister}
          />
        </form>
      </div>

      {/* iOS Home Indicator Bar */}
      <div className="ios-home-indicator" />
    </div>
  );
};

export default LoginView;

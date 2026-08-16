import React, { useState } from 'react';
import { User, Mail, Lock, ArrowLeft } from 'lucide-react';
import StatusBar from '../common/StatusBar';
import HeaderBadge from '../common/HeaderBadge';
import InputField from '../common/InputField';
import Button from '../common/Button';
import PasswordChecklist from '../common/PasswordChecklist';
import SocialAuth from '../common/SocialAuth';
import BottomCard from '../common/BottomCard';
import { useAuth } from '../../context/AuthContext';
import './AuthViews.css';

const RegisterView = ({ onSwitchToLogin }) => {
  const { register, loading, showToast } = useAuth();

  const [formData, setFormData] = useState({
    fullName: '',
    email: '',
    password: '',
    confirmPassword: '',
  });

  const [errors, setErrors] = useState({});

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }));
    if (errors[name]) {
      setErrors((prev) => ({ ...prev, [name]: null }));
    }
  };

  const validateForm = () => {
    const newErrors = {};
    if (!formData.fullName.trim()) newErrors.fullName = 'Full name is required';
    if (!formData.email.trim()) {
      newErrors.email = 'Email is required';
    } else if (!/\S+@\S+\.\S+/.test(formData.email)) {
      newErrors.email = 'Please enter a valid email address';
    }

    if (!formData.password) {
      newErrors.password = 'Password is required';
    } else {
      if (formData.password.length < 8) newErrors.password = 'Must be at least 8 characters';
      else if (!/\d/.test(formData.password)) newErrors.password = 'Must include at least one number';
      else if (!/[A-Z]/.test(formData.password)) newErrors.password = 'Must include an uppercase letter';
    }

    if (formData.password !== formData.confirmPassword) {
      newErrors.confirmPassword = 'Passwords do not match';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validateForm()) return;

    await register(formData.fullName, formData.email, formData.password);
  };

  const handleSocial = (provider) => {
    showToast(`Registering with ${provider}...`, 'info');
  };

  return (
    <div className="auth-phone-card">
      <StatusBar time="9:41" />

      <div className="auth-card-content">
        {/* Back navigation button */}
        <div className="auth-nav-row">
          <button
            type="button"
            className="auth-back-button"
            onClick={onSwitchToLogin}
            aria-label="Go back to login"
          >
            <ArrowLeft size={18} strokeWidth={2.4} />
          </button>
        </div>

        {/* Header with Title and 3D Badge */}
        <div className="auth-header-row">
          <div className="auth-title-block">
            <h1 className="auth-main-heading">
              Create account <span className="auth-sparkle">✨</span>
            </h1>
            <p className="auth-sub-heading">Join us and start your journey</p>
          </div>
          <HeaderBadge type="user" />
        </div>

        {/* Form Fields */}
        <form onSubmit={handleSubmit} className="auth-form-body">
          <InputField
            label="Full Name"
            name="fullName"
            type="text"
            placeholder="Enter your full name"
            icon={User}
            value={formData.fullName}
            onChange={handleChange}
            error={errors.fullName}
            required
            autoComplete="name"
          />

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
            placeholder="Create a password"
            icon={Lock}
            value={formData.password}
            onChange={handleChange}
            error={errors.password}
            required
            autoComplete="new-password"
          />

          <InputField
            label="Confirm Password"
            name="confirmPassword"
            type="password"
            placeholder="Confirm your password"
            icon={Lock}
            value={formData.confirmPassword}
            onChange={handleChange}
            error={errors.confirmPassword}
            required
            autoComplete="new-password"
          />

          {/* Real-time Password checklist */}
          <PasswordChecklist password={formData.password} />

          {/* Submit Button */}
          <Button
            type="submit"
            variant="primary"
            loading={loading}
          >
            Register
          </Button>

          {/* Social Auth */}
          <SocialAuth onSocialSelect={handleSocial} />

          {/* Bottom Card Navigation */}
          <BottomCard
            prompt="Already have an account?"
            actionText="Login"
            onAction={onSwitchToLogin}
          />
        </form>
      </div>

      {/* iOS Home Indicator Bar */}
      <div className="ios-home-indicator" />
    </div>
  );
};

export default RegisterView;

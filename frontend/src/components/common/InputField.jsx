import React, { useState } from 'react';
import { Eye, EyeOff } from 'lucide-react';
import './InputField.css';

const InputField = ({
  label,
  type = 'text',
  name,
  value,
  onChange,
  placeholder,
  icon: Icon,
  required = false,
  error,
  autoComplete
}) => {
  const [showPassword, setShowPassword] = useState(false);
  const isPassword = type === 'password';

  const inputType = isPassword ? (showPassword ? 'text' : 'password') : type;

  return (
    <div className={`input-field-group ${error ? 'has-error' : ''}`}>
      {label && <label className="input-label" htmlFor={name}>{label}</label>}
      <div className="input-box-wrapper">
        {Icon && (
          <div className="input-leading-icon">
            <Icon size={19} strokeWidth={1.8} />
          </div>
        )}
        
        <input
          id={name}
          name={name}
          type={inputType}
          value={value}
          onChange={onChange}
          placeholder={placeholder}
          required={required}
          autoComplete={autoComplete}
          className={`input-element ${Icon ? 'with-leading-icon' : ''} ${isPassword ? 'with-trailing-icon' : ''}`}
        />

        {isPassword && (
          <button
            type="button"
            className="input-trailing-action"
            onClick={() => setShowPassword(!showPassword)}
            tabIndex={-1}
            aria-label={showPassword ? 'Hide password' : 'Show password'}
          >
            {showPassword ? (
              <EyeOff size={18} strokeWidth={1.8} />
            ) : (
              <Eye size={18} strokeWidth={1.8} />
            )}
          </button>
        )}
      </div>
      {error && <span className="input-error-msg">{error}</span>}
    </div>
  );
};

export default InputField;

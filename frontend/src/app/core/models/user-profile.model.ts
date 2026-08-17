export interface UserProfile {
  id: number;
  username: string;
  firstName?: string;
  lastName?: string;
  email?: string;
  phone?: string;
  roles?: string[];
  specialty?: string;
  enabled?: boolean;
}

export interface CreateUserRequest {
  username: string;
  password?: string;
  firstName: string;
  lastName: string;
  email?: string;
  phone?: string;
  roles?: string[];
  specialty: string;
}

export interface AdminUpdateUserRequest {
  firstName: string;
  lastName: string;
  email?: string;
  phone?: string;
  roles?: string[];
  specialty: string;
  enabled?: boolean;
}

export interface AdminResetPasswordRequest {
  newPassword: string;
}

export interface UpdateProfileRequest {
  firstName?: string;
  lastName?: string;
  email?: string;
  phone?: string;
}

export interface UpdatePasswordRequest {
  currentPassword?: string;
  newPassword?: string;
}

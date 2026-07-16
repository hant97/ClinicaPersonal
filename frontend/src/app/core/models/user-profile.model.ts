export interface UserProfile {
  id: number;
  username: string;
  firstName?: string;
  lastName?: string;
  email?: string;
  phone?: string;
  roles?: string[];
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

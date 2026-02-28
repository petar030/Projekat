export interface AdminUserItem {
  id?: number;
  username?: string;
  ime?: string;
  prezime?: string;
  email?: string;
  telefon?: string;
  role?: string;
  status?: string;
}

export interface AdminUsersResponse {
  content?: AdminUserItem[];
}

export interface AdminUpdateUserRequest {
  ime: string;
  prezime: string;
  telefon: string;
  email: string;
  status: 'na_cekanju' | 'odobren' | 'odbijen';
}

export interface AdminUserStatusResponse {
  userId?: number;
  status?: string;
}

export interface AdminRegistrationRequestItem {
  userId?: number;
  username?: string;
  role?: string;
  status?: string;
  createdAt?: string;
}

export interface AdminRegistrationRequestsResponse {
  requests?: AdminRegistrationRequestItem[];
}

export interface AdminSpaceRequestItem {
  spaceId?: number;
  naziv?: string;
  grad?: string;
  firmaNaziv?: string;
  status?: string;
}

export interface AdminSpaceRequestsResponse {
  requests?: AdminSpaceRequestItem[];
}

export interface AdminSpaceStatusResponse {
  spaceId?: number;
  status?: string;
}

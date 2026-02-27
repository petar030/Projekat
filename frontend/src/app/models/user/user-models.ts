export class UserFirmaDto {
  id?: number;
  naziv?: string;
  adresa?: string;
  maticniBroj?: string;
  pib?: string;
}

export class UserProfileResponse {
  id?: number;
  username?: string;
  ime?: string;
  prezime?: string;
  telefon?: string;
  email?: string;
  profilnaSlika?: string;
  uloga?: string;
  status?: string;
  firma?: UserFirmaDto | null;
}

export class UpdateUserProfileRequest {
  ime?: string;
  prezime?: string;
  telefon?: string;
  email?: string;
}

export class UpdateProfileImageResponse {
  profileImage?: string;
}

export class UpdatePasswordRequest {
  newPassword?: string;
}
export class PublicHomeSpace {
  spaceId?: number;
  naziv?: string;
  grad?: string;
  likes?: number;
  dislikes?: number;
}

export class PublicHomeResponse {
  totalApprovedSpaces?: number;
  top5Spaces?: PublicHomeSpace[];
}

export class PublicCitiesResponse {
  cities?: string[];
}

export class PublicSearchItem {
  id?: number;
  naziv?: string;
  grad?: string;
  adresa?: string;
  firmaNaziv?: string;
  likes?: number;
  dislikes?: number;
}

export class PublicSearchResponse {
  content?: PublicSearchItem[];
}

export class PublicSearchRequest {
  name?: string;
  cities?: string[];
  sortBy?: 'naziv' | 'grad';
  sortDir?: 'asc' | 'desc';
}

export class PublicFirma {
  id?: number;
  naziv?: string;
}

export class PublicMenadzer {
  id?: number;
  imePrezime?: string;
}

export class PublicGeolocation {
  lat?: number;
  lng?: number;
}

export class PublicReactions {
  likes?: number;
  dislikes?: number;
}

export class PublicLatestComment {
  id?: number;
  username?: string;
  createdAt?: string;
  text?: string;
}

export class PublicSpaceDetailsResponse {
  id?: number;
  naziv?: string;
  grad?: string;
  adresa?: string;
  opis?: string;
  cenaPoSatu?: number;
  firma?: PublicFirma;
  menadzer?: PublicMenadzer;
  geolocation?: PublicGeolocation;
  reactions?: PublicReactions;
  images?: string[];
  latestComments?: PublicLatestComment[];
}

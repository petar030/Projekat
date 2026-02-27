export class MemberReservationItem {
  id?: number;
  spaceId?: number;
  spaceName?: string;
  city?: string;
  from?: string;
  to?: string;
  status?: string;
  cancellable?: boolean;
}

export class MemberReservationsResponse {
  content?: MemberReservationItem[];
}

export class MemberCancelReservationResponse {
  id?: number;
  status?: string;
}

export class MemberSearchRequest {
  name?: string;
  cities?: string[];
  type?: 'otvoreni' | 'kancelarija' | 'sala';
  officeMinDesks?: number;
}

export class MemberSearchItem {
  id?: number;
  naziv?: string;
  grad?: string;
  adresa?: string;
  firmaNaziv?: string;
  likes?: number;
  dislikes?: number;
  openDesks?: number;
  officeCount?: number;
  maxOfficeDesks?: number;
  meetingRoomCount?: number;
  matchingSubspaceIds?: number[];
}

export class MemberSearchResponse {
  content?: MemberSearchItem[];
}
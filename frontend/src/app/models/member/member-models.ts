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
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

export class MemberAvailabilityRequest {
  type?: 'otvoreni' | 'kancelarija' | 'sala';
  resourceIds?: number[];
  weekStart?: string;
}

export class MemberBusySlot {
  from?: string;
  to?: string;
}

export class MemberAvailabilityResource {
  resourceId?: number;
  resourceName?: string;
  additionalEquipment?: string;
  busySlots?: MemberBusySlot[];
}

export class MemberAvailabilityResponse {
  spaceId?: number;
  type?: string;
  weekStart?: string;
  resources?: MemberAvailabilityResource[];
}

export class MemberCreateReservationRequest {
  spaceId?: number;
  type?: 'otvoreni' | 'kancelarija' | 'sala';
  resourceId?: number;
  from?: string;
  to?: string;
}

export class MemberCreateReservationResponse {
  id?: number;
  status?: string;
  spaceId?: number;
  type?: string;
  resourceId?: number;
  from?: string;
  to?: string;
}

export class MemberCreateReactionRequest {
  tip?: 'svidjanje' | 'nesvidjanje';
}

export class MemberCreateReactionResponse {
  id?: number;
  spaceId?: number;
  userId?: number;
  tip?: string;
  createdAt?: string;
}

export class MemberCreateCommentRequest {
  text?: string;
}

export class MemberCommentItem {
  id?: number;
  userId?: number;
  username?: string;
  text?: string;
  createdAt?: string;
  mine?: boolean;
}

export class MemberLatestCommentsResponse {
  comments?: MemberCommentItem[];
}

export class MemberCreateCommentResponse {
  id?: number;
  spaceId?: number;
  userId?: number;
  username?: string;
  text?: string;
  createdAt?: string;
}
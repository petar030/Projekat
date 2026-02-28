export class ManagerOpenSpace {
  id?: number;
  brojStolova?: number;
}

export class ManagerOffice {
  id?: number;
  naziv?: string;
  brojStolova?: number;
}

export class ManagerMeetingRoom {
  id?: number;
  naziv?: string;
  brojMesta?: number;
  dodatnaOprema?: string;
}

export class ManagerSpaceElements {
  openSpace?: ManagerOpenSpace | null;
  offices?: ManagerOffice[];
  meetingRooms?: ManagerMeetingRoom[];
}

export class ManagerSpaceItem {
  id?: number;
  naziv?: string;
  grad?: string;
  status?: string;
  pragKazni?: number;
  elements?: ManagerSpaceElements;
}

export class ManagerSpacesResponse {
  spaces?: ManagerSpaceItem[];
}

export class ManagerCreateOfficeRequest {
  naziv?: string;
  brojStolova?: number;
}

export class ManagerOfficeResponse {
  id?: number;
  spaceId?: number;
  naziv?: string;
  brojStolova?: number;
}

export class ManagerCreateMeetingRoomRequest {
  naziv?: string;
  dodatnaOprema?: string;
}

export class ManagerMeetingRoomResponse {
  id?: number;
  spaceId?: number;
  naziv?: string;
  brojMesta?: number;
  dodatnaOprema?: string;
}

export class ManagerCreateSpaceOpenSpace {
  brojStolova?: number;
}

export class ManagerCreateSpaceRequest {
  naziv?: string;
  grad?: string;
  adresa?: string;
  opis?: string;
  cenaPoSatu?: number;
  pragKazni?: number;
  geografskaSirina?: number;
  geografskaDuzina?: number;
  openSpace?: ManagerCreateSpaceOpenSpace;
}

export class ManagerCreateSpaceResponse {
  id?: number;
  status?: string;
  message?: string;
}

export class ManagerReservationMember {
  id?: number;
  username?: string;
}

export class ManagerReservationItem {
  id?: number;
  member?: ManagerReservationMember;
  spaceId?: number;
  type?: string;
  resourceName?: string;
  from?: string;
  to?: string;
  status?: string;
  canConfirmOrNoShow?: boolean;
}

export class ManagerReservationsResponse {
  content?: ManagerReservationItem[];
}

export class ManagerReservationStatusResponse {
  id?: number;
  status?: string;
  penaltyCreated?: boolean;
}

export class ManagerCalendarEvent {
  reservationId?: number;
  title?: string;
  from?: string;
  to?: string;
  status?: string;
}

export class ManagerCalendarResponse {
  events?: ManagerCalendarEvent[];
}

export class ManagerMoveReservationRequest {
  from?: string;
  to?: string;
}

export class ManagerMoveReservationResponse {
  id?: number;
  from?: string;
  to?: string;
  status?: string;
}

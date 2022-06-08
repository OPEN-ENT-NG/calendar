export class Bookings {
    all: Array<Booking|SavedBooking>
};

export type SavedBooking = {
    id: number;
    quantity: number;
    status: number;
    start_date: string;
    end_date: string;
};

export type Booking = {
    id: number;
    resourceId: number;
    owner: string;
    booking_reason: string;
    created: string;
    modified: string;
    start_date: string;
    end_date: string;
    status: number;
    moderatorId: string;
    refusalReason: string;
    parentBookingId: number;
    days: number;
    periodicity: number;
    occurrences: number;
    is_periodic: boolean;
    quantity: number;
    ownerName: string;
    moderatorName: string;
    resource: any;
    type: any;
}
export class Bookings {
    all: Array<any|SavedBooking>
};

export type SavedBooking = {
    id: number;
    quantity: number;
    status: number;
    start_date: string;
    end_date: string;
};
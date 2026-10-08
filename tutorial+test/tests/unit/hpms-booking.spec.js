import {beforeAll, describe, expect, test} from '@jest/globals';
import {bookRepo, customerRepo, initSampleDB} from '@/sample_db'
import axios from "axios";

describe('Booking tests', () => {
    let last_count;
    const filter = {}

    const baseUrl = "http://localhost:7007/api/hpms/bookings/";

    beforeAll(async () => {
    });


    test('searchBooking', async () => {
        const url = `${baseUrl}`
        const response = await axios.post(url, {
            arrivalDate: '2025-02-24',
        });
        console.log(response.data);
    });

    test('getStayHistory', async () => {
        const url = `${baseUrl}` + 3307 + "/stayHistory"
        const response = await axios.get(url);
        console.log(response.data);
    });

});


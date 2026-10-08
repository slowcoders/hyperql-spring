import {beforeAll, describe, expect, test} from '@jest/globals';
import {bookRepo, customerRepo, initSampleDB} from '@/sample_db'
import axios from "axios";

describe('Block tests', () => {
    let last_count;
    const filter = {}

    const baseUrl = "http://localhost:7007/api/hpms/blocks/";

    beforeAll(async () => {
    });


    test('searchBlock', async () => {
        const url = `${baseUrl}`
        const response = await axios.post(url, {
            startDate: '2025-02-08',
            endDate: '2025-02-10',
        });
        console.log(response.data);
    });

    test('ViewMapper test', async () => {
        const url = `${baseUrl}15960/details`
        const response = await axios.get(url);
        console.log(response.data);

        // expect(customers.length).toBe(1)
    });

    test('Joined ViewMapper test', async () => {
        const url = `${baseUrl}15960`
        const response = await axios.get(url);
        console.log(response.data);

        // expect(customers.length).toBe(1)
    });

});


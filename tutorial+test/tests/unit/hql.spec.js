import {beforeAll, describe, expect, test} from '@jest/globals';
import { customerRepo, initSampleDB } from '@/sample_db'
import axios from "axios";

describe('Hyper Query operations', () => {

    // initSampleDB();

    const baseUrl = "http://localhost:7007/api/hq"

    beforeAll(async () => {
    })

    test('Collection property mapping', async () => {
        const filter = {
            "name": '강'
        }
        const url = `${baseUrl}/authors`
        const response = await axios.post(url, filter);
        console.log(response.data);

        // expect(customers.length).toBe(1)
    });

    test('Mapper Test', async () => {
        const filter = {
            "startDate": '1970-01-01',
            "endDate": '3070-01-01'
        }
        const url = `${baseUrl}/books/sales`
        const response = await axios.get(url);
        console.log(response.data);

        // expect(customers.length).toBe(1)
    });

    test('Joined Mapper Test', async () => {
        const filter = {
            "startDate": '1970-01-01',
            "endDate": '3070-01-01'
        }
        const url = `${baseUrl}/books/3003`
        const response = await axios.get(url);
        console.log(response.data);

        // expect(customers.length).toBe(1)
    });
});


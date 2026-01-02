import {beforeAll, describe, expect, test} from '@jest/globals';
import { customerRepo, initSampleDB } from '@/sample_db'
import axios from "axios";

describe('Hyper Query operations', () => {

    initSampleDB();

    const baseUrl = "http://localhost:7007/api/hq/authors"

    beforeAll(async () => {
    })

    test('Collection property mapping', async () => {
        const filter = {
            "name": '강'
        }
        const url = `${baseUrl}/`
        const response = await axios.post(url, filter);
        console.log(response.data);

        // expect(customers.length).toBe(1)
    });
});


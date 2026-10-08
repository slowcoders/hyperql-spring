import {beforeAll, describe, expect, test} from '@jest/globals';
import { customerRepo, initSampleDB } from '@/sample_db'
import axios from "axios";

describe('Hyper Query operations', () => {

    // initSampleDB();

    const baseUrl = "http://localhost:7007/api/hq"

    beforeAll(async () => {
        await initSampleDB();
    })

    test('Insert Book', async () => {
        const url = `${baseUrl}/books/`
        const response = await axios.post(url, {
            id: 3103,
            title: "Test book",
            price: 10000,
            authorId: 2,
            publisherId: 5001
        });
        console.log(response.data);
    });

    test('Update Book', async () => {
        const url = `${baseUrl}/books/`
        const response = await axios.put(url, {
            id: 3003,
            title: "Test book",
            price: 30000,
            authorId: 2,
            publisherId: 5001
        });
        console.log(response.data);
    });

    test('Delete Book', async () => {
        const url = `${baseUrl}/books/3003`
        const response = await axios.delete(url);
        console.log(response.data);
    });

    test('Patch Book', async () => {
        const url = `${baseUrl}/books/updateSalesPricePercent`
        const response = await axios.patch(url, {
            id: 3003,
            percent: 70
        });
        console.log(response.data);
    });


    test('Collection property mapping', async () => {
        const filter = {
            "name": '강'
        }
        const url = `${baseUrl}/authors/`
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
        const response = await axios.post(url, filter);
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

    test('Cascaded Update Book order Test', async () => {

        const orders = (await axios.get(`${baseUrl}/orders/book/3003`))?.data
        console.log(orders);

        for (const order of orders) {
            order.orderDate = '2025-02-24';
        }
        orders.pop();
        orders.push({
            "bookId": 3003,
            "orderDate": '2025-02-24',
            "customerId": 1005,
        })
        const url = `${baseUrl}/orders/book/3003`
        const response = await axios.post(url, orders);
        console.log(response.data);

        // expect(customers.length).toBe(1)
    });

    test('Nested Cascaded Update Book order Test', async () => {

        const book = (await axios.get(`${baseUrl}/books/3003`))?.data
        console.log(book);

        for (const order of book.orders) {
            order.orderDate = '2025-02-24';
        }
        book.orders.pop();
        book.orders.push({
            "bookId": 3003,
            "orderDate": '2025-02-24',
            "customerId": 1002,
        })
        const url = `${baseUrl}/books/3003`
        const response = await axios.patch(url, book);
        console.log(response.data);
    });

    test('Dynamic Filter Test', async () => {

        const authors = (await axios.post(`${baseUrl}/authors/`,
            {
                name: '한',
                p1: "A",
                p2: "B",
            }))?.data
        console.log(authors);

    });
});

